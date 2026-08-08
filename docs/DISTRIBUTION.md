# APK配布ガイド

本書は、社内評価用APKと正式配布用APKを混同せず、安全に配布するための手順を定める。

## 配布区分

| 区分 | 用途 | 署名 | 配布先 |
|---|---|---|---|
| 社内評価版 | 限定されたテスターによる検証 | Androidのデバッグ鍵 | PrivateリポジトリのGitHub Release |
| 正式リリース | 組織が承認した利用者への配布 | 組織管理のリリース鍵 | 承認済みの配布経路 |

APKやAABはGit履歴へコミットせず、Releaseの添付ファイルとして配布する。実走ログ、接続設定、署名鍵およびパスワードは配布物へ含めない。

## 社内評価版を作成する

次のスクリプトは、Unit Test、Debug APKのビルド、Lintを実行し、バージョン付きAPKとSHA-256チェックサムを`dist/`へ生成する。

```sh
./scripts/prepare-evaluation-release.sh
```

生成物の例:

```text
dist/qlm29h-rtk-0.1.0-evaluation-debug.apk
dist/qlm29h-rtk-0.1.0-evaluation-debug.apk.sha256
```

デバッグ鍵はビルド環境ごとに異なる場合がある。同じApplication IDでも異なる鍵で署名したAPKへは上書き更新できないため、継続評価では同じビルド環境を使用する。鍵が変わった場合は、端末内データを必要に応じてエクスポートしたうえで旧アプリをアンインストールする。

## Private GitHub Releaseへ登録する

1. バージョンと変更内容を確認する。
2. `dist/`のAPKおよび`.sha256`をReleaseへ添付する。
3. Release名に`Evaluation`を含め、正式版ではないことを明記する。
4. 対応Androidバージョン、確認済みGNSS機器、既知の制約をRelease Notesへ記載する。
5. テスターには信頼できる社内経路からReleaseのURLを案内する。

APKそのもの、`dist/`、`misc/`、`wireguard.conf`、`keystore.properties`および署名鍵は、`.gitignore`でGit管理対象から除外している。初回コミット前に`git status --ignored`でも確認する。

## 正式リリース署名を準備する

正式配布の署名鍵は、配布主体のセキュリティ規程に従って生成・保管する。鍵を失うと同一アプリとして更新できなくなるため、アクセス制御された保管先と復旧手順を用意する。

1. `keystore.properties.example`を`keystore.properties`へコピーする。
2. `storeFile`には、リポジトリ外に保管した鍵の絶対パスを指定する。
3. パスワードとAliasをローカルで設定する。
4. 次のコマンドで署名済みAPKを生成する。

```sh
./gradlew testDebugUnitTest lintDebug assembleRelease
```

署名設定がない状態で`assembleRelease`または`bundleRelease`を実行すると、誤って未署名成果物を配布しないようビルドを失敗させる。

生成後はAndroid SDK Build Toolsの`apksigner`で署名を確認する。

```sh
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
shasum -a 256 app/build/outputs/apk/release/app-release.apk
```

## バージョン管理

- 配布ごとに`versionCode`を必ず増やす。
- 利用者向けバージョンは`versionName`で表す。
- Git tag、Release名、APKファイル名、Release Notesのバージョンを一致させる。
- どのコミットから生成したかをRelease Notesへ記載する。

## 配布前チェックリスト

- 必須テスト、APKビルド、Lintが成功している。
- 実機でインストール、USB権限、Foreground Service、Map、NTRIP、SORACOM送信を確認した。
- APKの署名者とSHA-256を確認した。
- Release Notesに評価版/正式版、対応環境、変更点、既知の制約を記載した。
- `misc/`などの実走ログや、認証情報・秘密鍵がGitと配布物に含まれていない。
- 地図および第三者ソフトウェアの帰属・ライセンス表示を確認した。
- 本アプリの配布ライセンスと配布主体の承認を確認した。

## ライセンス未決定時の扱い

本アプリの`LICENSE`が決まるまでは、Privateリポジトリ内の社内評価に限定する。社外公開、再配布可能なAPKの提供、オープンソースとしての表示は行わない。ライセンス文面は配布主体と法務・知財担当者が決定する。
