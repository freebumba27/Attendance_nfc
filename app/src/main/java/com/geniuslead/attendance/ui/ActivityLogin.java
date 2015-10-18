package com.geniuslead.attendance.ui;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.text.Spanned;
import android.widget.Button;
import android.widget.CheckBox;
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
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

import static com.geniuslead.attendance.utils.ReuseableClass.haveNetworkConnection;

public class ActivityLogin extends AppCompatActivity {

    private static final int TIME_INTERVAL = 2000;
    @Bind(R.id.EditTextUserName)
    EditText EditTextUserName;
    @Bind(R.id.EditTextCollegeId)
    EditText EditTextCollegeId;
    @Bind(R.id.EditTextPassword)
    EditText EditTextPassword;
    @Bind(R.id.buttonLogin)
    Button buttonLogin;
    @Bind(R.id.checkBoxRememberMe)
    CheckBox checkBoxRememberMe;
    private long mBackPressed;

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

        if (!ReuseableClass.getFromPreference("userDetailsObject", this).equalsIgnoreCase("")) {
            UserDetails userDetails = new Gson().fromJson(ReuseableClass.getFromPreference("userDetailsObject", this), UserDetails.class);
            EditTextUserName.setText(userDetails.getUserName());
            EditTextCollegeId.setText(userDetails.getCollegeId());
            EditTextPassword.setText(userDetails.getPassword());
            checkBoxRememberMe.setChecked(true);
        }

        EditTextUserName.setFilters(new InputFilter[]{filter});
    }

    @OnClick(R.id.buttonLogin)
    public void goingScanning() {
        if (haveNetworkConnection(this)) {
            if (validated()) {
                String url = "User?userName=" + EditTextUserName.getText() + "&password=" + EditTextPassword.getText() +
                        "&collegeId=" + EditTextCollegeId.getText() + "&IMeid=" + ReuseableClass.getImeiNo(this);
                MyApplication.getInstance().getJobManager().addJob(new LoginAuthenticationJob(url));
                progressDialog = ProgressDialog.show(this, "", getString(R.string.loading), true);
            }
        } else
            Toast.makeText(this, R.string.error_internet_connection, Toast.LENGTH_LONG).show();
    }

    private boolean validated() {
        if (EditTextUserName.getText().toString().trim().length() == 0) {
            EditTextUserName.setError("All fields are mandatory");
            EditTextUserName.requestFocus();
            return false;
        }
        if (EditTextCollegeId.getText().toString().trim().length() == 0) {
            EditTextCollegeId.setError("All fields are mandatory");
            EditTextCollegeId.requestFocus();
            return false;
        }
        if (EditTextPassword.getText().toString().trim().length() == 0) {
            EditTextPassword.setError("All fields are mandatory");
            EditTextPassword.requestFocus();
            return false;
        }
        return true;
    }

    public void onEventMainThread(UserDetailsEvent.Success event) {
        if (progressDialog != null) progressDialog.dismiss();

        try {
            UserDetails ud = event.getObject();
            Boolean status = ud.getStatus();

            if (status) {
                if (checkBoxRememberMe.isChecked())
                    ReuseableClass.saveInPreference("userDetailsObject", new Gson().toJson(ud), ActivityLogin.this);
                else
                    ReuseableClass.saveInPreference("userDetailsObject", "", ActivityLogin.this);

                Intent i = new Intent(this, ActivitySelectSubject.class);
                i.putExtra("userDetailsObj", new Gson().toJson(ud));
                startActivity(i);
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
            new AlertDialog.Builder(this)
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

    @Override
    public void onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed();
            return;
        } else {
            Toast.makeText(getBaseContext(), getString(R.string.double_back_press_message), Toast.LENGTH_SHORT).show();
        }

        mBackPressed = System.currentTimeMillis();
    }
}
