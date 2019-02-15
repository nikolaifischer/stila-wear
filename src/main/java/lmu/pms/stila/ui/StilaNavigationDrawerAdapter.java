package lmu.pms.stila.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.wear.internal.widget.drawer.WearableNavigationDrawerPresenter;
import android.support.wear.widget.drawer.WearableNavigationDrawerView;

import lmu.pms.stila.R;


/**
 * This class manages the data for the Navigational Drawer.
 */
public class StilaNavigationDrawerAdapter extends WearableNavigationDrawerView.WearableNavigationDrawerAdapter {
    private Context mContext;

    public StilaNavigationDrawerAdapter(Context context){
        mContext = context;
    }

    @Override
    public CharSequence getItemText(int pos) {
        switch(pos){
            case 0: return "Activities";
            case 1: return "Graph";
            case 2: return "Settings";
            default: return "Not Implemented";
        }
    }

    @Override
    public Drawable getItemDrawable(int pos) {
        int drawableId;
        switch(pos){
            case 0: drawableId = R.drawable.ic_home_white; break;
            case 1: drawableId = R.drawable.ic_timeline_white; break;
            case 2: drawableId = R.drawable.ic_settings_white; break;
            default: drawableId = R.drawable.ic_home_white;
        }
        Drawable drawable = mContext.getDrawable(drawableId);
        return drawable;
    }

    @Override
    public int getCount() {
        return 3;
    }

    
}
