package com.geniuslead.attendance.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Dream on 19-Oct-15.
 */
public class CustomExceptionHandler implements UncaughtExceptionHandler {

    public static String sendErrorLogsTo = "anirbansjob27@gmail.com";
    Activity activity;
    private UncaughtExceptionHandler defaultUEH;

    public CustomExceptionHandler(Activity activity) {
        this.defaultUEH = Thread.getDefaultUncaughtExceptionHandler();
        this.activity = activity;
    }

    // convert InputStream to String
    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }

    public void uncaughtException(Thread t, Throwable e) {

        final Writer result = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(result);
        e.printStackTrace(printWriter);
        String stacktrace = result.toString();
        printWriter.close();
        String filename = "error" + System.nanoTime() + ".stacktrace";

        Log.e("Hi", "url != null");
        sendToServer(stacktrace, filename);

        StackTraceElement[] arr = e.getStackTrace();
        String report = e.toString() + "\n\n";
        report += "--------- Stack trace ---------\n\n";
        for (int i = 0; i < arr.length; i++) {
            report += "    " + arr[i].toString() + "\n";
        }
        report += "-------------------------------\n\n";

        report += "--------- Cause ---------\n\n";
        Throwable cause = e.getCause();
        if (cause != null) {
            report += cause.toString() + "\n\n";
            arr = cause.getStackTrace();
            for (int i = 0; i < arr.length; i++) {
                report += "    " + arr[i].toString() + "\n";
            }
        }
        report += "-------------------------------\n\n";

        defaultUEH.uncaughtException(t, e);
    }

    private void sendToServer(String stacktrace, String filename) {
        AsyncTaskClass async = new AsyncTaskClass(stacktrace, filename,
                getAppLable(activity));
        async.execute("");
    }

    public String getAppLable(Context pContext) {
        PackageManager lPackageManager = pContext.getPackageManager();
        ApplicationInfo lApplicationInfo = null;
        try {
            lApplicationInfo = lPackageManager.getApplicationInfo(
                    pContext.getApplicationInfo().packageName, 0);
        } catch (final NameNotFoundException e) {
        }
        return (String) (lApplicationInfo != null ? lPackageManager
                .getApplicationLabel(lApplicationInfo) : "Unknown");
    }

    public class AsyncTaskClass extends AsyncTask<String, String, InputStream> {
        final String filename;
        InputStream is = null;
        String stacktrace;
        String applicationName;

        AsyncTaskClass(final String stacktrace, final String filename,
                       String applicationName) {
            this.applicationName = applicationName;
            this.stacktrace = stacktrace;
            this.filename = filename;
        }

        @Override
        protected InputStream doInBackground(String... params) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost(
                    "http://suo-yang.com/books/sendErrorLog/sendErrorLogs.php?");

            Log.i("Error", stacktrace);

            try {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(
                        6);

                nameValuePairs.add(new BasicNameValuePair("data", stacktrace));
                nameValuePairs.add(new BasicNameValuePair("to", sendErrorLogsTo));
                nameValuePairs.add(new BasicNameValuePair("subject", applicationName));

                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                HttpResponse response = httpclient.execute(httppost);

                HttpEntity entity1 = response.getEntity();

                BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(
                        entity1);

                is = bufHttpEntity.getContent();

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return is;
        }

        @Override
        protected void onPostExecute(InputStream result) {
            super.onPostExecute(result);

            Log.e("Stream Data", getStringFromInputStream(is));
        }
    }
}
