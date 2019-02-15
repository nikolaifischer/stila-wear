package lmu.pms.stila.communication;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.support.wearable.complications.ProviderUpdateRequester;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityDatabaseHandler;
import lmu.pms.stila.Database.ActivityTypeDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabaseHandler;
import lmu.pms.stila.Database.HRVDatabase;
import lmu.pms.stila.Database.HRVDatabaseHandler;
import lmu.pms.stila.Database.ProfileDatabase;
import lmu.pms.stila.Database.ProfileDatabaseHandler;
import lmu.pms.stila.SysConstants.CommunicationConstants;
import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.common.App;
import lmu.pms.stila.model.HeartRateDbHelper;
import lmu.pms.stila.provider.ComputedStressComplicationProvider;
import lmu.pms.stila.smartNotifications.NotificationAlarmReceiver;


/**
 * Service to listen to incoming transmissions from the phone to the Watch.
 */
public class DataLayerListenerService extends WearableListenerService {
    private static final String TAG = "DataLayLisSerWear";
    private GoogleApiClient mGoogleApiClient;
    private HeartrateSyncJobService heartrateSyncJobService;

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
        heartrateSyncJobService = new HeartrateSyncJobService();


    }


    /**
     * Listens for messages (commands) from the phone to the watch and relays them to the corresponding
     * methods.
     * @param messageEvent
     */
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.e(TAG, "onMessageReceived: MESSAGE RECEIVED!");
        String path = messageEvent.getPath();
        final Context context = this;
        switch(path){
            case CommunicationConstants.WEAR_PATH+CommunicationConstants.RESYNC_PATH:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        heartrateSyncJobService.syncHeartRateToPhone(context);
                    }
                }).start();

                break;
            case CommunicationConstants.WEAR_PATH+CommunicationConstants.CONFIRM_PATH:
                Log.d(TAG, "onMessageReceived: Got confirm Message");
                byte[] data  = messageEvent.getData();
                if(data == null ||data.length == 0)
                    break;
                try {
                    // Confirmation Message contains Information about the first and the last
                    // timestamp the phone received in a packet
                    // This information is then used to delete synched data:
                    String dataString = new String(data, "UTF-8");
                    JSONObject dataJson = new JSONObject(dataString);
                    int firstTimestamp = dataJson.getInt("from");
                    int lastTimestamp = dataJson.getInt("to");
                    HeartRateDbHelper dbHelper = HeartRateDbHelper.getInstance(this);
                    dbHelper.deleteEntriesInTimeFrame(firstTimestamp,lastTimestamp);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

                case CommunicationConstants.WEAR_PATH+CommunicationConstants.CHECK_ONLINE:
                    Log.d(TAG, "onMessageReceived: Got Online Check Message");
                    MessageToPhoneUtil messageToPhoneUtil = new MessageToPhoneUtil(this);
                    messageToPhoneUtil.sendMessage(MessageToPhoneUtil.MessageMode.CONFIRM_ONLINE,null);

                    break;
            default:
                Log.d(TAG, "onMessageReceived: Got Message but did not recognize path: "+path);
        }

    }



    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            DataItem item = event.getDataItem();
            String path = item.getUri().getPath();
            Log.e(TAG, "onDataChanged: "+ path);
            if( event.getType() == DataEvent.TYPE_CHANGED){
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                switch(path){
                    case CommunicationConstants.WEAR_PATH+CommunicationConstants.ACTIVITY_PATH:
                        ArrayList<String> activityJsons = dataMap.getStringArrayList(CommunicationConstants.KEY_ACTIVITIES_TODAY);
                        writeActivitiesToDb(activityJsons);
                        break;
                    case CommunicationConstants.WEAR_PATH+CommunicationConstants.DELETED_ACTIVITY_PATH:
                        ArrayList<String> deletedActivityJsons = dataMap.getStringArrayList(CommunicationConstants.KEY_ACTIVITIES_TODAY);
                        deleteActivitiesFromDb(deletedActivityJsons);
                        break;
                    case CommunicationConstants.WEAR_PATH+CommunicationConstants.ACTIVITY_TYPE_PATH:
                        ArrayList<String> activityTypeJsons = dataMap.getStringArrayList(CommunicationConstants.KEY_ACTIVITY_TYPES);
                        addActivityTypesToDb(activityTypeJsons);
                        break;
                    case CommunicationConstants.WEAR_PATH+CommunicationConstants.HRV_PATH:
                        ArrayList<String> hrvJsons = dataMap.getStringArrayList(CommunicationConstants.KEY_HRVS_TODAY);
                        writeHRVsToDb(hrvJsons);
                        break;

                    case CommunicationConstants.WEAR_PATH+CommunicationConstants.PROFILE_PATH:
                        String profileJson = dataMap.getString(CommunicationConstants.KEY_PROFILE);
                        writeProfileToDb(profileJson);
                        break;

                    default:
                        return;
                }
            }
        }
    }




    private void writeActivitiesToDb(ArrayList<String> activityJsons){

        try {

            ActivityDatabaseHandler dbHandler = new ActivityDatabaseHandler(this);
            for(String jsonString: activityJsons){
                ActivityDatabase nextActivity = new ActivityDatabase(new JSONObject(jsonString));
                dbHandler.addIfNotExists(nextActivity,true);
            }

            //Update Wear UI
            Intent intent = new Intent();
            intent.setAction(getString(R.string.refresh_wear_overview_broadcast));
            sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "added "+activityJsons.size()+" new Activities");

    }

    private void deleteActivitiesFromDb(ArrayList<String> activityJsons){
        try {

            ActivityDatabaseHandler dbHandler = new ActivityDatabaseHandler(this);
            for(String jsonString: activityJsons){
                ActivityDatabase nextActivity = new ActivityDatabase(new JSONObject(jsonString));
                dbHandler.deleteEntryWithoutSync(nextActivity);
            }

            //Update Wear UI
            Intent intent = new Intent();
            intent.setAction(getString(R.string.refresh_wear_overview_broadcast));
            sendBroadcast(intent);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void writeHRVsToDb(ArrayList<String> hrvJsons ){

        HRVDatabaseHandler dbHandler = new HRVDatabaseHandler(this);
        for(String hrvString: hrvJsons){
            HRVDatabase entry = new HRVDatabase(hrvString);

            Log.d(TAG, "writeHRVsToDb: Trying to add Entry "+entry.getTimestamp()+" "+entry.getComputedStress());
            boolean added = dbHandler.addIfNotExist(entry);
            Log.d(TAG, "writeHRVsToDb: Added? "+added);
        }
        // Update the Complication to show whether HR Recording is active
        ComponentName provider = new ComponentName(getApplicationContext(), ComputedStressComplicationProvider.class);
        new ProviderUpdateRequester(getApplicationContext(), provider)
                .requestUpdateAll();


        //Manually check if new Notification should be sent:
        NotificationAlarmReceiver n = new NotificationAlarmReceiver(this);
        n.onReceive(this,null);
    }

    private void addActivityTypesToDb(final ArrayList<String> typeJSONS){
        Log.d(TAG, "addActivityTypesToDb: Trying to add Activity Types to DB");
        final ActivityTypeDatabaseHandler dbHandler = new ActivityTypeDatabaseHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<ActivityTypeDatabase> types = new ArrayList<>();
                for(String entryJSON: typeJSONS){
                    try {
                        ActivityTypeDatabase entry;
                        entry = new ActivityTypeDatabase(new JSONObject(entryJSON));
                        types.add(entry);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                dbHandler.replaceDBWith(types);

            }
        }).start();
    }


    private void writeProfileToDb(String profileJson) {
        ProfileDatabase entry = new ProfileDatabase(profileJson);
        ProfileDatabaseHandler dbHandler = new ProfileDatabaseHandler(this);

        Log.d(TAG, "writeProfileToDb: "+entry.getName());
        Log.d(TAG, "writeProfileToDb: "+entry.getGoogleID());
        if (dbHandler.getEntryCount() > 0) {
            ProfileDatabase oldEntry = dbHandler.getEntry(1);
            entry.setId(oldEntry.getId());
            dbHandler.updateEntry(entry);
        }
        else{
            dbHandler.addEntry(entry);
        }

        // If profiles are synchronized before the app is opened a context is needed.
        if(App.context == null){
            App.context = getApplicationContext();
        }

        // This is always true except when getApplicationContext() returns null.
        if(App.context != null){
            AnalyticsHelper.getInstance().resetGoogleID();
        }


    }

}
