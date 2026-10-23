public class Task {

    private String taskId;
    private String operation;
    private int input;
    private String status;

    public Task(
            String taskId,
            String operation,
            int input) {

        this.taskId = taskId;
        this.operation = operation;
        this.input = input;
        this.status = "PENDING";
    }

    public String getTaskId() {
        return taskId;
    }

    public String getOperation() {
        return operation;
    }

    public int getInput() {
        return input;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {

        return taskId
                + " | "
                + operation
                + " | "
                + input
                + " | "
                + status;
    }
}