package interdroid.swan.sensors;

import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;

import java.io.IOException;
import java.util.List;

import android.os.Bundle;

/**
 * This is the interface that sensors which make use of the
 * AbstractSensorBase or one of its subclasses must implement.
 *
 * @author nick &lt;palmer@cs.vu.nl&gt;
 *
 */
public interface SensorInterface {

	/**
	 * Handle registering an expression.
	 * @param deviceId the id of the device to which the sensor belongs
	 * @param id the expression to register
	 * @param valuePath the value path being registered
	 * @param configuration the configuration for the expression
	 * @throws IOException if there is a problem with the sensor
	 */
	void register(String id, ContextTypedValue value) throws IOException;

	/**
	 * Handle unregistering an expression.
	 * @param id the expression to unregister
	 */
	void unregister(String id, ContextTypedValue value);

	/**
	 * @return the scheme for this sensor
	 */
	String getScheme();

	/**
	 *
	 * @param id the id of the expression to use
	 * @param now the time right now
	 * @param timespan the timespan desired
	 * @return the values requested
	 */
	List<TimestampedValue> getValues(String id, long now,
			long timespan);

	/**
	 * @return the value paths this sensor puts out
	 */
	String[] getValuePaths();

	/**
	 * Callback when a sensor is being destroyed.
	 */
	void onDestroySensor();

	/**
	 * Callback when connection to Swan has been set up.
	 */
	void onConnected();

}
