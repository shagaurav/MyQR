package com.scanner.myqr;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Splash screen timer
        int SPLASH_TIME_OUT= 1000;
        new Handler().postDelayed(new Runnable() {

            //Showing splash screen with a timer. This will be useful when you
            // want to show case your app logo / company

            @Override
            public void run() {
                // Start the app home activity
                Intent homeIntent = new Intent(SplashActivity.this, MainActivity.class);
                startActivity(homeIntent);

                // close this activity
                finish();
            }
        }, SPLASH_TIME_OUT);
    }
}
