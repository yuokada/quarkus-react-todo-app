---
applyTo: "src/test/java/**/*.java,README.md,docs/**/*.md,.github/workflows/**/*.yml,.github/workflows/**/*.yaml"
---

テスト、ドキュメント、CI 関連ファイルを編集する場合は、以下を守ってください。

- テストは変更対象の API や振る舞いに近い場所へ追加し、既存の Quarkus JUnit5 + RestAssured パターンを優先する。
- Redis / DynamoDB の切り替えや DevServices 前提の挙動を変える場合は、設定条件と期待結果を明確にする。
- README や `docs/` の記述は実際のコマンド、設定キー、ビルド手順と一致させる。
- GitHub Actions では既存の Maven 中心のワークフローを前提にし、ローカル環境の絶対パスを含めない。
- ユーザー影響のある変更では、検証手順や必要なセットアップ差分を同じ変更で更新する。
