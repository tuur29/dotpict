package net.tuurlievens.dotpict;

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
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;

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

    private CanvasView canvas = null;

    private int[] tempPixelColors = null;

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean("dialogOpen", dialogOpen);
        savedInstanceState.putInt("color", color);

        if (canvas != null) {
            savedInstanceState.putInt("rows", canvas.pixels.length);
            savedInstanceState.putInt("columns", canvas.pixels[0].length);
            savedInstanceState.putIntArray("pixels", canvas.toArray());
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
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

            if (savedInstanceState.get("rows") != null) {
                tempPixelColors = savedInstanceState.getIntArray("pixels");
                // wait until body has been drawn
                body.post( new Runnable() {
                    @Override
                    public void run() {
                        onDialogPositiveClick(savedInstanceState.getInt("rows"), savedInstanceState.getInt("columns"));
                    }
                });
            }
        } else {
            setColor(ColorUtils.setAlphaComponent( getResources().getColor(R.color.colorAccent), 255));
            openDialog();
        }


        // open color picker, source: https://github.com/jaredrummler/ColorPicker
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
                if (fillButton.getVisibility() == View.VISIBLE)
                    hideExtraTools();
                else
                    showExtraTools();
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
                canvas.fill(color);
                hideExtraTools();
            }
        });

        // reset canvas on button click
        clearButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        final CanvasView canvas = new CanvasView(MainActivity.this);
        canvas.setOrientation(LinearLayout.VERTICAL);
        canvas.setBackgroundColor(Color.WHITE);
        canvas.setElevation(8);

        final int[] pixelColors = tempPixelColors;
        tempPixelColors = null;

        // TODO: move to init function? how to add child views in itself? callback on init function?
        new Thread(new Runnable() {
            public void run() {
                // calculate best pixel size
                int calculatedHeight = (body.getMeasuredHeight() - 100) / rows;
                int calculatedWidth = (body.getMeasuredWidth() - 100) / columns;
                canvas.pixelRadius = calculatedHeight < calculatedWidth ? calculatedHeight : calculatedWidth;

                // setup canvas pixels
                canvas.pixels = new TextView[rows][columns];

                for (int i = 0; i < rows; i++) {
                    // make rows
                    LinearLayout row = new LinearLayout(MainActivity.this);
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    for (int j = 0; j < columns; j++) {
                        // make pixel
                        TextView pixel = new TextView(MainActivity.this);
                        pixel.setHeight(canvas.pixelRadius);
                        pixel.setWidth(canvas.pixelRadius);
                        int color = Color.WHITE;
                        if (pixelColors != null)
                            color = pixelColors[i*columns+j];
                        pixel.setBackgroundColor(color);
                        canvas.pixels[i][j] = pixel;
                        row.addView(pixel);
                    }
                    canvas.addView(row);
                }
                addCanvas(canvas);
            }
        }).start();

        // inform people of secondary tools
        Toast.makeText(getApplicationContext(), R.string.toolsmessage, Toast.LENGTH_SHORT).show();
    }

    private void addCanvas(final CanvasView canvas) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.INVISIBLE);
                body.addView(canvas, 0);
                body.invalidate();

                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) canvas.getLayoutParams();
                layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                canvas.setLayoutParams(layoutParams);

                registerCanvas(canvas);
            }
        });
    }

    private void registerCanvas(final CanvasView canvas) {
        // touch 'canvas'
        canvas.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent motionEvent) {
                int x = (int) motionEvent.getRawX();
                int y = (int) motionEvent.getRawY();

                // find pixel under current coordinates
                rowloop: for (TextView[] row : canvas.pixels) {
                    for (TextView pixel : row) {
                        int params[] = new int[2];
                        pixel.getLocationOnScreen(params);

                        if ( y >= params[1] - canvas.pixelRadius && y <= params[1] + canvas.pixelRadius) {
                            if ( x >= params[0] - canvas.pixelRadius && x <= params[0] + canvas.pixelRadius) {
                                if (pickingColor) {
                                    setColor(((ColorDrawable) pixel.getBackground()).getColor());
                                    pickingColor = false;
                                } else {
                                    pixel.setBackgroundColor(color);
                                }
                                break rowloop;
                            }
                        } else {
                            continue rowloop;
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
