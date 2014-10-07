package com.scarviz.samplebtnotification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * SMS受信クラス
 */
public class SMSReceiver extends BroadcastReceiver {
	private static final String TAG = "SMSReceiver";
	private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    public SMSReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
		Log.d(TAG, "onReceive");
		// SMSでない場合は無視する
		if (!SMS_RECEIVED.equals(intent.getAction())){
			Log.d(TAG, "not SMS_RECEIVED");
			return;
		}

		Bundle bundle = intent.getExtras();
		if(bundle == null){
			Log.d(TAG, "bundle is null");
			return;
		}

		// PDUデータ取得
		Object[] pdus = (Object[]) bundle.get("pdus");
		if(pdus == null || pdus.length < 0){
			Log.d(TAG, "pdus is null");
			return;
		}

		StringBuilder sb = new StringBuilder();
		for(int i=0; i<pdus.length; i++){
			SmsMessage msg = SmsMessage.createFromPdu((byte[])pdus[i]);

			String callerNo = msg.getOriginatingAddress();
			String mesBody = msg.getMessageBody();
			// auニュースEXなどの速報サービスで既に利用されていないが垂れ流されているようなので無視させる
			// ※メッセージの先頭に番号が記載されているので、とりあえず最初の文字列を見るようにしておく
			if(mesBody != null
				&& (mesBody.startsWith("9701060") || mesBody.startsWith("9711060"))) {
				continue;
			}

			sb.append(ComFunc.GetCallerName(context, callerNo));
			sb.append("\n");
			sb.append("Message:");
			sb.append("\n");
			sb.append(mesBody);

			Drawable icon =  context.getResources().getDrawable(R.drawable.ic_launcher);

			NotificationInfo notifyInfo = new NotificationInfo();
			notifyInfo.Title = "SMS";
			notifyInfo.Text = sb.toString();
			notifyInfo.Icon = ((BitmapDrawable) icon).getBitmap();
			notifyInfo.GroupName = SMS_RECEIVED;

			Intent startServiceIntent = new Intent(context, BTService.class);
			startServiceIntent.putExtra(BTService.REQUEST_TYPE, BTService.REQUEST_TYPE_ID);
			startServiceIntent.putExtra(BTService.NOTIFY_INFO_JSON, notifyInfo.GetJsonStr(notifyInfo));
			context.startService(startServiceIntent);
		}
    }
}
