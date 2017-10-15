package net.tuurlievens.dotpict;

import android.app.Activity;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

public class DimensionDialogFragment extends DialogFragment {

    public interface DimensionDialogListener {
        void onDialogPositiveClick(int rows, int cols);
    }

    DimensionDialogListener dListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            dListener = (DimensionDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement DimensionDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(inflater.inflate(R.layout.dialog, null, false))
        .setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            // get input
            AlertDialog d = (AlertDialog) dialog;
            SeekBar rows = d.findViewById(R.id.seekBarRows);
            SeekBar columns = d.findViewById(R.id.seekBarColumns);
            dialog.dismiss();
            dListener.onDialogPositiveClick(rows.getProgress() + 3, columns.getProgress() + 3 );
            }
        });

        return builder.create();
    }
}
