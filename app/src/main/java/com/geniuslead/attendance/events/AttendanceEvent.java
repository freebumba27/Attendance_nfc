package com.geniuslead.attendance.events;

import com.geniuslead.attendance.model.UserDetails;
import com.geniuslead.attendance.utils.MyException;

import java.util.ArrayList;

/**
 * Created by AJ on 10/3/15.
 */
public class AttendanceEvent {

    public static class Fail {
        MyException ex;

        public Fail(MyException ex) {
            this.ex = ex;
        }

        public MyException getEx() {
            return ex;
        }
    }

    public static class Success {
        // --------- To return arrayList ----------
        ArrayList<UserDetails> serverReply;

        public Success(ArrayList<UserDetails> arrayListToilet) {
            serverReply = arrayListToilet;
        }

        public ArrayList<UserDetails> getArray() {
            return serverReply;
        }

        // --------- To return json ----------
        UserDetails userDetails;

        public Success(UserDetails ud) {
            userDetails = ud;
        }

        public UserDetails getObject() {
            return userDetails;
        }

        // --------- To return String ----------
        String result;

        public Success(String result) {
            this.result = result;
        }

        public String getResult() {
            return result;
        }
    }
}
