package io.github.yuokada.practice;

public record TodoTask(
    Integer id,
    String title,
    boolean completed
){

    public TodoTask {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
    }

    public boolean isCompleted() {
        return this.completed;
    }
}
