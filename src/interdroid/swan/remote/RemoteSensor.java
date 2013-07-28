package interdroid.swan.remote;

import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;
import interdroid.swan.sensors.AbstractSensorBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;

public class RemoteSensor extends AbstractSensorBase {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteContextService.class);

	/** the length of the array to keep for each value path */
	private static int HISTORY_SIZE = 1000;

	/**
	 * The map of values for this sensor.
	 */
	private final Map<String, List<TimestampedValue>> values = new HashMap<String, List<TimestampedValue>>();

	/** The object used to connect to the remote service context */
	private RemoteContextServiceConnector connector;

	/** the receiver to receive values from the RemoteContextManager */
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			LOG.debug("Received new values from RemoteContextManager");
			// get the id of the ContextTypedValue for which we have values
			String id = intent.getExtras().getString("id");
			// get the new values
			List<TimestampedValue> values = intent.getExtras()
					.getParcelableArrayList("values");

			LOG.debug("Received new values for value path with id: " + id);
			putValuesTrimSize(id, values, HISTORY_SIZE);
		}
	};

	@Override
	protected void init() {

		// subscribe to get intents for the following action
		this.registerReceiver(
				mReceiver,
				new IntentFilter(RemoteContextService.SEND_VALUES_ACTION));

		this.connector = new RemoteContextServiceConnector(this);
		this.connector.start();
	}

	@Override
	public void onDestroySensor() {
		// unsubscribe from getting intents
		this.unregisterReceiver(mReceiver);

		if (this.connector != null && this.connector.isConnected())
			this.connector.stop();
		this.connector = null;
	}

	/**
	 * @return the remoteContextService
	 */
	private IRemoteContextService getRemoteContextService() {
		if (this.connector != null && this.connector.isConnected())
			return this.connector.getRemoteContextService();
		return null;
	}

	@Override
	public void register(final String id, final ContextTypedValue value)
			throws IOException {
		synchronized (getValues()) {
			if (!getValues().containsKey(id)) {
				try {
					// add the value path to the internal list and register it
					// to the remote context service
					getValues().put(id, new ArrayList<TimestampedValue>());
					LOG.debug("Registering value path with id: " + id
							+ " to the RemoteContextService");
					getRemoteContextService().registerContextTypedValue(id,
							value);
					LOG.debug("Value path with id: " + id + " registered");
				} catch (RemoteException e) {
					LOG.debug("An error occured while registering value path with id: "
							+ id);
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	public void unregister(final String id, final ContextTypedValue value) {
		if (getValues().containsKey(id)) {
			synchronized (getValues()) {
				if (getValues().containsKey(id)) {
					try {
						getValues().remove(id);
						LOG.debug("Unregistering value path with id: " + id);
						getRemoteContextService().unregisterContextTypedValue(
								id);
						LOG.debug("Value path with id: " + id + " unregistered");
					} catch (RemoteException e) {
						LOG.debug("An error occured when trying to unregister the value path with id: "
								+ id);
						e.printStackTrace();
					}
				}
			}
		}
	}

	@Override
	public List<TimestampedValue> getValues(String id, long now, long timespan) {
		return getValuesForTimeSpan(getValues().get(id), now, timespan);
	}

	/**
	 * @return the values
	 */
	private final Map<String, List<TimestampedValue>> getValues() {
		return values;
	}

	/**
	 * adds the values to the list of values for the specified value path and
	 * trims the resulting array to the history size
	 * 
	 * @param id
	 * @param values
	 */
	private final void putValuesTrimSize(final String id,
			final List<TimestampedValue> values, final int historySize) {
		synchronized (getValues()) {
			if (getValues().containsKey(id)) {
				List<TimestampedValue> savedValues = getValues().get(id);
				savedValues.addAll(values);
				while (savedValues.size() > historySize)
					savedValues.remove(0);

				// notify that the data has changed for this value path
				notifyDataChangedForId(id);
			}
		}
	}

	@Override
	public String getScheme() {
		return null;
	}

	@Override
	public void onConnected() {
	}

	@Override
	public void initDefaultConfiguration(Bundle defaults) {
	}

	@Override
	public String[] getValuePaths() {
		return null;
	}
}
