# カスタマイズガイド

## 変更前の手順

1. `AGENTS.md`と関連要件IDを確認する。
2. 変更が仕様変更、リファクタリング、依存更新のどれかを明確にする。
3. 既存テストを実行する。
4. 変更対象の不変条件を保護するテストを先に追加する。

## 比較的安全な変更

- 品質色・点径・ズーム値: `ui/map/MapStyleSpec.kt`
- Mapの表示文言・ステータス表示: `ui/map/MapScreen.kt`
- Settingsの表示と確認ダイアログ: `ui/settings/SettingsScreen.kt`
- 数値設定の許容範囲: `settings/SettingsValidator.kt`と対応テスト
- テーマ: `ui/theme/Theme.kt`
- 最下部タブの表示: `MainActivity.AppBottomTabBar`と`drawable/ic_tab_*.xml`。画面切替状態とタップ処理は変更せず、選択面と下線にはテーマの`primary`、文字とアイコンには`onSurfaceVariant`を使用する。
- Settingsの選択チップ: `SettingsScreen.SettingsChoiceChip`。選択時も背景を変えず、テーマの`primary`による2dp枠と太字だけで強調する。Switchや実行Buttonへ流用しない。
- 保存上限: `TrackRetentionPolicy`、`TrackRepository`、`SmartphoneTrackRepository`、`SettingsRepository`。選択肢、表示文言、永続化、保持テストを同時更新し、生ログの保持とは分離する。
- SORACOM payload項目: `soracom/SoracomPayload.kt`。SPを混入させない。

## 注意が必要な変更

- Map追従: `MapViewportPolicy`の`MAP-01`, `MAP-02`を維持する。
- Foreground Service: `ForegroundServicePolicy`を経由し、権限のない種別を直接指定しない。
- Smartphone GNSS: NTRIP/SORACOM/Consoleの入力へ流さない。
- セッション: LiveとPast sessionを混在させない。
- NTRIP: RTCMバイト列を整形、文字列化、再エンコードしない。
- 認証情報: DataStoreへ平文保存せず、ログにも出さない。

## 地図サービスを変更する場合

MapLibreは描画SDK、OpenFreeMapは現在のスタイル提供元、OpenStreetMapは地図データの由来である。これらを混同しない。商用利用条件、帰属表示、公開タイルの利用規約、SLA、キャッシュ条件を確認する。

## DBを変更する場合

Roomのversionを上げ、明示的migrationを追加し、既存のセッション・QLM点・SP点が保持されることを実機またはinstrumentation testで確認する。`fallbackToDestructiveMigration`は使用しない。

## 完了条件

- 関連要件IDが文書とテストから追跡できる。
- `./gradlew testDebugUnitTest`と`./gradlew assembleDebug`が成功する。
- Android権限や実機I/Oに関係する変更は`TESTING.md`の試験を実施する。
