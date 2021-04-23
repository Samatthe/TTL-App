package com.solid.circuits.TelTail;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class SetupWizardConnectorsOrientation extends Fragment {

    public Spinner InputSpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.setup_wizard_connector_orientation, container, false);

        InputSpinner = rootView.findViewById(R.id.wizard_connector_orientation_spinner);
        ArrayAdapter<CharSequence> InputSpinnerAdapter= ArrayAdapter.createFromResource(getActivity(),
                R.array.wizard_orientation_array, R.layout.wizard_spinner_item);
        InputSpinnerAdapter.setDropDownViewResource(R.layout.wizard_spinner_item);
        InputSpinner.setAdapter(InputSpinnerAdapter);
        InputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((SetupWizardActivity)getActivity()).ConnectorOrientationValue = ""+(parent.getSelectedItemPosition()+1);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return rootView;
    }
}