package com.scarviz.samplebtnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class BTService extends Service {
	private BtProcHandler mBtProcHandler;
	private BluetoothHelper mBtHelper;
	private final String UUID = "9dfda1d0-dc92-477b-b109-b731ecce2c21";

	public static final String REQUEST_TYPE = "REQUEST_TYPE";
	public static final int REQUEST_TYPE_ID = 1000;
	public static final String NOTIFY_INFO_JSON = "NOTIFY_INFO_JSON";

	@Override
	public void onCreate() {
		Log.d("BTService","onCreate");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d("BTService","onStartCommand");

		if(mBtProcHandler == null) {
			mBtProcHandler = new BtProcHandler(this);
		}
		if(mBtHelper == null) {
			mBtHelper = new BluetoothHelper(this, UUID, mBtProcHandler);
		}

		// Intentに格納されているものがあった場合
		if (intent != null && intent.getIntExtra(REQUEST_TYPE, 0) == REQUEST_TYPE_ID) {
			// NotificationInfoのJSONを取得する
			String notifyInfoJson = intent.getStringExtra(NOTIFY_INFO_JSON);
			// Notificationを送る
			SendNotification(notifyInfoJson);
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("BTService", "onDestroy");
		mBtHelper.Cancel();

		// Service起動の通知を非表示にする
		NotifyOff();
	}

	@Override
    public IBinder onBind(Intent intent) {
		Log.d("BTService","onBind");
		return mBinder;
    }

	/**
	 * Binder
	 */
	private final IBinder mBinder = new BTServicelBinder();
	public class BTServicelBinder extends Binder {
		/**
		 * サービスの取得
		 */
		BTService getService() {
			return BTService.this;
		}
	}

	/**
	 * 端末がBluetoothを使用できるかチェックする
	 * @return
	 */
	public boolean IsEnabledBluetooth(){
		return mBtHelper.IsEnabledBluetooth();
	}

	/**
	 * サーバー処理を開始する
	 * @return エラーメッセージ
	 */
	public String StartServer() {
		mBtHelper.StartServer();

		// BTServer起動後に通知表示するようにする
		NotifyOn();

		return null;
	}

	/**
	 * Notificationを送る
	 * @param notifyInfoJson
	 */
	public void SendNotification(String notifyInfoJson) {
		if(mBtHelper == null || !mBtHelper.IsConnected()){
			Log.d("BTService.SendNotification", "BlueTooth Not Connected");
			return;
		}

		Log.d("BTService.SendNotification", notifyInfoJson);
		//Toast.makeText(getApplicationContext(), notifyInfoJson, Toast.LENGTH_SHORT).show();
		mBtHelper.sendMessage(notifyInfoJson);
	}

	/**
	 * Service起動の通知表示を表示する
	 */
	private void NotifyOn() {
		Intent intent = new Intent(BTService.this, BTNotificationAct.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(BTService.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		NotificationCompat.Builder notification =	new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.app_name))
				.setContentText(getString(R.string.txt_sw_service))
				.setAutoCancel(false)
				.setWhen(System.currentTimeMillis())
				.setContentIntent(pendingIntent);

		NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		manager.cancel(0);

		// ServiceをKillされにくくする
		startForeground(R.string.app_name, notification.build());
	}

	/**
	 *  Service起動の通通知表示を非表示にする
	 */
	private void NotifyOff() {
		NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		nm.cancel(R.string.app_name);
	}

	/**
	 * Bluetooth通信処理のハンドラ
	 */
	private static class BtProcHandler extends Handler {
		WeakReference<BTService> ref;

		public BtProcHandler(BTService r) {
			ref = new WeakReference<BTService>(r);
		}

		@Override
		public void handleMessage(Message msg) {
			final BTService btSrv = ref.get();
			if (btSrv == null) {
				return;
			}

			switch (msg.what) {
				case BluetoothHelper.RES_HANDL_ID:
					String message = (String)msg.obj;
					Toast.makeText(btSrv.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}
}
