
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Worker {

    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 5000;

    private final String workerId;

    public Worker(String workerId) {
        this.workerId = workerId.replace("_", " ");
    }

    public void start() {
        System.out.println("==================================");
        System.out.println("  DISTRIBUTED TASK SCHEDULER");
        System.out.println("  Worker: " + workerId);
        System.out.println("==================================");

        try (
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            PrintWriter writer = new PrintWriter(
                socket.getOutputStream(), true
            )
        ) {
            // Register the complete worker name as one value.
            writer.println("WORKER|" + workerId);

            System.out.println("Connected to scheduler server.");
            System.out.println("Worker ID: " + workerId);
            System.out.println("Status: AVAILABLE");
            System.out.println("Waiting for tasks...\n");

            String message;

            while ((message = reader.readLine()) != null) {
                String[] taskParts = message.split("\\|", 4);

                if (taskParts.length != 4 ||
                    !taskParts[0].equals("TASK")) {
                    System.out.println("Received message: " + message);
                    continue;
                }

                String taskId = taskParts[1];
                String operation = taskParts[2];
                String input = taskParts[3];

                System.out.println("----------------------------------");
                System.out.println("Worker    : " + workerId);
                System.out.println("Task ID   : " + taskId);
                System.out.println("Operation : " + operation);
                System.out.println("Input     : " + input);
                System.out.println("Status    : PROCESSING");

                String result;

                try {
                    long number = Long.parseLong(input.trim());

                    // Simulate processing time.
                    Thread.sleep(3000);

                    switch (operation.toUpperCase()) {
                        case "FACTORIAL":
                            result = factorial(number);
                            break;

                        case "FIBONACCI":
                            result = fibonacci(number);
                            break;

                        case "PRIME_CHECK":
                            result = isPrime(number)
                                ? number + " is a prime number."
                                : number + " is not a prime number.";
                            break;

                        case "SUM":
                            result = sumToNumber(number);
                            break;

                        case "SQUARE":
                            result = square(number);
                            break;

                        default:
                            result = "ERROR: Unknown operation: " + operation;
                    }

                } catch (NumberFormatException e) {
                    result = "ERROR: Invalid numeric input.";
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    result = "ERROR: Task processing interrupted.";
                } catch (ArithmeticException e) {
                    result = "ERROR: Arithmetic overflow or invalid input.";
                }

                System.out.println("Result    : " + result);
                System.out.println("Status    : COMPLETED");
                System.out.println("----------------------------------\n");

                writer.println("RESULT|" + taskId + "|" + result);
            }

        } catch (Exception e) {
            System.out.println(
                "Worker " + workerId + " disconnected: " + e.getMessage()
            );
            System.out.println(
                "Make sure the server is running on port " + SERVER_PORT + "."
            );
        }
    }

    private String factorial(long n) {
        if (n < 0) {
            return "ERROR: Factorial requires a non-negative number.";
        }

        if (n > 20) {
            return "ERROR: Factorial supports numbers from 0 to 20.";
        }

        long result = 1;

        for (long i = 2; i <= n; i++) {
            result = Math.multiplyExact(result, i);
        }

        return "Factorial of " + n + " = " + result;
    }

    private String fibonacci(long n) {
        if (n < 0) {
            return "ERROR: Fibonacci requires a non-negative number.";
        }

        if (n > 92) {
            return "ERROR: Fibonacci supports numbers from 0 to 92.";
        }

        long a = 0;
        long b = 1;

        for (long i = 0; i < n; i++) {
            long next = a + b;
            a = b;
            b = next;
        }

        return "Fibonacci number at position " + n + " = " + a;
    }

    private boolean isPrime(long n) {
        if (n < 2) {
            return false;
        }

        for (long i = 2; i <= n / i; i++) {
            if (n % i == 0) {
                return false;
            }
        }

        return true;
    }

    private String sumToNumber(long n) {
        if (n < 0) {
            return "ERROR: Sum requires a non-negative number.";
        }

        long result = Math.multiplyExact(n, n + 1) / 2;

        return "Sum from 1 to " + n + " = " + result;
    }

    private String square(long n) {
        long result = Math.multiplyExact(n, n);

        return "Square of " + n + " = " + result;
    }

    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "Worker";
        new Worker(name).start();
    }
}
