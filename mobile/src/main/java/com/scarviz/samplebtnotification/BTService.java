package com.scarviz.samplebtnotification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class BTService extends Service {
	private BtProcHandler mBtProcHandler;
	private BluetoothHelper mBtHelper;
	private final String UUID = "9dfda1d0-dc92-477b-b109-b731ecce2c21";

	private IBTAccessibilityService mService;

	private final static String ERR_MES_AS_NOT_BIND = "AccessibilityService Not Bind";

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

		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		Log.d("BTService", "onDestroy");
		mBtHelper.Cancel();
		if (mService != null) {
			try {
				mService.unregisterCallback(mCallback);
				mService = null;
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

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
		try {
			IBinder binder = BTAccessibilityService.GetBinder();
			if(binder == null){
				return ERR_MES_AS_NOT_BIND;
			}

			mService = IBTAccessibilityService.Stub.asInterface(binder);
			mService.registerCallback(mCallback);
		} catch (Exception e) {
			e.printStackTrace();
			return ERR_MES_AS_NOT_BIND;
		}

		mBtHelper.StartServer();

		// BTServer起動後に通知表示するようにする
		NotifyOn();

		return null;
	}

	/**
	 * Service起動の通知表示を表示する
	 */
	public void NotifyOn() {
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
	public void NotifyOff() {
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

	/**
	 * BTAccessibilityServiceのコールバック
	 */
	private IBTAccessibilityServiceCallback mCallback = new IBTAccessibilityServiceCallback.Stub() {
		@Override
		public void SendNotification(String notifyInfoJson) throws RemoteException {
			if(!mBtHelper.IsConnected()){
				Log.d("BTService.SendNotification", "BlueTooth Not Connected");
				return;
			}

			Log.d("BTService.SendNotification", notifyInfoJson);
			//Toast.makeText(getApplicationContext(), notifyInfoJson, Toast.LENGTH_SHORT).show();
			mBtHelper.sendMessage(notifyInfoJson);
		}
	};
}
