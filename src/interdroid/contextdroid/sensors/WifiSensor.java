package interdroid.contextdroid.sensors;

import java.util.List;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

public class WifiSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "Wifi";

	public static final String SSID_FIELD = "ssid";
	public static final String BSSID_FIELD = "bssid";
	public static final String LEVEL_FIELD = "level";
	public static final String SCAN_RESULT_FIELD = "scanresult";

	public static final String DISCOVERY_INTERVAL = "discovery_interval";

	protected static final int HISTORY_SIZE = 60 * 60 * 1000;
	public static final long EXPIRE_TIME = 5 * 60 * 1000; // 5 minutes?
	public static final long DEFAULT_DISCOVERY_INTERVAL = 30 * 1000;

	private boolean stopPolling = false;

	private WifiManager wifiManager;

	private BroadcastReceiver wifiReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			long now = System.currentTimeMillis();

			trimValueByTime(now - HISTORY_SIZE);
			long expire = now + EXPIRE_TIME;

			List<ScanResult> results = wifiManager.getScanResults();
			for (ScanResult scanResult : results) {
				putValue(SSID_FIELD, now, expire, scanResult.SSID);
				putValue(BSSID_FIELD, now, expire, scanResult.BSSID);
				putValue(LEVEL_FIELD, now, expire, scanResult.level);
				putValue(SCAN_RESULT_FIELD, now, expire, scanResult);
			}
		}



	};

	private Thread wifiPoller = new Thread() {
		public void run() {
			while (!stopPolling) {
				long start = System.currentTimeMillis();
				if (registeredConfigurations.size() > 0) {
					wifiManager.startScan();
				}
				try {
					Thread.sleep(currentConfiguration
							.getLong(DISCOVERY_INTERVAL)
							+ start
							- System.currentTimeMillis());
				} catch (InterruptedException e) {
				}
			}
		}
	};

	public void onDestroy() {
		try {
			unregisterReceiver(wifiReceiver);
		} catch (IllegalArgumentException e) {
			// TODO: look into this, this should not happen but it does
		}

		stopPolling = true;
		wifiPoller.interrupt();

		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { SSID_FIELD, BSSID_FIELD, LEVEL_FIELD,
				SCAN_RESULT_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(DISCOVERY_INTERVAL,
				DEFAULT_DISCOVERY_INTERVAL);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'wifi', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ SSID_FIELD
				+ "', 'type': 'string'},"
				+ "            {'name': '"
				+ BSSID_FIELD
				+ "', 'type': 'string'},"
				+ "            {'name': '"
				+ LEVEL_FIELD
				+ "', 'type': 'integer'},"
				+ "            {'name': '"
				+ SCAN_RESULT_FIELD
				+ "', 'type': 'bytes'}"
				+ "           ]" + "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
		wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		if (registeredConfigurations.size() == 1) {
			registerReceiver(wifiReceiver, new IntentFilter(
					WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
			if (!wifiPoller.isAlive()) {
				wifiPoller.start();
			}
		}
		updatePollRate();
	}

	private void updatePollRate() {
		boolean keepDefault = true;
		long updatedPollRate = Long.MAX_VALUE;
		for (Bundle configuration : registeredConfigurations.values()) {
			if (configuration.containsKey(DISCOVERY_INTERVAL)) {
				keepDefault = false;
				updatedPollRate = Math.min(updatedPollRate,
						configuration.getLong(DISCOVERY_INTERVAL));
			}
		}
		if (keepDefault) {
			currentConfiguration.putLong(DISCOVERY_INTERVAL,
					DEFAULT_DISCOVERY_INTERVAL);
		} else {
			currentConfiguration.putLong(DISCOVERY_INTERVAL, updatedPollRate);
		}
	}

	@Override
	protected void unregister(String id) {
		if (registeredConfigurations.size() == 0) {
			unregisterReceiver(wifiReceiver);
		}
		updatePollRate();
	}

}
