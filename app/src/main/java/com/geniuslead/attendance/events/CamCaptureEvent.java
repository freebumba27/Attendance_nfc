package com.geniuslead.attendance.events;

import com.geniuslead.attendance.utils.MyException;

/**
 * Created by AJ on 10/3/15.
 */
public class CamCaptureEvent {

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
        String nfcValue = "";

        public Success(String nfcValue) {
            this.nfcValue = nfcValue;
        }

        public String getNfcValue() {
            return nfcValue;
        }
    }
}
