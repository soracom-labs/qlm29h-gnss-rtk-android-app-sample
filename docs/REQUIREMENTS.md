# 現行要求仕様

## 1. 文書の位置付け

本書は実装と実機試験で確定した現行要求を記録する。原初仕様 `qlm29h_android_app_spec.md` と矛盾する場合、本書を優先する。

## 2. USB・QLM29H

- `USB-01`: 対応USBシリアル機器を列挙し、ユーザーが選択して接続できること。
- `USB-02`: 接続成功後にRTK有効化PQTMコマンドを送信すること。
- `USB-03`: USB切断時に読取、NTRIP、定期SORACOM送信、測位セッションを安全に終了すること。
- `USB-04`: 権限済みの単一機器は設定に従って自動再接続できること。

## 3. NTRIP・RTCM

- `NTRIP-01`: NTRIP v1、Basic認証、ICY/HTTP 200へ対応すること。
- `NTRIP-02`: 最新の有効なQLM GGAをCasterへ送信し、受信RTCMを加工せずUSBへ転送すること。
- `NTRIP-03`: 一時的なネットワーク障害では再接続し、認証失敗では無限再試行しないこと。
- `NTRIP-04`: RTCM最終受信から10秒以上経過した状態をStaleとして表示すること。
- `NTRIP-05`: Smartphone GNSSをNTRIPのGGAとして使用しないこと。
- `NTRIP-06`: 未設定時はSORACOMのQLM29H手順に合わせ、Host `qrtksa1.quectel.com`、Port `2101`、Mount Point `AUTO`を初期値とし、他Caster向けに編集可能であること。

## 4. SORACOM Unified Endpoint

- `SORACOM-01`: 有効化前にUSB、送信先、最新Fixを検証し、テスト送信成功後に有効化すること。
- `SORACOM-02`: 定期送信はQLM測位だけを対象とし、SPを含めないこと。
- `SORACOM-03`: Disabledでも確認ダイアログを経由して最新Fixのテスト送信ができること。
- `SORACOM-04`: 失敗ペイロードを永続化・自動再送しないこと。
- `SORACOM-05`: Unified Endpointへの定期送信間隔は5〜3,600秒とし、入力途中や不正値でも5秒未満で送信しないこと。

## 5. Smartphone GNSS

- `SP-01`: SPはMap上の参考表示と内部保存だけに使用し、NTRIP、SORACOM、Consoleへ連携しないこと。
- `SP-02`: SPの点と軌跡はQLMと別テーブル・別セグメント・別レイヤーで保持すること。
- `SP-03`: 初期状態およびプロセス再生成後はDisabledとすること。権限またはForeground Service開始失敗時も強制Disabledとすること。
- `SP-04`: 初回有効化時に正確な位置情報権限を要求すること。
- `SP-05`: Background設定が有効な間は画面消灯や他アプリ表示中も継続取得できること。

## 6. Map

- `MAP-01`: Live表示ではQLMをセンタリング対象として優先し、QLMが存在しない場合だけSPへフォールバックすること。
- `MAP-02`: Follow中でも、優先対象の時刻が前進した場合だけカメラを更新すること。
- `MAP-03`: Past sessionはユーザーが選択した1セッション全体を表示し、Live更新へ自動切替せず、SPを重ねないこと。
- `MAP-04`: 品質色はSPS赤、DGPS青、Float黄、Fixed緑、DR橙、No Fix灰、SP薄赤とすること。
- `MAP-05`: 縮小時は点径と白枠を抑え、密集時にも品質色を判別可能にすること。最新QLM点は専用レイヤーで強調すること。
- `MAP-06`: QLMとSPの更新差分だけを反映し、無関係なレイヤーやカメラを再設定しないこと。

## 7. セッション・保存

- `DATA-01`: QLM測位はUSB接続から切断までをセッションとして集計すること。
- `DATA-02`: QLMとSPは各50,000点または7日の早い方を保存上限とすること。
- `DATA-03`: Live Mapの表示上限とDB保存上限は独立させること。
- `DATA-04`: 終了済みセッションは選択表示および確認ダイアログ付き削除ができること。
- `DATA-05`: Track、Session、NMEAログ、Map cacheの破壊的削除には確認ダイアログを表示すること。
- `DATA-06`: USBセッションごとに受信NMEAとNTRIP受信RTCMを別ファイルへ生バイトのまま保存し、終了済みセッションからQGNSS互換ログとして共有できること。生ログ導入前のセッションは保存済みGGAを時刻順・CRLF区切りで出力すること。
- `DATA-07`: ConsoleのShareは現在の診断ログ共有と過去セッション共有を明示的に分け、後者はSettingsのSessionsへ遷移すること。

## 8. 表示・安全性

- `DISPLAY-01`: Dark themeとKeep screen onをDisplay設定へ集約すること。
- `DISPLAY-02`: 配布APKはQLM29H GNSS/RTK Sample用ランチャーアイコンをAndroid標準密度ごとに備えること。
- `FGS-01`: USB権限が現在有効な場合だけ`connectedDevice`種別を使用すること。
- `FGS-02`: 位置情報権限が現在有効な場合だけ`location`種別を使用すること。
- `FGS-03`: 有効な種別がなければサービスを安全に停止し、例外でアプリを終了させないこと。
- `SEC-01`: 認証情報、Authorizationヘッダー、走行座標を診断ログへ出力しないこと。
