package com.geniuslead.attendance.model;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

/**
 * Created by AJ on 10/3/15.
 */
public class UserDetails {
    private Boolean Status;
    private String UserId;
    private String FirstName;
    private String LastName;
    private String UserName;
    private String CollegeId;
    private String DeviceId;
    private ArrayList<Course> Course;
    private ArrayList<Subject> Subjects;
    private String Password;

    public Boolean getStatus() {
        return Status;
    }

    public void setStatus(Boolean status) {
        Status = status;
    }

    public String getUserId() {
        return UserId;
    }

    public void setUserId(String userId) {
        UserId = userId;
    }

    public String getFirstName() {
        return FirstName;
    }

    public void setFirstName(String firstName) {
        FirstName = firstName;
    }

    public String getLastName() {
        return LastName;
    }

    public void setLastName(String lastName) {
        LastName = lastName;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public String getCollegeId() {
        return CollegeId;
    }

    public void setCollegeId(String collegeId) {
        CollegeId = collegeId;
    }

    public String getDeviceId() {
        return DeviceId;
    }

    public void setDeviceId(String deviceId) {
        DeviceId = deviceId;
    }

    public ArrayList<com.geniuslead.attendance.model.Course> getCourse() {
        return Course;
    }

    public void setCourse(ArrayList<com.geniuslead.attendance.model.Course> course) {
        Course = course;
    }

    public ArrayList<Subject> getSubjects() {
        return Subjects;
    }

    public void setSubjects(ArrayList<Subject> subjects) {
        Subjects = subjects;
    }

    public String getPassword() {
        return Password;
    }

    public void setPassword(String password) {
        Password = password;
    }

    public static UserDetails fromJson(String json) {
        return new Gson().fromJson(json, UserDetails.class);
    }

    public static ArrayList<UserDetails> toList(String json) {
        return new Gson().fromJson(json, new TypeToken<ArrayList<UserDetails>>() {
        }.getType());
    }
}
