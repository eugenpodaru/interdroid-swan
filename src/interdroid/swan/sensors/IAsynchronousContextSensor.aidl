package interdroid.swan.sensors;

import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;

interface IAsynchronousContextSensor {

	void register(in String id, in ContextTypedValue value);

	void unregister(in String id, in ContextTypedValue value);

	List<TimestampedValue> getValues(in String id, long now, long timespan);

	String getScheme();
}

