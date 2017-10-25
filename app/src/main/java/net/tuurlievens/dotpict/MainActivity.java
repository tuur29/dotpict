package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

public class MainActivity extends FragmentActivity implements DimensionDialogFragment.DimensionDialogListener, DrawFragment.DrawFragmentListener, SavesFragment.SavesFragmentListener, ColorPickerDialogListener {

    private boolean dialogOpen = false;
    private boolean dualpane = false;
    private boolean firsttime = true;
    private DrawFragment drawfragment;
    private SavesFragment savesfragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // portrait or landscape mode
        savesfragment = (SavesFragment) getSupportFragmentManager().findFragmentById(R.id.savesfragment);
        dualpane = savesfragment != null;

        drawfragment = (DrawFragment) getSupportFragmentManager().findFragmentById(R.id.drawfragment);

        if (savedInstanceState != null) {
            dialogOpen = savedInstanceState.getBoolean("dialogOpen");
            firsttime = false;
        } else {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    onDialogPositiveClick(20,20);
                }
            }, 500);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean("dialogOpen", dialogOpen);
        // TODO: second time landscape mode crashes app
        super.onSaveInstanceState(savedInstanceState);
    }

    private void openDialog() {
        // prevent multiple dialog on orientation change
        if (dialogOpen)
            return;
        dialogOpen = true;

        // ask for number of rows/cols
        DialogFragment dialog = new DimensionDialogFragment();
        dialog.setCancelable(false);
        dialog.show(getSupportFragmentManager(), "dialog");
    }

    @Override
    public void onDialogPositiveClick(int rows, int columns) {
        // make canvas
        dialogOpen = false;
        drawfragment.generate(rows,columns);

        // inform people of secondary tools
        if (firsttime)
            Toast.makeText(MainActivity.this, R.string.toolsmessage, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onCanvasResetted() {
        openDialog();
        firsttime = false;
    }

    @Override
    public void onCanvasSaved(String key, String data) {
        // save new drawing to file
        SharedPreferences sharedPref = getSharedPreferences("saves", Context.MODE_PRIVATE);
        sharedPref.edit().putString(key,data).apply();
        String message = getString(R.string.savedmessage) + (!dualpane ? "\n"+getString(R.string.longpresssavemessage) : "");
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();

        // immediately add to list
        if (dualpane)
            savesfragment.addSave(key);
    }

    @Override
    public void openSaves() {
        // open savefragment for portrait mode
        Intent intent = new Intent(MainActivity.this, SavesActivity.class);
        startActivityForResult(intent, 1);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
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

        drawfragment.tempPixelColors = pixels;
        drawfragment.generate(rows,columns);
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        drawfragment.setColor(color);
    }
    @Override
    public void onCloseSavesFragment() {}
    @Override
    public void onDialogDismissed(int dialogId) { }
}
