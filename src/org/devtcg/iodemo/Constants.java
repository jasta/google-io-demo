package org.devtcg.iodemo;

import java.util.Calendar;
import java.util.TimeZone;

public class Constants {
    public static final boolean DEBUG = true;

    public static final long COUNTDOWN_TO_WHEN;

    static {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Los_Angeles"));
        calendar.set(2011, Calendar.MAY, 10, 9, 0);
        COUNTDOWN_TO_WHEN = calendar.getTimeInMillis();
    }
}
