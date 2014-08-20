package com.scarviz.samplebtnotification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Notification情報
 */
public class NotificationInfo {
	private final String JSON_ICON = "JSON_ICON";
	private final String JSON_TITLE = "JSON_TITLE";
	private final String JSON_TEXT = "JSON_TEXT";
	private final String JSON_GROUP_NAME = "JSON_GROUP_NAME";

	/** アイコン */
	public Bitmap Icon;
	/** タイトル */
	public String Title;
	/** テキスト内容 */
	public String Text;
	/** グループ名 */
	public String GroupName;


	/**
	 * Json文字列からNotification情報を取得する
	 * @param jsonStr
	 * @return
	 */
	public NotificationInfo GetNotificationInfo(String jsonStr){
		NotificationInfo notify = new NotificationInfo();
		if(jsonStr == null || jsonStr == ""){
			return notify;
		}

		try {
			// 文字列をJSONオブジェクトに変換する
			JSONObject jsonObj = new JSONObject(jsonStr);
			if(jsonObj == null){
				return notify;
			}

			// JSONオブジェクトから、各値を取得する
			notify.Icon = ToBitmap(Base64.decode(jsonObj.getString(JSON_ICON), Base64.NO_WRAP));
			notify.Title = jsonObj.getString(JSON_TITLE);
			notify.Text = jsonObj.getString(JSON_TEXT);
			notify.GroupName = jsonObj.getString(JSON_GROUP_NAME);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return notify;
	}

	/**
	 * Notification情報からJson文字列を取得する
	 * @param notifyInfo
	 * @return
	 */
	public String GetJsonStr(NotificationInfo notifyInfo){
		JSONObject jsonObj = new JSONObject();
		if(notifyInfo == null){
			return jsonObj.toString();
		}

		try {
			jsonObj.put(JSON_ICON, Base64.encodeToString(ToByteArray(notifyInfo.Icon), Base64.NO_WRAP));
			jsonObj.put(JSON_TITLE, notifyInfo.Title);
			jsonObj.put(JSON_TEXT, notifyInfo.Text);
			jsonObj.put(JSON_GROUP_NAME, notifyInfo.GroupName);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return jsonObj.toString();
	}

	/**
	 * byte配列に変換する
	 * @param bmp
	 * @return
	 */
	private byte[] ToByteArray(Bitmap bmp){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] bytes = baos.toByteArray();
		return bytes;
	}

	/**
	 * Bitmapに変換する
	 * @param bytes
	 * @return
	 */
	private Bitmap ToBitmap(byte[] bytes){
		Bitmap bmp = null;
		if (bytes != null) {
			bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
		}

		return bmp;
	}
}
