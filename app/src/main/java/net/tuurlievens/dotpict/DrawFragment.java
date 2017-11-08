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
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
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
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import net.tuurlievens.dotpict.saves.Save;

import static android.app.Activity.RESULT_OK;

public class DrawFragment extends Fragment {

    private boolean pickingColor = false;
    private ViewGroup body;
    private ViewGroup buttonlist;
    private ProgressBar progressbar;
    private CanvasView canvas = null;
    private DrawFragmentListener mListener;

    public DrawFragment() {}

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof DrawFragmentListener)
            mListener = (DrawFragmentListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement DrawFragmentListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putInt("color", canvas.getColor());
        if (canvas != null) {
            savedInstanceState.putInt("rows", canvas.getRows());
            savedInstanceState.putInt("columns", canvas.getColumns());
            savedInstanceState.putIntArray("pixels", canvas.toArray());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw, container, false);
        body = view.findViewById(R.id.body);
        buttonlist = view.findViewById(R.id.buttonlist);
        progressbar = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            setColor(savedInstanceState.getInt("color"));

            if (savedInstanceState.get("pixels") != null) {
                final int[] pixelColors = savedInstanceState.getIntArray("pixels");
                final int rows = savedInstanceState.getInt("rows");
                final int columns = savedInstanceState.getInt("columns");
                // wait until body has been drawn
                body.post( new Runnable() {
                    @Override
                    public void run() {
                        generate(rows,columns,pixelColors);
                    }
                });
            }
        } else {
            body.post(new Runnable() {
                @Override
                public void run() {
                    generate(20, 20);
                }
            });
        }

        // open color picker, source: https://github.com/jaredrummler/ColorPicker
        view.findViewById(R.id.colorButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.newBuilder().setAllowPresets(true).setColor(canvas.getColor()).show(getActivity());
                buttonlist.setVisibility(View.INVISIBLE);
            }
        });

        // show other floating action buttons on drag
        view.findViewById(R.id.colorButton).setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (buttonlist.getVisibility() == View.VISIBLE)
                    buttonlist.setVisibility(View.INVISIBLE);
                else
                    buttonlist.setVisibility(View.VISIBLE);
                return true;
            }
        });

        // fill canvas on button click
        view.findViewById(R.id.pickerButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                pickingColor = true;
                Toast.makeText(getActivity(), R.string.colorpickermessage, Toast.LENGTH_SHORT).show();
            }
        });

        // fill canvas on button click
        view.findViewById(R.id.fillButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                canvas.fill();
                buttonlist.setVisibility(View.INVISIBLE);
            }
        });

        // reset canvas on button click
        view.findViewById(R.id.clearButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonlist.setVisibility(View.INVISIBLE);
                mListener.onCanvasResetted();
            }
        });

        // save button
        view.findViewById(R.id.saveButton).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                buttonlist.setVisibility(View.INVISIBLE);

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
                            Save save = new Save(input.getText().toString(), canvas.toString());
                            mListener.onCanvasSaved(save);
                        } else {
                            Toast.makeText(getActivity(), R.string.invalidsavename, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
                dialog.show();
            }
        });

        view.findViewById(R.id.saveButton).setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.openSaves();
                buttonlist.setVisibility(View.INVISIBLE);
                return true;
            }
        });

        view.findViewById(R.id.cameraButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, 2);
                }
                buttonlist.setVisibility(View.INVISIBLE);
            }
        });

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
                    generate(rotatedPhoto.getHeight(),rotatedPhoto.getWidth(), pixels);
                }
            });

            dialog.show();

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    public void generate(final int rows, final int columns) {
        generate(rows,columns,null);
    }

    public void generate(final int rows, final int columns, final int[] pixelColors) {
        // show loading spinner
        progressbar.setVisibility(View.VISIBLE);

        // remove old canvas
        if (body != null && canvas != null)
            body.removeView(canvas);

        // make canvas
        final CanvasView canvas = new CanvasView(getActivity());
        canvas.setOrientation(LinearLayout.VERTICAL);
        canvas.setBackgroundColor(Color.WHITE);
        canvas.setElevation(8);

        new Thread(new Runnable() {
            public void run() {
                canvas.generate(body,rows,columns,pixelColors);

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressbar.setVisibility(View.INVISIBLE);
                        body.addView(canvas, 0);

                        // inform people of secondary tools
                        Toast.makeText(getActivity(), R.string.toolsmessage, Toast.LENGTH_SHORT).show();

                        // center canvas
                        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) canvas.getLayoutParams();
                        layoutParams.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
                        layoutParams.width = RelativeLayout.LayoutParams.WRAP_CONTENT;
                        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                        canvas.setLayoutParams(layoutParams);

                        canvas.setOnTouchListener(new CanvasView.OnTouchListener() {
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                if (pickingColor) {
                                    setColor( ((ColorDrawable) canvas.findPixel(motionEvent).getBackground()).getColor() );
                                    pickingColor = false;
                                    return false;
                                }
                                return false;
                            }
                        });

                        registerCanvas(canvas);
                    }
                });

            }
        }).start();

    }

    private void registerCanvas(final CanvasView canvas) {
        this.canvas = canvas;
    }

    public void setColor(int color) {
        if (canvas != null)
            canvas.color = color;
        if (getView() != null) {
            getView().findViewById(R.id.colorButton).setBackgroundTintList(ColorStateList.valueOf(color));
            for (int i = 0; i < buttonlist.getChildCount(); i++)
                buttonlist.getChildAt(i).setBackgroundTintList(ColorStateList.valueOf(color));
        }
    }

    public interface DrawFragmentListener {
        void onCanvasResetted();
        void onCanvasSaved(Save save);
        void openSaves();
    }

}
