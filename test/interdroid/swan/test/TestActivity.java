package interdroid.swan.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import interdroid.swan.SwanException;
import interdroid.swan.ContextManager;
import interdroid.swan.R;
import interdroid.swan.contextexpressions.ContextTypedValue;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;

public class TestActivity extends Activity {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(TestActivity.class);

	private ContextManager contextManager;

	@Override
	protected void onResume() {
		contextManager.start();
		super.onResume();
	}

	@Override
	protected void onPause() {
		LOG.debug("unbind context service from app: " + getClass());
		contextManager.stop();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		try {
			contextManager.destroy();
		} catch (SwanException e) {
			e.printStackTrace();
		}
		super.onDestroy();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		final ContextTypedValue left = new ContextTypedValue("location",
				"latitude", "remote_device");
		final String valueName = "custom_value";
		contextManager = new ContextManager(TestActivity.this);
		
		findViewById(R.id.register).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						LOG.debug("registering expression");
						try {
							contextManager.registerContextTypedValue(valueName,
									left, null);
						} catch (SwanException e) {
							e.printStackTrace();
						}
					}
				});

		findViewById(R.id.unregister).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						LOG.debug("unregistering expression");
						try {
							contextManager
									.unregisterContextTypedValue(valueName);
						} catch (SwanException e) {
							e.printStackTrace();
						}
					}
				});
		findViewById(R.id.shutdown).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						contextManager.shutdown();
					}
				});

	}
}
