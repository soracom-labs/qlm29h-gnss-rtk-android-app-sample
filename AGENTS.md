# AI開発ガイド

このファイルは、本リポジトリをAIまたは新しい開発者が変更する際の最初の入口です。詳細は `docs/` を参照してください。

## 最初に読む文書

1. `docs/REQUIREMENTS.md` — 現行要求と要件ID
2. `docs/ARCHITECTURE.md` — 責務とデータフロー
3. `docs/DESIGN_DECISIONS.md` — 実機試験から得た設計理由
4. `docs/CUSTOMIZATION_GUIDE.md` — 安全な変更箇所
5. `docs/TESTING.md` — 必須確認
6. `docs/TRACEABILITY.md` — 要件と実装・テストの対応

原初仕様は `qlm29h_android_app_spec.md` である。ただし、実機試験後の現行仕様は `docs/REQUIREMENTS.md` を優先する。

## 維持すべき不変条件

- `MAP-01`: QLM29HをSmartphone GNSSより常に優先して追従する。
- `MAP-02`: SPだけの更新でQLM追従カメラを動かさない。
- `MAP-03`: Past session表示中はLiveデータへ自動切替しない。SPはセッションIDまたは移行前データの時間範囲で対応する点だけを重ねる。
- `SP-01`: Smartphone GNSSはNTRIP、SORACOM、Consoleへ連携しない。
- `SP-02`: QLMとSPの軌跡、保存、セグメントを混ぜない。
- `SP-03`: プロセス再生成後のSmartphone GNSSは必ずDisabledから始める。
- `FGS-01`: USB権限なしで`connectedDevice` Foreground Serviceを開始しない。
- `FGS-02`: 位置情報権限なしで`location` Foreground Serviceを開始しない。
- `FGS-03`: 有効なサービス種別がなければForeground Serviceを終了する。
- `SEC-01`: NTRIP認証情報、Authorizationヘッダー、正確な走行座標をログへ出さない。
- `DATA-01`: Map表示上限とDB保存上限を同一視しない。
- `NTRIP-07`: RTCM無受信による段階式再接続は`NtripSessionController`だけが所有し、認証・設定エラーを無限再試行しない。
- `NET-01`: Internet表示はtransport名だけで緑にせず、Androidが検証済みのdefault networkだけをOnlineとする。

## 変更時のルール

- 仕様変更とリファクタリング、依存更新を同じ変更に混ぜない。
- コメントは処理内容ではなく、制約と理由を書く。
- 要件に関係する変更では、該当要件IDをKDoc、テスト名または変更説明へ記載する。
- UI Composableへ`RtkRuntime`を渡さない。MapUiState/MapActionsやSettingsActionsのような機能別状態と操作境界を使用する。
- Settingsの各カードへ`AppState`全体を渡さず、`SettingsUiState`内の担当する機能状態だけを渡す。
- `AppState`を分割する際は一機能ずつ投影・永続化・利用箇所を同時移行し、部分状態のcopyが無関係な状態を保持するテストを追加する。
- `AppState`へ平坦なフィールドを再追加せず、所有する機能別`App*State`へ追加する。ユーザー向け一時エラーは`AppNoticeState`、NMEA解析統計は`AppDiagnosticsState`を使用する。
- NTRIPとSORACOMのCoroutineをRtkRuntimeから直接起動せず、各Session/Schedule Controllerを唯一のJob所有者にする。
- USB受信FlowとAndroid LocationProviderをRtkRuntimeから直接登録せず、UsbSessionControllerとSmartphoneLocationControllerを唯一の所有者にする。
- default network callbackはForeground ServiceやActivityへ重複登録せず、ApplicationスコープのAndroidConnectivityMonitorを唯一の所有者にする。
- 文字列の接続状態は新規追加せず、型付き状態への移行を優先する。
- 型付き接続状態の`label`は表示境界だけで使用し、Runtimeの分岐ではenum/sealed型そのものを比較する。
- DBスキーマを変更する場合はRoom migrationと既存DB保持試験を必須とする。
- 実機ログを解析する場合は座標と認証情報を回答や成果物へ含めない。

## 最低限の検証

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

USB、Foreground Service、権限、MapLibre、Room migrationを変更した場合は `docs/TESTING.md` の該当実機試験も行う。
