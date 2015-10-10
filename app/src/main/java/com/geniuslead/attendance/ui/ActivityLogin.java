package com.geniuslead.attendance.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.geniuslead.attendance.R;
import com.geniuslead.attendance.events.UserDetailsEvent;
import com.geniuslead.attendance.jobs.LoginAuthenticationJob;
import com.geniuslead.attendance.model.UserDetails;
import com.geniuslead.attendance.utils.MyApplication;
import com.geniuslead.attendance.utils.MyException;
import com.geniuslead.attendance.utils.ReuseableClass;
import com.google.gson.Gson;

import butterknife.Bind;
import butterknife.ButterKnife;
import de.greenrobot.event.EventBus;

import static com.geniuslead.attendance.utils.ReuseableClass.haveNetworkConnection;

public class ActivityLogin extends AppCompatActivity {

    @Bind(R.id.EditTextUserName)
    EditText EditTextUserName;
    @Bind(R.id.EditTextCollegeId)
    EditText EditTextCollegeId;
    @Bind(R.id.EditTextPassword)
    EditText EditTextPassword;

    private String blockCharacterSet = "~!@#$%^&*()_-";
    private ProgressDialog progressDialog;
    private InputFilter filter = new InputFilter() {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            if (source != null && blockCharacterSet.contains(("" + source))) {
                return "";
            }
            return null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_screen);
        ButterKnife.bind(this);

        EditTextUserName.setFilters(new InputFilter[]{filter});
    }

    public void goingScanning(View view) {
        if (haveNetworkConnection(this)) {
            String url = "User?userName=" + EditTextUserName.getText() + "&password=" + EditTextPassword.getText() +
                    "&collegeId=" + EditTextCollegeId.getText() + "&IMeid=" + ReuseableClass.getImeiNo(this);
            MyApplication.getInstance().getJobManager().addJob(new LoginAuthenticationJob(url));
            progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), true);
        }
        //
        else
            Toast.makeText(this, R.string.error_internet_connection, Toast.LENGTH_LONG).show();
    }

    public void onEventMainThread(UserDetailsEvent.Success event) {
        if (progressDialog != null) progressDialog.dismiss();

        try {
            UserDetails ud = event.getObject();
            Boolean status = ud.getStatus();

            if (status) {
                Intent i = new Intent(this, ActivitySelectSubject.class);
                i.putExtra("userDetailsObj", new Gson().toJson(ud));
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show();
            }
        } catch (Throwable e) {
            EventBus.getDefault().post(new UserDetailsEvent.Fail(new MyException(e.getMessage())));
        }
    }

    public void onEventMainThread(UserDetailsEvent.Fail event) {
        if (progressDialog != null) progressDialog.dismiss();
        if (event.getEx() != null) {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(event.getEx().getMessage())
                    .setPositiveButton("OK", null)
                    .show();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }
}
