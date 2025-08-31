# Repository Guidelines

本ドキュメントは本リポジトリで作業するコントリビュータ向けのガイドです。簡潔・実用的な手順をまとめています。

## プロジェクト構成
- Backend(Java/Quarkus): `src/main/java/io/github/yuokada/practice/`
- Resources/設定: `src/main/resources/`（`application.properties`, Redis ロードスクリプト, OpenAPI 出力先指定）
- Frontend(React/Vite): `src/main/webui/`
- テスト: `src/test/java/...`
- CI: `.github/workflows/maven.yml`
- 主要ファイル: `pom.xml`, `README.md`

## ビルド・テスト・開発
- 開発起動(ホットリロード/Quinoa連携): `./mvnw compile quarkus:dev`
- ビルド(JAR): `./mvnw package`
- ネイティブ: `./mvnw package -Pnative`
- テスト: `./mvnw test`
- Frontend 単体: `cd src/main/webui && npm run dev|build|preview`
補足: Dev時は Vite(既定 5173) を Quarkus がプロキシ。OpenAPI は `openapi-definition/` に出力。

## コーディング規約・命名
- Java: 4スペースインデント、`record` による DTO（例: `TodoTask`）。パッケージは `io.github.yuokada.practice`。
- REST: ベースは `/api/todos`, `/api/async/todos`, `/increments`。`@Produces(MediaType.APPLICATION_JSON)` を基本。
- Lint/Format: Frontend は Biome（`npm run biome:lint`, `npm run biome:format`）。YAML/JSON は pre-commit（`yamlfmt`, `check-yaml`, `check-json`）。

## テスト方針
- フレームワーク: Quarkus JUnit5 + RestAssured（例: `TodoAsyncResourceTest`）。
- 実行: `./mvnw test`。必要に応じ Redis DevServices かローカル `redis://localhost:6379/0` を選択。
- 命名: テストクラスは `*Test.java`。エンドツーエンドに近い API テストを推奨。

## コミット / PR
- コミット: 変更の意図が伝わる短句 + 要点（例: "fix: async 更新404の扱いを修正"）。
- PR 要件: 概要・背景、変更点、動作確認手順（コマンド/スクリーンショット）、関連Issueのリンク、影響範囲（API/設定）。

## セキュリティ / 設定のヒント
- Redis: DevServicesを使う場合は `quarkus.redis.hosts` をコメントアウト。ローカル使用時は `%dev.quarkus.redis.hosts=redis://localhost:6379/0`。
- Node/JDK: `.tool-versions` に準拠。CI も同設定を利用。
- コンテナ: Jib によりイメージ化可能（`quarkus-container-image-jib`）。

