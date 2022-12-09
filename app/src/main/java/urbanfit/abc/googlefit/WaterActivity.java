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

public class WaterActivity extends AppCompatActivity {
    private static final int REQUEST_OAUTH_REQUEST_CODE = 1111;
    private static final String TAG = "WaterActivity";
    private TextView waterCounter;
    private TextView waterWeekCounter;
    float waterConsumed = 0.3f;

    static DataSource ESTIMATED_WATER_DATA = new DataSource.Builder()
            .setDataType(DataType.TYPE_HYDRATION)
            .setType(DataSource.TYPE_RAW)
            .setStreamName("estimated_WATER")
            .setAppPackageName("com.google.android.gms")
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.water);


        // Initialise JodaTime
        JodaTimeAndroid.init(this);

        waterCounter = findViewById(R.id.waterCounter);
        waterWeekCounter = findViewById(R.id.water_week_counter);

        if (hasFitPermission()) {
            readWaterCountDelta();
            readHistoricWaterCount();
        } else {
            requestFitnessPermission();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.water_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.water_action_revoke) {
            revokeFitnessPermissions();
        }
        if (id == R.id.water_action_read_data) {
            readWaterCountDelta();
            return true;
        } else if (id == R.id.water_action_read_historic_data) {
            readHistoricWaterCount();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void revokeFitnessPermissions() {
        if (!hasFitPermission()) {
            // No need to revoke if we don't already have permissions
            return;
        }
        // Stop recording the water count
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .unsubscribe(DataType.TYPE_HYDRATION);

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

    private void readHistoricWaterCount() {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        // Invoke the History API to fetch the data with the query
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(queryFitnessWaterData())
                .addOnSuccessListener(
                        new OnSuccessListener<DataReadResponse>() {
                            @Override
                            public void onSuccess(DataReadResponse dataReadResponse) {
                                // For the sake of the sample, we'll print the data so we can see what we just
                                // added. In general, logging fitness information should be avoided for privacy
                                // reasons.
                                printWaterData(dataReadResponse);
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

    private void readWaterCountDelta() {

        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.AGGREGATE_HYDRATION)
                .addOnSuccessListener(
                        new OnSuccessListener<DataSet>() {
                            @Override
                            public void onSuccess(DataSet dataSet) {
                                long total =
                                        dataSet.isEmpty()
                                                ? 0
                                                : (long) dataSet.getDataPoints().get(0).getValue(Field.FIELD_VOLUME).asFloat();;

                                waterCounter.setText(String.format(Locale.ENGLISH, "%d", total));
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the water count.", e);
                            }
                        });
    }

    private void printWaterData(DataReadResponse dataReadResult) {
        StringBuilder result = new StringBuilder();

        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    result.append(formatWaterDataSet(dataSet));
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                result.append(formatWaterDataSet(dataSet));
            }
        }
        // [END parse_read_data_result]
        waterWeekCounter.setText(result);
    }

    private static String formatWaterDataSet(DataSet dataSet) {
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

    private DataReadRequest queryFitnessWaterData()
    {
        DateTime dt = new DateTime().withTimeAtStartOfDay();
        long endTime = dt.getMillis();
        long startTime = dt.minusWeeks(1).getMillis();

        return new DataReadRequest.Builder()

                .aggregate(ESTIMATED_WATER_DATA, DataType.AGGREGATE_HYDRATION)

                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    private boolean hasFitPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }
    private FitnessOptions getFitnessSignInOptions() {
        // Request access to water count data from Fit history
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_HYDRATION)
                .build();
    }

    private void subscribeWaterCount() {
        // To create a subscription, invoke the Recording API. As soon as the subscription is
        // active, fitness data will start recording.
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .subscribe(DataType.TYPE_HYDRATION);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // When the user has accepted the use of Fit data, subscribeWaterCount to record data
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_OAUTH_REQUEST_CODE) {
                Log.i(TAG, "Fitness permission granted");
                subscribeWaterCount();
                readWaterCountDelta(); // Read today's data
                readHistoricWaterCount(); // Read last weeks data
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
