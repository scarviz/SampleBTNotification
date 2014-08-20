package com.scarviz.samplebtnotification;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class DeviceListAdapter extends BaseAdapter {
	private ArrayList<BluetoothDevice> mLeDevices;
	private LayoutInflater mInflator;

	public DeviceListAdapter(Activity act) {
		super();
		mLeDevices = new ArrayList<BluetoothDevice>();
		mInflator = act.getLayoutInflater();
	}

	public void addDevice(BluetoothDevice device) {
		if(!mLeDevices.contains(device)) {
			mLeDevices.add(device);
		}
	}

	public BluetoothDevice getDevice(int position) {
		return mLeDevices.get(position);
	}

	public void clear() {
		mLeDevices.clear();
	}

	@Override
	public int getCount() {
		return mLeDevices.size();
	}

	@Override
	public Object getItem(int i) {
		return mLeDevices.get(i);
	}

	@Override
	public long getItemId(int i) {
		return i;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup) {
		ViewHolder viewHolder;	//View一時保存クラス（下の方で定義している）
		// 初回（リストがNull）の時、Viewをインフレート
		if (view == null) {
			view = mInflator.inflate(R.layout.listitem_device, null);
			viewHolder = new ViewHolder();
			viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
			viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
			view.setTag(viewHolder);
		}
		//　初回以外はインフレートしない。
		else {
			viewHolder = (ViewHolder) view.getTag();
		}
		//　各値をセット
		BluetoothDevice device = mLeDevices.get(i);
		final String deviceName = device.getName();
		if (deviceName != null && deviceName.length() > 0)
			viewHolder.deviceName.setText(deviceName);
		else
			viewHolder.deviceName.setText("unknown_device");
		viewHolder.deviceAddress.setText("アドレス："+device.getAddress());

		return view;
	}

	/**
	 * Viewを一時保存するクラス
	 */
	static class ViewHolder {
		TextView deviceName;
		TextView deviceAddress;
	}
}
