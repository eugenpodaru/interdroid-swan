package interdroid.swan.remote;

import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;
import interdroid.swan.sensors.AbstractSensorBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
	private final Map<String, List<TimestampedValue>> store = new HashMap<String, List<TimestampedValue>>();

	private boolean needToRegisterReceiver = true;

	/** the receiver to receive values from the RemoteContextManager */
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			LOG.debug("Received new values from RemoteContextManager");
			// get the id of the ContextTypedValue for which we have values
			String id = intent.getExtras().getString("id");

			// get the new values
			ArrayList<TimestampedValue> values = intent.getExtras().getParcelableArrayList("values");

			LOG.debug("Received new values for value path with id: " + id);
			putValuesTrimSize(id, values, HISTORY_SIZE);
		}
	};

	@Override
	public void register(final String id, final ContextTypedValue value)
			throws IOException {
		synchronized (store) {
			if (!store.containsKey(id)) {

				store.put(id, new ArrayList<TimestampedValue>());
				boolean registrationSucceded = false;

				try	{
					registrationSucceded = tryRegister(id, value, 5);
				}
				finally	{
					if(!registrationSucceded)
						store.remove(id);
				}

			}
		}
	}

	private boolean tryRegister(final String id, final ContextTypedValue value, final int retries) {
		boolean registerSucceded = true;
		try
		{
			LOG.debug("Registering value path with id: " + id + " to the RemoteContextService");
			RemoteContextServiceConnector connector = new RemoteContextServiceConnector(this);
			connector.start();
			if(connector.isConnected())
			{
				IRemoteContextService remoteContextService = connector.getRemoteContextService();
				if(remoteContextService == null)
					throw new RemoteException();

				connector.getRemoteContextService().registerContextTypedValue(id,value);
				connector.stop();

				LOG.debug("Value path with id: " + id + " registered");

				if(needToRegisterReceiver)
				{
					registerReceiver(mReceiver, new IntentFilter(RemoteContextService.SEND_VALUES_ACTION));
					needToRegisterReceiver = false;
				}
			}
		} catch (RemoteException e) {
			LOG.debug("An error occured while registering value path with id: "	+ id);
			e.printStackTrace();
			
			registerSucceded = false;
		}
		
		if(!registerSucceded && retries > 0)
		{
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			registerSucceded = tryRegister(id, value, retries - 1);
		}
		
		return registerSucceded;
	}

	@Override
	public void unregister(final String id, final ContextTypedValue value) {
		synchronized (store) {
			if (store.containsKey(id)) {
				store.remove(id);
				tryUnregister(id, value, 5);
			}
		}
	}

	private boolean tryUnregister(final String id, final ContextTypedValue value, final int retries) {
		boolean unregisterSucceded = true;
		try
		{
			LOG.debug("Unregistering value path with id: " + id);
			RemoteContextServiceConnector connector = new RemoteContextServiceConnector(this);
			connector.start();
			if(connector.isConnected())
			{
				IRemoteContextService remoteContextService = connector.getRemoteContextService();
				if(remoteContextService == null)
					throw new RemoteException();

				remoteContextService.unregisterContextTypedValue(id);
				connector.stop();

				LOG.debug("Value path with id: " + id + " unregistered");

				if(store.size() == 0)
				{
					unregisterReceiver(mReceiver);
					needToRegisterReceiver = true;
				}
			}
		} catch (RemoteException e) {
			LOG.debug("An error occured when trying to unregister the value path with id: "	+ id);
			e.printStackTrace();
			
			unregisterSucceded = false;
		}
		
		if(!unregisterSucceded && retries > 0)
		{
			try {
				Thread.sleep(500);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			unregisterSucceded = tryUnregister(id, value, retries - 1);
		}
		
		return unregisterSucceded;
	}

	@Override
	public List<TimestampedValue> getValues(String id, long now, long timespan) {
		return getValuesForTimeSpan(store.get(id), now, timespan);
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
		synchronized (store) {
			if (store.containsKey(id)) {
				List<TimestampedValue> savedValues = store.get(id);
				
				savedValues.addAll(values);
				Collections.sort(savedValues, new Comparator<TimestampedValue>() {
					@Override
					public int compare(TimestampedValue lhs,
							TimestampedValue rhs) {
						if(lhs.getTimestamp() < rhs.getTimestamp())
							return -1;
						else if(lhs.getTimestamp() > rhs.getTimestamp())
							return 1;
						else
							return 0;
					}
				});
				
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

	@Override
	public void onDestroySensor() {
	}

	@Override
	protected void init() {
	}
}
