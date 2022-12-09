package urbanfit.abc.googlefit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import urbanfit.abc.googlefit.SleepFiles.SleepActivityMainGithub;

public class Splash extends AppCompatActivity {

    Button stepBtn,sleepBtn, waterBtn, SleepActivityBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        stepBtn = findViewById(R.id.stepBtn);
        sleepBtn = findViewById(R.id.sleepBtn);
        waterBtn = findViewById(R.id.waterBtn);
        SleepActivityBtn = findViewById(R.id.SleepActivityBtn);

        stepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), StepsActivity.class));

            }
        });

        sleepBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SleepActivity.class));

            }
        });

        waterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), WaterActivity.class));

            }
        });

        SleepActivityBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), SleepActivityMainGithub.class));

            }
        });
    }
}