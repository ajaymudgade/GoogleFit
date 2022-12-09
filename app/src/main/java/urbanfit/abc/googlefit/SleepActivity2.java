package urbanfit.abc.googlefit;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataSource;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SleepActivity2 extends AppCompatActivity {
    private static final int REQUEST_OAUTH_REQUEST_CODE = 111111;
    private static final String TAG = "SleepActivity2";
    private TextView sleepCounter;
    private TextView sleepWeekCounter;

    static DataSource ESTIMATED_SLEEP_DATA = new DataSource.Builder()
            .setDataType(DataType.TYPE_SLEEP_SEGMENT)
            .setType(DataSource.TYPE_RAW)
            .setStreamName("estimated_sleep")
            .setAppPackageName("com.google.android.gms")
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sleep_xml);


        // Initialise JodaTime
        JodaTimeAndroid.init(this);

        sleepCounter = findViewById(R.id.sleepCounter);
        sleepWeekCounter = findViewById(R.id.sleep_week_counter);

        if (hasFitPermission()) {
            readSleepCountDelta();
            readHistoricSleepCount();
        } else {
            requestFitnessPermission();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sleep_main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.sleep_action_revoke) {
            revokeFitnessPermissions();
        }
        if (id == R.id.sleep_action_read_data) {
            readSleepCountDelta();
            return true;
        } else if (id == R.id.sleep_action_read_historic_data) {
            readHistoricSleepCount();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void revokeFitnessPermissions() {
        if (!hasFitPermission()) {
            // No need to revoke if we don't already have permissions
            return;
        }

        // Stop recording the sleep count
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_SLEEP_SEGMENT);

        // Revoke Fitness permissions
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder().addExtension(getFitnessSignInOptions()).build();
        GoogleSignIn.getClient(this, signInOptions).revokeAccess();

        Toast.makeText(this, "Fitness permissions revoked", Toast.LENGTH_SHORT).show();
    }

    private void requestFitnessPermission() {
        GoogleSignIn.requestPermissions(
                this,
                REQUEST_OAUTH_REQUEST_CODE,
                GoogleSignIn.getLastSignedInAccount(this),
                getFitnessSignInOptions());

    }

    private void readHistoricSleepCount() {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        // Invoke the History API to fetch the data with the query
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(queryFitnessSleepData())
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                // For the sake of the sample, we'll print the data so we can see what we just
                                // added. In general, logging fitness information should be avoided for privacy
                                // reasons.
                                printSleepData(dataReadResponse);
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "There was a problem reading the historic data.", e);
                            }
                        });
    }

    private void readSleepCountDelta() {

        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.AGGREGATE_ACTIVITY_SUMMARY)
                .addOnSuccessListener(
                        new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                long total =
                                        dataSet.isEmpty()
                                                ? 0
                                                : (long) dataSet.getDataPoints().get(0).getValue(Field.FIELD_SLEEP_SEGMENT_TYPE).asInt();
                                ;

                                sleepCounter.setText(String.format(Locale.ENGLISH, "%d", total));
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the sleep count.", e);
                            }
                        });
    }

    private void printSleepData(DataReadResponse dataReadResult) {
        StringBuilder result = new StringBuilder();
        // [START parse_read_data_result]
        // If the DataReadRequest object specified aggregated data, dataReadResult will be returned
        // as buckets containing DataSets, instead of just DataSets.
        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    result.append(formatSleepDataSet(dataSet));
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                result.append(formatSleepDataSet(dataSet));
            }
        }
        // [END parse_read_data_result]
        sleepWeekCounter.setText(result);
    }

    private static String formatSleepDataSet(DataSet dataSet) {
        StringBuilder result = new StringBuilder();

        for (DataPoint dp : dataSet.getDataPoints()) {
            // Get the day of the week JodaTime property
            DateTime sDT = new DateTime(dp.getStartTime(TimeUnit.MILLISECONDS));
            DateTime eDT = new DateTime(dp.getEndTime(TimeUnit.MILLISECONDS));

            result.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s %s to %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            sDT.toLocalTime().toString("HH:mm"),
                            eDT.dayOfWeek().getAsShortText(),
                            eDT.toLocalTime().toString("HH:mm")
                    )
            );

            result.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s: %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            dp.getValue(dp.getDataType().getFields().get(0)).toString(),
                            dp.getDataType().getFields().get(0).getName()));
        }

        return String.valueOf(result);
    }

    private DataReadRequest queryFitnessSleepData() {
        DateTime dt = new DateTime().withTimeAtStartOfDay();
        long endTime = dt.getMillis();
        long startTime = dt.minusWeeks(1).getMillis();

        return new DataReadRequest.Builder()
                // The data request can specify multiple data types to return, effectively
                // combining multiple data queries into one call.
                // In this example, it's very unlikely that the request is for several hundred
                // datapoints each consisting of a few sleeps and a timestamp.  The more likely
                // scenario is wanting to see how many sleeps were walked per day, for 7 days.
                .aggregate(ESTIMATED_SLEEP_DATA, DataType.AGGREGATE_ACTIVITY_SUMMARY)
                // Analogous to a "Group By" in SQL, defines how data should be aggregated.
                // bucketByTime allows for a time span, whereas bucketBySession would allow
                // bucketing by "sessions", which would need to be defined in code.
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();

    }

    private boolean hasFitPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }

    private FitnessOptions getFitnessSignInOptions() {
        // Request access to sleep count data from Fit history
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_SLEEP_SEGMENT)
                .build();
    }

    private void subscribeSleepCount() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_SLEEP_SEGMENT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When the user has accepted the use of Fit data, subscribesleepCount to record data
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.i(TAG, "Fitness permission granted");
                subscribeSleepCount();
                readSleepCountDelta(); // Read today's data
                readHistoricSleepCount(); // Read last weeks data
            }
        } else {
            Log.i(TAG, "Fitness permission denied");
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        startActivity(new Intent(getApplicationContext(), Splash.class));
    }
}
