package interdroid.swan.remote;

import ibis.smartsockets.SmartSocketsProperties;
import ibis.smartsockets.naming.NameResolver;
import ibis.smartsockets.virtual.InitializationException;
import ibis.smartsockets.virtual.VirtualServerSocket;
import ibis.smartsockets.virtual.VirtualSocket;
import ibis.smartsockets.virtual.VirtualSocketAddress;
import ibis.smartsockets.virtual.VirtualSocketFactory;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SmartSocketsManager {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(RemoteContextService.class);
	/**
	 * The properties for the smart sockets subsystem.
	 */
	private static Properties sSocketProperties = new Properties();
	static {

		// TODO: put effective address here
		// run a hub locally and add the ip of the hub here

		// TODO: use the naming script in the bin folder of the smartsockets
		// project to start a hub locally
		// TODO: use the viz script to visualize the smartsockets overlay
		sSocketProperties.put(SmartSocketsProperties.HUB_ADDRESSES, "");
		sSocketProperties.put(SmartSocketsProperties.DIRECT_CACHE_IP, "false");
		sSocketProperties.put(SmartSocketsProperties.START_HUB, "true");
	}

	/** The scheme used by this transport. */
	public static final String SMARTSOCKETS_TRANSPORT_SCHEME = "ss";

	/** The port used to listen for incoming connections */
	public static final int DEFAULT_PORT = 12345;

	/** TCP socket backlog. */
	private static final int BACKLOG = 5;

	/** The timeout used when trying to connect to a remote device */
	private static final int SOCKET_TIMEOUT = 5000;

	/**
	 * The id that is used to register the device to the smart sockets resolver
	 */
	String mLocalDeviceId;

	/**
	 * The listener for receive events
	 */
	RemoteContextServiceReceiveListener receiveListener;

	/**
	 * The listen socket.
	 */
	private VirtualServerSocket mListenSock;
	/**
	 * The factory we are using to build sockets.
	 */
	private VirtualSocketFactory mSocketFactory;

	/** True if we are running. */
	private boolean mRun;

	/** The thread pool used to run the threads handling connections */
	private ExecutorService mClientThreadPool;

	/** The thread accepting connections. */
	private Thread mAcceptThread;

	public SmartSocketsManager(String externalLocalDeviceId,
			RemoteContextServiceReceiveListener receiveListener) {
		this.mLocalDeviceId = externalLocalDeviceId;
		this.receiveListener = receiveListener;
	}

	/**
	 * Starts the SmartSocketManager
	 */
	public void start() {
		try {
			LOG.info("Starting the SmartSocketsManager.");

			// get the socket factory from the resolver
			mSocketFactory = this.getResolver().getSocketFactory();
			LOG.debug("Socket Factory has serviceLink: {}",
					mSocketFactory.getServiceLink());

			// create a server socket to listen for incoming connections
			mListenSock = mSocketFactory.createServerSocket(DEFAULT_PORT,
					BACKLOG, null);
			if (mListenSock == null) {
				throw new InitializationException(
						"Unable to construct server socket.");
			}

			// register the local device id with the listening socket
			this.getResolver().register(this.mLocalDeviceId,
					mListenSock.getLocalSocketAddress(), null);

			mClientThreadPool = Executors.newCachedThreadPool();

			mRun = true;
			mAcceptThread = new Thread("Smart-Sockets-Manager-Accept") {
				public void run() {
					while (isRunning()) {
						try {
							startClient(mListenSock.accept());
						} catch (IOException e) {
							break;
						}
					}

					try {
						LOG.debug("Closing listen socket.");
						mListenSock.close();
						LOG.debug("Listen socket closed.");
					} catch (IOException err) {
						LOG.warn("Ignored while closing socket: ", err);
					}
				}
			};

			// start the listening thread
			mAcceptThread.start();
		} catch (InitializationException e) {
			LOG.warn("SmartSocketsManager.start():", e);
		} catch (IOException e) {
			LOG.warn("SmartSocketsManager.start():", e);
		}
	}

	/**
	 * Stops the SmartSocketsManager
	 */
	public void stop() {
		LOG.info("Stopping the SmartSocketsManager.");

		// close the name resolver
		NameResolver.closeAllResolvers();

		// close the accepting thread, so no new clients can connect
		if (mAcceptThread != null) {
			synchronized (this) {
				mRun = false;
				try {
					LOG.debug("Closing accept socket.");
					mListenSock.close();
					mAcceptThread.interrupt();
				} catch (Exception e) {
					LOG.warn("Ignored while closing accept socket.", e);
				}
			}
			LOG.debug("Joining accept thread.");
			try {
				mAcceptThread.join();
			} catch (InterruptedException e) {
				LOG.warn("Ignored while joining accept thread.", e);
			}
			LOG.debug("Joined accept thread.");
			mAcceptThread = null;
		}

		// close the thread pool for the connected clients
		if (mClientThreadPool != null) {
			mClientThreadPool.shutdown();
			mClientThreadPool = null;
		}
	}

	/**
	 * Start a client handler on the given socket.
	 * 
	 * @param virtualSocket
	 *            the virtual socket for this client.
	 */
	private void startClient(final VirtualSocket virtualSocket) {
		LOG.debug("Starting client for: {}", virtualSocket);

		mClientThreadPool.execute(new Runnable() {
			@Override
			public void run() {
				try {
					// get the peers address
					final SocketAddress peer = virtualSocket
							.getRemoteSocketAddress();

					// get the input stream for the socket
					InputStream in = virtualSocket.getInputStream();

					// create an ObjectInputStream to deserialize the objects
					ObjectInputStream oin = new ObjectInputStream(in);

					// the first object should be a string with the id of the
					// remote device
					String remoteDeviceId = (String) oin.readObject();

					// create an array to hold all the objects
					ArrayList<Object> results = new ArrayList<Object>();
					LOG.debug(remoteDeviceId + " :Start reading objects");
					try {
						while (true) {
							results.add(oin.readObject());
						}
					} catch (EOFException e) {
						LOG.debug(remoteDeviceId + " : " + results.size()
								+ " objects read");
					}

					// close the input streams
					oin.close();
					in.close();

					// call the receive listener
					if (receiveListener != null)
						receiveListener.onReceive(remoteDeviceId, results);

					// pass the results to the remote context service
				} catch (Exception e) {
					LOG.warn("Exception while servicing client ignored.", e);
				} finally {
					LOG.debug("Closing streams");
					try {
						virtualSocket.getInputStream().close();
					} catch (IOException e) {
						LOG.warn("Exception while closing input stream: ", e);
					}
					try {
						virtualSocket.getOutputStream().close();
					} catch (IOException e) {
						LOG.warn("Exception while closing output stream: ", e);
					}
					LOG.debug("Closing socket.");
					try {
						virtualSocket.close();
					} catch (IOException e) {
						LOG.warn("Ignored while closing socket", e);
					}
				}
				LOG.debug("Thread is complete.");

			}
		});

		LOG.debug("Launched client thread.");
	}

	/**
	 * sends a list of messages to the remote device
	 * 
	 * @param deviceId
	 * @param messages
	 */
	public void send(String deviceId, Collection<Object> messages) {
		try {
			// get the remote peer address
			VirtualSocketAddress peerAddress = this.getResolver().resolve(
					deviceId);

			LOG.debug("Trying to create a client socket to the peer: "
					+ deviceId);
			
			// create a socket to connect to the remote peer
			VirtualSocket clientSocket = mSocketFactory.createClientSocket(
					peerAddress, SOCKET_TIMEOUT, null);
			LOG.debug("Client socket created for the peer: " + deviceId);

			// get the output stream for connection
			OutputStream out = clientSocket.getOutputStream();
			ObjectOutputStream oout = new ObjectOutputStream(out);

			// send the device id as the first object on the wire
			oout.writeObject(deviceId);

			LOG.debug("Writing the objects to the output stream.");
			// write all the messages to the output stream
			for (Object message : messages) {
				oout.writeObject(message);
			}
			LOG.debug("All objects were written.");

			// close the streams
			oout.close();
			out.close();

			// close the connection
			clientSocket.close();
		} catch (InitializationException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/** @return true if the manager is receiving connections. */
	public final synchronized boolean isRunning() {
		return mRun;
	}

	/**
	 * @return the name resolver used by the transport
	 * @throws InitializationException
	 *             if the name resolver fails to initialize
	 */
	public NameResolver getResolver() throws InitializationException {
		return NameResolver.getOrCreateResolver(SMARTSOCKETS_TRANSPORT_SCHEME,
				sSocketProperties, true);
	}
}
