package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;

public class DrawFragment extends Fragment {

    private ViewGroup body;
    private ProgressBar progressBar;
    private FloatingActionButton colorButton;
    private FloatingActionButton fillButton;
    private FloatingActionButton pickerButton;
    private FloatingActionButton clearButton;
    private FloatingActionButton saveButton;

    private int color;
    private boolean pickingColor = false;

    private CanvasView canvas = null;

    public int[] tempPixelColors = null;

    public DrawFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    DrawFragmentListener mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DrawFragmentListener) {
            mListener = (DrawFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement DrawFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("color", color);

        if (canvas != null) {
            savedInstanceState.putInt("rows", canvas.pixels.length);
            savedInstanceState.putInt("columns", canvas.pixels[0].length);
            savedInstanceState.putIntArray("pixels", canvas.toArray());
        }
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_draw, container, false);

        body = view.findViewById(R.id.body);
        progressBar = view.findViewById(R.id.progress_loader);
        colorButton = view.findViewById(R.id.colorButton);
        fillButton = view.findViewById(R.id.fillButton);
        pickerButton = view.findViewById(R.id.pickerButton);
        clearButton = view.findViewById(R.id.clearButton);
        saveButton = view.findViewById(R.id.saveButton);

        if (savedInstanceState != null) {
            setColor(savedInstanceState.getInt("color"));

            if (savedInstanceState.get("rows") != null) {
                tempPixelColors = savedInstanceState.getIntArray("pixels");
                // wait until body has been drawn
                final int rows = savedInstanceState.getInt("rows");
                final int columns = savedInstanceState.getInt("columns");
                body.post( new Runnable() {
                    @Override
                    public void run() {
                        generate(rows,columns);
                    }
                });
            }
        } else {
            setColor(ColorUtils.setAlphaComponent(getResources().getColor(R.color.colorAccent), 255));
        }

        // open color picker, source: https://github.com/jaredrummler/ColorPicker
        colorButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.newBuilder().setAllowPresets(true).setColor(color).show(getActivity());
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
                Toast.makeText(getActivity(), R.string.colorpickermessage, Toast.LENGTH_SHORT).show();
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
                hideExtraTools();
                mListener.onCanvasResetted();
            }
        });

        // save button
        saveButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                hideExtraTools();

                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.save);
                dialog.setMessage(R.string.savemessage);

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                input.setHint(R.string.savename);
                dialog.setView(input,60,0,60,0);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onCanvasSaved(input.getText().toString(), canvas.toString());
                    }
                });

                dialog.show();
            }
        });

        saveButton.setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.openSaves();
                hideExtraTools();
                return true;
            }
        });

        return view;
    }

    public void generate(final int rows, final int columns) {
        body.removeView(canvas);

        // show loading spinner
        progressBar.setVisibility(View.VISIBLE);

        // make canvas
        final CanvasView canvas = new CanvasView(getActivity());
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
                    LinearLayout row = new LinearLayout(getActivity());
                    row.setOrientation(LinearLayout.HORIZONTAL);

                    for (int j = 0; j < columns; j++) {
                        // make pixel
                        TextView pixel = new TextView(getActivity());
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

    }

    private void addCanvas(final CanvasView canvas) {
        getActivity().runOnUiThread(new Runnable() {
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
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton,saveButton} )
            button.setVisibility(View.VISIBLE);
    }

    private void hideExtraTools() {
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton,saveButton} )
            button.setVisibility(View.INVISIBLE);
    }

    public void setColor(int color) {
        this.color = color;
        for (FloatingActionButton button : new FloatingActionButton[] {colorButton,fillButton,pickerButton,clearButton,saveButton} )
            button.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public interface DrawFragmentListener {
        void onCanvasResetted();
        void onCanvasSaved(String key, String data);
        void openSaves();
    }

}
