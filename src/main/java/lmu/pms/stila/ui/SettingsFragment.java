package lmu.pms.stila.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.config.ConfigureAdapter;
import lmu.pms.stila.config.RecyclerItemClickListener;
import lmu.pms.stila.model.HeartRateDbHelper;
import lmu.pms.stila.model.WatchfaceConfigData;


/**
 * This Class represents the Settings Fragment which shows a List of all possible Settings and
 * options
 */
public class SettingsFragment extends Fragment {

    public final static String TAG = SettingsFragment.class.getSimpleName();
    private WearableRecyclerView mRecyclerView;
    private WatchfaceConfigData configData ;

    public SettingsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment SettingsFragment.
     *
     **/
    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());
        configData  = new WatchfaceConfigData(getActivity());

        View view = inflater.inflate(R.layout.activity_stila_digital_config, container, false);
        mRecyclerView = (WearableRecyclerView) view.findViewById(R.id.digital_configure_recycler_view);
        mRecyclerView.setEdgeItemsCenteringEnabled(true);



        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity()));
        ConfigureAdapter adapter = new ConfigureAdapter(getActivity());


        mRecyclerView.setAdapter(adapter);

        mRecyclerView.addOnItemTouchListener(
                new RecyclerItemClickListener(getActivity(), mRecyclerView ,new RecyclerItemClickListener.OnItemClickListener() {
                    @Override public void onItemClick(View view, int position) {

                        Intent intent = new Intent(getActivity(),configData.getConfigActivity(position));
                        startActivity(intent);


                    }

                    @Override public void onLongItemClick(View view, int position) {
                    }
                })
        );

        return view;
    }


    /**
     * Debug Method to seed the DB with HR Data.

    private void seedDBwithData(){
        Log.d(TAG, "seedDBwithData: start");
        final Context context = getContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                long timeStampStart = 1528783200;
                long timeStampStop = 1528819200;
                long size = timeStampStop - timeStampStart;
                HeartRateDbHelper dbHelper = HeartRateDbHelper.getInstance(context);
                for(long i = timeStampStart; i<timeStampStop; i+=25){
                    Random rand = new Random();
                    int hr = rand.nextInt(50) + 60; // randomly creates HRs between 60 and 110
                    dbHelper.addEntry(hr,i);
                    Log.d(TAG, "Left: "+String.valueOf(timeStampStop-i));
                }
                dbHelper.close();
                Log.d(TAG, "seedDBwithData: Finished seeding");

            }
        }).start();


    }
     */

}
