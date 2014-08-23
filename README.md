SampleBTNotification
====================
Android4.0以上の端末(handheld)のNotification情報を、AndroidWearに通知するサンプルソースです。

動作手順
--------
#### インストール
AndroidStudioでSampleBTNotificationを開き、端末とAndroidWearにそれぞれ実行する

#### 端末(handheld)の設定
1. Bluetoothを有効にする
2. 端末のSampleBTNotification画面からAccessibilityServiceボタンを押下し、ユーザ補助画面のSampleBTNotificationの項目を有効にする
3. ServiceのスイッチをONにする(デフォルトでONになっている)

#### AndroidWearの設定
ServiceのスイッチをONにする(デフォルトでONになっている)

#### 端末とAndroidWearの接続
1. 一度もペアリングを実施したことがない場合、端末のDiscoverableボタンを押下し、AndroidWearのSearchボタンを押下する
2. AndroidWearのリストに、端末の情報が表示されているのを確認する
3. 端末のStart serverボタンを押下する
4. AndroidWearのリストで、端末情報の項目をタップする
5. 端末とAndroidWearのそれぞれでペアリング許可を求められるので、許可する
6. AndroidWearのTestボタンを押下し、端末側でToastが表示されていれば、接続成功

#### Notificationの通知テスト
1. 端末のTestボタンを押下する
2. AndroidWearにNotificationが表示されていれば成功