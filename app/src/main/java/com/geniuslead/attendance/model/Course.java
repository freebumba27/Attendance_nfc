package com.geniuslead.attendance.model;

/**
 * Created by AJ on 10/3/15.
 */
public class Course {
    private String CourseId;
    private String CourseName;

    public String getCourseId() {
        return CourseId;
    }

    public void setCourseId(String courseId) {
        CourseId = courseId;
    }

    public String getCourseName() {
        return CourseName;
    }

    public void setCourseName(String courseName) {
        CourseName = courseName;
    }
}
