package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

// TODO: fix fragments
// TODO: save to sqlite instead of settings
// TODO: crashes on picture convert to high row count

public class MainActivity extends FragmentActivity implements DimensionDialogFragment.DimensionDialogListener, DrawFragment.DrawFragmentListener, SavesFragment.SavesFragmentListener, ColorPickerDialogListener {

    private boolean dualpane = false;
    private DrawFragment drawfragment;
    private SavesFragment savesfragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        drawfragment = (DrawFragment) getSupportFragmentManager().findFragmentById(R.id.drawfragment);
        savesfragment = (SavesFragment) getSupportFragmentManager().findFragmentById(R.id.savesfragment);
        if (findViewById(R.id.savesparent) == null) {
            dualpane = true;
        } else {
            findViewById(R.id.closebutton).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    onCloseSavesFragment();
                }
            });
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
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
        String message = getString(R.string.savedmessage) + (!dualpane ? "\n"+getString(R.string.longpresssavemessage) : "");
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && findViewById(R.id.savesparent)!= null && findViewById(R.id.savesparent).getVisibility() == View.VISIBLE ) {
            onCloseSavesFragment();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void openSaves() {
        if (findViewById(R.id.savesparent) != null) {
            findViewById(R.id.savesparent).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onCloseSavesFragment() {
        if (findViewById(R.id.savesparent) != null)
            findViewById(R.id.savesparent).setVisibility(View.GONE);
    }

}
