package interdroid.swan.remote;

import interdroid.swan.ContextManager;
import interdroid.swan.ContextTypedValueListener;
import interdroid.swan.SwanException;
import interdroid.swan.contextexpressions.ContextExpressionParser;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.TimestampedValue;
import interdroid.swan.contextservice.SwanServiceException;
import interdroid.swan.remote.messages.ContextTypedValueNewReadingMessage;
import interdroid.swan.remote.messages.Message;
import interdroid.swan.remote.messages.RegisterContextTypedValueMessage;
import interdroid.swan.remote.messages.UnregisterContextTypedValueMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The Class RemoteContextService.
 */
public class RemoteContextService extends Service {
	/**
	 * The broadcast values action
	 */
	public static String SEND_VALUES_ACTION = "interdroid.swan.remote.RemoteContextService.SEND_VALUES_ACTION";
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteContextService.class);

	/** The access control manager */
	private AccessControlManager accessControlManager;

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

	/**
	 * Handles boot notifications
	 * 
	 * @author eugen
	 * 
	 */
	public static class BootHandler extends BroadcastReceiver {

		@Override
		public final void onReceive(final Context context, final Intent intent) {
			LOG.debug("Got boot notification!");
			context.startService(new Intent(context, RemoteContextService.class));
			LOG.debug("Finished handling boot.");
		}
	}

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

		//restart the service
		if(isRunning)
		{
			try {
				stop();
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		start();

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

		super.onCreate();
	}

	@Override
	public final void onDestroy() {
		LOG.debug("RemoteContextService:onDestroy");

		stop();

		super.onDestroy();
	}

	/**
	 * connects to the context service and starts the smart sockets manager
	 */
	private void start() {
		if (!isRunning) {

			// create the access control manager
			accessControlManager = new AccessControlManager(this);
			// create the device manager
			deviceManager = new DeviceManager(this);

			// start the smart socket manager
			smartSocketsManager = new SmartSocketsManager(
					deviceManager.getExternalLocalDeviceId(),
					receiveListener);
			smartSocketsManager.start();

			// initialize the context manager
			contextManager = new ContextManager(this);
			contextManager.start();

			isRunning = true;
		}
	}

	/**
	 * disconnects from the context service and stops the smart socket manager
	 */
	private void stop() {
		if (isRunning) {
			contextManager.stop();
			contextManager = null;

			if (smartSocketsManager != null)
				smartSocketsManager.stop();
			smartSocketsManager = null;
			deviceManager = null;

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

				// add the value to the list of registered values
				localContextTypedValues.put(id, contextTypedValue);

				// send a message to the remote device to register the value
				RegisterContextTypedValueMessage message = new RegisterContextTypedValueMessage();
				message.id = id;
				message.contextTypedValue = contextTypedValue.toParseString();

				ArrayList<Message> messages = new ArrayList<Message>();
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

					// create a message to send to the remote device
					UnregisterContextTypedValueMessage message = new UnregisterContextTypedValueMessage();
					message.id = id;

					ArrayList<Message> messages = new ArrayList<Message>();
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
		public void onReceive(final String deviceId, final Collection<Message> messages) {
			//go through all the received messages
			for(Message message : messages)
			{
				if(message == null)
					continue;

				if(message instanceof RegisterContextTypedValueMessage)
				{
					RegisterContextTypedValueMessage(deviceId, (RegisterContextTypedValueMessage)message);
				}
				else if(message instanceof UnregisterContextTypedValueMessage)
				{
					UnregisterContextTypedValueMessage(deviceId, (UnregisterContextTypedValueMessage)message);
				}
				else if(message instanceof ContextTypedValueNewReadingMessage)
				{
					ContextTypedValueNewReadingMessage(deviceId, (ContextTypedValueNewReadingMessage)message);
				}
			}
		}

		private void ContextTypedValueNewReadingMessage(final String deviceId,
				final ContextTypedValueNewReadingMessage message) {

			String id = message.id;

			ArrayList<TimestampedValue> values = new ArrayList<TimestampedValue>();
			for(int i=0;i<message.values.length;i++)
				values.add(message.values[i]);

			ContextTypedValue value = localContextTypedValues.get(id);
			if(value == null)
				return;

			Intent broadcastIntent = new Intent();
			broadcastIntent.setAction(SEND_VALUES_ACTION);
			broadcastIntent.putExtra("id", id);
			broadcastIntent.putExtra("values", values);
			sendBroadcast(broadcastIntent);
		}

		private void UnregisterContextTypedValueMessage(final String deviceId,
				final UnregisterContextTypedValueMessage message) {
			try {
				final String localId = deviceId + "_" + message.id;
				contextManager.unregisterContextTypedValue(localId);
			} catch (SwanException e) {
				e.printStackTrace();
			}
		}

		private void RegisterContextTypedValueMessage(final String deviceId,
				final RegisterContextTypedValueMessage message) {

			try {
				final String remoteId = message.id;
				final String localId = deviceId + "_" + remoteId;

				ContextTypedValue contextTypedValue = (ContextTypedValue)ContextExpressionParser.parseTypedValue(message.contextTypedValue);
				//make sure the value has the local device id
				contextTypedValue.setDeviceId(DeviceManager.LOCAL_DEVICE_ID);

				if(!accessControlManager.hasAccess(deviceId, contextTypedValue.getEntity()))
					//TODO: add an access request for the specified device and sensor
					return;

				contextManager.registerContextTypedValue(localId, contextTypedValue, new ContextTypedValueListener() {

					@Override
					public void onReading(String id, TimestampedValue[] values) {

						// create a message to send to the remote device
						ContextTypedValueNewReadingMessage newMessage = new ContextTypedValueNewReadingMessage();
						newMessage.id = remoteId;
						newMessage.values = values;

						ArrayList<Message> newMessages = new ArrayList<Message>();
						newMessages.add(newMessage);

						// send messages to the remote device
						smartSocketsManager.send(deviceId, newMessages);
					}
				});
			} catch (SwanException e) {
				e.printStackTrace();
			}
		}
	};

	public final static class ConnectivityChangedBroadcastReceiver extends BroadcastReceiver 
	{
		/**
		 * Access to logger. Connectivity
		 */
		private static final Logger LOG = LoggerFactory
				.getLogger(RemoteContextService.class);

		@Override
		public void onReceive(Context context, Intent intent) {

			LOG.debug("ConnectivityChangedBroadcastReceiver:onReceive");

			context.startService(new Intent(context, RemoteContextService.class));
		}
	};
}
