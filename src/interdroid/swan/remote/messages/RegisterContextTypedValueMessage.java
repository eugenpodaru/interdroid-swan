package interdroid.swan.remote.messages;

import interdroid.swan.contextexpressions.ContextTypedValue;

public class RegisterContextTypedValueMessage extends Message {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2865907456683959442L;

	/** The id of the context typed value */
	public String id;

	/** The context typed value */
	public ContextTypedValue value;
}
