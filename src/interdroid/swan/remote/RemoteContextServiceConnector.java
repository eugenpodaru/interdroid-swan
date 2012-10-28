package interdroid.swan.remote;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class RemoteContextServiceConnector {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteContextService.class);

	/** The RemoteContextService intent action. */
	public static final String REMOTE_CONTEXT_SERVICE = "interdroid.swan.intent.REMOTECONTEXTSERVICE";

	/** The remote context service interface. */
	private IRemoteContextService remoteContextService;

	/** The context that the connector uses to bind the service */
	private Context context;

	/** Is the remote context service connected? */
	private boolean isConnected = false;

	/** The service connection to the RemoteContextService. */
	private final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(final ComponentName name,
				final IBinder service) {
			LOG.debug("service connected: {}", name);
			setRemoteContextService(IRemoteContextService.Stub
					.asInterface(service));
		}

		@Override
		public void onServiceDisconnected(final ComponentName name) {
			LOG.debug("service disconnected");
			setRemoteContextService(null);
		}
	};

	/**
	 * 
	 * @param context
	 */
	public RemoteContextServiceConnector(Context context) {
		this.setContext(context);
	}

	/**
	 * Checks if the context manager is connected to the context service.
	 * 
	 * @return true, if connected
	 */
	public final boolean isConnected() {
		return isConnected;
	}

	/**
	 * connects to the remote context service
	 * 
	 * @return
	 */
	public void start() {
		isConnected = this.getContext().bindService(
				new Intent(REMOTE_CONTEXT_SERVICE), serviceConnection,
				Service.BIND_AUTO_CREATE);
	}

	/**
	 * disconnects from the remote context service
	 */
	public void stop() {
		this.getContext().unbindService(serviceConnection);
		this.isConnected = false;
	}

	/**
	 * @return the remoteContextService
	 */
	public IRemoteContextService getRemoteContextService() {
		return remoteContextService;
	}

	/**
	 * @param service
	 *            the contextService to set
	 */
	private void setRemoteContextService(final IRemoteContextService service) {
		this.remoteContextService = service;
	}

	/**
	 * gets the context that this class uses to bind to the remote context
	 * service
	 * 
	 * @return
	 */
	private Context getContext() {
		return context;
	}

	/**
	 * sets the context to the provided value
	 * 
	 * @param context
	 */
	private void setContext(final Context context) {
		this.context = context;
	}
}
