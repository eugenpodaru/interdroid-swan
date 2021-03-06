package interdroid.swan.sensors;

import interdroid.swan.R;
import interdroid.swan.contextexpressions.ContextTypedValue;
import interdroid.swan.contextexpressions.HistoryReductionMode;
import interdroid.swan.contextexpressions.TypedValueExpression;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;

/**
 * Base for ConfigurationActivities for configuring sensors.
 * 
 * @author nick &lt;palmer@cs.vu.nl&gt;
 * 
 */
public abstract class AbstractConfigurationActivity extends PreferenceActivity
		implements OnPreferenceChangeListener {
	/**
	 * Access to logger.
	 */
	private static final Logger LOG = LoggerFactory
			.getLogger(AbstractConfigurationActivity.class);

	/**
	 * Returns the id for the sensors preferences XML setup.
	 * 
	 * @return the id for the preferences XML
	 */
	public abstract int getPreferencesXML();

	private List<String> keys = new ArrayList<String>();

	// Android includes lifecycle checks ensuring super.onCreate() was called.
	// CHECKSTYLE:OFF
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.default_preferences);
		addPreferencesFromResource(getPreferencesXML());
		setupPrefs();
		setResult(RESULT_CANCELED);
	}

	// CHECKSTYLE:ON

	/**
	 * Sets up this activity.
	 */
	private void setupPrefs() {
		setupPref(getPreferenceScreen());
	}

	/**
	 * Sets up using the given preferences.
	 * 
	 * @param preference
	 *            the preferences for the sensor.
	 */
	private void setupPref(final Preference preference) {
		if (preference instanceof PreferenceGroup) {
			for (int i = 0; i < ((PreferenceGroup) preference)
					.getPreferenceCount(); i++) {
				// setup all sub prefs
				setupPref(((PreferenceGroup) preference).getPreference(i));
			}
		} else {
			keys.add(preference.getKey());
			// setup the listener
			preference.setOnPreferenceChangeListener(this);
			// set the summary
			String summary = null;
			if (preference instanceof ListPreference) {
				try {
					summary = ((ListPreference) preference).getValue()
							.toString();
				} catch (NullPointerException e) {
					LOG.warn("Got null pointer while getting summary.", e);
				}
			} else if (preference instanceof EditTextPreference) {
				summary = ((EditTextPreference) preference).getText();
			}
			if (summary != null) {
				preference.setSummary(summary);
			}

			if (preference instanceof ListPreference) {
				if (((ListPreference) preference).getEntries().length == 1) {
					preference.setEnabled(false);
				}
				((ListPreference) preference)
						.setValue(((ListPreference) preference)
								.getEntryValues()[0].toString());
				preference.setSummary(((ListPreference) preference)
						.getEntries()[0]);
			}

		}
	}

	@Override
	public final void onBackPressed() {
		setResult(RESULT_OK,
				getIntent()
						.putExtra("Expression", prefsToConfigurationString()));
		finish();
	}

	/**
	 * Converts the prefs to a parseable configuration string.
	 * 
	 * @return the prefs as a string.
	 */
	private String prefsToConfigurationString() {
		Map<String, ?> map = PreferenceManager.getDefaultSharedPreferences(
				getBaseContext()).getAll();

		String path = map.remove("valuepath").toString();
		HistoryReductionMode mode = HistoryReductionMode.parse(map.remove(
				"history_reduction_mode").toString());
		long timespan = Long.parseLong(map.remove("history_window").toString());
		String entityId = getIntent().getStringExtra("entityId");

		Map<String, String> stringMap = new HashMap<String, String>();
		for (String key : keys) {
			if (map.containsKey(key)) {
				stringMap.put(key, map.get(key).toString());
			}
		}

		ContextTypedValue sensor = new ContextTypedValue(entityId, path,
				stringMap, mode, timespan);

		TypedValueExpression typedValueExpression = new TypedValueExpression(
				sensor);
		return typedValueExpression.toParseString();
	}

	@Override
	public final boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		if (preference instanceof ListPreference) {
			for (int i = 0; i < ((ListPreference) preference).getEntryValues().length; i++) {
				if (((ListPreference) preference).getEntryValues()[i]
						.toString().equals(newValue.toString())) {
					preference.setSummary(((ListPreference) preference)
							.getEntries()[i]);
					return true;
				}
			}

		} else {
			preference.setSummary(newValue.toString());
		}
		return true;
	}

}
