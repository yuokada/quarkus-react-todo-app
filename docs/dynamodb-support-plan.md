# DynamoDB 対応 実装計画

このドキュメントは、この Todo アプリケーションに DynamoDB サポートを追加するための全体実装計画をまとめたものです。

目的は Redis 実装を即時に廃止することではなく、既存の Clean Architecture 風の構成を維持したまま、Redis と DynamoDB の両方を選択可能にすることです。

## 1. 背景と方針

現在のアプリケーションは以下の層に分かれています。

- `domain.model`
- `domain.repository`
- `application.service`
- `infrastructure.redis`
- `presentation.rest`

DynamoDB 対応では、この構造を崩さずに `infrastructure.dynamodb` を追加します。

基本方針:

- `presentation` 層は DynamoDB を意識しない
- `application` 層は repository interface のみを見る
- DynamoDB 固有の SDK 利用は `infrastructure.dynamodb` に閉じ込める
- Redis 実装と DynamoDB 実装は設定で切り替える

## 2. 対象範囲

今回の DynamoDB 対応で対象にするもの:

- Todo の同期 API
- Todo の非同期 API
- Increment API
- 開発環境とテスト環境
- 設定切り替え
- README / docs 更新

対象外として後回しにしてよいもの:

- Redis から DynamoDB への本番データ移行自動化
- 高度なページング API
- 単一テーブル設計への最適化

## 3. 現状の課題

Redis 前提の実装・設計として、DynamoDB 化で見直しが必要なものがあります。

- `findAll()` が Redis の key 走査に近い発想になっている
- ID 採番が Redis の atomic counter に依存している
- `IncrementRepository.keys()` が DynamoDB と相性が悪い
- 非同期 API は Redis の reactive API に寄った実装になっている

このため、単純な置き換えではなく、repository 契約と実装戦略を整理してから進める必要があります。

## 4. 目標アーキテクチャ

追加後の構成は以下を想定します。

- `domain.repository`
  - `TodoRepository`
  - `TodoAsyncRepository`
  - `IncrementRepository`
- `application.service`
  - 既存サービスを維持
- `infrastructure.redis`
  - 既存の Redis 実装
- `infrastructure.dynamodb`
  - 新規の DynamoDB 実装

追加候補クラス:

- `io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbTodoRepository`
- `io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbTodoAsyncRepository`
- `io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbIncrementRepository`
- `io.github.yuokada.practice.infrastructure.dynamodb.DynamoDbClientProducer`

## 5. テーブル設計

最初の実装では、シンプルな 2 テーブル構成を採用します。

### 5-1. Todo テーブル

| 項目 | 値 |
| --- | --- |
| テーブル名 | `todo_tasks` |
| Partition Key | `id` `Number` |
| 属性 | `title` `String` |
| 属性 | `completed` `Boolean` |
| 属性 | `createdAt` `Number` (Unix ミリ秒) |
| 属性 | `updatedAt` `Number` (Unix ミリ秒) |
| 用途 | Todo の作成、取得、更新、削除 |

### 5-2. Counter テーブル

| 項目 | 値 |
| --- | --- |
| テーブル名 | `app_counters` |
| Partition Key | `counterName` `String` |
| 属性 | `value` `Number` |
| 用途 | Todo ID 採番 |
| 用途 | Increment API の値管理 |

## 6. 一覧取得戦略

Todo 一覧取得には 2 つの案があります。

### 6-1. 初期実装案

- `todo_tasks` に対して `Scan` を使う
- 取得後にアプリケーション側で ID 昇順にソートする

利点:

- 実装が単純
- 既存 API 契約を維持しやすい

欠点:

- 件数増加時にコストが高い

### 6-2. 将来の改善案

- `createdAt` を Sort Key にした GSI (`createdAt-index`) を追加して Query ベースの一覧取得に切り替える
- テーブル作成時から GSI の追加余地を意識したキー設計にしておく

このリポジトリでは、まずは初期実装案で十分です。

## 7. ID 採番方針

Redis の `INCRBY` 相当は、DynamoDB の atomic counter で置き換えます。

実装案:

- `app_counters` の `counterName = todo_id` を更新
- `UpdateItem` で加算
- 戻り値として最新値を受け取る

採番仕様:

- Redis 実装と同様に **2 ずつ増加** する仕様を維持する
- `UpdateItem` で `value` を `+2` して最新値を返す

## 8. Repository 契約の確認ポイント

既存 interface は基本的に再利用できますが、以下は注意が必要です。

### 8-1. `TodoRepository`

維持可能:

- `findById`
- `create`
- `update`
- `delete`
- `findAll`

注意点:

- `findAll` は DynamoDB では内部的に `Scan` になりやすい

### 8-2. `TodoAsyncRepository`

維持可能:

- 戻り値は `Uni<T>` を維持

実装方針:

- AWS SDK v2 async client の `CompletionStage` を `Uni.createFrom().completionStage(...)` で包む
- `findAll` の戻り値は `Uni<List<Todo>>` とする
- DynamoDB の Scan はページネーションを持つため、`LastEvaluatedKey` が存在する間は再帰的に `scanAsync` を呼び出して全ページを結合する
- 件数増加時のページング最適化は将来の改善案に委ねる

### 8-3. `IncrementRepository`

契約定義:

- `keys()` は「Increment API で管理するカウンター名の一覧」を返す
- Todo ID 採番用の内部キー (`todo_id`) は返却対象外とする

DynamoDB 実装方針:

- `app_counters` テーブルを Scan して全レコードを取得
- 内部キーを定数 `INTERNAL_KEYS = Set.of("todo_id")` でフィルタリングして除外
- 将来内部キーが増えた場合も定数に追加するだけで対応できる

## 9. 設定設計

切り替え用のアプリ設定を追加します。

例:

```properties
app.repository.type=redis

app.dynamodb.region=ap-northeast-1
app.dynamodb.endpoint-override=
app.dynamodb.table.todo=todo_tasks
app.dynamodb.table.counter=app_counters
```

ローカル開発例:

```properties
%dev.app.repository.type=dynamodb
%dev.app.dynamodb.region=ap-northeast-1
%dev.app.dynamodb.endpoint-override=http://localhost:8000
```

テスト例:

```properties
%test.app.repository.type=dynamodb
%test.app.dynamodb.region=ap-northeast-1
%test.app.dynamodb.endpoint-override=http://localhost:8000
```

## 10. DI 切り替え方式

Repository 実装は設定で切り替えます。

採用方式: `@LookupIfProperty`

- Redis 実装: `app.repository.type=redis`
- DynamoDB 実装: `app.repository.type=dynamodb`

これにより `application.service` は変更最小で済みます。

注意点:

- `DynamoDbClientProducer` 自体にも `@LookupIfProperty(name = "app.repository.type", stringValue = "dynamodb")` を付与する
- これにより `app.repository.type=redis` の環境では DynamoDB クライアントが一切初期化されず、DynamoDB Local が存在しない環境での起動エラーを防ぐ

## 11. 依存ライブラリ方針

追加候補:

- `software.amazon.awssdk:dynamodb`
- `software.amazon.awssdk:dynamodb-enhanced`
- `software.amazon.awssdk:url-connection-client`

理由:

- sync / async の両クライアントを利用しやすい
- Enhanced Client を使うとマッピング実装を簡潔にできる

## 12. 開発環境

ローカルおよび CI 環境では DynamoDB Local を使います。将来的に実際の AWS DynamoDB へ切り替える際は認証情報管理方針を別途定める。

候補イメージ:

- `amazon/dynamodb-local`

想定する `compose.yaml` 追加内容:

- `dynamodb-local`
- `8000:8000`

初期化方法:

- ローカル開発時は `compose.yaml` の DynamoDB Local を使用
- 初回起動用のテーブル作成スクリプトを `scripts/init-dynamodb.sh` として提供
- README に起動手順を追記

CI 環境:

- GitHub Actions で DynamoDB Local をサービスコンテナとして起動する
- ダミー認証情報 (`AWS_ACCESS_KEY_ID=dummy`, `AWS_SECRET_ACCESS_KEY=dummy`) を使用
- 実際の AWS へのアクセスは後続フェーズで対応する

## 13. テスト戦略

### 13-1. Repository テスト

追加:

- DynamoDB 実装専用テスト
- `@BeforeAll` で AWS SDK を使ってテーブルを作成し、`@AfterAll` で削除する

### 13-2. Resource テスト

既存の API テストを再利用します。

目標:

- Redis 実装でも DynamoDB 実装でも同じ API 契約が通る

### 13-3. テスト用実行環境

候補:

- DynamoDB Local
- LocalStack

推奨:

- まずは DynamoDB Local

理由:

- 構成が単純
- DynamoDB 以外の AWS サービスを必要としない

## 14. フェーズ分割

### Phase 1: 契約整理と設定追加

作業:

- Repository interface の前提整理
- `application.properties` 追加
- AWS SDK 依存追加
- DI 切り替え方式導入

成果物:

- Redis / DynamoDB の切り替え基盤

### Phase 2: 同期 Todo の DynamoDB 実装

作業:

- `DynamoDbTodoRepository` 実装
- `findAll`, `findById`, `create`, `update`, `delete`
- 同期 API テスト

成果物:

- `/api/todos` の DynamoDB 対応

### Phase 3: 非同期 Todo の DynamoDB 実装

作業:

- `DynamoDbTodoAsyncRepository` 実装
- `CompletionStage` から `Uni` への変換
- 非同期 API テスト

成果物:

- `/api/async/todos` の DynamoDB 対応

### Phase 4: Increment の DynamoDB 実装

作業:

- `DynamoDbIncrementRepository` 実装
- counter 管理と `keys()` の初期実装
- Increment API テスト

成果物:

- `/increments` の DynamoDB 対応

### Phase 5: 開発環境とドキュメント整備

作業:

- DynamoDB Local を `compose.yaml` に追加
- README 更新
- 運用ドキュメント追加

成果物:

- 開発者がローカルで DynamoDB バックエンドを再現できる状態

## 15. PR 分割案

実装は以下の単位で PR 分割するのが安全です。

1. 基盤追加
   - 依存追加
   - 設定追加
   - DI 切り替え
2. 同期 Todo 実装
3. 非同期 Todo 実装
4. Increment 実装
5. 開発環境 / docs 整備

## 16. リスク

- `findAll()` の `Scan` が将来的に高コストになる
- `IncrementRepository.keys()` は DynamoDB で不自然
- Redis と DynamoDB の挙動差によりテストが割れる可能性がある
- ID 採番仕様が曖昧なままだと移行時に混乱する

## 17. 優先判断

最初に着手すべきもの:

1. 設定と DI 切り替え基盤
2. 同期 Todo 実装
3. 非同期 Todo 実装

後回しにしてよいもの:

- Increment の `keys()` 最適化
- 一覧取得の Query 最適化
- Redis からのデータ移行自動化

## 18. まとめ

このリポジトリで DynamoDB をサポートすることは十分可能です。ただし、Redis 依存の発想をそのまま移植するのではなく、repository 契約を維持しつつ、DynamoDB に合う形で `infrastructure` を追加するのが正しい進め方です。

実装順としては、まず基盤を作り、次に同期 Todo、次に非同期 Todo、その後 Increment と開発環境を整備する流れが最も安全です。
