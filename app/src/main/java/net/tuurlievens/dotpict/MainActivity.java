package net.tuurlievens.dotpict;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements DimensionDialogFragment.DimensionDialogListener,ColorPickerDialogListener {

    LinearLayout parent;
    FloatingActionButton colorButton;
    FloatingActionButton fillButton;
    FloatingActionButton clearButton;
    ArrayList<Button> buttons = new ArrayList<>();
    int color;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        color = ColorUtils.setAlphaComponent( getResources().getColor(R.color.colorAccent), 255);
        parent = findViewById(R.id.parent);
        colorButton = findViewById(R.id.colorButton);
        fillButton = findViewById(R.id.fillButton);
        clearButton = findViewById(R.id.clearButton);

        openDialog();

        // open color picker
        // Source: https://github.com/jaredrummler/ColorPicker
        colorButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.newBuilder().setAllowPresets(true).setColor(color).show(MainActivity.this);
            }
        });

        // show other floating action buttons on drag
        colorButton.setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (fillButton.getVisibility() == View.VISIBLE) {
                    fillButton.setVisibility(View.INVISIBLE);
                    clearButton.setVisibility(View.INVISIBLE);
                } else {
                    fillButton.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                }
                return true;
            }
        });

        // fill canvas on button click
        fillButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (Button button : buttons) {
                    button.setBackgroundColor(color);
                }
                for (FloatingActionButton button : new FloatingActionButton[] {fillButton,clearButton} )
                    button.setVisibility(View.INVISIBLE);
            }
        });

        // reset canvas on button click
        clearButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttons = new ArrayList<>();
                parent.removeAllViews();
                for (FloatingActionButton button : new FloatingActionButton[] {fillButton,clearButton} )
                    button.setVisibility(View.INVISIBLE);
                openDialog();
            }
        });

        // touch 'canvas'
        // Source: https://stackoverflow.com/questions/42960597/ontouchlistener-on-linearlayout-that-is-behind-buttons
        parent.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int x = (int) motionEvent.getX();
                int y = (int) motionEvent.getY();

                // find button under current coordinates
                for (Button button : buttons) {
                    int[] params = {0,0};
                    button.getLocationOnScreen(params);

                    if (x >= params[0] && x <= (params[0] + button.getWidth())) {
                        if (y >= params[1] && y <= (params[1] + button.getHeight())) {
                            button.setBackgroundColor(color);
                            break;
                        }
                    }
                }

                return true;
            }
        });
    }

    private void openDialog() {
        // ask for number of rows/cols
        FragmentManager fm = getSupportFragmentManager();
        DialogFragment dialog = new DimensionDialogFragment();
        dialog.setCancelable(false);
        dialog.show(fm, "dialog");
    }

    @Override
    public void onDialogPositiveClick(int rows, int columns) {

        // setup canvas buttons
        for (int i = 0; i < columns; i++) {
            // make rows
            LinearLayout row = new LinearLayout(MainActivity.this);
            row.setOrientation(LinearLayout.VERTICAL);

            for (int j = 0; j < rows; j++) {
                // calculate best button size
                int calculatedHeight = parent.getMeasuredHeight() / rows;
                int calculatedWidth = parent.getMeasuredWidth() / columns;
                int calculatedSize = calculatedHeight < calculatedWidth ? calculatedHeight : calculatedWidth;

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

            parent.addView(row);
            parent.invalidate();

        }
    }

    @Override
    public void onColorSelected(int dialogId, @ColorInt int color) {
        this.color = color;
        for (FloatingActionButton button : new FloatingActionButton[] {colorButton,fillButton,clearButton} )
            button.setBackgroundTintList(ColorStateList.valueOf(color));
    }
    @Override
    public void onDialogDismissed(int dialogId) {}
}
