package interdroid.swan.remote;

import java.util.Collection;

/**
 * The listener interface for receiving messages from the remote peers
 * @author eugen
 *
 */
public interface RemoteContextServiceReceiveListener {

	/**
	 * Called when a remote peer sends some messages to the local device
	 * @param deviceId
	 * @param results
	 */
	void onReceive(String deviceId, Collection<Object> results);
}
