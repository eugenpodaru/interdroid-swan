package interdroid.contextdroid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.contextdroid.contextservice.IContextService;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * The Class ContextServiceConnector.
 */
public abstract class ContextServiceConnector {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG =
			LoggerFactory.getLogger(ContextServiceConnector.class);


	/** The ContextService intent action. */
	public static final String CONTEXT_SERVICE = "interdroid.contextdroid.intent.CONTEXTSERVICE";

	/** The context service interface. */
	protected IContextService contextService;

	/** The context of the application using the Context Service Connector. */
	Context context;

	/** The context manager listener. */
	private ConnectionListener connectionListener;

	/** Is the context service connected? */
	boolean isConnected = false;

	/**
	 * Checks if the context manager is connected to the context service.
	 *
	 * @return true, if connected
	 */
	public boolean isConnected() {
		return isConnected;
	}

	public void start() {
		start(null);
	}

	/**
	 * Non-blocking start.
	 *
	 * Should always be called before any other calls to the ContextManager.
	 * This method makes sure the Context Service is running and sets up the
	 * listeners required to receive updates from the Context Service.
	 *
	 * You will generally want to call this method from an Activity's onResume()
	 * method.
	 *
	 * @param connectionListener
	 *            the connectionListener object to call onConnected() on after
	 *            the context manager has been initialized.
	 */
	public void start(ConnectionListener connectionListener) {
		this.connectionListener = connectionListener;
		context.bindService(new Intent(CONTEXT_SERVICE), serviceConnection,
				Service.BIND_AUTO_CREATE);
	}

	/**
	 * Unbind the connection to the Context Service and unregister receivers.
	 * You will generally want to call this method from an Activity's onPause()
	 * method to prevent leaked bindings
	 */
	public void stop() {
		context.unbindService(serviceConnection);
		isConnected = false;
	}

	public void shutdown() {
		try {
			contextService.shutdown();
		} catch (RemoteException e) {
			// ignore
		}
	}

	/** The service connection to the ContextService. */
	private ServiceConnection serviceConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName name, IBinder service) {
			LOG.debug("service connected: {}", name);
			contextService = IContextService.Stub.asInterface(service);
			isConnected = true;
			if (connectionListener != null) {
				connectionListener.onConnected();
			}
		}

		public void onServiceDisconnected(final ComponentName name) {
			LOG.debug("service disconnected");
			contextService = null;
			isConnected = false;
			if (connectionListener != null) {
				connectionListener.onDisconnected();
			}
		}
	};

	/**
	 * Instantiates a new context service connector.
	 *
	 * @param context
	 *            the application context
	 */
	public ContextServiceConnector(final Context context) {
		this.context = context;
	}

}
