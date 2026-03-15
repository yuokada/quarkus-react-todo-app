package io.github.yuokada.practice.domain.model;

public record TodoTask(
        Integer id, String title, boolean completed, long createdAt, long updatedAt) {

    public TodoTask {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be null or blank");
        }
    }

    public boolean isCompleted() {
        return this.completed;
    }
}
