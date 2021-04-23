package com.solid.circuits.TelTail;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class SetupWizardControls extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.setup_wizard_controls, container, false);

        ((SetupWizardActivity)getActivity()).UpdateControlsText((TextView)rootView.findViewById(R.id.wizard_controls_text1));
        return rootView;
    }
}
