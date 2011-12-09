package interdroid.contextdroid.ui;

import interdroid.contextdroid.ContextManager;
import interdroid.contextdroid.R;
import interdroid.contextdroid.SensorServiceInfo;
import interdroid.contextdroid.contextexpressions.ContextTypedValue;
import interdroid.contextdroid.contextexpressions.TypedValueExpression;

import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class SelectSensorDialog extends Activity {

	List<SensorServiceInfo> sensors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		sensors = ContextManager.getSensors(this);

		setContentView(R.layout.expression_builder_select_sensor_dialog);

		((ListView) findViewById(R.id.sensor_list))
				.setAdapter(new ArrayAdapter<SensorServiceInfo>(this,
						android.R.layout.simple_list_item_1, sensors));

		((ListView) findViewById(R.id.sensor_list))
				.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> parent, View view,
							int position, long id) {
						startActivityForResult(sensors.get(position)
								.getConfigurationIntent(), position);
					}
				});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			Intent result = new Intent();
			result.putExtra("Expression", new TypedValueExpression(
					new ContextTypedValue(sensors.get(requestCode).getEntity()
							+ ContextTypedValue.ENTITY_VALUE_PATH_SEPARATOR
							+ data.getStringExtra("configuration")))
					.toParseString());
			setResult(RESULT_OK, result);
			finish();
		}
	}

}
