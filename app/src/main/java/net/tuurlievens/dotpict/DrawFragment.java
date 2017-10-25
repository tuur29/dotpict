package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import static android.app.Activity.RESULT_OK;

public class DrawFragment extends Fragment {

    private ViewGroup body;
    private ProgressBar progressBar;
    private FloatingActionButton colorButton;
    private FloatingActionButton fillButton;
    private FloatingActionButton pickerButton;
    private FloatingActionButton clearButton;
    private FloatingActionButton saveButton;
    private FloatingActionButton cameraButton;

    private int color;
    private boolean pickingColor = false;

    private CanvasView canvas = null;

    public int[] tempPixelColors = null;
    private DrawFragmentListener mListener;

    public DrawFragment() {}

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
        cameraButton = view.findViewById(R.id.cameraButton);

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
            setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(getContext(),R.color.colorAccent), 255));
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
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginStart(60);
                params.setMarginEnd(60);
                input.setLayoutParams(params);
                dialog.setView(input);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(input.getText())) {
                            mListener.onCanvasSaved(input.getText().toString(), canvas.toString());
                        } else {
                            Toast.makeText(getActivity(), R.string.invalidsavename, Toast.LENGTH_SHORT).show();
                        }
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

        cameraButton.setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 2);
                }
                hideExtraTools();
            }
        });

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        // photo taken
        if (requestCode == 2 && resultCode == RESULT_OK) {

            // ask for rows & columns count
            AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
            dialog.setTitle(R.string.photoheight);
            dialog.setMessage(R.string.row);

            // add slider
            final SeekBar input = new SeekBar(getContext());
            input.setProgress(20);
            input.setMax(37);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(100);
            params.setMarginEnd(100);
            input.setLayoutParams(params);
            dialog.setView(input);

            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    // get camera sensor orientation
                    CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
                    int orientation = 0;
                    try {
                        String cameraId = manager.getCameraIdList()[0];
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    } catch (Exception e){}

                    // get picture
                    Bundle extras = data.getExtras();
                    Bitmap photo = (Bitmap) extras.get("data");

                    // resize photo
                    int rows = input.getProgress() + 3;
                    int columns = rows * (photo.getWidth() / photo.getHeight());
                    photo = Bitmap.createScaledBitmap(photo, rows, columns, false);

                    // rotate photo based on picture orientation
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    Bitmap rotatedPhoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

                    // convert to color array
                    int[] pixels = new int[rotatedPhoto.getWidth()*rotatedPhoto.getHeight()];
                    rotatedPhoto.getPixels(pixels, 0, rotatedPhoto.getWidth(), 0, 0, rotatedPhoto.getHeight(), rotatedPhoto.getWidth());
                    tempPixelColors = pixels;
                    generate(rotatedPhoto.getHeight(),rotatedPhoto.getWidth());
                }
            });

            dialog.show();

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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

                // center canvas
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
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton,saveButton,cameraButton} )
            button.setVisibility(View.VISIBLE);
    }

    private void hideExtraTools() {
        for (FloatingActionButton button : new FloatingActionButton[] {fillButton,pickerButton,clearButton,saveButton,cameraButton} )
            button.setVisibility(View.INVISIBLE);
    }

    public void setColor(int color) {
        this.color = color;
        for (FloatingActionButton button : new FloatingActionButton[] {colorButton,fillButton,pickerButton,clearButton,saveButton,cameraButton} )
            button.setBackgroundTintList(ColorStateList.valueOf(color));
    }

    public interface DrawFragmentListener {
        void onCanvasResetted();
        void onCanvasSaved(String key, String data);
        void openSaves();
    }

}
