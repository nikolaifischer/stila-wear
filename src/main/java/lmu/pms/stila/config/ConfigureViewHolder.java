package lmu.pms.stila.config;

import android.support.wear.widget.WearableRecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import lmu.pms.stila.R;

/**
 * Created by Niki on 03.02.2018.
 */

public class ConfigureViewHolder extends WearableRecyclerView.ViewHolder {
    public TextView optionTextView;
    public ImageView icon;
    public TextView headerTextView;

    public ConfigureViewHolder(View itemView) {
        super(itemView);
        optionTextView = itemView.findViewById(R.id.digital_config_option_text_view);
        icon = itemView.findViewById(R.id.digital_config_option_icon);
    }
}
