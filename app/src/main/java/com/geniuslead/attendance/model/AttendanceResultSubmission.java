package com.geniuslead.attendance.model;

import com.google.gson.Gson;

import java.util.ArrayList;

/**
 * Created by Dream on 01-Nov-15.
 */
public class AttendanceResultSubmission {
    private ArrayList<Students> Students;
    private String DeviceId;
    private String UserName;
    private String Password;
    private String AttendanceDateTime;
    private String CourseId;
    private String SubjectId;
    private String UserId;
    private String CollegeId;


    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    public ArrayList<Students> getStudents() {
        return Students;
    }

    public void setStudents(ArrayList<Students> students) {
        this.Students = students;
    }

    public String getDeviceId() {
        return DeviceId;
    }

    public void setDeviceId(String deviceId) {
        DeviceId = deviceId;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public String getAttendanceDateTime() {
        return AttendanceDateTime;
    }

    public void setAttendanceDateTime(String attendanceDateTime) {
        AttendanceDateTime = attendanceDateTime;
    }

    public String getCourseId() {
        return CourseId;
    }

    public void setCourseId(String courseId) {
        CourseId = courseId;
    }

    public String getSubjectId() {
        return SubjectId;
    }

    public void setSubjectId(String subjectId) {
        SubjectId = subjectId;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getCollegeId() {
        return CollegeId;
    }

    public void setCollegeId(String collegeId) {
        CollegeId = collegeId;
    }
}
