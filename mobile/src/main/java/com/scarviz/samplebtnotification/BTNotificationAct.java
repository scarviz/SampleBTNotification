package com.scarviz.samplebtnotification;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

public class BTNotificationAct extends Activity {
	private BTService mBoundService;
	private boolean mIsBound;

	private static final int TEST_NOTIFY_ID = Integer.MAX_VALUE;

	private static final int REQUEST_DISCOVERABLE_BT = 2001;
	private static final int DURATION = 300;

	private Button mBtnDiscoverable, mBtnStartServer, mBtnAccessibilityService, mBtnTest;
	private TextView mTxtText;
	private Switch mSwService;

	/** An intent for launching the system settings. */
	private static final Intent sSettingsIntent =
			new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btnotification);

		mBtnDiscoverable = (Button) findViewById(R.id.btnDiscoverable);
		mBtnStartServer = (Button) findViewById(R.id.btnStartServer);
		mBtnAccessibilityService = (Button) findViewById(R.id.btnAccessibilityService);
		mTxtText = (TextView) findViewById(R.id.txtText);
		mSwService = (Switch) findViewById(R.id.swService);
		mBtnTest = (Button) findViewById(R.id.btnTest);

		mBtnDiscoverable.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mBoundService == null){
					mTxtText.setText("Service Not Bound");
					return;
				} else if(!mBoundService.IsEnabledBluetooth()){
					mTxtText.setText("BlueTooth Not Enable");
					return;
				}
				StartDiscoverable();
				mTxtText.setText("BlueTooth Discoverable Started");
			}
		});

		mBtnStartServer.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (mBoundService == null){
					mTxtText.setText("Service Not Bound");
					return;
				} else if(!mBoundService.IsEnabledBluetooth()){
					mTxtText.setText("BlueTooth Not Enable");
					return;
				}

				String errMes = mBoundService.StartServer();
				if(errMes != null && errMes != ""){
					mTxtText.setText(errMes);
				} else {
					mTxtText.setText("BlueTooth Server Started");
				}
			}
		});

		mBtnAccessibilityService.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				startActivity(sSettingsIntent);
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

		mBtnTest.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				NotifyTest();
			}
		});

		StartService();
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(mIsBound) {
			unbindService(mConnection);
		}
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
		}
	};

	/**
	 * Bluetooth機器として検出されるようにする
	 */
	public void StartDiscoverable() {
		Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
		intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DURATION);
		startActivityForResult(intent, REQUEST_DISCOVERABLE_BT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		System.out.println("requestCode:" + requestCode + " resultCode:" + resultCode + " data:" + data);
		if (requestCode == REQUEST_DISCOVERABLE_BT) {
			if (resultCode == DURATION) {
				// 「はい」が選択された
			}
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.btnotification, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

	/**
	 * テスト用Notification
	 */
	private void NotifyTest(){
		NotificationCompat.Builder notification =	new NotificationCompat.Builder(this)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentTitle(getString(R.string.txt_test))
				.setContentText(getString(R.string.txt_test))
				.setAutoCancel(true)
				// 空Intentでdismissさせるようにする
				.setContentIntent(PendingIntent.getActivity(this, 0, new Intent(), 0))
				.setWhen(System.currentTimeMillis());

		NotificationManager manager = (NotificationManager) getSystemService(Service.NOTIFICATION_SERVICE);
		manager.notify(TEST_NOTIFY_ID, notification.build());
	}
}
