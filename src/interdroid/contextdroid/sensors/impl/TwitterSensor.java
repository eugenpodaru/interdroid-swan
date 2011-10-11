package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.sensors.AbstractAsynchronousSensor;
import interdroid.contextdroid.contextexpressions.TimestampedValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import android.os.Bundle;

/**
 * Based on the original TwitterSensor written by Rick de Leeuw
 *
 * @author rkemp
 */
public class TwitterSensor extends AbstractAsynchronousSensor {

	public static final String TAG = "Twitter";

	public static final String NR_MENTIONS_FIELD = "mentions";

	public static final String SAMPLE_INTERVAL = "sample_interval";
	public static final String ACCESS_TOKEN = "access_token";
	public static final String SECRET_ACCESS_TOKEN = "secret_access_token";

	public static final long DEFAULT_SAMPLE_INTERVAL = 5 * 60 * 1000;

	protected static final int HISTORY_SIZE = 10;
	public static final long EXPIRE_TIME = 5 * 60 * 1000;

	// Buienradar specific variables
	private static final String API_KEY = "uKfdGc8DLZ7FBnZAePGg";
	private static final String API_SECRET = "NeDCR2mdMGJVeaBz7wL0R5V3rX7QviOb9IsIFpByY";
	private static final String MENTION_URL = "http://api.twitter.com/1/statuses/mentions.xml?trim_user=1";

	private static final OAuthService OAUTH_SERVICE = new ServiceBuilder()
			.provider(TwitterApi.class).apiKey(API_KEY).apiSecret(API_SECRET)
			.build();

	private Map<String, TwitterPoller> activeThreads = new HashMap<String, TwitterPoller>();

	private int sampleTwitter(Token accessToken) {
		OAuthRequest request = new OAuthRequest(Verb.GET, MENTION_URL);
		OAUTH_SERVICE.signRequest(accessToken, request); // the access token
															// from step
		// 4
		Response response = request.send();
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			Document doc = factory.newDocumentBuilder().parse(
					response.getBody());
			NodeList mentions = doc.getElementsByTagName("status");
			return mentions.getLength();
		} catch (Exception e) {
			return -1;
		}

	}

	public void onDestroy() {
		for (TwitterPoller twitterPoller : activeThreads.values()) {
			twitterPoller.interrupt();
		}
		super.onDestroy();
	}

	@Override
	public String[] getValuePaths() {
		return new String[] { NR_MENTIONS_FIELD };
	}

	@Override
	public void initDefaultConfiguration(Bundle DEFAULT_CONFIGURATION) {
		DEFAULT_CONFIGURATION.putLong(SAMPLE_INTERVAL, DEFAULT_SAMPLE_INTERVAL);
	}

	@Override
	public String getScheme() {
		return "{'type': 'record', 'name': 'rain', 'namespace': 'context.sensor',"
				+ " 'fields': ["
				+ "            {'name': '"
				+ NR_MENTIONS_FIELD
				+ "', 'type': 'integer'}"
				+ "           ]"
				+ "}".replace('\'', '"');
	}

	@Override
	public void onConnected() {
	}

	@Override
	protected void register(String id, String valuePath, Bundle configuration) {
		TwitterPoller twitterPoller = new TwitterPoller(id, valuePath,
				configuration);
		activeThreads.put(id, twitterPoller);
		twitterPoller.start();
	}

	@Override
	protected void unregister(String id) {
		activeThreads.remove(id).interrupt();
	}

	@Override
	protected List<TimestampedValue> getValues(String id, long now,
			long timespan) {
		return getValuesForTimeSpan(activeThreads.get(id).getValues(), now,
				timespan);
	}

	class TwitterPoller extends Thread {

		private Bundle configuration;
		private List<TimestampedValue> values = new ArrayList<TimestampedValue>();
		private String id;

		TwitterPoller(String id, String valuePath, Bundle configuration) {
			this.configuration = configuration;
			this.id = id;
		}

		public void run() {
			Token token = new Token(configuration.getString(ACCESS_TOKEN),
					configuration.getString(SECRET_ACCESS_TOKEN));
			while (!isInterrupted()) {
				long start = System.currentTimeMillis();
				if (values.size() >= HISTORY_SIZE) {
					values.remove(0);
				}
				int mentions = sampleTwitter(token);
				if (mentions >= 0) {
					values.add(new TimestampedValue(mentions, start, start
							+ EXPIRE_TIME));
					notifyDataChangedForId(id);
				}

				try {
					Thread.sleep(Math.max(
							0,
							configuration.getLong(SAMPLE_INTERVAL,
									DEFAULT_CONFIGURATION
											.getLong(SAMPLE_INTERVAL))
									+ start - System.currentTimeMillis()));
				} catch (InterruptedException e) {
				}
			}
		}

		public List<TimestampedValue> getValues() {
			return values;
		}
	};

}
