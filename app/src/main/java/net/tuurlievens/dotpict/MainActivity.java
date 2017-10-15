package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DimensionDialogFragment.DimensionDialogListener, ColorPickerDialogListener {

    private ViewGroup body;
    private ProgressBar progressBar;
    private FloatingActionButton colorButton;
    private FloatingActionButton fillButton;
    private FloatingActionButton pickerButton;
    private FloatingActionButton clearButton;

    private int color;
    private boolean dialogOpen = false;
    private boolean pickingColor = false;

    private ViewGroup canvas;
    private ArrayList<Button> buttons = new ArrayList<>();
    private int buttonCentreOffset = 0;

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("dialogOpen", dialogOpen);
        savedInstanceState.putInt("color", color);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        body = findViewById(R.id.body);
        progressBar = findViewById(R.id.progress_loader);
        colorButton = findViewById(R.id.colorButton);
        fillButton = findViewById(R.id.fillButton);
        pickerButton = findViewById(R.id.pickerButton);
        clearButton = findViewById(R.id.clearButton);

        if (savedInstanceState != null) {
            dialogOpen = savedInstanceState.getBoolean("dialogOpen");
            setColor(savedInstanceState.getInt("color"));
        } else {
            setColor(ColorUtils.setAlphaComponent( getResources().getColor(R.color.colorAccent), 255));
        }

        openDialog();

        // open color picker
        // Source: https://github.com/jaredrummler/ColorPicker
        colorButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.newBuilder().setAllowPresets(true).setColor(color).show(MainActivity.this);
                hideExtraTools();
            }
        });

        // show other floating action buttons on drag
        colorButton.setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (fillButton.getVisibility() == View.VISIBLE) {
                    hideExtraTools();
                } else {
                    showExtraTools();
                }
                return true;
            }
        });

        // fill canvas on button click
        pickerButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickingColor = true;
                Toast.makeText(getApplicationContext(), R.string.colorpickermessage, Toast.LENGTH_SHORT).show();
            }
        });

        // fill canvas on button click
        fillButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Button button : buttons) {
                    button.setBackgroundColor(color);
                }
                hideExtraTools();
            }
        });

        // reset canvas on button click
        clearButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttons.clear();
                body.removeView(canvas);
                hideExtraTools();
                openDialog();
            }
        });
    }

    private void openDialog() {
        // prevent multiple dialog on orientation change
        if (dialogOpen)
            return;
        dialogOpen = true;

        // ask for number of rows/cols
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment dialog = new DimensionDialogFragment();
        dialog.setCancelable(false);
        dialog.show(fm, "dialog");
    }

    @Override
    public void onDialogPositiveClick(final int rows, final int columns) {

        dialogOpen = false;
        // show loading spinner
        progressBar.setVisibility(View.VISIBLE);

        // make canvas
        final TouchableLinearView canvas = new TouchableLinearView(MainActivity.this);
        canvas.setBackgroundColor(Color.WHITE);
        canvas.setElevation(8);

        new Thread(new Runnable() {
            public void run() {
                // setup canvas buttons
                for (int i = 0; i < columns; i++) {
                    // make rows
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.VERTICAL);

                    for (int j = 0; j < rows; j++) {
                        // calculate best button size
                        int calculatedHeight = body.getMeasuredHeight() / rows;
                        int calculatedWidth = body.getMeasuredWidth() / columns;
                        int calculatedSize = calculatedHeight < calculatedWidth ? calculatedHeight : calculatedWidth;
                        buttonCentreOffset = calculatedSize;

                        // make button
                        Button button = new Button(MainActivity.this);
                        button.setHeight(calculatedSize);
                        button.setWidth(calculatedSize);
                        button.setMinimumHeight(5);
                        button.setMinimumWidth(5);
                        button.setBackgroundColor(Color.WHITE);
                        buttons.add(button);
                        row.addView(button);
                    }

                    canvas.addView(row);
                }
                addCanvas(canvas);
            }
        }).start();

        // inform people of secondary tools
        Toast.makeText(getApplicationContext(), R.string.toolsmessage, Toast.LENGTH_SHORT).show();
    }

    private void addCanvas(final ViewGroup canvas) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                body.addView(canvas, 0);
                body.invalidate();
                registerCanvas(canvas);
            }
        });

    }

    private void registerCanvas(ViewGroup canvas) {

        // touch 'canvas'
        canvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();

                // find button under current coordinates
                for (Button button : buttons) {
                    int params[] = new int[2];
                    button.getLocationOnScreen(params);

                    if ( x >= params[0] - buttonCentreOffset && x <= params[0] + buttonCentreOffset ) {
                        if ( y >= params[1] - buttonCentreOffset && y <= params[1] + buttonCentreOffset ) {
                            if (pickingColor) {
                                setColor(((ColorDrawable) button.getBackground()).getColor());
                                pickingColor = false;
                            } else {
                                button.setBackgroundColor(color);
                            }
                            break;
                        }
                    }
                }

                return true;
            }
        });

        this.canvas = canvas;
    }

    private void showExtraTools() {
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton} )
            button.setVisibility(View.VISIBLE);
    }

    private void hideExtraTools() {
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton} )
            button.setVisibility(View.INVISIBLE);
    }

    private void setColor(int color) {
        this.color = color;
        for (FloatingActionButton button : new FloatingActionButton[] {colorButton,fillButton,pickerButton,clearButton} )
            button.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        setColor(color);
    }
    @Override
    public void onDialogDismissed(int dialogId) {}
}
