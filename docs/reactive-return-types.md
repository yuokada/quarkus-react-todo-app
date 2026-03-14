# Uni と CompletionStage の使い分け

このリポジトリでは、Quarkus Redis の reactive API を使う層と、HTTP レスポンスへ変換する層を分けて考える。

## 結論
- Service 層では `Uni` を使う
- JAX-RS Resource 層では `CompletionStage<Response>` を返してよい
- `Uni` から `CompletionStage` への変換は Resource 層の境界でだけ行う

## 使い分けの理由

### Uni を使う場面
- `ReactiveRedisDataSource` や Mutiny ベース API を直接扱うとき
- `map`、`transform`、`transformToUni`、`invoke` など Mutiny の演算子を使って処理を組み立てるとき
- reactive な失敗伝播、順序制御、結合をそのまま表現したいとき

### CompletionStage を使う場面
- JAX-RS のメソッド戻り値として非同期レスポンスを返すとき
- 既存フレームワークや API 契約が `CompletionStage` を要求するとき

## このリポジトリでの推奨境界

### Service 層
- Redis へのアクセス
- reactive な取得、更新、削除
- 一覧取得時の結合やソート
- 戻り値は `Uni<T>`

例:

```java
public Uni<TodoTask> create(TodoTask task) {
    return nextId()
            .onItem()
            .transform(id -> new TodoTask(id, task.title(), task.isCompleted()))
            .onItem()
            .transformToUni(newTask -> todoCommands.set(newTask.id().toString(), newTask).replaceWith(newTask));
}
```

### Resource 層
- HTTP ステータスへの変換
- `Response` の組み立て
- `Uni` を `subscribeAsCompletionStage()` で `CompletionStage<Response>` に変換

例:

```java
public CompletionStage<Response> post(TodoTask task) {
    return service.create(task)
            .onItem()
            .transform(todoTask -> Response.ok(todoTask).build())
            .subscribeAsCompletionStage();
}
```

## 避けるべき使い方

### 1. Service 層で早く CompletionStage に落とす
- `ReactiveRedisDataSource` を使っているのに、途中で `subscribeAsCompletionStage()` して `CompletionStage` 連鎖に切り替える
- 問題:
  - Mutiny の演算子を使いにくくなる
  - 非同期処理の境界が曖昧になる
  - service と resource の責務が混ざる

### 2. reactive API の結果を同期的に待つ
- `subscribeAsCompletionStage().toCompletableFuture().get()`
- 問題:
  - ブロッキングになる
  - reactive API を使う意味が薄れる
  - 実行スレッド設計を壊しやすい

### 3. Uni と CompletionStage を同じ層で混在させる
- `create()` は `CompletionStage`
- `update()` は `Uni`
- `delete()` は `CompletableFuture`
- 問題:
  - 呼び出し側の扱いが揺れる
  - エラー処理やテストが読みにくくなる

## 実装ルール
- Redis reactive API を直接使うメソッドは `Uni` を返す
- `CompletionStage` へ変換するのは Resource 層だけにする
- `CompletableFuture` を独自に使う必要がなければ避ける
- `subscribeAsCompletionStage()` は境界変換として使い、内部実装の標準型にはしない
- 一覧取得で順序が意味を持つ場合は、`merge` 任せにせず明示ソートする

## 判断に迷ったとき
- Redis や Mutiny の演算子をまだ使うか
  - Yes: `Uni`
  - No: Resource 層で `CompletionStage<Response>` へ変換
- 同期的に `get()` したくなったか
  - その設計は見直す
