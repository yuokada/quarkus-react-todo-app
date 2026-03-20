# GitHub Copilot Instructions

このリポジトリで作業する際は、以下の方針を優先してください。

## 基本方針

- ユーザー向けの説明、提案、レビューコメント、PR 要約は日本語で書く。
- 変更は最小限に留め、既存の Quarkus + React + Quinoa + Redis/DynamoDB 構成との整合性を保つ。
- ローカル環境の絶対パス、開発者固有の設定、特定マシン前提の手順を新たに持ち込まない。
- REST API の契約やフロントエンドの挙動を変更する場合は、関連するテストや README も同じ変更で更新する。
- 無関係なリファクタは避け、目的に対して小さく追いやすい差分にする。

## プロジェクト理解

- バックエンドは `src/main/java/io/github/yuokada/practice` 配下にあり、`domain`、`application`、`infrastructure`、`presentation` に責務を分けている。
- フロントエンドは `src/main/webui` 配下の Vite/React アプリで、Quinoa を通して Quarkus と統合されている。
- 設定は主に `src/main/resources/application.properties` と `pom.xml`、フロントエンド依存関係は `src/main/webui/package.json` で管理している。
- テストは Quarkus JUnit5 + RestAssured を中心に `src/test/java` 配下に置かれている。

## 実装と検証

- Java 側の確認は `./mvnw test`、必要に応じて `./mvnw package` や `./mvnw package -Pnative` を使う。
- フロントエンド変更では `cd src/main/webui && npm run biome:check` を優先して使う。
- 変更内容に応じて、実施した確認と未実施の確認を明確に区別して伝える。
