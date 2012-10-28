package interdroid.swan.remote;

import interdroid.swan.contextexpressions.Expression;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextservice.SwanServiceException;

interface IRemoteContextService {

	SwanServiceException registerContextTypedValue(in String id, in ContextTypedValue contextTypedValue);
	SwanServiceException unregisterContextTypedValue(in String id);

	void start();
	void stop();
}
