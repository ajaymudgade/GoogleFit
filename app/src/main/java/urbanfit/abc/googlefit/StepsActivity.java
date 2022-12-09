package urbanfit.abc.googlefit;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import urbanfit.abc.googlefit.Model.StepsModel;

public class StepsActivity extends AppCompatActivity  {
    int REQUEST_OAUTH_REQUEST_CODE = 0x1001;
    String TAG = "MainActivity";
    TextView counter,weekCounter,dayWise_counter,selectDate, endSelectDate;
    Context context;
    Calendar myCalendar= Calendar.getInstance();
    DatePickerDialog datePickerDialog;
    int year = 0,month = 0,day = 0;
    long Start_timeInMilliseconds =0, END_timeInMilliseconds = 0;
    static DataSource ESTIMATED_STEP_DELTAS = new DataSource.Builder()
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setType(DataSource.TYPE_DERIVED)
            .setStreamName("estimated_steps")
            .setAppPackageName("com.google.android.gms")
            .build();
    String myFormat = " dd ";

    ArrayList<StepsModel> stepsModels = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.steps);
        JodaTimeAndroid.init(this);
        context = this;
        counter = findViewById(R.id.counter);
        weekCounter = findViewById(R.id.week_counter);
        dayWise_counter = findViewById(R.id.dayWise_counter);
        selectDate = findViewById(R.id.selectDate);
        endSelectDate = findViewById(R.id.endSelectDate);
        year = myCalendar.get(Calendar.YEAR);
        month = myCalendar.get(Calendar.MONTH);
        day = myCalendar.get(Calendar.DAY_OF_MONTH);

        if (!hasFitPermission()) {
            requestFitnessPermission();
        }

        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd",Locale.US);
        String ssdate = sd.format(new Date());
        selectDate.setText(ssdate);
        endSelectDate.setText(ssdate);

        selectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        month = month+1;
                        String daystr = String.valueOf(day);
                        if(daystr.length()==1)
                        {
                            daystr = "0"+daystr;
                        }
                        String monthstr = String.valueOf(month);
                        if(monthstr.length()==1)
                        {
                            monthstr = "0"+monthstr;
                        }
                        String date = year+"-"+monthstr+"-"+daystr;

                        //String date_ = date;
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd",Locale.US);
                        try
                        {
                            Date mDate = sdf.parse(date);
                            Start_timeInMilliseconds = mDate.getTime();
                        }
                        catch (ParseException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                        selectDate.setText(date);
                        Log.d(TAG, "onDateSet: select date : "+date);
                    }
                }, year, month, day);
                datePickerDialog.show();
            }
        });

        endSelectDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                datePickerDialog = new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                        month = month+1;
                        String daystr = String.valueOf(day);
                        if(daystr.length()==1)
                        {
                            daystr = "0"+daystr;
                        }
                        String monthstr = String.valueOf(month);
                        if(monthstr.length()==1)
                        {
                            monthstr = "0"+monthstr;
                        }
                        String date = year+"-"+monthstr+"-"+daystr;



                        //String date_ = date;
                        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);
                        try
                        {
                            Date mDate = sdf.parse(date+"T23:59:00");
                            END_timeInMilliseconds = mDate.getTime();
                        }
                        catch (ParseException e)
                        {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }

                        endSelectDate.setText(date);
                        try {
                            readHistoricStepCount1();
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        Log.d(TAG, "onDateSet: select date : "+date);
                    }
                }, year, month, day);
                datePickerDialog.show();
            }
        });
    }

    public void requestFitnessPermission() {
        GoogleSignIn.requestPermissions(this,REQUEST_OAUTH_REQUEST_CODE,GoogleSignIn.getLastSignedInAccount(this),getFitnessSignInOptions());
    }

    public boolean hasFitPermission() {
        FitnessOptions fitnessOptions = getFitnessSignInOptions();
        return GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions);
    }

    public FitnessOptions getFitnessSignInOptions() {
        return FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA)
                .build();
    }

    public DataReadRequest queryFitnessData1() throws ParseException {
        long startTime = Start_timeInMilliseconds;
        long endTime = END_timeInMilliseconds;

        Log.d(TAG, "queryFitnessData1: start : "+startTime+" end : "+endTime);

        return new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    public void readStepCountDelta() {

        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readDailyTotal(DataType.AGGREGATE_STEP_COUNT_DELTA)
                .addOnSuccessListener(new OnSuccessListener<DataSet>() {
                    @Override
                    public void onSuccess(DataSet dataSet) {
                        long total = dataSet.isEmpty()
                                ? 0
                                : dataSet.getDataPoints().get(0).getValue(Field.FIELD_STEPS).asInt();

                        counter.setText(String.format(Locale.ENGLISH, "%d", total));
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the step count.", e);
                            }
                        });
    }

    public void readHistoricStepCount1() throws ParseException {

        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }

        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(queryFitnessData1())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        dayWise_counter.setText(printData(dataReadResponse));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.e(TAG, "There was a problem reading the historic data.", e);
                    }
                });
    }

    public void readHistoricStepCount() {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }
        // Invoke the History API to fetch the data with the query
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(queryFitnessData())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        weekCounter.setText(printData(dataReadResponse));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e)
                    {
                        Log.e(TAG, "There was a problem reading the historic data.", e);
                    }
                });
    }

    public DataReadRequest queryFitnessData() {
        DateTime dt = new DateTime().withTimeAtStartOfDay();
        long endTime = dt.getMillis();
        long startTime = dt.minusWeeks(1).getMillis();
        Log.e(TAG, "queryFitnessData: dates start end : "+startTime+" "+endTime);
        return new DataReadRequest.Builder()
                .aggregate(ESTIMATED_STEP_DELTAS, DataType.AGGREGATE_STEP_COUNT_DELTA)
                .bucketByTime(1, TimeUnit.DAYS)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .build();
    }

    public String printData(DataReadResponse dataReadResult) {
        StringBuilder result = new StringBuilder();

        if (dataReadResult.getBuckets().size() > 0) {
            Log.i(TAG, "Number of returned buckets of DataSets is: " +dataReadResult.getBuckets().size());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets) {
                    result.append(formatDataSet(dataSet));
                }
            }
        } else if (dataReadResult.getDataSets().size() > 0) {
            Log.i(TAG, "Number of returned DataSets is: " +dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets()) {
                result.append(formatDataSet(dataSet));
            }
        }

        return  result.toString();
        // [END parse_read_data_result]
    }

    private String formatDataSet(DataSet dataSet) {

        StringBuilder result = new StringBuilder();

        for (DataPoint dp : dataSet.getDataPoints()) {
            // Get the day of the week JodaTime property
            DateTime sDT = new DateTime(dp.getStartTime(TimeUnit.MILLISECONDS));
            DateTime eDT = new DateTime(dp.getEndTime(TimeUnit.MILLISECONDS));

            Log.e(TAG, "formatDataSet: sDT and eDT : start "+sDT+" end "+eDT);

            /**

             see output Date wise Data
             1) Convert Milliseconds to Date to get numeric date in array
             2) We have take Array and get numeric date instead of Day name
             3) Remove time format from response
             4) print date and steps on each index
             5) break response in parts to get data in required format



             private void Get_BlogList() {
             blogsModels = new ArrayList<>();
             @SuppressLint("Recycle") Cursor c1s221 = Splash.db.rawQuery("SELECT id,Title,BlogImage FROM "+ Tablecreation.U_BlogMaster + " ORDER BY id DESC Limit 4", null);
             while (c1s221.moveToNext())
             {
             String id = c1s221.getString(0);
             String Title = c1s221.getString(1);
             String BlogImage = c1s221.getString(2);

             blogsModels.add(new BlogsModel(id,Title,BlogImage));
             }
             blogAdapter = new BlogAdapter(getActivity(),blogsModels);
             blogslist.setAdapter(blogAdapter);
             }


             */


            Log.e(TAG, "formatDataSet: for loop checking : "+dataSet.getDataPoints() );

            stepsModels.add(new StepsModel(sDT.dayOfWeek().getAsShortText(), eDT.dayOfWeek().getAsShortText(),
                    String.valueOf(dp.getValue(dp.getDataType().getFields().get(0))), dp.getDataType().getFields().get(0).getName()));

            Log.e(TAG, "formatDataSet: for loop checking : "+stepsModels.size() );


            for (int i = 0; i < stepsModels.size(); i++) {
                Log.e(TAG, "formatDataSet: steps model start date : "+stepsModels.get(i).startDate );
                Log.e(TAG, "formatDataSet: steps model end date : "+stepsModels.get(i).endDate );
                Log.e(TAG, "formatDataSet: steps model steps count : "+stepsModels.get(i).stepsCount );
                Log.e(TAG, "formatDataSet: steps model steps unit : "+stepsModels.get(i).unitSteps );
            }




            Date date = new Date(sDT.getMillis());
            DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
            Log.e(TAG, "formatDataSet: nusti date "+ dateFormat.format(date));


            long s = sDT.getMillis();
            Log.e(TAG, "formatDataSet: long date millis : "+s);
            Log.e(TAG, "formatDataSet: long date millis converted : "+getDateToConvert(s, myFormat));

/*            result.append(
                    String.format(Locale.ENGLISH,
//                            "%s %s to %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
//                            dateFormat.format(sDT.toLocalTime().toDateTimeToday().toDate()),
                            eDT.dayOfWeek().getAsShortText()));
//                            dateFormat.format(eDT.toLocalTime().toDateTimeToday().toDate())));
*/

            for (int i = 0; i < stepsModels.size(); i++)
            {
                    result.append(stepsModels.get(i).startDate+" "+ getDateToConvert(s, myFormat)
                            +"  Steps count : "+stepsModels.get(i).stepsCount +" "+"\n\n");
            }
            stepsModels.clear();

/*            result.append(String.format(
                            Locale.ENGLISH,
                            "%s : %s %s \n\n",
                            getDateToConvert(s, myFormat),
                            dp.getValue(dp.getDataType().getFields().get(0)),
                            dp.getDataType().getFields().get(0).getName()));*/

            Log.e(TAG, "formatDataSet: Format Data set : appended converted date : "+getDateToConvert(s, myFormat));
            Log.e(TAG, "formatDataSet: Format Data set : dp.getValue(dp.getDataType().getFields().get(0)) : "+dp.getValue(dp.getDataType().getFields().get(0)));
            Log.e(TAG, "formatDataSet: Format Data set : steps : "+dp.getDataType().getFields().get(0).getName());

        }

        return String.valueOf(result);
    }


    public static String getDateToConvert(long milliSeconds, String dateFormat)
    {
        // Create a DateFormatter object for displaying date in specified format.
        SimpleDateFormat formatter = new SimpleDateFormat(dateFormat);

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_revoke) {
            revokeFitnessPermissions();
        }
        if (id == R.id.action_read_data) {
            try {
                readStepCountDelta();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        } else if (id == R.id.action_read_historic_data) {
            try {
                readHistoricStepCount();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void revokeFitnessPermissions() {
        if (!hasFitPermission()) {
            return;
        }
        Fitness.getRecordingClient(this, GoogleSignIn.getLastSignedInAccount(this)).unsubscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE);
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder().addExtension(getFitnessSignInOptions()).build();
        GoogleSignIn.getClient(this, signInOptions).revokeAccess();
        Toast.makeText(this, "Fitness permissions revoked", Toast.LENGTH_SHORT).show();
    }

    /*public void readDateWiseData() throws ParseException {
        if (!hasFitPermission()) {
            requestFitnessPermission();
            return;
        }
        Fitness.getHistoryClient(this, GoogleSignIn.getLastSignedInAccount(this))
                .readData(queryFitnessData1())
                .addOnSuccessListener(new OnSuccessListener<DataReadResponse>() {
                    @Override
                    public void onSuccess(DataReadResponse dataReadResponse) {
                        long total = dataReadResponse.getBuckets().isEmpty()
                                ? 0
                                : dataReadResponse.getBuckets().get(0).getDataSets().size();

                        dayWise_counter.setText(String.format(Locale.ENGLISH, "%d", total));
                    }
                })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "There was a problem getting the step count.", e);
                            }
                        });
    }*/

   /* public void retriveDataDayWise(DataReadResponse dataReadResult) {
        StringBuilder result = new StringBuilder();
        if (dataReadResult.getBuckets().size() > 0)
        {
            Log.i(TAG, "Number of returned buckets of DataSets is: " + dataReadResult.getBuckets());
            for (Bucket bucket : dataReadResult.getBuckets()) {
                List<DataSet> dataSets = bucket.getDataSets();
                for (DataSet dataSet : dataSets)
                {
                    result.append(formatDataSetDays(dataSet));
                }
            }
        }
        else if (dataReadResult.getDataSets().size() > 0)
        {
            Log.i(TAG, "Number of returned DataSets is: " + dataReadResult.getDataSets().size());
            for (DataSet dataSet : dataReadResult.getDataSets())
            {
                result.append(formatDataSetDays(dataSet));
            }
        }
        dayWise_counter.setText(result);
    }*/

   /* public String formatDataSetDays(DataSet dataSet) {
        StringBuilder result1 = new StringBuilder();
        for (DataPoint dp : dataSet.getDataPoints()) {
            // Get the day of the week JodaTime property
            DateTime sDT = new DateTime(dp.getStartTime(TimeUnit.DAYS));
            DateTime eDT = new DateTime(dp.getEndTime(TimeUnit.DAYS));

            result1.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s %s to %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            sDT.toLocalTime().toString("HH:mm"),
                            eDT.dayOfWeek().getAsShortText(),
                            eDT.toLocalTime().toString("HH:mm")));

            result1.append(
                    String.format(
                            Locale.ENGLISH,
                            "%s: %s %s\n",
                            sDT.dayOfWeek().getAsShortText(),
                            dp.getValue(dp.getDataType().getFields().get(0)).toString(),
                            dp.getDataType().getFields().get(0).getName()));
        }

        return String.valueOf(result1);

    }*/

    private void weekFirstLast(){
        Calendar calendar = Calendar.getInstance();

        Date date1 = calendar.getTime();
        //current date to check that our week lies in same month or not
        SimpleDateFormat checkformate = new SimpleDateFormat("MM/yyyy");
        String currentCheckdate= checkformate.format(date1);

        int weekn = calendar.get(Calendar.WEEK_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year  = calendar.get(Calendar.YEAR);
        //resat calender without date
        calendar.clear();
        calendar.setFirstDayOfWeek(Calendar.SUNDAY);
        calendar.set(Calendar.WEEK_OF_MONTH,weekn);
        calendar.set(Calendar.MONTH,month);
        calendar.set(Calendar.YEAR,year);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

        Date datef = calendar.getTime();
        //move date to 6 days + to get last date of week
        Long timeSixDayPlus = calendar.getTimeInMillis()+518400000L;
        Date dateL = new Date(timeSixDayPlus);
        String firtdate = simpleDateFormat.format(datef);
        String lastdate = simpleDateFormat.format(dateL);
        String firtdateCheck = checkformate.format(datef);
        String lastdateCheck = checkformate.format(dateL);

        //if our week lies in two different months then we show only current month week part only
        if (!firtdateCheck.toString().equalsIgnoreCase(currentCheckdate)) {
            firtdate = "1" + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR);
        }
        if (!lastdateCheck.toString().equalsIgnoreCase(currentCheckdate)) {
            int ma = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
            lastdate = String.valueOf(ma) + "/" + calendar.get(Calendar.MONTH) + "/" + calendar.get(Calendar.YEAR);
        }
        Log.e("current","=>>" +firtdate+" to "+lastdate);
    }

}
