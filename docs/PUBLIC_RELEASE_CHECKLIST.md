# 公開サンプルAPK リリースチェックリスト

本書は、GitHub ReleasesでAPKを公開サンプルとして配布する際の判定基準である。実装が`main`へマージされたことだけではリリース可能と判断しない。

## 1. 公開条件

- [ ] リポジトリの`LICENSE`が配布主体によって承認され、READMEの説明と一致している。
- [ ] 法務、セキュリティ、プライバシーおよび商標表記の確認が完了している。
- [ ] Release対象コミットが`main`にあり、Android CIが成功している。
- [ ] `docs/TESTING.md`の変更影響に対応する実機試験が完了している。
- [ ] QLM29H、Android、NTRIP、Map、ログ保存・共有の基本動作を確認している。
- [ ] NTRIP再接続などRelease Notesに記載する重要な変更のフィールド試験が完了している。

フィールド試験が未完了の場合、APKをStable Releaseへ昇格しない。必要ならDraft ReleaseでRelease Notesと成果物名だけを準備し、APKの公開は試験完了後に行う。

## 2. ソースと秘密情報

- [ ] `git status --ignored`で`misc/`、`wireguard.conf`、`local.properties`、署名鍵、`keystore.properties`、`dist/`が除外されている。
- [ ] Git履歴と差分にNTRIP認証情報、秘密鍵、実走ログ、正確な座標がない。
- [ ] GitHubのSecret scanning、Push protection、Private vulnerability reportingが有効である。
- [ ] Issue/PRへ添付した画面とログも同じ基準で確認している。

## 3. バージョンと署名

- [ ] 前回配布より`versionCode`を増やし、Release名と一致する`versionName`を設定している。
- [ ] 組織管理のリリース鍵を使用し、鍵とパスワードをリポジトリ外で管理している。
- [ ] `./scripts/prepare-sample-release.sh`が成功している。
- [ ] `apksigner verify --verbose --print-certs`で署名者を確認している。
- [ ] 既存配布APKから上書き可能か確認し、署名変更で上書きできない場合はデータのエクスポートと再インストール手順をRelease Notesへ記載している。

## 4. APK確認

- [ ] Release候補APKをクリーンインストールできる。
- [ ] 同じ署名鍵の直前版から`adb install -r`で更新でき、DBと設定が保持される。
- [ ] ホーム画面のアイコン、アプリ名、バージョン、必要権限を確認している。
- [ ] USB権限、通知権限、Smartphone GNSS位置権限の拒否・許可を確認している。
- [ ] アンインストール時に内部データが消えることを確認し、必要なログを事前にエクスポートしている。

## 5. GitHub Release

- [ ] Git tag、Release名、APK名、チェックサムのバージョンが一致している。
- [ ] APKと`.sha256`だけをRelease Assetsへ添付し、APKをGit履歴へコミットしていない。
- [ ] Release Notesに対象機器、Android要件、署名区分、主な変更、確認済み項目、既知の制約、データ移行上の注意を記載している。
- [ ] SHA-256チェックサムをダウンロード後のファイル名で検証できる。
- [ ] 初期評価用デバッグ署名APKを継続利用向けのStable Releaseとして扱っていない。

公開後は、ReleaseページからAPKとチェックサムを新しいフォルダーへダウンロードし、READMEの手順だけで検証・インストールできることを最終確認する。
