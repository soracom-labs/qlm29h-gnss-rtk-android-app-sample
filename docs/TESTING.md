# テスト方針

## 自動テスト

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew lintDebug
```

純粋ロジックは端末なしで試験する。特にNMEA framing/checksum/GGA、NTRIP request/response、SORACOM policy、保持上限、Map viewport、Foreground Service policyを対象とする。

Lintのerrorはリリース前に0件とする。依存更新、target SDK、アイコン形状など意図的に別工程へ分離したwarningは内容を確認し、無条件にbaselineへ隠さない。

## 基本実機試験

1. 既存DBを保持した上書きインストールが成功する。
2. 起動直後にSmartphone GNSSがDisabledである。
3. USB権限を許可し、QLM接続とPQTM送信を確認する。
4. NTRIP接続後にRTCM Receivingとなる。
5. USB切断でアプリがクラッシュせず、NTRIPが停止する。
6. SPをON/OFFしてクラッシュしない。
7. BackgroundをONにして画面消灯中も取得し、OFF後に通知が不要なら消える。
8. Past sessionを選択し、全点表示、SP非表示、Return to liveを確認する。
9. Mapを縮小し、密集点でも品質色が判別できることを確認する。
10. ConsoleのShareから`Share current log`が従来形式を共有し、`Share historical log`がSessionsまでスクロールすることを確認する。
11. 終了済みSessionの`Share logs`からNMEA `.log`を共有し、QGNSS v2.5 Log Playで読み込めることを確認する。NTRIPを使用した新規セッションではRTCM `.log`も同時共有されることを確認する。
12. 生ログ導入前のSessionはGGA-onlyと表示され、位置・Fix Qualityを再生できることを確認する。
13. SORACOM送信間隔は`POST interval`と同じ行に`default 60s`、次の行に30、15、10、6、5、3秒の順で表示されることを確認する。初期値および旧版からの初回移行値は60秒とし、60秒への変更は直ちに適用され、それ以外は確認ダイアログのCancelで元の値を維持し、Confirm後だけ適用されることを確認する。
14. 上書きインストール後、ホーム画面とアプリ一覧に新しいランチャーアイコンが表示されることを確認する。

## 車載試験

- 1時間以上および複数セッション合計3時間以上を記録する。
- 停止、低速、40〜59km/h、60km/h以上を含める。
- Fixed/Float/SPS率、衛星数、HDOP、NTRIP接続、RTCM鮮度を速度帯別に確認する。
- 高架下などの遮蔽後にFixedへ復帰する時間を記録する。
- Mapのちらつき、瞬間ジャンプ、追従対象の入れ替わりを確認する。

## 回帰時のログ取扱い

クラッシュbuffer、アプリプロセスlogcat、Room DBの解析用コピーを用いる。報告には認証情報と正確な座標を含めず、一時コピーは解析後に削除する。

詳細な操作チェックリストは `../HARDWARE_TEST_CHECKLIST.md` を参照する。
