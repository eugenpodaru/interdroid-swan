package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractMemorySensor;
import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextTypedValueListener;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.List;

import android.os.Bundle;

public class IntentSensor extends AbstractMemorySensor {

	public static final String TAG = "Intent";

	public static final String STARTED_FIELD = "started";

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000;

	private static final String MAGIC_RELAY = "MAGIC_RELAY";

	@Override
	public String[] getValuePaths() {
		return new String[] { STARTED_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'train', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ STARTED_FIELD
				+ "', 'type': 'string'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	public void register(final String id, final String valuePath,
			final Bundle configuration) {
		try {
			contextServiceConnector.registerContextTypedValue(id + "."
					+ MAGIC_RELAY, new ContextTypedValue(
					"logcat/log?logcat_parameters=ActivityManager:I *:S"),
					new ContextTypedValueListener() {

						@Override
						public void onReading(String relayedId,
								TimestampedValue[] newValues) {
							// values is always of length 1
							if (newValues[0].value.toString().contains(
									"Starting: Intent {")) {
								if (getValues().size() >= HISTORY_SIZE) {
									getValues().remove(0);
								}
								getValues().get(valuePath)
										.add(new TimestampedValue(
												getIntentFrom(newValues[0].value),
												newValues[0].timestamp,
												newValues[0].expireTime));
								notifyDataChangedForId(id);
							}

						}

					});
		} catch (ContextDroidException e) {
			e.printStackTrace();
		}
	}

	private String getIntentFrom(final Object value) {
		String string = value.toString();
		string = string.substring(string.indexOf("cmp=") + 4);
		string = string.substring(0, string.indexOf(" "));
		return string;
	}

	@Override
	public void unregister(final String id) {
		try {
			contextServiceConnector.unregisterContextTypedValue(id + "."
					+ MAGIC_RELAY);
		} catch (ContextDroidException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onDestroySensor() {
		// Nothing to do
	}
}
