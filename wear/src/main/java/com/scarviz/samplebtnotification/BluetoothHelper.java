package com.scarviz.samplebtnotification;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Bluetoothヘルパークラス
 */
public class BluetoothHelper {
	private Context mContext;
	private UUID mUuid;
	private Handler mHandler;

	private BluetoothManager mBluetoothManager;
	private BluetoothAdapter mBluetoothAdapter;
	private ClientThread mClientThread;
	private ServerThread mServerThread;

	public final static int RES_HANDL_ID = 1001;

	private boolean mIsConnected = false;

	/**
	 * コンストラクタ
	 * @param context
	 * @param uuid
	 * @param handler
	 */
	public BluetoothHelper(Context context, String uuid, Handler handler){
		mContext = context;
		mUuid = UUID.fromString(uuid);
		mHandler = handler;

		GenBluetoothAdapter();
	}

	/**
	 * BluetoothAdapterの生成
	 */
	private void GenBluetoothAdapter() {
		if(Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR2) {
			mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
			mBluetoothAdapter = mBluetoothManager.getAdapter();
		} else {
			mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		}
	}

	/**
	 * ペアリング済み機器を取得する
	 * @return
	 */
	public Set<BluetoothDevice> GetPairedDevices(){
		Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
		return pairedDevices;
	}

	/**
	 * 端末がBluetoothを使用できるかチェックする
	 * @return
	 */
	public boolean IsEnabledBluetooth() {
		if(mBluetoothAdapter == null) {
			GenBluetoothAdapter();
		}

		if(mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()){
			return false;
		}
		else{
			return true;
		}
	}

	/**
	 * Bluetooth機器のスキャン
	 * @param devieFoundReceiver
	 */
	public void ScanDevice(BroadcastReceiver devieFoundReceiver){
		//インテントフィルターとBroadcastReceiverの登録
		IntentFilter filter = new IntentFilter();
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		filter.addAction(BluetoothDevice.ACTION_FOUND);
		filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);
		filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		mContext.registerReceiver(devieFoundReceiver, filter);

		//接続可能なデバイスを検出
		if(mBluetoothAdapter.isDiscovering()){
			//検索中の場合は検出をキャンセルする
			mBluetoothAdapter.cancelDiscovery();
		}
		//デバイスを検索する
		//一定時間の間検出を行う
		mBluetoothAdapter.startDiscovery();
	}

	/**
	 * Bluetooth機器の接続
	 * @param address
	 */
	public void Connect(String address) {
		// クライアント用のスレッドを生成
		mClientThread = new ClientThread(address);
		mClientThread.start();
	}

	/**
	 * サーバー処理を開始する
	 */
	public void StartServer() {
		if (mServerThread != null) {
			mServerThread.cancel();
		}
		mServerThread = new ServerThread();
		mServerThread.start();
	}

	/**
	 * メッセージを送信する
	 * @param message
	 */
	public void sendMessage(String message) {
		try {
			if (mServerThread != null) {
				mServerThread.sendMessage(message);
			}
			if (mClientThread != null) {
				mClientThread.sendMessage(message);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * キャンセル処理
	 */
	public void Cancel() {
		if (mServerThread != null) {
			mServerThread.cancel();
			mServerThread = null;
		}
		if (mClientThread != null) {
			mClientThread.cancel();
			mClientThread = null;
		}
	}

	/**
	 * 接続中かどうか
	 * @return
	 */
	public boolean IsConnected() {
		return mIsConnected;
	}

	/**
	 * 接続フラグを設定する
	 * @param isConnected
	 */
	private void SetIsConnected(boolean isConnected){
		boolean befmIsConnected = mIsConnected;
		mIsConnected = isConnected;

		// 接続状態から切断状態に変更された場合
		if(!isConnected && befmIsConnected
				&& mContext != null && mHandler != null) {
			Drawable icon =  mContext.getResources().getDrawable(R.drawable.ic_launcher);
			NotificationInfo notifyInfo = new NotificationInfo();
			notifyInfo.Title = mContext.getString(R.string.mes_title_dis_connected);
			notifyInfo.Text = mContext.getString(R.string.mes_text_dis_connected);
			notifyInfo.Icon =  ((BitmapDrawable) icon).getBitmap();
			notifyInfo.GroupName = this.getClass().getPackage().toString();

			Message mes = Message.obtain();
			mes.what = RES_HANDL_ID;
			mes.obj = notifyInfo.GetJsonStr(notifyInfo);
			mHandler.sendMessage(mes);
		}
	}

	/**
	 * クライアント側の処理スレッド
	 */
	private class ClientThread extends ReceiverThread  {
		private final BluetoothDevice mServer;

		private ClientThread(String address) {
			mServer = mBluetoothAdapter.getRemoteDevice(address);
			try {
				mSocket = mServer.createRfcommSocketToServiceRecord(mUuid);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			// connect() の前にデバイス検出をやめる必要がある
			mBluetoothAdapter.cancelDiscovery();
			try {
				// サーバに接続する
				mSocket.connect();
				SetIsConnected(true);
				loop();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				cancel();
			}

		}

		private void cancel() {
			try {
				mSocket.close();
				SetIsConnected(false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * サーバ側の処理スレッド
	 */
	private class ServerThread extends ReceiverThread  {
		private BluetoothServerSocket mServerSocket;

		private ServerThread() {
			try {
				mServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(mContext.getPackageName(), mUuid);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				Log.d("ServerThread", "accepting...");
				mSocket = mServerSocket.accept();
				Log.d("ServerThread", "accepted");
				SetIsConnected(true);
				loop();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				cancel();
			}
		}

		private void cancel() {
			try {
				mServerSocket.close();
				SetIsConnected(false);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 処理スレッド
	 */
	private abstract class ReceiverThread extends Thread {
		protected BluetoothSocket mSocket;

		protected void sendMessage(String message) throws IOException {
			OutputStream os = mSocket.getOutputStream();
			os.write(message.getBytes());
			os.write("\n".getBytes());
		}

		protected void loop() throws IOException {
			BufferedReader br = new BufferedReader(new InputStreamReader(mSocket.getInputStream()));
			String message;
			while ((message = br.readLine()) != null) {
				Message mes = Message.obtain();
				mes.what = RES_HANDL_ID;
				mes.obj = message;
				mHandler.sendMessage(mes);
			}
		}
	}
}
