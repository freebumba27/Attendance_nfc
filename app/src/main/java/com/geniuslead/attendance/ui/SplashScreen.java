package com.geniuslead.attendance.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.geniuslead.attendance.R;


public class SplashScreen extends Activity {

    boolean backPressed = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);
    }

    @Override
    protected void onResume() {
        super.onResume();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!backPressed) {
                    Intent myIntent = new Intent(SplashScreen.this, ActivityLogin.class);
                    finish();
                    startActivity(myIntent);
                    overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                }
            }
        }, 2500);
    }

    @Override
    public void onBackPressed() {
        backPressed = true;
        finish();
    }
}
