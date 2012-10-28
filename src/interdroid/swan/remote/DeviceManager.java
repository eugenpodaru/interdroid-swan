package interdroid.swan.remote;

import android.content.Context;
import android.telephony.TelephonyManager;

public class DeviceManager {
	/**
	 * The Id of the local device
	 */
	public static final String LOCAL_DEVICE_ID = "";

	/**
	 * The context used by the device manager
	 */
	Context context;

	public DeviceManager(Context context) {
		this.setContext(context);
	}
	
	/**
	 * gets the id used internaly by the device
	 * 
	 * @return
	 */
	public final String getLocalDeviceId() {
		return DeviceManager.LOCAL_DEVICE_ID;
	}

	/**
	 * gets the id of the local device that is visible to the remote devices
	 * 
	 * @return
	 */
	public final String getExternalLocalDeviceId() {
		TelephonyManager telephonyManager = (TelephonyManager) this
				.getContext().getSystemService(Context.TELEPHONY_SERVICE);
		return telephonyManager.getLine1Number();
	}

	private void setContext(Context context) {
		this.context = context;
	}

	private Context getContext() {
		return this.context;
	}
}
