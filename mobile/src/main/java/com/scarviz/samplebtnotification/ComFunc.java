package com.scarviz.samplebtnotification;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

public class ComFunc {
	/**
	 * 発信者名を取得する
	 * @param context コンテキスト
	 * @param caller_id 発信者番号
	 * @return 発信者名
	 */
	public static String GetCallerName(Context context, String caller_id) {
		Cursor cursor = context.getContentResolver().query(
				ContactsContract.Data.CONTENT_URI,
				new String[]{ContactsContract.Data.DISPLAY_NAME},
				"replace(" + ContactsContract.Data.DATA1 + ",'-','') = ? ",
				new String[]{ caller_id },
				null);

		if(cursor.getCount() <= 0){
			return caller_id;
		}

		cursor.moveToFirst();
		String name = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.DISPLAY_NAME));
		return name;
	}
}
