package lmu.pms.stila.provider;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

import lmu.pms.stila.Database.HRVDatabaseHandler;

public class ThreePartsStressedIndicator implements StressedIndicatorAlgorithm {
    private Context mContext;

    /**
     * The amount of HRV Database Entries needed to perform this algorithm.
     */
    private static final int MIN_DATA_LIMIT = 150;

    /**
     * The amount of HRV Database Entries the algorithm should work with
     * Older entries are ignored.
     */
    private static final int MAX_DATA_LIMIT = 2000;
    /**
     * Indicates whether there is enough data to perform this algorithm.
     */
    private boolean sufficientData = false;

    /**
     * The HRV Data is Algorithm is perfomed upon.
     */
    private ArrayList<Float> data;

    /**
     * The UPPER BOUND for Comp. Stress values to be considered 'relaxed'
     */
    private float relaxedBound = -1;

    /**
     * The LOWER BOUND for Comp. Stress Values to be considered 'stressed'
     */
    private float stressedBound = -1;

    public ThreePartsStressedIndicator(Context context){
        mContext = context;
    }

    @Override
    public float getRelaxedBound(){
        if(relaxedBound==-1)
            return 30;
        return relaxedBound;
    }

    @Override
    public float getStressedBound(){
        if(stressedBound == -1)
            return 60;
        return stressedBound;
    }

    @Override
    public void updateData() {
        data = new ArrayList<>();
        HRVDatabaseHandler dbHandler = new HRVDatabaseHandler(mContext);
        SQLiteDatabase database = dbHandler.getReadableDatabase();
        int entryCount =dbHandler.getEntryCount();
        if(entryCount < MIN_DATA_LIMIT){
            sufficientData = false;
            relaxedBound = -1;
            stressedBound = -1;
            return;
        }
        else{
            sufficientData = true;
        }
        Cursor cursor = database.query(dbHandler.TABLE_HRVSCORE,null,null,null,null,null,dbHandler.KEY_TIMESTAMP+" desc",String.valueOf(MAX_DATA_LIMIT));
        cursor.moveToFirst();
        while(!cursor.isAfterLast()){
            data.add(cursor.getFloat(cursor.getColumnIndex(dbHandler.KEY_COMPUTEDSTRESS)));
            cursor.moveToNext();
        }
        cursor.close();

        // Get max value in data set
        float max = getMax(data);
        float min = getMin(data);

        // The difference between max and min
        float difference = max - min;

        // The Values get divided in 3 equally sized parts
        float sizeOfPart = difference / 3;

        relaxedBound = min+sizeOfPart;
        stressedBound = relaxedBound+sizeOfPart;
        database.close();
    }


    /**
     *  HELPER METHODS FOR ALGORITHM
     */
    private float getMax(ArrayList<Float> list){
        float max = 0;
        for(float current: list){
            if(current > max){
                max = current;
            }
        }
        return max;
    }

    private float getMin(ArrayList<Float> list){
        float min = Long.MAX_VALUE;
        for(float current: list){
            if(current < min){
                min = current;
            }
        }
        return min;
    }
}
