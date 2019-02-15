package lmu.pms.stila.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import lmu.pms.stila.Database.ActivityDatabase;
import lmu.pms.stila.Database.ActivityTypeDatabase;
import lmu.pms.stila.R;
import lmu.pms.stila.SysConstants.Constants;
import lmu.pms.stila.Utils.IconUtil;

/**
 * This Class represents an Adapter for the Recycler View in the Select Activity of the App
 */
public class SelectAdapter extends WearableRecyclerView.Adapter<SelectAdapter.SelectViewHolderItem> {

    String TAG = SelectAdapter.class.getSimpleName();
    private ArrayList<ActivityTypeDatabase> mActivityTypes;
    private Context mContext;

    private static final int VIEW_TYPE_HEADLINE = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public SelectAdapter(Context context, ArrayList<ActivityTypeDatabase> dataset){
        super();
        mActivityTypes = dataset;
        mContext = context;
    }

    @Override
    public SelectViewHolderItem onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == VIEW_TYPE_ITEM){
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_overview_item, parent, false);
            return new SelectViewHolderItem(v);
        }
        else{
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.daily_overview_headline, parent, false);
            return new SelectViewHolderItem(v);
        }
    }

    @Override
    public void onBindViewHolder(SelectViewHolderItem holder, int position) {
        if(position > 0){
            String name = mActivityTypes.get(position-1).getName();
            holder.activityNameTextView.setText(name);
            // Changing the color to white programatically to allow for usage
            // of icons from the Phone App
            Drawable normalDrawable = mContext.getDrawable(IconUtil.getIconFromId(mActivityTypes.get(position-1).getIconID()));
            // Default if Icon is not found
            if(normalDrawable == null){
               normalDrawable = mContext.getDrawable(R.drawable.ic_work_white_36dp);
            }
            Drawable wrapDrawable = DrawableCompat.wrap(normalDrawable);
            DrawableCompat.setTint(wrapDrawable, mContext.getColor(R.color.white));
            holder.icon.setImageDrawable(wrapDrawable);
        }


    }

    @Override
    public int getItemCount() {
        return mActivityTypes.size()+1;
    }

    @Override
    public int getItemViewType(int position) {
        if(position == 0){
            return VIEW_TYPE_HEADLINE;
        }
        else{
            return VIEW_TYPE_ITEM;
        }
    }

    public String getActivityName(int position){
        return mActivityTypes.get(position).getName();
    }

    public class SelectViewHolderItem extends WearableRecyclerView.ViewHolder {
        protected TextView activityNameTextView;
        protected TextView from;
        protected TextView to;
        protected TextView line;
        protected ImageView icon;

        public SelectViewHolderItem(View itemView) {
            super(itemView);

            if(itemView.findViewById(R.id.overview_headline_textview)!=null){
                TextView headerTextView = itemView.findViewById(R.id.overview_headline_textview);
                headerTextView.setText(R.string.select_activity_to_track);

            }
            else{
                activityNameTextView = itemView.findViewById(R.id.daily_overview_text_view);
                icon = itemView.findViewById(R.id.daily_overview_icon);

                activityNameTextView = itemView.findViewById(R.id.daily_overview_text_view);
                icon = itemView.findViewById(R.id.daily_overview_icon);
                from = itemView.findViewById(R.id.overview_from_textview);
                line = itemView.findViewById(R.id.overview_line_textview);
                to = itemView.findViewById(R.id.overview_to_textview);

                // Not displayed in select mode:
                from.setVisibility(View.GONE);
                to.setVisibility(View.GONE);
                line.setVisibility(View.GONE);

            }

        }
    }

}