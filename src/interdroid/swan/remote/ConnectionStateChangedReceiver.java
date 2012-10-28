package interdroid.swan.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.RemoteException;

/**
 * a class used to monitor changes in network connectivity
 * 
 * @author eugen
 * 
 */
public class ConnectionStateChangedReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {

		// get the ConnectivityManager
		ConnectivityManager connectivityManager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);

		// get the active network information and the background data setting
		NetworkInfo activeNetInfo = connectivityManager.getActiveNetworkInfo();
		boolean backgroundDataSetting = connectivityManager
				.getBackgroundDataSetting();

		// get a connector to the remote context service
		RemoteContextServiceConnector connector = new RemoteContextServiceConnector(
				context);
		connector.start();

		// if the connection succeed, restart the remote context service
		if (connector.isConnected()) {
			try {
				// Check if background data is allowed
				if (!backgroundDataSetting || activeNetInfo == null
						|| !activeNetInfo.isConnected()) {
					connector.getRemoteContextService().stop();
				} else {
					connector.getRemoteContextService().start();
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}

			connector.stop();
			connector = null;
		}
	}
}
