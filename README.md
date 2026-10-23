# Distributed Task Scheduler

A small Java socket programming project that distributes calculation tasks across worker processes. A client submits a number and an operation to a central server. The server queues the task, assigns it to an available worker, and sends the result back to the client that submitted it.

## How it works

```text
Client -- SUBMIT --> Server -- TASK --> Worker
Client <-- RESULT -- Server <-- RESULT -- Worker
```

- **Client:** Provides a terminal menu, submits one task at a time, and waits for its result.
- **Server:** Listens on TCP port `5000`, assigns task IDs, stores pending tasks in memory, and schedules available workers in round-robin order.
- **Worker:** Registers with the server, processes assigned tasks, and returns the result. You can start multiple worker processes with different names.

Workers are selected in registration order, starting after the worker chosen last time. Busy workers are skipped. If all workers are busy, tasks wait in the queue until a worker finishes. The server can handle multiple client and worker connections using a thread per connection.

## Requirements

- JDK 17 or later (the project was compiled with Java 17)
- A terminal for the server, one for each worker, and one for each client

No external libraries or build tool are required.

## Build and run

Run these commands from the project root. Compile first:

```sh
javac -d out src/*.java
```

Open separate terminals and start the processes in this order:

```sh
java -cp out Server
```

```sh
java -cp out Worker JOEY
java -cp out Worker MARKY
java -cp out Worker DEE_DEE
```

Run each worker command in its **own terminal**. Underscores in a worker's command-line name become spaces, so `DEE_DEE` registers as `DEE DEE`. You can use other names or run a different number of workers.

Finally, in another terminal:

```sh
java -cp out Client
```

Choose an operation from the menu and enter an integer. For example, choose **5. Square of a Number** and enter `12`. The client receives a task ID, waits while a worker processes it, and then displays `Square of 12 = 144` and the worker's name. Choose **6. Exit** to close the client. Stop the server and workers with `Ctrl+C`.

The client and workers currently connect to the local machine (`127.0.0.1` / `localhost`). To run them on different machines, change `SERVER_ADDRESS` in `Client.java` and `SERVER_HOST` in `Worker.java` to the server's reachable address, then allow TCP port `5000` through the network firewall.

## Supported operations

| Menu option | Protocol name | Example | Result |
| --- | --- | --- | --- |
| Factorial | `FACTORIAL` | `5` | `120` |
| Fibonacci | `FIBONACCI` | `10` | `55` |
| Prime number check | `PRIME_CHECK` | `17` | Prime number |
| Sum from 1 to *n* | `SUM` | `10` | `55` |
| Square | `SQUARE` | `12` | `144` |

Inputs are signed 32-bit integers. Factorial accepts `0` through `20`, Fibonacci accepts `0` through `92`, and sum accepts non-negative values. The worker reports errors for unsupported values or arithmetic overflow.

## Message flow

The programs use persistent TCP sockets and newline-terminated text messages. Fields are separated with `|`.

```text
Worker -> Server: WORKER|JOEY
Client -> Server: CLIENT
Server -> Client: CONNECTED

Client -> Server: SUBMIT|SQUARE|12
Server -> Client: ACCEPTED|T1
Server -> Worker: TASK|T1|SQUARE|12
Worker -> Server: RESULT|T1|Square of 12 = 144
Server -> Client: RESULT|T1|Square of 12 = 144|JOEY
```

`ACCEPTED` means the task was queued; the calculation is complete only when the `RESULT` message arrives. Task IDs let the server route each result to its submitting client. The client waits for each result before offering the next menu choice, although separate clients can submit tasks at the same time. Each worker simulates a three-second processing delay before calculating.

## Project structure

```text
src/
  Client.java   Terminal menu and server connection
  Server.java   Connections, in-memory queue, scheduling, result routing
  Worker.java   Task execution and worker registration
  Task.java     Task ID, operation, input, and status
out/           Compiled Java classes
```

## Current limitations

- Tasks, worker state, and client mappings exist only in server memory. Restarting the server loses pending work.
- A task assigned to a worker is not retried if that worker disconnects before returning a result. The waiting client may wait indefinitely.
- The protocol has no authentication or encryption. It is intended as a local learning project.
- The text protocol does not escape `|` or newline characters, so it is suited to the current fixed operation names and numeric inputs.
