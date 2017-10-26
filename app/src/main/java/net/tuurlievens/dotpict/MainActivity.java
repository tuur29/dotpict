package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

public class MainActivity extends FragmentActivity implements DimensionDialogFragment.DimensionDialogListener, DrawFragment.DrawFragmentListener, SavesFragment.SavesFragmentListener, ColorPickerDialogListener {

    private DrawFragment drawfragment;
    private SavesFragment savesfragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawfragment = (DrawFragment) getSupportFragmentManager().findFragmentById(R.id.drawfragment);
        if (findViewById(R.id.portrait) == null) {
            savesfragment = (SavesFragment) getSupportFragmentManager().findFragmentById(R.id.savesfragment);
        } else {
            savesfragment = new SavesFragment();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        // TODO: second time landscape mode crashes app
        super.onSaveInstanceState(savedInstanceState);
    }

    // dimensions dialog

    private void askForDimensions() {
        // ask for number of rows/cols
        DialogFragment dialog = new DimensionDialogFragment();
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onDialogPositiveClick(int rows, int columns) {
        // make canvas on dialog dimensions dialog positive click
        drawfragment.generate(rows,columns);
    }

    @Override
    public void onDialogDismissed(int dialogId) { }


    // canvasview

    @Override
    public void onCanvasResetted() {
        askForDimensions();
    }

    @Override
    public void onCanvasSaved(String key, String data) {
        // save new drawing to file on savename dialog positive click
        SharedPreferences sharedPref = getSharedPreferences("saves", Context.MODE_PRIVATE);
        sharedPref.edit().putString(key,data).apply();
        String message = getString(R.string.savedmessage) + (savesfragment!=null ? "\n"+getString(R.string.longpresssavemessage) : "");
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

        // update list when in portrait mode
        if (savesfragment != null)
            savesfragment.addSave(key);
    }

    // loading saves

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // load selected save when pressing from savesactivity
        if (requestCode == 1 && RESULT_OK == resultCode) {
            onSaveLoad(data.getStringExtra("key"));
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onSaveLoad(String key) {
        // get data from storage

        SharedPreferences sharedPref = getSharedPreferences("saves", Context.MODE_PRIVATE);
        String[] arr = sharedPref.getString(key,"").split(";");
        int rows = Integer.parseInt(arr[0]);
        int columns = Integer.parseInt(arr[1]);
        String[] colors = arr[2].split(",");

        // parse colors
        int[] pixels = new int[colors.length];
        for (int i=0;i<colors.length;i++)
            pixels[i] = Integer.parseInt(colors[i]);

        drawfragment.generate(rows,columns,pixels);
    }

    // color picker

    @Override
    public void onColorSelected(int dialogId, int color) {
        drawfragment.setColor(color);
    }

    // open saves fragment

    @Override
    public void openSaves() {
        if (findViewById(R.id.portrait) != null) {
            Bundle bundle = new Bundle();
            bundle.putBoolean("singlepane", true);
            savesfragment.setArguments(bundle);

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.portrait, savesfragment).addToBackStack(null).commit();
        }
    }

    @Override
    public void onCloseSavesFragment() {
        if (findViewById(R.id.portrait) != null) {
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.remove(savesfragment).commit();
        }
    }

}
