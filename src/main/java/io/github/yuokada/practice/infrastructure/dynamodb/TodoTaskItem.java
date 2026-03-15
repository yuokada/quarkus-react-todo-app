package io.github.yuokada.practice.infrastructure.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import io.github.yuokada.practice.domain.model.TodoTask;

@DynamoDbBean
public class TodoTaskItem {

    private Integer id;
    private String title;
    private Boolean completed;
    private Long createdAt;
    private Long updatedAt;

    public TodoTaskItem() {}

    @DynamoDbPartitionKey
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public static TodoTaskItem from(TodoTask task) {
        TodoTaskItem item = new TodoTaskItem();
        item.setId(task.id());
        item.setTitle(task.title());
        item.setCompleted(task.isCompleted());
        item.setCreatedAt(task.createdAt());
        item.setUpdatedAt(task.updatedAt());
        return item;
    }

    public TodoTask toTask() {
        return new TodoTask(id, title, completed, createdAt, updatedAt);
    }
}
