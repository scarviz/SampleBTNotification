package com.scarviz.samplebtnotification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Vibrator;
import android.text.format.Time;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Set;

public class BTService extends Service {
	private BluetoothHelper mBtHelper;
	private BtProcHandler mBtProcHandler;
	private final String UUID = "9dfda1d0-dc92-477b-b109-b731ecce2c21";

	private static final int BT_NOTIFICATION_ID = 1001;

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
	}

	/**
	 * Bind処理
	 * @param intent
	 * @return
	 */
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
	 * ペアリング済み機器を取得する
	 */
	public Set<BluetoothDevice> GetPairedDevices() {
		Set<BluetoothDevice> pairedDevices = mBtHelper.GetPairedDevices();
		return pairedDevices;
	}

	/**
	 * Bluetooth機器のスキャン
	 * @param devieFoundReceiver
	 */
	public void ScanDevice(BroadcastReceiver devieFoundReceiver){
		mBtHelper.ScanDevice(devieFoundReceiver);
	}

	/**
	 * Bluetooth機器の接続
	 * @param deviceAddress
	 */
	public void Connect(String deviceAddress){
		mBtHelper.Connect(deviceAddress);
	}

	/**
	 * 接続中かどうか
	 * @return
	 */
	public boolean IsConnected() {
		return mBtHelper.IsConnected();
	}

	/**
	 * メッセージを送信する
	 * @param message
	 */
	public void sendMessage(String message) {
		mBtHelper.sendMessage(message);
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
					btSrv.SetNotify(message);
					break;
				default:
					super.handleMessage(msg);
					break;
			}
		}
	}

	/**
	 * Notificationを設定する
	 * @param message
	 */
	private void SetNotify(String message) {
		NotificationInfo notifyInfo = new NotificationInfo();
		notifyInfo = notifyInfo.GetNotificationInfo(message);

		Notification.Builder notification =	new Notification.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(notifyInfo.Title)
				.setContentText(notifyInfo.Text)
				.setGroup(notifyInfo.GroupName)
				.setStyle(new Notification.BigPictureStyle()
						.bigPicture(notifyInfo.Icon));

		((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
				.notify(GetNotifyId(), notification.build());

		Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
		long[] pattern = {500, 1000};
		vibrator.vibrate(pattern, -1);
	}

	private int mNotifyId = 0;
	/**
	 * Notification用のIDを取得する
	 * @return
	 */
	private int GetNotifyId() {
		mNotifyId++;
		if(mNotifyId == Integer.MAX_VALUE){
			mNotifyId = 0;
		}

		return mNotifyId;
	}
}
