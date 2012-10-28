package interdroid.swan.contextexpressions;

import interdroid.swan.SwanException;
import interdroid.swan.contextservice.SensorConfigurationException;
import interdroid.swan.contextservice.SensorManager;
import interdroid.swan.contextservice.SensorSetupFailedException;
import interdroid.swan.remote.DeviceManager;

import java.io.Serializable;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Represents a value of a particular type within an expression.
 * 
 * @author roelof &lt;rkemp@cs.vu.nl&gt;
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public abstract class TypedValue implements Serializable, Parcelable,
		Parseable<TypedValue> {

	/**
	 * Serial version id.
	 */
	private static final long serialVersionUID = -2920550668593484018L;

	/**
	 * The reduction mode this typed value runs with.
	 */
	private HistoryReductionMode mMode;
	
	/**
	 * The device id on which this typed value is to be registered
	 */
	private String mDeviceId;

	/**
	 * Constructs a typed value with the given reduction mode.
	 * 
	 * @param mode
	 *            the reduction mode. If mode is null mode is set to
	 *            HistoryReductionMode.DEFAULT_MODE.
	 */
	public TypedValue(final HistoryReductionMode mode){
		this(mode, DeviceManager.LOCAL_DEVICE_ID);
	}
	
	/**
	 * Constructs a typed value with the given reduction mode and device Id
	 * @param mode
	 * 			the reduction mode. If mode is null mode is set to
	 *          HistoryReductionMode.DEFAULT_MODE.
	 * @param deviceId
	 * 			the device Id. If the device Id is null, it will be set to the local device Id.
	 */
	public TypedValue(final HistoryReductionMode mode, final String deviceId) {
		if (mode == null) {
			mMode = HistoryReductionMode.DEFAULT_MODE;
		} else {
			mMode = mode;
		}

		if (deviceId == null) {
			mDeviceId = DeviceManager.LOCAL_DEVICE_ID;
		} else {
			mDeviceId = deviceId;
		}
	}

	/**
	 * Construct from a parcel.
	 * 
	 * @param source
	 *            the parcel to get data from.
	 */
	public TypedValue(final Parcel source) {
		readFromParcel(source);
	}

	/**
	 * @return the reduction mode
	 */
	public final HistoryReductionMode getHistoryReductionMode() {
		return mMode;
	}
	
	/**
	 * @return the deviceId
	 */
	public final String getDeviceId(){
		return mDeviceId;
	}
	
	/**
	 * @return a value indicating if the typed value addresses a remote device
	 */
	public final Boolean isRemote(){
		return mDeviceId != DeviceManager.LOCAL_DEVICE_ID;
	}

	/**
	 * @param id
	 *            the id of the expression.
	 * @param now
	 *            the time to evaluate at
	 * @return the values for this typed value
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	public abstract TimestampedValue[] getValues(final String id, final long now)
			throws SwanException;

	/**
	 * Applies the reduction mode to the values.
	 * 
	 * @param values
	 *            the value to reduce
	 * @return the reduced values.
	 */
	public final TimestampedValue[] applyMode(final TimestampedValue[] values) {
		switch (mMode) {
		case MAX:
			return new TimestampedValue[] { TimestampedValue
					.findMaxValue(values) };
		case MIN:
			return new TimestampedValue[] { TimestampedValue
					.findMinValue(values) };
		case MEAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMean(values) };
		case MEDIAN:
			return new TimestampedValue[] { TimestampedValue
					.calculateMedian(values) };
		case ALL:
		case ANY:
		default:
			return values;
		}
	}

	/**
	 * Initializes this value with the sensor manager.
	 * 
	 * @param id
	 *            the id of the expression
	 * @param sensorManager
	 *            the sensor manager to init with
	 * @throws SensorConfigurationException
	 *             if the config is problematic
	 * @throws SensorSetupFailedException
	 *             if there is a problem.
	 */
	public abstract void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException;

	/**
	 * Destroys these values.
	 * 
	 * @param id
	 *            the id of the expression
	 * @param sensorManager
	 *            the sensor manager to work with
	 * @throws SwanException
	 *             if something goes wrong.
	 */
	public abstract void destroy(String id, SensorManager sensorManager)
			throws SwanException;

	/**
	 * @return true if this value has the current time.
	 */
	protected abstract boolean hasCurrentTime();

	/**
	 * Sets values from a parcel.
	 * 
	 * @param source
	 *            parcel to read from
	 */
	private void readFromParcel(final Parcel source) {
		mMode = HistoryReductionMode.convert(source.readInt());
		mDeviceId = source.readString();
	}

	@Override
	public final void writeToParcel(final Parcel dest, final int flags) {
		dest.writeInt(mMode.convert());
		dest.writeString(mDeviceId);
		writeSubclassToParcel(dest, flags);
	}

	/**
	 * Interface for subclasses to get involved in parceling an instance.
	 * 
	 * @param dest
	 *            the parcel to write to
	 * @param flags
	 *            the flags for writing
	 */
	protected abstract void writeSubclassToParcel(Parcel dest, int flags);

	/**
	 * Parses a string version of a TypedValue.
	 * 
	 * @param value
	 *            the string to parse
	 * @return the string version
	 * @throws ExpressionParseException
	 *             if the string is not recognized
	 */
	public static final TypedValue parse(final String value)
			throws ExpressionParseException {
		return ContextExpressionParser.parseTypedValue(value);
	}

	/**
	 * @return true if this value never changes.
	 */
	public abstract boolean isConstant();

	/**
	 * @return the timespan of this typed value
	 */
	public abstract long getHistoryLength();
}
