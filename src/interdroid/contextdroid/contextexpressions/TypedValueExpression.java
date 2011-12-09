package interdroid.contextdroid.contextexpressions;

import interdroid.contextdroid.ContextDroidException;
import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.contextservice.SensorConfigurationException;
import interdroid.contextdroid.contextservice.SensorManager;
import interdroid.contextdroid.contextservice.SensorSetupFailedException;
import android.os.Parcel;

/**
 * This class wraps a value in an expression.
 * 
 * TODO: Refactor TypedValue so that it is an expression.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public class TypedValueExpression extends Expression {

	/**
	 * Serial Version ID for this class.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The typed value we wrap.
	 */
	private final TypedValue mValue;

	public TypedValueExpression(TypedValue value) {
		mValue = value;
	}

	public TypedValueExpression(Parcel in) {
		mValue = in.readParcelable(TypedValue.class.getClassLoader());
	}

	@Override
	protected int getSubtypeId() {
		return Expression.TYPED_VALUE_EXPRESSION_TYPE;
	}

	@Override
	public void initialize(String id, SensorManager sensorManager)
			throws SensorConfigurationException, SensorSetupFailedException {
		mValue.initialize(id, sensorManager);
	}

	@Override
	public void destroy(String id, SensorManager sensorManager)
			throws ContextDroidException {
		mValue.destroy(id, sensorManager);
	}

	@Override
	protected String toStringImpl() {
		return mValue.toString();
	}

	@Override
	protected String toParseStringImpl() {
		return mValue.toParseString();
	}

	@Override
	protected void evaluateImpl(long now) throws ContextDroidException {
		// Result is meaningless here.
		setResult(ContextManager.UNDEFINED, now);
	}

	@Override
	protected void writeToParcelImpl(Parcel dest, int flags) {
		dest.writeParcelable(mValue, flags);
	}

	@Override
	protected long getDeferUntilImpl() {
		// TODO look at reasoning below!
		// Defer until is meaningless here. Rather use current time millis than
		// 0, otherwise it will be the head of the queue continuously. Maybe add
		// some threshold?
		if (isConstant()) {
			return Long.MAX_VALUE;
		} else {
			return System.currentTimeMillis()
					+ ContextTypedValue.DEFAULT_HISTORY_LENGTH / 2;
		}
	}

	@Override
	protected boolean hasCurrentTime() {
		return mValue.hasCurrentTime();
	}

	@Override
	public TimestampedValue[] getValues(String string, long now)
			throws ContextDroidException {
		return mValue.getValues(string, now);
	}

	@Override
	public HistoryReductionMode getHistoryReductionMode() {
		return mValue.getHistoryReductionMode();
	}

	@Override
	public void sleepAndBeReadyAt(long readyTime) {
		// TODO: What do we do here?
	}

	@Override
	public long getHistoryLength() {
		return mValue.getHistoryLength();
	}

	@Override
	public boolean isConstant() {
		return mValue.isConstant();
	}

}
