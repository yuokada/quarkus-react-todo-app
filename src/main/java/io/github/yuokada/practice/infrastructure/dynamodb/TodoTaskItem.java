package io.github.yuokada.practice.infrastructure.dynamodb;

import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticAttributeTags;
import software.amazon.awssdk.enhanced.dynamodb.mapper.StaticTableSchema;

import io.github.yuokada.practice.domain.model.TodoTask;

public class TodoTaskItem {

    public static final TableSchema<TodoTaskItem> TABLE_SCHEMA =
            StaticTableSchema.builder(TodoTaskItem.class)
                    .newItemSupplier(TodoTaskItem::new)
                    .addAttribute(
                            Integer.class,
                            a ->
                                    a.name("id")
                                            .getter(TodoTaskItem::getId)
                                            .setter(TodoTaskItem::setId)
                                            .tags(StaticAttributeTags.primaryPartitionKey()))
                    .addAttribute(
                            String.class,
                            a ->
                                    a.name("title")
                                            .getter(TodoTaskItem::getTitle)
                                            .setter(TodoTaskItem::setTitle))
                    .addAttribute(
                            Boolean.class,
                            a ->
                                    a.name("completed")
                                            .getter(TodoTaskItem::getCompleted)
                                            .setter(TodoTaskItem::setCompleted))
                    .addAttribute(
                            Long.class,
                            a ->
                                    a.name("createdAt")
                                            .getter(TodoTaskItem::getCreatedAt)
                                            .setter(TodoTaskItem::setCreatedAt))
                    .addAttribute(
                            Long.class,
                            a ->
                                    a.name("updatedAt")
                                            .getter(TodoTaskItem::getUpdatedAt)
                                            .setter(TodoTaskItem::setUpdatedAt))
                    .build();

    private Integer id;
    private String title;
    private Boolean completed;
    private Long createdAt;
    private Long updatedAt;

    public TodoTaskItem() {}

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
