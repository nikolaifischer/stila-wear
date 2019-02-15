package lmu.pms.stila.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabaseHandler;
import lmu.pms.stila.R;
import lmu.pms.stila.SysConstants.Constants;
import lmu.pms.stila.Utils.IconUtil;
import lmu.pms.stila.common.App;

/**
 * This Class represents an Adapter for the Recycler View in the Main Activity of the App
 */
public class DailyOverviewAdapter extends WearableRecyclerView.Adapter<DailyOverviewAdapter.DailyOverviewViewHolder> {

    private String TAG = "ConfigureAdapter";
    private ArrayList<ActivityDatabase> mActivites;
    private Context mContext;

    /**
     * An Element in this RecyclerView can have 2 Modes:
     * 1. Normal Item
     * 3. Headline Item
     *
     * These Constants and enum are used to manage the modes.
     */

    private static final int VIEW_TYPE_HEADLINE = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public enum Mode{
        HEADLINE, OVERVIEW
    }

    public DailyOverviewAdapter(Context context, ArrayList<ActivityDatabase> dataset){
        super();
        mActivites = dataset;
        mContext = context;
    }

    @Override
    public DailyOverviewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_ITEM){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_overview_item, parent, false);
            DailyOverviewViewHolder vh = new DailyOverviewViewHolder(v, Mode.OVERVIEW);
            return vh;
        }
        else{
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_overview_headline, parent, false);
            DailyOverviewViewHolder vh = new DailyOverviewViewHolder(v, Mode.HEADLINE);
            return vh;
        }
    }

    @Override
    public void onBindViewHolder(DailyOverviewViewHolder holder, int position) {
        // Don't use the Headline ViewHolder (Position 0)
        if(position>0){
            ActivityDatabase currentElement = mActivites.get(position-1);
            String name = currentElement.getActivity();
            String mood = moodToText(currentElement);
            String stress = stressToText(currentElement);
            long fromTimestamp = currentElement.getStartTimestamp();
            long toTimestamp = currentElement.getEndTimestamp();
            Date fromTimestampAsDate = new Date(fromTimestamp * 1000);
            Date toTimestampAsDate = new Date(toTimestamp * 1000);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
            sdf.setTimeZone(Calendar.getInstance().getTimeZone());
            String fromString = sdf.format(fromTimestampAsDate);
            String toString = sdf.format(toTimestampAsDate);
            holder.from.setText(fromString);
            holder.to.setText(toString);
            holder.activityNameTextView.setText(name);
            holder.mood.setText(mood);
            holder.stress.setText(stress);
            //Get the Appropriate Icon from the Constants class.
            ActivityTypeDatabaseHandler activityTypeDatabaseHandler = new ActivityTypeDatabaseHandler(App.context);
            ActivityTypeDatabase entry = activityTypeDatabaseHandler.getEntryByName(name);
            int id = entry == null ? -1 : entry.getIconID();
            Drawable normalDrawable = mContext.getDrawable(IconUtil.getIconFromId(id));
            Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
            DrawableCompat.setTint(wrapDrawable, mContext.getColor(R.color.white));
            holder.icon.setImageDrawable(wrapDrawable);
        }

    }

    private String stressToText(ActivityDatabase element){
        int stressInteger = element.getStressLevel();
        String stressString = "";

        switch (stressInteger){
            case -2:
                stressString = "very relaxed";
                break;
            case -1:
                stressString = "relaxed";
                break;
            case 0:
                stressString = "neutral";
                break;
            case 1:
                stressString = "stressed";
                break;
            case 2:
                stressString = "very stressed";
                break;
            default:
                stressString = "neutral";
        }

        return  stressString;
    }

    private String moodToText(ActivityDatabase element) {
        String moodUnformatted = element.getMood();
        if(moodUnformatted.equals("")){
            moodUnformatted = "neutral";
        }
        String formatted = moodUnformatted.toLowerCase();
        return formatted;

    }
    @Override
    public int getItemCount() {
        return mActivites.size()+1; // +1 because of the Headline
    }

    /**
     * Gets the View Type
     * @param position the index in the list
     * @return whether the item is a Headline or a normal item
     */
    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return VIEW_TYPE_HEADLINE;
        }
        else{
            return VIEW_TYPE_ITEM;
        }
    }

    public class DailyOverviewViewHolder extends WearableRecyclerView.ViewHolder {
        public TextView activityNameTextView;
        public TextView from;
        public TextView to;
        public TextView line;
        public ImageView icon;
        public TextView stress;
        public TextView mood;


        public DailyOverviewViewHolder(View itemView, Mode mode) {
            super(itemView);
            // HEADLINE
            if(mode == Mode.HEADLINE){
                TextView headerTextView = itemView.findViewById(R.id.overview_headline_textview);
                headerTextView.setText("Today's Activities");
            }
            // LIST ITEM
            else if (mode == Mode.OVERVIEW){
                activityNameTextView = itemView.findViewById(R.id.daily_overview_text_view);
                icon = itemView.findViewById(R.id.daily_overview_icon);
                from = itemView.findViewById(R.id.overview_from_textview);
                to = itemView.findViewById(R.id.overview_to_textview);
                stress = itemView.findViewById(R.id.stress);
                mood = itemView.findViewById(R.id.mood);
            }
        }
    }



}