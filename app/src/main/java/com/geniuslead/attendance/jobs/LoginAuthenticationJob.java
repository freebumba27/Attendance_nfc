package com.geniuslead.attendance.jobs;

import android.content.Context;
import android.util.Log;

import com.geniuslead.attendance.events.UserDetailsEvent;
import com.geniuslead.attendance.model.UserDetails;
import com.geniuslead.attendance.utils.MyApplication;
import com.geniuslead.attendance.utils.MyException;
import com.geniuslead.attendance.utils.Priority;
import com.geniuslead.attendance.utils.ReuseableClass;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;
import com.path.android.jobqueue.Job;
import com.path.android.jobqueue.Params;

import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;

import de.greenrobot.event.EventBus;

/**
 * Created by AJ on 10/3/15.
 */
public class LoginAuthenticationJob extends Job {

    private static final AtomicInteger jobCounter = new AtomicInteger(0);
    private final int id;
    private String urlWithParamenter;

    public LoginAuthenticationJob(String url) {
        super(new Params(Priority.HIGH).requireNetwork());
        id = jobCounter.incrementAndGet();
        this.urlWithParamenter = url;
    }
    @Override
    public void onAdded() {

    }

    @Override
    public void onRun() throws Throwable {

        if (id != jobCounter.get()) {
            // looks like other fetch jobs has been added after me. no reason to
            // keep fetching many times, cancel me, let the other one fetch
            return;
        }

        String url = ReuseableClass.baseUrl + urlWithParamenter;
        Context context = MyApplication.getInstance();
        Response<String> response = Ion.with(context)
                .load("GET", url)
                .addHeader("Content-Type", "application/json")
                .asString()
                .withResponse()
                .get();

        String json = response.getResult();
        Log.d("TAG", "url = " + url);
        Log.d("TAG","response = " + json);
        Log.d("TAG","Header Code = " + response.getHeaders().code());

        if (response.getHeaders().code() != HttpURLConnection.HTTP_OK) {
            MyException ex = MyException.parse(json);
            EventBus.getDefault().post(new UserDetailsEvent.Fail(ex));
            return;
        }

        UserDetails ud = UserDetails.fromJson(json);
        EventBus.getDefault().post(new UserDetailsEvent.Success(ud));
    }

    @Override
    protected void onCancel() {

    }

    @Override
    protected boolean shouldReRunOnThrowable(Throwable throwable) {
        EventBus.getDefault().post(new UserDetailsEvent.Fail(new MyException(throwable.getMessage())));
        return false;
    }
}
