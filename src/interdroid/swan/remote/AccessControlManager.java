package interdroid.swan.remote;

import android.content.Context;

public class AccessControlManager {
	
	/**
	 * The context used by the device manager
	 */
	Context context;

	public AccessControlManager(Context context) {
		this.setContext(context);
	}

	private void setContext(Context context) {
		this.context = context;
	}

	private Context getContext() {
		return this.context;
	}
	
	/*
	 * verify if the device with the specified id has access to the specified sensor
	 */
	public boolean hasAccess(String deviceId, String sensor)
	{
		return true;
	}
}
