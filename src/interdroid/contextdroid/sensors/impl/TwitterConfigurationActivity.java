package interdroid.contextdroid.sensors.impl;

import interdroid.contextdroid.R;
import interdroid.contextdroid.sensors.AbstractConfigurationActivity;

public class TwitterConfigurationActivity extends
		AbstractConfigurationActivity {

	@Override
	public int getPreferencesXML() {
		return R.xml.twitter_preferences;
	}

}
