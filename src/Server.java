import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static final int PORT = 5000;

    // ==========================================
    // CONNECTED WORKERS
    // ==========================================

    private static final Map<String, Socket> workers =
            new ConcurrentHashMap<>();

    private static final Map<String, PrintWriter> workerWriters =
            new ConcurrentHashMap<>();

    private static final Map<String, String> workerStatus =
            new ConcurrentHashMap<>();

    // Maintains worker order for Round-Robin scheduling
    private static final List<String> workerList =
            new ArrayList<>();

    // Points to the next worker to check
    private static int nextWorkerIndex = 0;

    // ==========================================
    // TASK QUEUE
    // ==========================================

    private static final BlockingQueue<Task> taskQueue =
            new LinkedBlockingQueue<>();

    // ==========================================
    // TASK → CLIENT MAPPING
    // ==========================================

    private static final Map<String, PrintWriter> taskClients =
            new ConcurrentHashMap<>();

    // ==========================================
    // TASK ID COUNTER
    // ==========================================

    private static final AtomicInteger taskCounter =
            new AtomicInteger(0);

    // ==========================================
    // MAIN
    // ==========================================

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("      DISTRIBUTED TASK SCHEDULER");
        System.out.println("======================================");

        try {

            ServerSocket serverSocket =
                    new ServerSocket(PORT);

            System.out.println(
                    "Server started successfully."
            );

            System.out.println(
                    "Server is listening on port: "
                            + PORT
            );

            System.out.println(
                    "Waiting for workers or clients..."
            );

            while (true) {

                Socket socket =
                        serverSocket.accept();

                Thread connectionThread =
                        new Thread(
                                () -> handleConnection(socket)
                        );

                connectionThread.start();
            }

        } catch (IOException e) {

            System.out.println(
                    "Server error: "
                            + e.getMessage()
            );
        }
    }

    // ==========================================
    // HANDLE CONNECTION
    // ==========================================

    private static void handleConnection(
            Socket socket) {

        try {

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            PrintWriter writer =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            String firstMessage =
                    reader.readLine();

            if (firstMessage == null) {

                socket.close();
                return;
            }

            // ==========================================
            // WORKER CONNECTION
            // ==========================================

            if (firstMessage.startsWith("WORKER|")) {

                String workerId =
                        firstMessage.substring(7);

                registerWorker(
                        workerId,
                        socket,
                        writer
                );

                String message;

                while (
                        (message = reader.readLine())
                                != null
                ) {

                    if (message.startsWith("RESULT|")) {

                        handleResult(
                                workerId,
                                message
                        );
                    }
                }

                removeWorker(workerId);
            }

            // ==========================================
            // CLIENT CONNECTION
            // ==========================================

            else if (firstMessage.equals("CLIENT")) {

                System.out.println();
                System.out.println(
                        "Client connected."
                );

                writer.println("CONNECTED");

                String message;

                while (
                        (message = reader.readLine())
                                != null
                ) {

                    if (message.startsWith("SUBMIT|")) {

                        submitTask(
                                message,
                                writer
                        );
                    }
                }

                System.out.println(
                        "Client disconnected."
                );
            }

            else {

                System.out.println(
                        "Unknown connection type."
                );

                socket.close();
            }

        } catch (IOException e) {

            System.out.println(
                    "Connection error: "
                            + e.getMessage()
            );
        }
    }

    // ==========================================
    // REGISTER WORKER
    // ==========================================

    private static synchronized void registerWorker(
            String workerId,
            Socket socket,
            PrintWriter writer) {

        workers.put(
                workerId,
                socket
        );

        workerWriters.put(
                workerId,
                writer
        );

        workerStatus.put(
                workerId,
                "AVAILABLE"
        );

        // Add worker to scheduling list
        if (!workerList.contains(workerId)) {

            workerList.add(workerId);
        }

        System.out.println();
        System.out.println(
                "Worker registered: "
                        + workerId
        );

        System.out.println(
                "Status: AVAILABLE"
        );

        printWorkerStatus();

        scheduleTasks();
    }

    // ==========================================
    // CLIENT SUBMITS TASK
    // ==========================================

    private static synchronized void submitTask(
            String message,
            PrintWriter clientWriter) {

        try {

            String[] parts =
                    message.split("\\|");

            if (parts.length < 3) {

                clientWriter.println(
                        "ERROR|Invalid task format"
                );

                return;
            }

            String operation =
                    parts[1];

            int input =
                    Integer.parseInt(parts[2]);

            // Validate operation
            if (
                    !operation.equals("FACTORIAL")
                            && !operation.equals("FIBONACCI")
                            && !operation.equals("PRIME_CHECK")
                            && !operation.equals("SUM")
                            && !operation.equals("SQUARE")
            ) {

                clientWriter.println(
                        "ERROR|Unsupported operation"
                );

                return;
            }

            int number =
                    taskCounter.incrementAndGet();

            String taskId =
                    "T" + number;

            Task task =
                    new Task(
                            taskId,
                            operation,
                            input
                    );

            taskQueue.add(task);

            // Remember the client
            taskClients.put(
                    taskId,
                    clientWriter
            );

            System.out.println();
            System.out.println(
                    "======================================"
            );

            System.out.println(
                    "NEW TASK RECEIVED"
            );

            System.out.println(
                    "Task ID: "
                            + taskId
            );

            System.out.println(
                    "Operation: "
                            + operation
            );

            System.out.println(
                    "Input: "
                            + input
            );

            System.out.println(
                    "Task added to queue."
            );

            System.out.println(
                    "======================================"
            );

            clientWriter.println(
                    "ACCEPTED|"
                            + taskId
            );

            scheduleTasks();

        } catch (NumberFormatException e) {

            clientWriter.println(
                    "ERROR|Input must be a number"
            );
        }
    }

    // ==========================================
    // ROUND-ROBIN SCHEDULER
    // ==========================================

    private static synchronized void scheduleTasks() {

        while (!taskQueue.isEmpty()
                && !workerList.isEmpty()) {

            String selectedWorker =
                    findNextAvailableWorker();

            if (selectedWorker == null) {

                // All workers are currently BUSY
                return;
            }

            Task task =
                    taskQueue.poll();

            if (task == null) {
                return;
            }

            sendTask(
                    selectedWorker,
                    task
            );
        }
    }

    // ==========================================
    // FIND NEXT AVAILABLE WORKER
    // ==========================================

    private static String findNextAvailableWorker() {

        int workerCount =
                workerList.size();

        if (workerCount == 0) {
            return null;
        }

        /*
         * Check every worker once,
         * starting from nextWorkerIndex.
         */

        for (int i = 0; i < workerCount; i++) {

            int index =
                    (nextWorkerIndex + i)
                            % workerCount;

            String workerId =
                    workerList.get(index);

            String status =
                    workerStatus.get(workerId);

            if ("AVAILABLE".equals(status)) {

                // Next search starts after this worker
                nextWorkerIndex =
                        (index + 1)
                                % workerCount;

                return workerId;
            }
        }

        return null;
    }

    // ==========================================
    // SEND TASK TO WORKER
    // ==========================================

    private static void sendTask(
            String workerId,
            Task task) {

        PrintWriter writer =
                workerWriters.get(workerId);

        if (writer == null) {

            taskQueue.add(task);
            return;
        }

        task.setStatus("RUNNING");

        workerStatus.put(
                workerId,
                "BUSY"
        );

        String message =
                "TASK|"
                        + task.getTaskId()
                        + "|"
                        + task.getOperation()
                        + "|"
                        + task.getInput();

        writer.println(message);

        System.out.println();
        System.out.println(
                "TASK ASSIGNED"
        );

        System.out.println(
                task.getTaskId()
                        + " → "
                        + workerId
        );

        System.out.println(
                "Operation: "
                        + task.getOperation()
        );

        System.out.println(
                "Worker status: BUSY"
        );
    }

    // ==========================================
    // RECEIVE RESULT FROM WORKER
    // ==========================================

    private static synchronized void handleResult(
            String workerId,
            String message) {

        String[] parts =
                message.split("\\|");

        if (parts.length < 3) {
            return;
        }

        String taskId =
                parts[1];

        String result =
                parts[2];

        System.out.println();
        System.out.println(
                "======================================"
        );

        System.out.println(
                "RESULT RECEIVED"
        );

        System.out.println(
                "Worker: "
                        + workerId
        );

        System.out.println(
                "Task: "
                        + taskId
        );

        System.out.println(
                "Result: "
                        + result
        );

        System.out.println(
                "======================================"
        );

        // Send result to correct client
        PrintWriter clientWriter =
                taskClients.get(taskId);

        if (clientWriter != null) {

            clientWriter.println(
                    "RESULT|"
                            + taskId
                            + "|"
                            + result
                            + "|"
                            + workerId
            );

            System.out.println(
                    "Final result sent to client."
            );

            taskClients.remove(taskId);
        }

        // Worker becomes available
        workerStatus.put(
                workerId,
                "AVAILABLE"
        );

        System.out.println(
                workerId
                        + " → AVAILABLE"
        );

        // Assign next queued task
        scheduleTasks();
    }

    // ==========================================
    // REMOVE WORKER
    // ==========================================

    private static synchronized void removeWorker(
            String workerId) {

        workers.remove(workerId);

        workerWriters.remove(workerId);

        workerStatus.remove(workerId);

        workerList.remove(workerId);

        // Keep index valid
        if (!workerList.isEmpty()) {

            nextWorkerIndex =
                    nextWorkerIndex
                            % workerList.size();

        } else {

            nextWorkerIndex = 0;
        }

        System.out.println();
        System.out.println(
                "Worker disconnected: "
                        + workerId
        );

        printWorkerStatus();

        scheduleTasks();
    }

    // ==========================================
    // DISPLAY WORKER STATUS
    // ==========================================

    private static void printWorkerStatus() {

        System.out.println();
        System.out.println(
                "CURRENT WORKER STATUS"
        );

        System.out.println(
                "---------------------"
        );

        for (
                String workerId :
                workerList
        ) {

            System.out.println(
                    workerId
                            + " → "
                            + workerStatus.get(workerId)
            );
        }

        System.out.println();
    }
}