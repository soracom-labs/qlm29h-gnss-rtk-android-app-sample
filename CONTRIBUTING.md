# コントリビューションガイド

このリポジトリは、QLM29HBAA-GMとAndroid端末を使ったGNSS/RTK検証のサンプルです。QuectelまたはSORACOMの公式サポート対象アプリではありません。

## Issueを作成する前に

- 最新のGitHub Releaseと`README.md`の既知の制約を確認してください。
- NTRIPのUsername/Password、Authorizationヘッダー、SORACOMの認証情報、正確な住所・座標をIssueへ記載しないでください。
- NMEA/RTCMログと画面キャプチャには移動経路や時刻が含まれます。必要な箇所だけを切り出し、位置と認証情報を除去してください。
- セキュリティ上の問題は公開Issueではなく、`SECURITY.md`の非公開報告手順を使用してください。

## Pull Request

1. `AGENTS.md`と`docs/`の関連要件を確認します。
2. 仕様変更、リファクタリング、依存更新を一つのPRへ混在させません。
3. 変更理由と影響する要件IDをPR本文へ記載します。
4. 次の確認を実行します。

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

USB、Foreground Service、権限、MapLibre、Room migrationを変更した場合は、`docs/TESTING.md`の対応する実機試験も必要です。実走ログ、署名鍵、`keystore.properties`、`local.properties`、APK/AABをコミットしないでください。

## 開発環境

- JDK 17
- Android SDK 35
- Android 14（API level 34）以降を対象

セットアップと設計資料への入口は`README.md`と`AGENTS.md`を参照してください。コントリビューションを受け入れる前に、リポジトリの`LICENSE`と組織のコントリビューション方針を確認してください。
