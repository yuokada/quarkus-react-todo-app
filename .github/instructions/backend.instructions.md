---
applyTo: "src/main/java/**/*.java,src/main/resources/application.properties,pom.xml"
---

このリポジトリのバックエンド関連ファイルを編集する場合は、以下を守ってください。

- レイヤー分割を維持し、`presentation` から `infrastructure` を直接呼び出さない。
- Redis や DynamoDB などの永続化実装は `infrastructure` に閉じ込め、業務ロジックは `application` と `domain` に寄せる。
- REST リソースでは HTTP 入出力の責務に集中し、永続化や変換ロジックを過度に持ち込まない。
- Java 17 と既存の Quarkus コーディングスタイルを前提にし、Spotless の整形規則を崩さない。
- 設定値の追加や変更では、`application.properties` と README の記述に不整合を残さない。
