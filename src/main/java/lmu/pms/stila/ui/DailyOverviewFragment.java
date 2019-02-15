package lmu.pms.stila.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.wear.widget.WearableRecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;

import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityDatabaseHandler;
import lmu.pms.stila.R;
import lmu.pms.stila.analytics.AnalyticsHelper;
import lmu.pms.stila.onboarding.FirstStartCheckerWatch;


/**
 * This Class represents the DailyOverview Fragment of the App. It's the Start Screen when
 * the App is opened.
 */
public class DailyOverviewFragment extends Fragment {

    private static final String TAG = DailyOverviewFragment.class.getSimpleName();
    private WearableRecyclerView mRecyclerView;
    private BroadcastReceiver mUpdateUIReceiver;
    private DailyOverviewAdapter mAdapter;
    private TextView mTutorialTextView;


    public DailyOverviewFragment() {
        // Required empty public constructor
    }

    public static DailyOverviewFragment newInstance() {
        DailyOverviewFragment fragment = new DailyOverviewFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /**
         * If this is the first visit of the user the activity is instantly killed to show the
         * newly picked watch face:
         */
        boolean isFirstTimeUser = FirstStartCheckerWatch.isFirstStart(getContext());
        if(isFirstTimeUser){
            // On the next visit, the activity won't be killed:
            FirstStartCheckerWatch.setFirstStart(getContext(),false);
            getActivity().finish();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        AnalyticsHelper.getInstance().trackScreen(AnalyticsHelper.DeviceType.WATCH,this.getClass().getSimpleName());

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_daily_overview, container, false);

        mRecyclerView = (WearableRecyclerView) view.findViewById(R.id.daily_overview_recycler_view);
        mRecyclerView.setEdgeItemsCenteringEnabled(false);

        mRecyclerView.setLayoutManager(
                new LinearLayoutManager(getActivity()));

        mTutorialTextView = (TextView) view.findViewById(R.id.overview_tutorial_textview);

        refreshView();

       return view;
    }


    @Override
    public void onResume() {
        super.onResume();

        // Receive Broadcast when data has changed
        // to reload activity list:
        IntentFilter filter = new IntentFilter();
        filter.addAction(getActivity().getString(R.string.refresh_wear_overview_broadcast));

        mUpdateUIReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                refreshView();

            }
        };
        getActivity().registerReceiver(mUpdateUIReceiver,filter);

        refreshView();
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(mUpdateUIReceiver);
    }


    /**
     * Gets the activity data from the DB and invalidates the Recycler View
     */
    private void refreshView(){

        // Load Activity Data from Watch:
        ActivityDatabaseHandler dbHandler = new ActivityDatabaseHandler(getContext());

        // Gets the midnight timestamp of the current timezone
        Calendar date = Calendar.getInstance();
        date.set(Calendar.HOUR_OF_DAY, 0);
        date.set(Calendar.MINUTE, 0);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);

        ArrayList<ActivityDatabase> activitiesToday =  dbHandler.getEntriesToday(date.getTimeInMillis()/1000);

        mAdapter = new DailyOverviewAdapter(getActivity(), activitiesToday);

        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.invalidate();

        if(mAdapter.getItemCount() > 1){
            mTutorialTextView.setVisibility(View.GONE);
            mRecyclerView.setPadding(0,40,0,80);
        }
        else{
            mTutorialTextView.setVisibility(View.VISIBLE);
        }


    }



}
