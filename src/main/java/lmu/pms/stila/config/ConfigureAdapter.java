package lmu.pms.stila.config;

import android.content.Context;
import android.support.wear.widget.WearableRecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import lmu.pms.stila.R;
import lmu.pms.stila.model.WatchfaceConfigData;


/**
 * Adapter for Settings Data
 */

public class ConfigureAdapter extends WearableRecyclerView.Adapter<ConfigureViewHolder> {

    String TAG = "ConfigureAdapter";
    WatchfaceConfigData configData;

    public ConfigureAdapter(Context context){
        super();
        configData =  new WatchfaceConfigData(context);
    }

    @Override
    public ConfigureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.config_list_digital_item, parent, false);

        ConfigureViewHolder vh = new ConfigureViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ConfigureViewHolder holder, int position) {
         holder.optionTextView.setText(configData.getMenuOptionString(position));
         int iconResource = configData.getIcon(position);
         if(iconResource > 0)
            holder.icon.setImageResource(configData.getIcon(position));

    }

    @Override
    public int getItemCount() {
        return configData.getMenuOptionsSize();
    }


}
