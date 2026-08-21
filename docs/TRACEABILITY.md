# 要件トレーサビリティ

重要要件から主要実装と保護テストを引くための索引である。ファイル移動時は本表も更新する。

| 要件 | 設計・実装 | 保護テスト |
|---|---|---|
| `MAP-01`, `MAP-02` | `ui/map/MapViewportPolicy.kt`, `ui/map/TrackMap.kt` | `MapViewportPolicyTest.kt` |
| `MAP-03`, `SP-06`, `DATA-03` | `RtkRuntime.selectMapSession`, `MapUiState.kt`, `SmartphoneTrackDao.observeSessionRange`, `MIGRATION_2_3` | `MapUiStateTest.kt`, `AppDatabaseMigrationTest.kt`; Runtime/DAO結合テストを今後追加 |
| `MAP-04`, `MAP-05` | `ui/map/MapStyleSpec.kt` | 実機ズーム試験 |
| `MAP-06` | `TrackMap.MapRenderState`, `updateTrack` | MapLibre実機試験 |
| `SP-01`, `SP-02` | `SmartphoneTrackRepository`, `SmartphoneTrackDao`, `RtkRuntime.reconcileSmartphoneLocation` | `SmartphoneGnssPolicyTest.kt`と実機確認 |
| `SP-03`, `SP-05` | `SmartphoneGnssPolicy.kt`, `RtkRuntime`, `RtkForegroundService` | `SmartphoneGnssPolicyTest.kt`, `ForegroundServicePolicyTest.kt` |
| `USB-01`〜`USB-04` | `UsbSessionController.kt`, `AndroidUsbSerialTransport.kt`, `RtkRuntime` | `UsbSessionControllerTest.kt`, `RtkRuntimeIntegrationTest.kt` |
| SP provider登録 | `SmartphoneLocationController.kt`, `AndroidSmartphoneLocationProvider` | `SmartphoneLocationControllerTest.kt` |
| `FGS-01`〜`FGS-03` | `ForegroundServicePolicy.kt`, `RtkForegroundService.kt` | `ForegroundServicePolicyTest.kt` |
| Android 14 connectedDevice FGS権限 | `AndroidManifest.xml`, USB permission gate | `lintDebug`, `ForegroundServicePolicyTest.kt` |
| `DATA-02` | `TrackRetentionPolicy`, `TrackRepository`, `SmartphoneTrackRepository`, `SettingsRepository`, `AppStorageState`, `TrackControls` | `TrackRetentionPolicyTest.kt`, `AppStateTest.kt`, `SettingsUiStateTest.kt`、設定永続化の実機試験 |
| `DATA-06`, `DATA-07` | `SessionRawLogStore`, `QgnssSessionLogExporter`, `NmeaConsoleCard`, `SessionHistoryCard` | `SessionRawLogStoreTest.kt`, `QgnssSessionLogExporterTest.kt`, `UsbSessionControllerTest.kt`; QGNSS v2.5実機試験 |
| `NTRIP-01`〜`NTRIP-04`, `NTRIP-06`, `NTRIP-07` | `NtripClient.kt`, `NtripSessionController.kt`, `NtripRetryPolicy`, `NtripDefaults`, `RtkRuntime.connectNtrip` | `NtripCoreTest.kt`, `NtripSessionControllerTest.kt`, `AppStateTest.kt` |
| `DATA-08` | `TrackSamplingPolicy`, `TrackRepository.record` | `TrackRetentionPolicyTest.samplingUsesGgaUtcSecondInsteadOfArrivalJitter` |
| `SORACOM-01`〜`SORACOM-05` | `SoracomScheduleController.kt`, `SoracomSendPolicy.kt`, `SettingsValidator.kt`, `SoracomIntervalPersistencePolicy.kt`, `RtkRuntime` | `SoracomScheduleControllerTest.kt`, `SoracomSendPolicyTest.kt`, `SoracomSenderTest.kt`, `SettingsValidatorTest.kt`, `SoracomIntervalPersistencePolicyTest.kt` |
| `DISPLAY-02` | `mipmap-*/ic_launcher.png` | `assembleDebug`, `lintDebug`、実機ランチャー確認 |
| `DISPLAY-03` | `MainActivity.AppBottomTabBar`, `drawable/ic_tab_*.xml`, `Qlm29hTheme` | `assembleDebug`, `lintDebug`、Light/Dark theme実機タブ切替確認 |
| `DISPLAY-04` | `SettingsScreen.SettingsChoiceChip`, `Qlm29hTheme` | `assembleDebug`, `lintDebug`、Light/Dark theme実機選択確認 |
| Settings境界値 | `settings/SettingsValidator.kt` | `SettingsValidatorTest.kt` |
| UIとRuntimeの境界 | `MapUiState.kt`, `SettingsUiState.kt`, `MapActions`, `SettingsActions`, `RtkSettingsActions` | `MapUiStateTest.kt`, `SettingsUiStateTest.kt`とコンパイル検証 |
| Runtime部分状態 | `AppState.kt`内の機能別`App*State`と各更新ヘルパー | `AppStateTest.kt`, `RtkRuntimeIntegrationTest.kt`, `MapUiStateTest.kt`, `SettingsUiStateTest.kt` |
| USB接続状態の型付け | `UsbConnectionState`, `AppUsbState`, `RtkForegroundService` | `AppStateTest.kt`, `RtkRuntimeIntegrationTest.kt` |
| NTRIP接続状態の型付け | `NtripConnectionState`, `AppNtripState`, `RtkRuntime.onNtripSessionEvent` | `AppStateTest.kt`, `NtripSessionControllerTest.kt` |
| その他Runtime状態の型付け | `RtcmStreamState`, `SoracomPublicationState`, `SmartphoneGnssStatus`, `SettingsPersistenceState`, `SourceTableStatus` | `AppStateTest.kt`, 各Controller/Policyテスト |

## 未整備の優先テスト

- RoomインメモリDBによるPast session全件取得と削除（SP時間範囲照合を含む）
- 選択中セッション削除時のLive復帰
- プロセス再生成後にSPがDisabledであること
- MapLibreレイヤー順序と低ズーム表現のUI試験
