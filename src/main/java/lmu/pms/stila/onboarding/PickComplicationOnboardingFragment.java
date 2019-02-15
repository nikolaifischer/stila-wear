package lmu.pms.stila.onboarding;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import lmu.pms.stila.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class PickComplicationOnboardingFragment extends Fragment {


    public PickComplicationOnboardingFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_pick_complication_onboarding, container, false);
        return view;
    }

}
