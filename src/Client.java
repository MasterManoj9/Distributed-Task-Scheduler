import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {

    private static final String SERVER_ADDRESS = "127.0.0.1";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       DISTRIBUTED TASK CLIENT");
        System.out.println("======================================");

        try {

            Socket socket =
                    new Socket(
                            SERVER_ADDRESS,
                            SERVER_PORT
                    );

            System.out.println(
                    "Connected to Scheduler Server!"
            );

            PrintWriter writer =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            // Identify this connection as a client
            writer.println("CLIENT");

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            // Receive server handshake
            String connectionResponse =
                    reader.readLine();

            if (connectionResponse != null) {

                System.out.println(
                        "Server: "
                                + connectionResponse
                );
            }

            BufferedReader keyboard =
                    new BufferedReader(
                            new InputStreamReader(
                                    System.in
                            )
                    );

            while (true) {

                System.out.println();
                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "       DISTRIBUTED TASK CLIENT"
                );

                System.out.println(
                        "========================================"
                );

                System.out.println(
                        "1. Factorial"
                );

                System.out.println(
                        "2. Fibonacci"
                );

                System.out.println(
                        "3. Prime Number Check"
                );

                System.out.println(
                        "4. Sum of Numbers"
                );

                System.out.println(
                        "5. Square of a Number"
                );

                System.out.println(
                        "6. Exit"
                );

                System.out.print(
                        "\nEnter your choice: "
                );

                String choice =
                        keyboard.readLine();

                // =========================================
                // FACTORIAL
                // =========================================

                if (choice.equals("1")) {

                    submitTask(
                            "FACTORIAL",
                            keyboard,
                            writer,
                            reader
                    );
                }

                // =========================================
                // FIBONACCI
                // =========================================

                else if (choice.equals("2")) {

                    submitTask(
                            "FIBONACCI",
                            keyboard,
                            writer,
                            reader
                    );
                }

                // =========================================
                // PRIME CHECK
                // =========================================

                else if (choice.equals("3")) {

                    submitTask(
                            "PRIME_CHECK",
                            keyboard,
                            writer,
                            reader
                    );
                }

                // =========================================
                // SUM
                // =========================================

                else if (choice.equals("4")) {

                    submitTask(
                            "SUM",
                            keyboard,
                            writer,
                            reader
                    );
                }

                // =========================================
                // SQUARE
                // =========================================

                else if (choice.equals("5")) {

                    submitTask(
                            "SQUARE",
                            keyboard,
                            writer,
                            reader
                    );
                }

                // =========================================
                // EXIT
                // =========================================

                else if (choice.equals("6")) {

                    System.out.println();
                    System.out.println(
                            "Client shutting down..."
                    );

                    break;
                }

                else {

                    System.out.println();
                    System.out.println(
                            "Invalid choice. Please try again."
                    );
                }
            }

            socket.close();

        } catch (IOException e) {

            System.out.println(
                    "Client error: "
                            + e.getMessage()
            );

        } catch (NumberFormatException e) {

            System.out.println();
            System.out.println(
                    "Please enter a valid number."
            );
        }
    }

    // =========================================
    // SUBMIT TASK
    // =========================================

    private static void submitTask(
            String operation,
            BufferedReader keyboard,
            PrintWriter writer,
            BufferedReader reader)
            throws IOException {

        System.out.print(
                "Enter a number: "
        );

        String input =
                keyboard.readLine();

        int number =
                Integer.parseInt(input);

        // Send task to server
        writer.println(
                "SUBMIT|"
                        + operation
                        + "|"
                        + number
        );

        System.out.println();
        System.out.println(
                "Task submitted to server."
        );

        // =========================================
        // RECEIVE ACCEPTANCE
        // =========================================

        String response =
                reader.readLine();

        if (response == null) {

            System.out.println(
                    "Server disconnected."
            );

            return;
        }

        if (response.startsWith("ACCEPTED|")) {

            String[] acceptedParts =
                    response.split("\\|");

            String taskId =
                    acceptedParts[1];

            System.out.println();
            System.out.println(
                    "Task accepted!"
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
                    "Waiting for worker result..."
            );

            // =========================================
            // RECEIVE FINAL RESULT
            // =========================================

            String resultMessage =
                    reader.readLine();

            if (resultMessage != null
                    && resultMessage.startsWith("RESULT|")) {

                String[] resultParts =
                        resultMessage.split("\\|");

                if (resultParts.length >= 4) {

                    String resultTaskId =
                            resultParts[1];

                    String result =
                            resultParts[2];

                    String workerId =
                            resultParts[3];

                    System.out.println();
                    System.out.println(
                            "========================================"
                    );

                    System.out.println(
                            "          TASK COMPLETED"
                    );

                    System.out.println(
                            "========================================"
                    );

                    System.out.println(
                            "Task ID : "
                                    + resultTaskId
                    );

                    System.out.println(
                            "Operation : "
                                    + operation
                    );

                    System.out.println(
                            "Input : "
                                    + number
                    );

                    System.out.println(
                            "Result : "
                                    + result
                    );

                    System.out.println(
                            "Worker : "
                                    + workerId
                    );

                    System.out.println(
                            "========================================"
                    );
                }
            }

        } else {

            System.out.println(
                    "Server response: "
                            + response
            );
        }
    }
}