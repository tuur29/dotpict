package net.tuurlievens.dotpict;

import android.app.Dialog;
import android.content.Context;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.widget.SeekBar;

public class DimensionDialogFragment extends DialogFragment {

    private DimensionDialogListener dListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            dListener = (DimensionDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DimensionDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater inflater = getActivity().getLayoutInflater();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(inflater.inflate(R.layout.dialog, null, false)).setPositiveButton(R.string.generate, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            // get number of rows & columns
            AlertDialog d = (AlertDialog) dialog;
            SeekBar rows = d.findViewById(R.id.seekBarRows);
            SeekBar columns = d.findViewById(R.id.seekBarColumns);
            dialog.dismiss();
            dListener.onDialogPositiveClick(rows.getProgress() + 5, columns.getProgress() + 5 );
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        return builder.create();
    }

    public interface DimensionDialogListener {
        void onDialogPositiveClick(int rows, int cols);
    }
}
