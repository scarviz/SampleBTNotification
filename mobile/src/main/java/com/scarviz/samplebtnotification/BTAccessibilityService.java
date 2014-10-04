package com.scarviz.samplebtnotification;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.util.List;

public class BTAccessibilityService extends AccessibilityService
{
	private BTService mBoundBTService;
	private boolean mIsBound;

	/** GooglePlayストアのパッケージ名 */
	private final String GPS_PKG_NM = "com.android.vending";

	TelephonyManager telephonyManager;

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();

		Log.d("onServiceConnected", "Connected BTAccessibilityService");
		// BTServiceをバインドする
		BindBTService();
		// 着信イベントのリスナー登録
		telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		telephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {
		int type = event.getEventType();
		// Notificationで無い場合
		if(type != AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
			|| !event.getClassName().toString().startsWith("android.app.Notification")){
			return;
		}

		CharSequence packageCs = event.getPackageName();
		// パッケージ名が取得できない場合は無視する
		if(packageCs == null || packageCs == ""){
			Log.d("onAccessibilityEvent", "event.getPackageName is null or empty");
			return;
		}
		String packageName = packageCs.toString();
		Log.d("onAccessibilityEvent", "packageName:"+packageName);

		boolean existPkg = false;
		// PackageManagerのオブジェクトを取得
		PackageManager pm = this.getPackageManager();
		// インストール済パッケージ情報を取得する
		final List<ApplicationInfo> appInfoList = pm.getInstalledApplications(BIND_AUTO_CREATE);
		for(ApplicationInfo ai : appInfoList){
			// Notificationがインストール済みのパッケージ名と一致するか確認する
			if (packageName.startsWith(ai.packageName)){
				Log.d("onAccessibilityEvent", "ApplicationInfo.packageName:"+ai.packageName);
				Log.d("onAccessibilityEvent", "ApplicationInfo.flags:"+ai.flags);
				// GooglePlayストアでなく、アップデート可能アプリでなく、システム(プリインストール)のものの場合、無視する
				if(!packageName.startsWith(GPS_PKG_NM)
					&& ((ai.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0)
					&& ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0)){
					continue;
				}
				// 一致する場合は確認済みとしてループから抜ける
				existPkg = true;
				break;
			}
		}

		// 一致するものがなかった場合
		if(!existPkg){
			return;
		}

		Notification n = (Notification)event.getParcelableData();
		if (n == null) {
			return;
		}

		RemoteViews rv = n.contentView;
		View view = rv.apply(this, null);
		if (view == null) {
			return;
		}

		NotificationInfo notifyInfo = new NotificationInfo();

		Drawable icon = null;
		try {
			// パッケージマネージャからアプリアイコンを取得する
			icon = pm.getApplicationIcon(packageName);
			if(icon == null) {
				// Notificationレイアウトからアイコンを取得する
				ImageView ivIcon = (ImageView) view.findViewById(android.R.id.icon);
				if(ivIcon != null){
					icon = ivIcon.getDrawable();
				}
			}
		} catch (Exception e){
			e.printStackTrace();
		}

		if(icon != null){
			notifyInfo.Icon = ((BitmapDrawable) icon).getBitmap();
		}

		TextView tvTitle = (TextView)view.findViewById(android.R.id.title);
		if(tvTitle != null) {
			notifyInfo.Title = tvTitle.getText().toString();
		}

		if(notifyInfo.Title == null){
			notifyInfo.Title = "no title";
		}

		StringBuilder sb = new StringBuilder();
		// event.getTextにはsetTickerで設定されている文字列が格納されている
		for (CharSequence cs : event.getText()) {
			// 既に文字列を格納済みの場合は改行コードを入れる
			if(0 < sb.length()){
				sb.append("\n");
			}
			sb.append(cs);
		}
		notifyInfo.Text =sb.toString();

		notifyInfo.GroupName = packageName;

		Log.d("onAccessibilityEvent", "Title:"+notifyInfo.Title);
		Log.d("onAccessibilityEvent", "Text:"+notifyInfo.Text);
		Log.d("onAccessibilityEvent", "GroupName:"+notifyInfo.GroupName);

		//Toast.makeText(getApplicationContext(), "Title:"+notifyInfo.Title+", Text:"+notifyInfo.Text, Toast.LENGTH_SHORT).show();
		SendNotification(notifyInfo);
	}

	@Override
	public void onInterrupt() {
		UnbindBTService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		UnbindBTService();
	}

	/**
	 * BTServiceにバインドする
	 */
	private void BindBTService(){
		if(!mIsBound) {
			// Serviceと接続
			Intent intent = new Intent(this, BTService.class);
			bindService(intent, mConnection, BIND_AUTO_CREATE);
			Log.d("BindBTService", "bind BTService");
		}
	}

	/**
	 * BTServiceのバインド解除
	 */
	private void UnbindBTService(){
		if(mIsBound) {
			unbindService(mConnection);
			Log.d("UnbindBTService", "unbind BTService");
		}
		mIsBound = false;
	}

	/**
	 * Serviceと接続するためのコネクション
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundBTService = null;
			mIsBound = false;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBoundBTService = ((BTService.BTServicelBinder)service).getService();
			mIsBound = true;
			Log.d("onServiceConnected", "BTService Connected");
		}
	};

	/**
	 * Notification情報を送信する
	 * @param notifyInfo
	 */
	public void SendNotification(NotificationInfo notifyInfo) {
		if(mBoundBTService == null){
			Log.d("SendNotification", "mBoundService is null");
			return;
		}
		mBoundBTService.SendNotification(notifyInfo.GetJsonStr(notifyInfo));
	}

	/**
	 * 着信イベントのリスナー
	 */
	PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String number) {
			switch (state) {
				// 着信
				case TelephonyManager.CALL_STATE_RINGING:
					Drawable icon =  getResources().getDrawable(R.drawable.ic_launcher);

					NotificationInfo notifyInfo = new NotificationInfo();
					notifyInfo.Title = "着信";
					notifyInfo.Text = ComFunc.GetCallerName(BTAccessibilityService.this, number);
					notifyInfo.Icon = ((BitmapDrawable) icon).getBitmap();
					notifyInfo.GroupName = "TelephonyManager.CALL_STATE_RINGING";
					SendNotification(notifyInfo);
					break;
			}
		}
	};
}
