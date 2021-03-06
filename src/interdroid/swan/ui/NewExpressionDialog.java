package interdroid.swan.ui;

import interdroid.swan.R;
import interdroid.swan.contextexpressions.Comparator;
import interdroid.swan.contextexpressions.ComparisonExpression;
import interdroid.swan.contextexpressions.Expression;
import interdroid.swan.contextexpressions.ExpressionParseException;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class NewExpressionDialog extends Activity {

	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int COMPARATOR = 3;

	boolean leftActive = false;
	boolean rightActive = false;
	boolean comparatorActive = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);

		setContentView(R.layout.expression_builder_new_dialog);

		findViewById(R.id.left).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getApplicationContext(),
						SelectTypedValueDialog.class), LEFT);
			}
		});

		findViewById(R.id.right).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				startActivityForResult(new Intent(getApplicationContext(),
						SelectTypedValueDialog.class), RIGHT);
			}
		});

		findViewById(R.id.comparator).setOnClickListener(
				new View.OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivityForResult(new Intent(
								getApplicationContext(),
								SelectComparatorDialog.class), COMPARATOR);
					}
				});

		findViewById(R.id.ok).setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				// check whether we have a left and a right
				// value and an operator, create a new
				// comparator expression out of it, then add
				// it to the list of expressions.
				try {
					Expression left = Expression
							.parse(((Button) findViewById(R.id.left)).getText()
									.toString());
					Expression right = Expression
							.parse(((Button) findViewById(R.id.right))
									.getText().toString());
					Comparator comparator = Comparator
							.parse(((Button) findViewById(R.id.comparator))
									.getText().toString());
					// String name = ((EditText) findViewById(R.id.name))
					// .getText().toString();
					Expression newExpression = new ComparisonExpression(left,
							comparator, right);
					Intent result = new Intent();
					result.putExtra("Expression", newExpression.toParseString());
					// if (name != null && !name.equals("")) {
					// result.putExtra("name", name);
					// }
					setResult(RESULT_OK, result);
					finish();
				} catch (ExpressionParseException e) {
					// TODO: improve this
					Toast.makeText(getApplicationContext(), "Failed!",
							Toast.LENGTH_LONG).show();
				}

			}
		});

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case LEFT:
				((Button) findViewById(R.id.left)).setText(data
						.getStringExtra("Expression"));
				leftActive = true;
				break;
			case RIGHT:
				((Button) findViewById(R.id.right)).setText(data
						.getStringExtra("Expression"));
				rightActive = true;
				break;
			case COMPARATOR:
				((Button) findViewById(R.id.comparator)).setText(data
						.getStringExtra("Comparator"));
				comparatorActive = true;
				break;
			default:
				break;
			}
			findViewById(R.id.ok).setEnabled(
					leftActive && rightActive && comparatorActive);
		}
	}

}
