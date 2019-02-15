package lmu.pms.stila.onboarding;

import android.app.WallpaperManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import lmu.pms.stila.R;
import lmu.pms.stila.watchface.StilaAnalogWatchFaceService;

/**
 * This class presents a custom Fragment, which shows the user a button to open the watch
 * face picker.
 */
public class PickWatchfaceOnboardingFragment extends Fragment {

    public PickWatchfaceOnboardingFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_last_onboarding, container, false);

        Button button = (Button)view.findViewById(R.id.btn_choose_watchface);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickWatchFace(getContext());
            }
        });
        return view;
    }

    /**
     * This methods presents the watch face picker and scrolls to the stila watchface
     * @param context
     */
    public void pickWatchFace(Context context) {
        ComponentName stilaWatchFace = new ComponentName(context, StilaAnalogWatchFaceService.class);
        Intent intent = new Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
                .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, stilaWatchFace)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);
        // Close the Tutorial/Onboarding activity
        getActivity().finish();
    }

}
