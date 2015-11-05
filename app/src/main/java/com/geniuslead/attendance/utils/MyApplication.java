package com.geniuslead.attendance.utils;

import android.app.Application;
import android.util.Log;

import com.geniuslead.attendance.BuildConfig;
import com.geniuslead.attendance.R;
import com.koushikdutta.ion.Ion;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.JobManager;
import com.path.android.jobqueue.config.Configuration;
import com.path.android.jobqueue.log.CustomLogger;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by AJ on 9/25/15.
 */

public class MyApplication extends Application {

    private static MyApplication instance;
    private JobManager jobManager;

    public static MyApplication getInstance() {
        return instance;
    }

    public static void addJobInBackground(Job job) {
        MyApplication.getInstance().getJobManager().addJobInBackground(job);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        configureJobManager();

        Ion.getDefault(this).configure().setLogging("Ion", Log.VERBOSE);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
    }

    private void configureJobManager() {
        Configuration configuration = new Configuration.Builder(this)
                .customLogger(new CustomLogger() {
                    private static final String TAG = "JOBS";

                    @Override
                    public boolean isDebugEnabled() {
                        return BuildConfig.DEBUG;
                    }

                    @Override
                    public void d(String text, Object... args) {
                        Log.d(TAG, String.format(text, args));
                    }

                    @Override
                    public void e(Throwable t, String text, Object... args) {
                        Log.e(TAG, String.format(text, args), t);
                    }

                    @Override
                    public void e(String text, Object... args) {
                        Log.e(TAG, String.format(text, args));
                    }
                })
                .minConsumerCount(1)// always keep at least one consumer alive
                .maxConsumerCount(3)// up to 3 consumers at a time
                .loadFactor(3)// 3 jobs per consumer
                .consumerKeepAlive(120)// wait 2 minute
                .build();

        jobManager = new JobManager(this, configuration);
    }

    public JobManager getJobManager() {
        return jobManager;
    }
}
