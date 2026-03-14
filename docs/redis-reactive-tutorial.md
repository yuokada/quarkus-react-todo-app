# Quarkus Redis 非同期 API チュートリアル

このドキュメントは、このリポジトリで使っている Quarkus Redis の非同期 API を題材に、`ReactiveRedisDataSource` と `Uni` の基本的な使い方をまとめたものです。

対象コード:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/src/main/java/io/github/yuokada/practice/TodoAsyncService.java)
- [`TodoAsyncResourceImpl.java`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/src/main/java/io/github/yuokada/practice/TodoAsyncResourceImpl.java)
- [`TodoAsyncResource.java`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/src/main/java/io/github/yuokada/practice/TodoAsyncResource.java)
- [`TodoTask.java`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/src/main/java/io/github/yuokada/practice/TodoTask.java)

## 1. まず全体像

このリポジトリの非同期 API は 2 層に分かれています。

- Service 層
  - Redis へのアクセスを担当
  - `ReactiveRedisDataSource` を使う
  - 戻り値は基本的に `Uni<T>`
- Resource 層
  - HTTP レスポンスへの変換を担当
  - `Uni<T>` を `CompletionStage<Response>` へ変換する

この分離が重要です。

理由:
- Redis の reactive API は Mutiny (`Uni`) と相性が良い
- HTTP 層では JAX-RS の契約に合わせて `CompletionStage<Response>` を返せる
- サービス層で早く `CompletionStage` に落とさない方が、処理の合成とエラー伝播が素直になる

補足:
- `Uni` と `CompletionStage` の使い分けルールは [`docs/reactive-return-types.md`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/docs/reactive-return-types.md) にまとめています

## 2. ReactiveRedisDataSource の基本

[`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/quarkus-react-todo-app/src/main/java/io/github/yuokada/practice/TodoAsyncService.java) では、コンストラクタで `ReactiveRedisDataSource` を受け取り、用途ごとの command object を取り出しています。

```java
public TodoAsyncService(ReactiveRedisDataSource ds, Logger logger) {
    this.todoCommands = ds.value(TodoTask.class);
    this.keyCommands = ds.key();
    this.logger = logger;
}
```

ポイント:
- `ds.value(TodoTask.class)`
  - 値型が `TodoTask` の key-value 操作を行う
- `ds.key()`
  - key の列挙や削除を行う

この時点ではまだ Redis にアクセスしていません。実際のアクセスは `get` / `set` / `del` / `keys` を呼んだときに `Uni` として表現されます。

## 3. 単一データの取得

最も単純なパターンは、キー 1 つを非同期に取得するケースです。

対象:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncService.java)
  - `asyncTask`

```java
public Uni<TodoTask> asyncTask(String id) {
    return todoCommands.get(id).onFailure().retry().atMost(5);
}
```

ここでやっていること:
- `todoCommands.get(id)`
  - Redis から値を読む
  - 戻り値は `Uni<TodoTask>`
- `onFailure().retry().atMost(5)`
  - 失敗時に最大 5 回までリトライする

使いどころ:
- 一時的な接続失敗を吸収したいとき

注意:
- すべての失敗にリトライしてよいとは限らない
- 入力値不正や永続的な障害では、無条件リトライは逆効果になることがある

## 4. 一覧取得

一覧取得は、Redis では「キー一覧を取る」処理と「各キーの値を取る」処理を組み合わせます。

対象:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncService.java)
  - `tasks`

```java
public Uni<List<TodoTask>> tasks() {
    return keyCommands
            .keys("*")
            .onItem()
            .transform(keys -> keys.stream()
                    .filter(k -> k.matches("^\\d+$"))
                    .collect(Collectors.toList()))
            .onItem()
            .transformToMulti(keys -> Multi.createFrom().items(keys.stream()))
            .onItem()
            .transformToUniAndConcatenate(todoCommands::get)
            .collect()
            .asList()
            .onItem()
            .transform(tasks -> tasks.stream()
                    .sorted(Comparator.comparingInt(TodoTask::id))
                    .collect(Collectors.toList()));
}
```

流れを分解するとこうなります。

### 4-1. キー一覧を取得する

```java
keyCommands.keys("*")
```

戻り値:
- `Uni<List<String>>`

### 4-2. 対象キーを絞る

```java
.onItem().transform(keys -> ...)
```

このリポジトリでは数値 ID のキーだけを Todo として扱っているため、正規表現で絞っています。

### 4-3. List を Multi に変換する

```java
.transformToMulti(keys -> Multi.createFrom().items(keys.stream()))
```

なぜ必要か:
- `List<String>` の各キーに対して非同期 `get` を 1 件ずつ適用したいから

### 4-4. 各キーの値を非同期取得する

```java
.transformToUniAndConcatenate(todoCommands::get)
```

ここが重要です。

- `transformToUniAndConcatenate`
  - 順序を保ちやすい
  - 1 件ずつ連結して処理する
- `transformToUniAndMerge`
  - 並列性は上がるが、返却順が不安定になりやすい

このリポジトリでは API 応答を安定させるために `Concatenate` を使い、最後に ID 昇順で明示的にソートしています。

### 4-5. List に戻す

```java
.collect().asList()
```

`Multi<TodoTask>` を `Uni<List<TodoTask>>` に戻しています。

## 5. 作成処理

Redis に値を書き込んで、そのまま作成済みオブジェクトを返す例です。

対象:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncService.java)
  - `create`

```java
public Uni<TodoTask> create(TodoTask task) {
    return nextId().onItem()
            .transform(nextId -> new TodoTask(nextId, task.title(), task.isCompleted()))
            .onItem()
            .transformToUni(newTask ->
                    todoCommands.set(newTask.id().toString(), newTask)
                            .replaceWith(newTask));
}
```

ポイント:
- `nextId()` は `Uni<Integer>`
- `transform(...)`
  - ID をもとに `TodoTask` を組み立てる
- `transformToUni(...)`
  - 書き込み処理そのものが `Uni` を返すため、flat map 的に接続する
- `replaceWith(newTask)`
  - `set` の戻り値ではなく、作成済みの `TodoTask` を返す

重要:
- 書き込んだあとに同じキーを再度 `get` しなくても、通常は `newTask` を返せば十分
- 不要な read-after-write は Redis 往復を増やすので避ける

## 6. 更新処理

更新では「存在確認」と「書き込み」をつなげます。

対象:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncService.java)
  - `updateAsync`

```java
public Uni<TodoTask> updateAsync(Integer id, TodoTask task) {
    return asyncTask(id.toString())
            .onItem()
            .transformToUni(currentTask -> {
                if (currentTask == null) {
                    return Uni.createFrom().nullItem();
                }
                TodoTask updatedTask = new TodoTask(id, task.title(), task.isCompleted());
                return todoCommands.set(id.toString(), updatedTask)
                        .onItem()
                        .transform(v -> updatedTask);
            });
}
```

ポイント:
- まず既存データを読む
- 存在しないなら `nullItem()` を返す
- 存在するなら `set` して更新済みオブジェクトを返す

設計上の意味:
- Service 層では HTTP ステータスを知らない
- 「見つからなかった」という事実だけを `null` で表現し、HTTP 404 への変換は Resource 層に任せる

## 7. 削除処理

Redis の `DEL` は削除件数を返します。

対象:
- [`TodoAsyncService.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncService.java)
  - `delete`

```java
public Uni<Boolean> delete(Integer id) {
    return keyCommands.del(id.toString()).map(l -> l == 1L);
}
```

ポイント:
- `del` の戻り値は件数
- `map(l -> l == 1L)`
  - 「1 件削除できたか」を boolean に変換している

この形だと Resource 層で:
- `true` なら 204
- `false` なら 404

と素直に変換できます。

## 8. Resource 層で HTTP に変換する

Service 層の `Uni<T>` を HTTP レスポンスへ変えるのが Resource 層です。

対象:
- [`TodoAsyncResourceImpl.java`](/Users/yuokada/ghq/github.com/yuokada/practice/TodoAsyncResourceImpl.java)

### 8-1. 一覧取得

```java
public CompletionStage<Response> keys() {
    return service.tasks()
            .onItem()
            .transform(list -> Response.ok(list).build())
            .subscribeAsCompletionStage();
}
```

ここで初めて `subscribeAsCompletionStage()` を呼びます。

意図:
- Service 層は `Uni`
- Resource 層の外向き契約は `CompletionStage<Response>`

### 8-2. 詳細取得

```java
public CompletionStage<Response> detail(Integer id) {
    return service.asyncTask(id.toString())
            .onItem()
            .transform(todoTask -> {
                if (todoTask == null) {
                    return Response.status(Response.Status.NOT_FOUND).build();
                }
                return Response.ok(todoTask).build();
            })
            .subscribeAsCompletionStage();
}
```

ポイント:
- `null` を 404 へ変換しているのは Resource 層

### 8-3. POST / PUT / DELETE

`post` / `put` / `delete` も同じ考え方です。

- Service 層で Redis 操作を完了させる
- Resource 層で HTTP ステータスへ変換する
- 最後に `subscribeAsCompletionStage()`

## 9. よくある落とし穴

### 9-1. Service 層で `CompletionStage` に変換してしまう

避けたい例:

```java
todoCommands.get(id).subscribeAsCompletionStage()
```

問題:
- 以後の処理が Mutiny 演算子で書きづらくなる
- `Uni` と `CompletionStage` が同じ層で混ざる

### 9-2. `.get()` で待ってしまう

避けたい例:

```java
subscribeAsCompletionStage().toCompletableFuture().get()
```

問題:
- ブロッキングになる
- reactive 設計を壊す

### 9-3. 一覧取得で merge を使って順序が壊れる

`transformToUniAndMerge` は便利ですが、返却順が不安定になりやすいです。

対策:
- `transformToUniAndConcatenate` を使う
- もしくは最後に明示ソートする

このリポジトリでは両方を使って順序を安定化しています。

## 10. 実装時のチェックリスト

- Redis reactive API を使う service メソッドは `Uni<T>` を返しているか
- `subscribeAsCompletionStage()` は Resource 層だけで呼んでいるか
- `.get()` で待っていないか
- 一覧取得の順序が API として安定しているか
- `set` 後に不要な `get` をしていないか
- `null` や boolean を HTTP ステータスへ変換する責務が Resource 層にあるか

## 11. 最小パターン

最後に、このリポジトリの考え方を最小化すると次の 2 行に集約されます。

### Service

```java
Uni<TodoTask> task = todoCommands.get(id);
```

### Resource

```java
return task.onItem().transform(Response::ok).subscribeAsCompletionStage();
```

まずはこの分離を守ると、Redis 非同期 API のコードはかなり読みやすくなります。
