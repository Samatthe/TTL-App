package com.solid.circuits.TelTail;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.fragment.app.Fragment;

public class SetupWizardWriteSave extends Fragment {

    public Spinner InputSpinner;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.setup_wizard_write_save, container, false);

        InputSpinner = rootView.findViewById(R.id.wizard_write_spinner);
        ArrayAdapter<CharSequence> InputSpinnerAdapter= ArrayAdapter.createFromResource(getActivity(),
                R.array.write_action_array, R.layout.wizard_spinner_item);
        InputSpinnerAdapter.setDropDownViewResource(R.layout.wizard_spinner_item);
        InputSpinner.setAdapter(InputSpinnerAdapter);
        InputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                ((SetupWizardActivity)getActivity()).WriteActionSelection = parent.getSelectedItemPosition();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return rootView;
    }
}
