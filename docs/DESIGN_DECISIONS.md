# 設計判断

## DD-01 Smartphone GNSSを再起動時に復元しない

Android 15実機で、USB権限失効とForeground Service種別変更が重なると連続クラッシュになり得た。Enabledは永続設定ではなく実行時状態とし、プロセス再生成後はDisabledへ戻す。Background設定と表示設定は保存してよい。

関連要件: `SP-03`, `FGS-01`〜`FGS-03`

## DD-02 QLMをMap追従の正とする

SPは比較用であり、補正済み測位やSORACOM送信の正ではない。独立した約1秒周期のSP更新でカメラを動かすと、約2秒周期のQLM位置との間でちらつくため、QLM存在中はSPを追従対象にしない。

関連要件: `MAP-01`, `MAP-02`, `SP-01`

## DD-03 過去セッションへSPを重ねない

現在のSPデータはQLMセッションIDを持たない。単純に最新SP点を過去QLMへ重ねると異なる時刻の軌跡を同一走行と誤認する。時刻対応を正式に設計するまではPast sessionでSPを隠す。

関連要件: `MAP-03`, `SP-02`

## DD-04 DB保存上限とMap表示上限を分離する

DBに行きと帰りの両セッションが残っていても、最新2,000点だけをMapへ渡すと帰りだけが残ったように見える。Live描画負荷は制限しつつ、選択したPast sessionは全点を取得する。

関連要件: `DATA-02`, `DATA-03`, `MAP-03`

## DD-05 低ズームでは白枠を消す

固定2pxの白枠は点が密集すると塗り色を覆い、RTK品質を判別できなくする。ズーム連動で半径と枠線を変化させ、最新QLMだけ専用レイヤーで強調する。

関連要件: `MAP-04`, `MAP-05`

## DD-06 SORACOM有効化をテスト送信で検証する

設定値だけでEnabledにすると、実際には送信不能なのにUIが有効状態を示す。USB、Fix、設定を検証し、テスト送信成功を有効化条件とする。ただし明示的な単発テスト送信はDisabledでも許可する。

関連要件: `SORACOM-01`, `SORACOM-03`

## DD-07 通信Jobの所有者を一つにする

再接続や設定変更のたびに複数のNTRIPストリーム、SORACOMタイマーが残ると、RTCMやPOSTが重複する。各Controllerが常に直前のJobをキャンセルしてから新しいJobを開始する。RtkRuntimeは開始条件とUI状態の反映だけを担当する。

認証エラーとTLSエラーは自動再試行せず、一時的なネットワーク障害だけを5秒後に再試行する。キャンセルは失敗件数や再接続回数へ数えない。

関連要件: `NTRIP-03`, `SORACOM-01`

## DD-08 USB受信とLocation登録の所有者を一つにする

USB再接続時に古いFlow collectorが残るとNMEAを二重解析し、点数・Console・SORACOM入力が重複する。`UsbSessionController`が受信Jobを一つだけ所有し、再接続時に置換する。

LocationManagerの重複登録も電池消費と多重保存を生むため、`SmartphoneLocationController`が開始停止を冪等にする。取得可否の判断は`SmartphoneGnssPolicy`、実際の登録はControllerという境界を維持する。

関連要件: `USB-03`, `SP-02`, `SP-03`, `SP-05`

## DD-09 通信RuntimeをAndroid ViewModelにしない

USB、NTRIP、RTCMはActivityの画面寿命ではなくApplicationプロセスとForeground Serviceに従う。ApplicationがViewModelを直接生成・保持するとUIライフサイクルの意味が崩れるため、通信本体を通常クラス`RtkRuntime`とし、独自SupervisorJobを所有させる。将来のUI ViewModelはRuntimeのStateFlowを画面用に変換する薄い層とする。

## DD-10 画面へRuntime全体を公開しない

Composableが`RtkRuntime`を直接受け取ると、画面から通信・保存・権限処理の任意の操作を呼べてしまい、Previewや単体試験にもRuntime構築が必要になる。Mapは専用状態とコールバック、Settingsは操作インターフェースを受け取る。機能別AppState分割やUI ViewModel導入時にも画面APIを維持できる。

関連要件: `MAP-01`〜`MAP-06`, `SP-01`, `DATA-03`

## DD-11 Runtime状態を機能別UI Stateへ投影する

平坦な`AppState`を各設定カードへ直接渡すと、無関係な通信状態や認証設定まで参照でき、Runtime内部の分割が全UIへ波及する。Settings入口で機能別の不変なUI Stateへ投影し、カードの入力を最小化する。この投影境界を先に保護することで、将来AppStateを分割またはUI ViewModelへ移行しても画面APIを維持できる。

## DD-12 AppStateは機能単位で段階的に分割する

AppStateを一括で置換すると、USB・NTRIP・RTCM・位置保存の同時更新に回帰が生じても原因を特定しにくい。まず通信と独立したDisplayを`AppDisplayState`へまとめ、投影・永続化・Activity参照を縦断的に移行する。各部分状態のcopyが無関係な状態を保持するテストを追加してから、USBなど次の機能へ進む。

USBは接続文字列だけでなく、選択端末、自動接続、端末一覧、送受信量、最終送受信時刻を`AppUsbState`へまとめる。これらは一つのUSBセッションに従うため、別機能へ分散させない。NMEA解析結果やConsoleはUSB入力から生成されるが、それぞれ独立した表示・診断モデルでありUSB transport状態には含めない。

NTRIPの接続設定、認証入力、Source Table、接続状態、RTCM統計、再接続回数は`AppNtripState`へまとめる。RTCMはNTRIPセッションから生じるため同じ部分状態に置く一方、USBへ転送したバイト数は`AppUsbState`にも反映する。この二つの更新は同じAppState copy内で行い、転送事実の片側だけが更新されないようにする。

SORACOMの有効化、送信間隔、品質ポリシー、ネットワーク種別、HTTP結果、成功・失敗回数は`AppSoracomState`へまとめる。USB接続とNTRIP接続は送信可否を決める入力であり、SORACOM状態へ複製しない。これにより各接続状態の正を一か所に保ち、古い複製値による誤送信を避ける。

Smartphone GNSSの有効化、バックグラウンド取得、Map表示、測位状態、SP点群、精度は`AppSmartphoneState`へまとめる。この状態をNTRIP・SORACOM・Console部分へ渡さず、SPの更新が補正・送信状態を変更しないことをcopy独立性テストで保護する。プロセス再生成時に`enabled=false`へ戻す規則もこの境界で維持する。

QLMの最新Fix、Live軌跡、保存件数、セッション一覧、選択中の過去セッション、Map追従は`AppTrackingState`へまとめる。過去セッション選択は表示ソースの明示的な切替であり、記録中のLive更新やSP更新で解除しない。SP点群はこの状態へ含めず、`MapUiState`の投影時だけ表示規則に従って組み合わせる。

Console履歴、Checksumエラー、GGA解析エラー、NMEA種別件数は`AppDiagnosticsState`へまとめる。接続状態や送信結果の正は各プロトコル状態に残し、Consoleクリアで診断カウンターや接続状態を初期化しない。ユーザー向け一時エラーは`AppNoticeState`へ分け、エラー解除が原因となった機能状態や診断履歴を消さない。

## DD-13 接続状態はRuntime内で型として扱う

自由な文字列は綴り違いと未定義状態をコンパイル時に検出できない。USBから`UsbConnectionState`へ移行し、接続可否やForeground Service稼働判定はenumで行う。既存UI文言を変えないため表示用`label`を型に持たせるが、Runtime判定でlabelを比較してはならない。NTRIPなど状態数の多い機能も同じ手順で段階移行する。

NTRIPは`NtripConnectionState`へ移行し、補正利用可能なのは`isConnected`、USB再接続時に停止すべきセッションは`hasActiveSession`で表現する。`Reconnecting`を補正済みと誤認せず、同時に動作中セッションとしては扱えるよう、二つの意味を一つの文字列集合へ混ぜない。

同じ理由でRTCM鮮度、SORACOM送信、Smartphone GNSS、設定保存、Source Table取得も型付けする。Source Tableの件数だけは`SourceTableStatus.Loaded(count)`の関連値として保持し、表示文字列をRuntime状態として保存しない。

## DD-14 connectedDevice FGSのManifest前提を明示する

Android 14以降の`connectedDevice` Foreground Serviceは専用権限に加え、接続系権限のManifest宣言が必要である。本アプリは`FOREGROUND_SERVICE_CONNECTED_DEVICE`と`CHANGE_NETWORK_STATE`を宣言する。ただしサービス開始可否の実際のゲートは従来どおりUSB device permissionであり、Manifest権限だけを根拠に開始しない。

## DD-15 表示用データと再解析用の原本を分ける

Consoleは直近10,000件の診断表示、TrackPointはGGAから抽出したMap表示用データであり、どちらも完全な受信ストリームではない。QGNSSや将来の自作Viewerで再解析できるよう、USB RXのNMEAとNTRIP RXのRTCMをセッション単位で生バイト保存する。NMEAとRTCMは役割が異なるため混在させず、ファイルI/Oは単一の順序付きキューで処理してUSB受信と次セッションを阻害しない。

ConsoleのShareは従来のタイムスタンプ付き診断ログを維持する。セッション原本はSettingsのSessionsから選択させ、終了済みセッションだけを共有する。これにより「画面に見えている直近ログ」と「走行セッションの原本」を同じShare操作から選べる一方、形式を曖昧にしない。

関連要件: `DATA-01`, `DATA-05`〜`DATA-07`, `SEC-01`

## DD-16 SORACOM向けNTRIP初期値を入力負担の軽減だけに使う

SORACOMから案内されるQLM29H用CasterはHost、Port、Mount Pointが共通であり、利用者ごとに異なるのは主にUsernameとPasswordである。未設定または旧版で空欄だった場合だけ公式手順の値を補い、保存済みの他Caster設定は上書きしない。Mount PointはUI入力を省略できてもNTRIP要求上は`AUTO`として保持し、Source Tableのルート要求と混同しない。

関連要件: `NTRIP-01`, `NTRIP-06`

## DD-17 SORACOM定期送信は選択肢と料金確認を設定・実行の両方で守る

送信回数はUnified Endpointおよび回線の料金に直結するため、定期送信間隔は3、5、6、10、15、30、60秒だけを選べるようにし、初期値を60秒とする。60秒以外は選択時に通信量と料金の試算を促し、確認後だけ適用・保存する。UIを迂回した不正値はSchedule PolicyとControllerで60秒へフォールバックする。また、確認機構導入前の保存値には利用者の確認記録がないため、設定ポリシーの世代を持たせて初回読込時に60秒へ移行する。単発の`Test send latest fix`は定期送信ではないため、この間隔制約の対象外とする。

関連要件: `SORACOM-03`, `SORACOM-05`

## DD-18 TrackPoint保持は件数だけをユーザーが選択する

固定7日では利用頻度によって保持できる走行量が大きく変わり、期限の根拠も利用者へ説明しにくい。日数制限を撤廃し、QLMとSPの各テーブルについて50,000、100,000、300,000点から共通の上限を選択する。上限超過時だけ古い点を削除し、既定値は従来と同じ50,000点とする。上限を下げる操作は即時に古い点を削除するため、DATA-05に従って確認ダイアログを必須とする。

この設定はMap表示用に抽出したTrackPointだけを対象とし、外部リプレイの原本であるNMEA/RTCM生ログには適用しない。上限を増やすとストレージ使用量とPast session描画負荷が増えるため、SettingsとREADMEで明示する。

関連要件: `DATA-02`, `DATA-03`, `DATA-05`, `DATA-06`
