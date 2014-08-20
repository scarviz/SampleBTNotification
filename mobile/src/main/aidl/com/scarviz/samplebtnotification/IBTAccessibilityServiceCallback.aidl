package com.scarviz.samplebtnotification;

oneway interface IBTAccessibilityServiceCallback {
	/**
	 * Notification情報を送信する
	 * @param notifyInfoJson
	 */
	void SendNotification(String notifyInfoJson);
}