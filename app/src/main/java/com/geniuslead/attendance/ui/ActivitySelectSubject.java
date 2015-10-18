package com.geniuslead.attendance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import com.geniuslead.attendance.R;
import com.geniuslead.attendance.model.Course;
import com.geniuslead.attendance.model.Subject;
import com.geniuslead.attendance.model.UserDetails;
import com.geniuslead.attendance.utils.CustomExceptionHandler;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by AJ on 10/3/15.
 */
public class ActivitySelectSubject extends AppCompatActivity {
    @Bind(R.id.SpinnerCourse)
    Spinner SpinnerCourse;
    @Bind(R.id.SpinnerSubject)
    Spinner SpinnerSubject;
    @Bind(R.id.ButtonSave)
    Button ButtonSave;
    MyData[] courseValues = null;
    MyData[] subjectValues = null;
    Map<Integer, MyData> myDataMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_subject_screen);
        ButterKnife.bind(this);

            Thread.setDefaultUncaughtExceptionHandler(new CustomExceptionHandler(this));


        UserDetails udobj = new Gson().fromJson(getIntent().getStringExtra("userDetailsObj"), UserDetails.class);
        prepareUi(udobj);
    }

    public void prepareUi(UserDetails userDetailsObj) {
        try {
            UserDetails ud = userDetailsObj;
            Boolean status = ud.getStatus();

            if (status) {
                ArrayList<Course> courses = ud.getCourse();
                courseValues = new MyData[courses.size()];

                for (int i = 0; i < courses.size(); i++) {
                    String courseId = courses.get(i).getCourseId();
                    String courseName = courses.get(i).getCourseName();
                    MyData myData = new MyData(courseName, courseId);
                    courseValues[i] = myData;
                }
                populateCourseSpinner(ud);
            } else {
                //Toast.makeText(this, R.string.login_error, Toast.LENGTH_LONG).show();
            }
        } catch (Throwable e) {

        }
    }

    private void populateCourseSpinner(final UserDetails ud) {
        ArrayAdapter<MyData> adapter = new ArrayAdapter<MyData>(this, android.R.layout.simple_spinner_item, courseValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerCourse.setAdapter(adapter);
        SpinnerCourse.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        MyData d = courseValues[position];
                        //Toast.makeText(ActivitySelectSubject.this, "Value: " + d.getValue() + " Name: " + d.getSpinnerText(), Toast.LENGTH_LONG).show();
                        populateSubjectSpinner(d.getValue(), ud);
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        //On Nothing selection do something
                    }
                }
        );
    }

    private void populateSubjectSpinner(String value, final UserDetails ud) {

        ArrayList<Subject> subjects = ud.getSubjects();
        int count = 0;
        for (Subject subject : subjects) {
            if (subject.getCourseId().equalsIgnoreCase(value)) {
                count++;
            }
        }
        Log.d("TAG", "count = " + count);
        subjectValues = new MyData[count];
        int i = 0;
        for (Subject subject : subjects) {
            if (subject.getCourseId().equalsIgnoreCase(value)) {
                subjectValues[i] = new MyData(subject.getSubjectName(), subject.getSubjectId());
                i++;
            }
        }
        ArrayAdapter<MyData> adapter = new ArrayAdapter<MyData>(this, android.R.layout.simple_spinner_item, subjectValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        SpinnerSubject.setAdapter(adapter);
        SpinnerSubject.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        MyData d = subjectValues[position];
                        //Toast.makeText(ActivitySelectSubject.this, "Value: " + d.getValue() + " Name: " + d.getSpinnerText(), Toast.LENGTH_LONG).show();
                    }

                    public void onNothingSelected(AdapterView<?> parent) {
                        //On Nothing selection do something
                    }
                }
        );
    }

    @OnClick(R.id.ButtonSave)
    public void saveButtonClicked() {
        Intent i = new Intent(this, ReadCardActivity.class);
        startActivity(i);
    }

    class MyData {
        String spinnerText;
        String value;

        public MyData(String spinnerText, String value) {
            this.spinnerText = spinnerText;
            this.value = value;
        }

        public String getSpinnerText() {
            return spinnerText;
        }

        public String getValue() {
            return value;
        }

        public String toString() {
            return spinnerText;
        }
    }
}
