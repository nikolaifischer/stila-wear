package lmu.pms.stila.model;

import android.provider.BaseColumns;

/**
 * Created by Niki on 01.02.2018.
 */

public class HeartRateContract {

    public static final class HeartRateEntry implements BaseColumns{
        public static final String TABLE_NAME = "heartrate";

        // COLUMNS
        public static final String COLUMN_TIMESTAMP = "timestamp";
        public static final String COLUMN_HEARTRATE = "heartrate";
    }
}
