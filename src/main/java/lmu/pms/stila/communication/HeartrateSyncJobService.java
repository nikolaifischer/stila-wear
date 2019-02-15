package lmu.pms.stila.communication;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

import lmu.pms.stila.SysConstants.CommunicationConstants;
import lmu.pms.stila.model.HeartRateContract;
import lmu.pms.stila.model.HeartRateDbHelper;

/**
 * This class provides a JobService which synchronizes Heartrate Data from the Watch with the
 * Phone.
 */

public class HeartrateSyncJobService extends com.firebase.jobdispatcher.JobService{
    private static final String TAG = "HeartrateSyncJobService";
    private static final int SENDING_PACKAGE_SIZE = 3000;
    int count = 0;
    HRSyncTask mBackgroundTask;
    com.firebase.jobdispatcher.JobParameters mJobParameters;
    private DataClient mDataClient;


    /**
     * Synchronizes the heartrate data with the phone. After completion the HR data is removed
     * from the watch to save Memory.
     * @param context The Context
     * @return if the synchronisation was sucessfull.
     */
    public synchronized boolean syncHeartRateToPhone(Context context){
        Log.d(TAG, "syncHeartRateToPhone: JOB IS RUNNING!");

        mDataClient =  Wearable.getDataClient(context);
        // Get unsynced HR Data:
        HeartRateDbHelper dbHelper = HeartRateDbHelper.getInstance(context);
        Log.d(TAG, "syncHeartRateToPhone: dbHelper: "+dbHelper);
        HashMap<String,ArrayList<Integer>> result = dbHelper.getHRData();
        ArrayList<Integer> timestamps = result.get("timestamps");
        ArrayList<Integer> heartrates = result.get("heartrates");

        assert timestamps.size() == heartrates.size();

        double maxPackets = (double)timestamps.size()/SENDING_PACKAGE_SIZE;
        Log.d(TAG, "syncHeartRateToPhone: MAXPACKETS = "+ String.valueOf(maxPackets));
        for(int sendingPacketCount = 0; sendingPacketCount<maxPackets;sendingPacketCount++){
            int newPacketStartingIndex = sendingPacketCount * SENDING_PACKAGE_SIZE;
            int newPacketEndingIndex;
            if(timestamps.size()-1< newPacketStartingIndex+SENDING_PACKAGE_SIZE){
                newPacketEndingIndex=timestamps.size()-1;
            }
            else {
                newPacketEndingIndex = newPacketStartingIndex+SENDING_PACKAGE_SIZE;
            }
            ArrayList<Integer> sendingTimestamps = new ArrayList<Integer> (timestamps.subList(newPacketStartingIndex,newPacketEndingIndex));
            ArrayList<Integer> sendingHeartrates = new ArrayList<Integer> (heartrates.subList(newPacketStartingIndex,newPacketEndingIndex));



            // SEND THE DATA PACKET TO THE PHONE:


            PutDataMapRequest putDataMapReq = PutDataMapRequest.create(CommunicationConstants.WEAR_PATH+CommunicationConstants.HR_PATH);
            putDataMapReq.getDataMap().putIntegerArrayList(CommunicationConstants.KEY_TIMESTAMPS,sendingTimestamps);
            putDataMapReq.getDataMap().putIntegerArrayList(CommunicationConstants.KEY_HRS,sendingHeartrates);
            putDataMapReq.getDataMap().putInt("count",count++); // TODO: Das ist Testcode, oder?
            PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
            putDataReq.setUrgent();
            Task<DataItem> putDataTask = mDataClient.putDataItem(putDataReq).addOnSuccessListener(new OnSuccessListener<DataItem>() {
                @Override
                public void onSuccess(DataItem dataItem) {
                    Log.d(TAG, "onSuccess: Sucessfully delivered the Data Items");



                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "onFailure: FAILURE WHILE SENDING DATA! ");
                    e.printStackTrace();
                }
            });
            try {
                Tasks.await(putDataTask);
            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            Log.d(TAG, "syncHeartRateToPhone: Send Packets: "+sendingPacketCount);
        }


        return true;
    }


    @Override
    public boolean onStartJob( final com.firebase.jobdispatcher.JobParameters jobParameters) {

        mJobParameters = jobParameters;

        Log.d(TAG, "onStartJob: ");
        mBackgroundTask = new HRSyncTask();
        mBackgroundTask.execute();
        return false;
    }

    @Override
    public boolean onStopJob(com.firebase.jobdispatcher.JobParameters job) {
        if(mBackgroundTask != null){
            mBackgroundTask.cancel(true);
        }
        return true;
    }

    private class HRSyncTask extends AsyncTask<Void,Void, Boolean>{


        @Override
        protected Boolean doInBackground(Void... params) {
            Context context = HeartrateSyncJobService.this;
            return syncHeartRateToPhone(context);
        }

        @Override
        protected void onPostExecute(Boolean successful) {
            super.onPostExecute(successful);
            jobFinished(mJobParameters,false);
        }
    }
}
