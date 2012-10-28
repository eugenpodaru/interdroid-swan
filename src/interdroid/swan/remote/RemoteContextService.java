package interdroid.swan.remote;

import interdroid.swan.ContextManager;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextservice.SwanServiceException;
import interdroid.swan.remote.messages.RegisterContextTypedValueMessage;
import interdroid.swan.remote.messages.UnregisterContextTypedValueMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The Class ContextService.
 */
public class RemoteContextService extends Service {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteContextService.class);

	/** The device manager */
	private DeviceManager deviceManager;

	/** The context manager */
	private ContextManager contextManager;

	/**
	 * The smart sockets manager is used to listen for registrations from remote
	 * devices or to register context values to remote devices and retrieve
	 * values from remote sensors
	 */
	private SmartSocketsManager smartSocketsManager;

	/** The context entities, mapped by id. */
	private final HashMap<String, ContextTypedValue> localContextTypedValues = new HashMap<String, ContextTypedValue>();

	/**
	 * Indicates if the service is running or not
	 */
	private volatile boolean isRunning = false;

	@Override
	public final IBinder onBind(final Intent intent) {
		LOG.debug("onBind {}", mBinder);
		return mBinder;
	}

	@Override
	public final boolean onUnbind(final Intent intent) {
		LOG.debug("onUnbind");

		return super.onUnbind(intent);
	}

	@Override
	public final int onStartCommand(final Intent intent, final int flags,
			final int startId) {
		LOG.debug("onStart: {} {}", intent, flags);

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public final void onCreate() {
		LOG.debug("onCreate");

		this.start();

		super.onCreate();
	}

	@Override
	public final void onDestroy() {
		LOG.debug("onDestroy");

		this.stop();

		super.onDestroy();
	}

	/**
	 * connects to the context service and starts the smart sockets manager
	 */
	private void start() {
		if (!isRunning) {

			// create the device manager
			this.deviceManager = new DeviceManager(this);

			// start the smart socket manager
			this.smartSocketsManager = new SmartSocketsManager(
					this.deviceManager.getExternalLocalDeviceId(),
					receiveListener);
			this.smartSocketsManager.start();

			// initialize the context manager
			this.contextManager = new ContextManager(this);
			this.contextManager.start();

			this.isRunning = true;
		}
	}

	/**
	 * disconnects from the context service and stops the smart socket manager
	 */
	private void stop() {
		if (isRunning) {
			this.contextManager.stop();
			this.contextManager = null;

			if (this.smartSocketsManager != null)
				this.smartSocketsManager.stop();
			this.smartSocketsManager = null;
			this.deviceManager = null;

			isRunning = false;
		}
	}

	/** The remote interface. */
	private final IRemoteContextService.Stub mBinder = new IRemoteContextService.Stub() {

		@Override
		public void start() throws RemoteException {
			start();
		}

		@Override
		public void stop() throws RemoteException {
			stop();
		}

		@Override
		public SwanServiceException registerContextTypedValue(final String id,
				final ContextTypedValue contextTypedValue)
				throws RemoteException {
			synchronized (localContextTypedValues) {
				// get the ids for the value
				String remoteDeviceId = contextTypedValue.getDeviceId();
				String remoteContextTypedValueId = remoteDeviceId + "_" + id;

				// add the value to the list of registered values
				localContextTypedValues.put(id, contextTypedValue);

				// send a message to the remote device to register the value
				RegisterContextTypedValueMessage message = new RegisterContextTypedValueMessage();
				message.id = remoteContextTypedValueId;
				message.value = contextTypedValue;

				Collection<Object> messages = new ArrayList<Object>();
				messages.add(message);

				smartSocketsManager.send(remoteDeviceId, messages);
			}

			// no exception so return null
			return null;
		}

		@Override
		public SwanServiceException unregisterContextTypedValue(final String id)
				throws RemoteException {
			synchronized (localContextTypedValues) {
				if (localContextTypedValues.containsKey(id)) {
					ContextTypedValue contextTypedValue = localContextTypedValues
							.get(id);
					String remoteDeviceId = contextTypedValue.getDeviceId();
					String remoteContextTypedId = remoteDeviceId + "_" + id;

					// create a message to send to the remote device
					UnregisterContextTypedValueMessage message = new UnregisterContextTypedValueMessage();
					message.id = remoteContextTypedId;

					Collection<Object> messages = new ArrayList<Object>();
					messages.add(message);

					// send messages to the remote device
					smartSocketsManager.send(remoteDeviceId, messages);

					// remove context typed value from the list of registered
					// values
					localContextTypedValues.remove(id);
				}
			}
			// no exception so return null
			return null;

		}
	};

	/**
	 * The listener to pass to the SmartSocketsManager in order to be notified
	 * of the incoming messages
	 */
	private final RemoteContextServiceReceiveListener receiveListener = new RemoteContextServiceReceiveListener() {

		@Override
		public void onReceive(String deviceId, Collection<Object> messages) {

			// TODO process the results
		}
	};
}
