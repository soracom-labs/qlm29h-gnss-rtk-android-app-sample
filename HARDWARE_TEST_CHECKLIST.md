# QLM29H RTK 実機受け入れチェックリスト

## 準備

- Android 14以降、USB Host対応端末
- QLM29HBAA-GM USBモデル
- USB OTGケーブルと十分な給電
- 接続可能なNTRIP Caster情報
- SORACOM SIMまたはSORACOM Arc接続
- OpenFreeMapへ接続可能なインターネット回線

## インストール

```sh
./gradlew testDebugUnitTest assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## USB・NMEA

- [ ] USBデバイス名、VID/PID、ドライバが一覧に表示される
- [ ] USB権限を許可すると115200/8N1で接続される
- [ ] TXコンソールに`$PQTMCFGRTK,W,1,1*6C`が表示される
- [ ] GGA/RMC/GSA/GSV等が欠落なく継続表示される
- [ ] GGAの緯度・経度・Quality・衛星数・HDOP・標高が妥当である
- [ ] 不正チェックサム行が位置更新へ使われない
- [ ] USBを抜くとDisconnectedになり、Foreground Serviceは接続待ちを維持する
- [ ] 権限済みの単一デバイスを再挿入すると自動接続する
- [ ] 8時間運転後もRX更新と画面操作が継続する

## NTRIP・RTCM

- [ ] TCP Casterへ接続できる
- [ ] 誤った認証情報でAuth Errorとなり自動再試行し続けない
- [ ] Source TableからMount Pointを選択できる
- [ ] 最新GGAが約1秒周期でCasterへ送られる
- [ ] RTCM受信量とメッセージIDが更新される
- [ ] RTCMがUSBへバイナリのまま転送される
- [ ] RTCM停止から10秒後にStaleとなる
- [ ] Wi-Fi/セルラー/VPN切替後にNTRIPが再接続する
- [ ] GGA Qualityが5（RTK Float）、4（RTK Fixed）へ遷移する

## 地図・保存

- [ ] 現在位置へ自動追従する
- [ ] ピンチズームとパンが動作する
- [ ] Quality別の色、縁取り、凡例、フィルタが一致する
- [ ] 測位点タップで座標・標高・衛星数・HDOPが表示される
- [ ] 再起動後もRoomの過去軌跡とセッションが表示される
- [ ] 表示済み範囲がオフラインで再表示できる
- [ ] ambient cacheが200 MB上限で動作する
- [ ] 地図キャッシュ削除後に再取得される
- [ ] 地図上にOpenStreetMap等の帰属表示が表示される
- [ ] Smartphone GNSS初回ON時に正確な位置情報の権限要求が表示される
- [ ] SPドットが薄い赤、SP軌跡が灰色でQLM29Hレイヤーの後面に表示される
- [ ] QLM29HとSPの軌跡が連結されず、10秒超のSP欠測でも別セグメントになる
- [ ] QLM29H表示中はQLM29Hを優先追従し、SPのみ表示時はSPを追従する
- [ ] Continue in background ON時に画面消灯後もSP点が保存される
- [ ] アプリ再起動後もSP軌跡が復元される
- [ ] Track cache削除でQLM29HとSPの両方が確認後に削除される

## SORACOM

- [ ] SORACOM経路で`http://uni.soracom.io`へのPOSTが成功する
- [ ] JSONにtimestamp/lat/lon/qualityが含まれる
- [ ] 指定周期で最新Fixのみ送信される
- [ ] No Fix、NTRIP未接続、Float+、Fixed only条件が反映される
- [ ] 失敗時に再送されず、次周期で最新データが新規送信される
- [ ] HTTP結果、ネットワーク種別、成功・失敗件数が更新される

## バックグラウンド

- [ ] 画面消灯中もUSB/NTRIP/RTCM/SORACOMが継続する
- [ ] 画面回転とActivity再生成で状態が維持される
- [ ] 通知にUSB/NTRIP/Fix状態が表示される
- [ ] 通知の停止操作で通信とセッションが安全に終了する

## 実機なしでは確定できない項目

- QLM29Hの実際のUSBチップとusb-serial-for-androidドライバ互換性
- QLM29Hファームウェア固有のPQTM成功応答
- RTCM転送遅延100 ms以内
- Android端末ごとのUSB給電・画面消灯・省電力制御
- 実NTRIP Caster固有のHTTP/ICY応答やGGA要求
- SORACOM SIM/Arcの実経路
- 8時間連続運転とネットワーク切替耐性
- OpenFreeMap公開サービスの長期運用時の可用性（SLAなし）
