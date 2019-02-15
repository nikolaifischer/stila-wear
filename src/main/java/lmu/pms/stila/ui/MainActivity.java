package lmu.pms.stila.ui;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.wear.ambient.AmbientModeSupport;
import android.support.wear.widget.drawer.WearableActionDrawerView;
import android.support.wear.widget.drawer.WearableDrawerLayout;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

import lmu.pms.stila.Database.HRVDatabase;
import lmu.pms.stila.Database.HRVDatabaseHandler;
import lmu.pms.stila.common.App;
import lmu.pms.stila.communication.HeartrateSyncJobService;
import lmu.pms.stila.communication.MessageToPhoneUtil;
import lmu.pms.stila.model.HeartRateDbHelper;
import lmu.pms.stila.model.RunningActivityDbHelper;
import lmu.pms.stila.R;
import lmu.pms.stila.SysConstants.Constants;
import lmu.pms.stila.onboarding.FirstStartCheckerWatch;
import lmu.pms.stila.onboarding.OnboardingActivity;
import lmu.pms.stila.smartNotifications.NotificationAlarmReceiver;
import lmu.pms.stila.smartNotifications.NotificationHelper;
import lmu.pms.stila.smartNotifications.NotificationHelperWatch;

/**
 * This class represents the MainActivity of the Wear App. In it multiple Fragments are
 * being displayed. This Activity also manages the Navigation and Action Drawers.
 */
public class MainActivity extends FragmentActivity implements AmbientModeSupport.AmbientCallbackProvider, WearableNavigationDrawerView.OnItemSelectedListener, MenuItem.OnMenuItemClickListener {

    private final String TAG = MainActivity.class.getSimpleName();
    private WearableNavigationDrawerView mWearableNavigationDrawer;
    private WearableActionDrawerView mWearableActionDrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Check if there is a Stila-Activity being tracked right now, if yes immediately change
        // to that
        RunningActivityDbHelper runningActivityDbHelper = new RunningActivityDbHelper(this);
        if(runningActivityDbHelper.getRunningActivity()!=null){
            Intent intent = new Intent(this, StartNewActivity.class);
            startActivity(intent);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        App.context = this;

        //Start Onboarding on first start:
        if(FirstStartCheckerWatch.isFirstStart(this)){
            Intent onboardingIntent = new Intent(this, OnboardingActivity.class);
            startActivity(onboardingIntent);
        }


        // Top navigation drawer
        mWearableNavigationDrawer = (WearableNavigationDrawerView) findViewById(R.id.top_navigation_drawer);
        mWearableNavigationDrawer.setAdapter(new StilaNavigationDrawerAdapter(this));

        // Peeks navigation drawer on the top.
        mWearableNavigationDrawer.getController().peekDrawer();
        mWearableNavigationDrawer.addOnItemSelectedListener(this);
        mWearableActionDrawer = (WearableActionDrawerView) findViewById(R.id.bottom_action_drawer);

        // Hide Action Drawer on all Pages but the Overview.
        mWearableActionDrawer.setVisibility(View.INVISIBLE);
        // Peeks action drawer on the bottom.
        mWearableActionDrawer.getController().peekDrawer();
        mWearableActionDrawer.setOnMenuItemClickListener(this);

        Bundle extras = getIntent().getExtras();
        //Check if the call comes from a complication Tap. If yes change to the Stress Graph!
        if(extras != null && extras.getBoolean(getString(R.string.flag_from_complication))){
            setFragmentView(new DailyStressGraphFragment());
        }
        else {
            setFragmentView(new DailyOverviewFragment());
        }
        runningActivityDbHelper.close();
    }

    public void setFragmentView(Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        if(fragment instanceof DailyOverviewFragment){
            mWearableActionDrawer.setVisibility(View.VISIBLE);
        }
        else{
            mWearableActionDrawer.setVisibility(View.INVISIBLE);
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    /**
     * Managing ambient (powersaving) Modes of the Watch:
     * https://developer.android.com/training/wearables/apps/always-on.html#ambient-mode-class
     * @return
     */
    @Override
    public AmbientModeSupport.AmbientCallback getAmbientCallback() {
        return new MyAmbientCallback();
    }


    @Override
    public void onItemSelected(int pos) {
        switch(pos){
            case 0:
                setFragmentView(new DailyOverviewFragment()); break;
            case 1: setFragmentView(new DailyStressGraphFragment()); break;
            case 2:
                setFragmentView(new SettingsFragment()); break;
            default:
                setFragmentView(new DailyOverviewFragment()); break;
        }

    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        Intent intent;
        if(item.getTitle() == getString(R.string.menu_add_activity)){
            intent = new Intent(this, SelectActivity.class);
            startActivity(intent);
            return true;
        }
        else if (item.getTitle() == getString(R.string.menu_current_activity)){
            intent = new Intent(this, StartNewActivity.class);
            startActivity(intent);
            return true;
        }
        return false;

    }

    @Override
    protected void onResume() {
        super.onResume();

        Menu menu = mWearableActionDrawer.getMenu();
        menu.getItem(0).setVisible(false);
        // If there is an Stila-Activity being tracked, don't show the add Activity Option
        RunningActivityDbHelper runningActivityDbHelper = new RunningActivityDbHelper(this);
        if(runningActivityDbHelper.getRunningActivity()!=null){
            menu.getItem(0).setIcon(R.drawable.ic_play_arrow_white_24dp);
            menu.getItem(0).setTitle(R.string.menu_current_activity);
        }
        else{
            menu.getItem(0).setIcon(R.drawable.ic_add_white_24dp);
            menu.getItem(0).setTitle(R.string.menu_add_activity);
        }


        /**
         * Sync HR to Phone when the User opens the App to always guarantee the newest
         * Stress Data on the Watch.
         */

        // Wake up the phone from deep sleep in order to get new data:
        MessageToPhoneUtil messageToPhoneUtil = new MessageToPhoneUtil(this);
        messageToPhoneUtil.sendMessage(MessageToPhoneUtil.MessageMode.CONFIRM_ONLINE,null);
        // Send HR data even if it is not scheduled:
        new Thread(new Runnable() {
            @Override
            public void run() {
                HeartrateSyncJobService heartrateSyncJobService = new HeartrateSyncJobService();
                heartrateSyncJobService.syncHeartRateToPhone(getApplication());
            }
        }).start();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * This class intercepts the ambient-mode callbacks of the watch and ignores them
     */
    private class MyAmbientCallback extends AmbientModeSupport.AmbientCallback {
        @Override
        public void onEnterAmbient(Bundle ambientDetails) {
            // Handle entering ambient mode
        }

        @Override
        public void onExitAmbient() {
            // Handle exiting ambient mode
        }

        @Override
        public void onUpdateAmbient() {
            // Update the content
        }
    }

}
