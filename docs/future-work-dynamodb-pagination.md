# Future Work: DynamoDB Scan Pagination

## Overview

Two DynamoDB repository methods currently perform a single `Scan` call without handling the
`LastEvaluatedKey` continuation token. DynamoDB caps a single scan response at 1 MB, so these
methods silently return incomplete results for large tables.

## Affected Methods

### 1. `DynamoDbTodoRepository.findAll()` (sync)

```java
// Current — stops at first page (~1 MB)
public List<TodoTask> findAll() {
    return client.scan(ScanRequest.builder().tableName(todoTable).build())
            .items().stream()
            .map(this::toTodoTask)
            .sorted(Comparator.comparingInt(TodoTask::id))
            .collect(Collectors.toList());
}
```

**Fix:** Use `DynamoDbClient.scanPaginator()` which automatically fetches all pages:

```java
public List<TodoTask> findAll() {
    return client.scanPaginator(ScanRequest.builder().tableName(todoTable).build())
            .items().stream()
            .map(this::toTodoTask)
            .sorted(Comparator.comparingInt(TodoTask::id))
            .collect(Collectors.toList());
}
```

### 2. `DynamoDbIncrementRepository.keys()` (async)

```java
// Current — stops at first page (~1 MB)
public Uni<List<String>> keys() {
    return Uni.createFrom()
            .completionStage(asyncClient.scan(...))
            .map(response -> response.items().stream()...);
}
```

**Fix:** Use recursive accumulation (same pattern as `DynamoDbTodoAsyncRepository.scanAll()`):

```java
public Uni<List<String>> keys() {
    return scanAllCounters(null, new ArrayList<>());
}

private Uni<List<String>> scanAllCounters(
        Map<String, AttributeValue> lastKey, List<String> accumulated) {
    var builder = ScanRequest.builder().tableName(counterTable);
    if (lastKey != null && !lastKey.isEmpty()) {
        builder.exclusiveStartKey(lastKey);
    }
    return Uni.createFrom()
            .completionStage(asyncClient.scan(builder.build()))
            .flatMap(response -> {
                response.items().stream()
                        .map(item -> item.get("counterName").s())
                        .filter(k -> !INTERNAL_KEYS.contains(k))
                        .forEach(accumulated::add);
                if (response.lastEvaluatedKey() != null && !response.lastEvaluatedKey().isEmpty()) {
                    return scanAllCounters(response.lastEvaluatedKey(), accumulated);
                }
                return Uni.createFrom().item(accumulated);
            });
}
```

## Notes

- In practice, the counter table (`app_counters`) will never have enough rows to hit 1 MB,
  so `keys()` is low priority.
- The todo table (`todo_tasks`) is more likely to grow large; `findAll()` should be fixed
  before going to production. The async counterpart (`DynamoDbTodoAsyncRepository.scanAll()`)
  already handles pagination correctly and can serve as a reference.
- A longer-term improvement is to replace `findAll()` + client-side sort with a DynamoDB Query
  on a GSI (see `docs/dynamodb-support-plan.md` for GSI plans).
