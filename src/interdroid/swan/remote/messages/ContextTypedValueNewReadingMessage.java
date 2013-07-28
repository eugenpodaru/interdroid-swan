package interdroid.swan.remote.messages;

import interdroid.swan.contextexpressions.TimestampedValue;

public class ContextTypedValueNewReadingMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1026822535847363187L;
	
	/**
	 * The id of the ContextTypedValue for which the new readings are available
	 */
	public String id;
	
	/**
	 * The new readings
	 */
	public TimestampedValue[] values;
}
