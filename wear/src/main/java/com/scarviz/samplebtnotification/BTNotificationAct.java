package com.scarviz.samplebtnotification;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class BTNotificationAct extends Activity {
	private DeviceListAdapter mDeviceListAdapter;
	private Handler mHandler;

	private Button mBtnSearch, mBtnTest;
	private TextView mTextView;
	private ListView mBleList;
	private Switch mSwService, mSwVibrator;

	private BTService mBoundService;
	private boolean mIsBound;

	private boolean mIsDispedPaired = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btnotification);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
			@Override
			public void onLayoutInflated(WatchViewStub stub) {
				mBtnSearch = (Button) stub.findViewById(R.id.btnSearch);
				mTextView = (TextView) stub.findViewById(R.id.text);
				mBleList = (ListView) stub.findViewById(R.id.list);
				mSwService = (Switch) stub.findViewById(R.id.swService);
				mSwVibrator = (Switch) stub.findViewById(R.id.swVibrator);

				mBtnSearch.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (mBoundService == null){
							mTextView.setText("Service Not Bound");
							return;
						} else if (!mBoundService.IsEnabledBluetooth()) {
							mTextView.setText("BlueTooth Not Enable");
							return;
						}

						mDeviceListAdapter.clear();
						mDeviceListAdapter.notifyDataSetChanged();

						mBoundService.ScanDevice(DevieFoundReceiver);
					}
				});

				mBleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
						if (mBoundService == null){
							mTextView.setText("Service Not Bound");
							return;
						} else if (!mBoundService.IsEnabledBluetooth()) {
							mTextView.setText("BlueTooth Not Enable");
							return;
						}

						BluetoothDevice device = mDeviceListAdapter.getDevice(i);
						mBoundService.Connect(device.getAddress());
						mTextView.setText("BlueTooth Connected");
					}
				});

				mBtnTest = (Button) stub.findViewById(R.id.btnTest);
				mBtnTest.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View view) {
						if (mBoundService == null){
							mTextView.setText("Service Not Bound");
							return;
						} else if (mBoundService.IsConnected()) {
							mBoundService.sendMessage("Test Message");
							Toast.makeText(BTNotificationAct.this, "Send Message", Toast.LENGTH_SHORT).show();
						} else {
							mTextView.setText("BlueTooth Not Connected");
						}
					}
				});

				mSwService.setChecked(true);
				mSwService.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
						if(isChecked){
							StartService();
						} else {
							StopService();
						}
					}
				});

				mSwVibrator.setChecked(true);
				mSwVibrator.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
						if(mBoundService == null){
							return;
						}
						mBoundService.SetEnableVibrator(isChecked);
					}
				});

				StartService();
				mHandler = new Handler();

				mDeviceListAdapter = new DeviceListAdapter(BTNotificationAct.this);
				mBleList.setAdapter(mDeviceListAdapter);
			}
		});
    }

	/**
	 * Serviceを開始する
	 */
	private void StartService(){
		// Serviceと接続
		Intent intent = new Intent(this, BTService.class);
		startService(intent);
		bindService(intent, mConnection, BIND_AUTO_CREATE);

		mIsBound = true;
	}

	/**
	 * Serviceを停止する
	 */
	private void StopService(){
		unbindService(mConnection);
		Intent intent = new Intent(this, BTService.class);
		stopService(intent);

		mIsBound = false;
	}

	/**
	 * Serviceと接続するためのコネクション
	 */
	private ServiceConnection mConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mBoundService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBoundService = ((BTService.BTServicelBinder)service).getService();
			if(mBoundService == null) {
				mTextView.setText("Service Not Bound");
				return;
			}

			mSwVibrator.setChecked(mBoundService.GetEnableVibrator());

			if(!mIsDispedPaired){
				SetPairedDevices();
				mIsDispedPaired = true;
			}
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mIsBound) {
			unbindService(mConnection);
		}
	}

	/**
	 * ペアリング済み機器を一覧に設定する
	 */
	private void SetPairedDevices(){
		Set<BluetoothDevice> pairedDevices = mBoundService.GetPairedDevices();
		if (pairedDevices != null && 0 < pairedDevices.size()){
			for(BluetoothDevice device : pairedDevices) {
				mDeviceListAdapter.addDevice(device);
				mDeviceListAdapter.notifyDataSetChanged();
			}
		}
	}

	/**
	 * Bluetooth機器のスキャンのコールバック
	 */
	private final BroadcastReceiver DevieFoundReceiver = new BroadcastReceiver(){
		//検出されたデバイスからのブロードキャストを受ける
		@Override
		public void onReceive(Context context, Intent intent){
			String action = intent.getAction();
			String dName = null;
			BluetoothDevice foundDevice;

			if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mTextView.setText("BlueTooth Device Scan ...");
					}
				});
			}

			if(BluetoothDevice.ACTION_FOUND.equals(action)){
				//デバイスが検出された
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null){
					Log.d("ACTION_FOUND", dName);

					mDeviceListAdapter.addDevice(foundDevice);
					mDeviceListAdapter.notifyDataSetChanged();

					////接続したことのないデバイスのみアダプタに詰める
					//if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//}
				}
			}

			if(BluetoothDevice.ACTION_NAME_CHANGED.equals(action)){
				foundDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if((dName = foundDevice.getName()) != null) {
					//名前が検出された
					Log.d("ACTION_NAME_CHANGED", dName);

					mDeviceListAdapter.addDevice(foundDevice);
					mDeviceListAdapter.notifyDataSetChanged();

					////接続したことのないデバイスのみアダプタに詰める
					//if(foundDevice.getBondState() != BluetoothDevice.BOND_BONDED){
					//}
				}
			}

			if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mTextView.setText("Stop Scan");
					}
				});
			}
		}
	};
}