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
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jaredrummler.android.colorpicker.ColorPickerDialog;

import net.tuurlievens.dotpict.saves.Save;

import java.util.List;

import static android.app.Activity.RESULT_OK;

public class DrawFragment extends Fragment {

    private boolean pickingColor = false;
    private ViewGroup body;
    private ViewGroup buttonlist;
    private ViewGroup scrolllist;
    private ProgressBar progressbar;
    private CanvasView canvas = null;
    private DrawFragmentListener mListener;
    private Bundle tempSavedInstanceState = null;

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

        if (canvas != null) {
            savedInstanceState.putInt("color", canvas.getColor());
            savedInstanceState.putInt("rows", canvas.getRows());
            savedInstanceState.putInt("columns", canvas.getColumns());
            savedInstanceState.putIntArray("pixels", canvas.toArray());

        } else if (tempSavedInstanceState != null) {
            // use old instancestate if turning display before canvas is loaded

            savedInstanceState.putInt("color", tempSavedInstanceState.getInt("color"));
            savedInstanceState.putInt("rows", tempSavedInstanceState.getInt("rows"));
            savedInstanceState.putInt("columns", tempSavedInstanceState.getInt("columns"));
            savedInstanceState.putIntArray("pixels", tempSavedInstanceState.getIntArray("pixels"));
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_draw, container, false);
        body = view.findViewById(R.id.body);
        buttonlist = view.findViewById(R.id.buttonlist);
        scrolllist = view.findViewById(R.id.scrolllist);
        progressbar = view.findViewById(R.id.progress);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        tempSavedInstanceState = savedInstanceState;
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

            // inform people of secondary tools
            Toast.makeText(getActivity(), R.string.toolsmessage, Toast.LENGTH_SHORT).show();

            body.post(new Runnable() {
                @Override
                public void run() {
                    generate(25, 25);
                }
            });
        }

        // open color picker, source: https://github.com/jaredrummler/ColorPicker
        view.findViewById(R.id.colorButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                ColorPickerDialog.newBuilder().setAllowPresets(true).setColor(canvas.getColor()).show(getActivity());
                scrolllist.setVisibility(View.INVISIBLE);
            }
        });

        // show other floating action buttons on drag
        view.findViewById(R.id.colorButton).setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (scrolllist.getVisibility() == View.VISIBLE)
                    scrolllist.setVisibility(View.INVISIBLE);
                else
                    scrolllist.setVisibility(View.VISIBLE);
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
                scrolllist.setVisibility(View.INVISIBLE);
            }
        });

        // reset canvas on button click
        view.findViewById(R.id.clearButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrolllist.setVisibility(View.INVISIBLE);
                mListener.onCanvasResetted();
            }
        });

        // save button
        view.findViewById(R.id.saveButton).setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrolllist.setVisibility(View.INVISIBLE);

                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.save);

                final EditText input = new EditText(getContext());
                input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                input.setHint(R.string.savename);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginStart(60);
                params.setMarginEnd(60);
                input.setLayoutParams(params);
                dialog.setView(input);

                dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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
                dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
            }
        });

        view.findViewById(R.id.saveButton).setOnLongClickListener(new FloatingActionButton.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mListener.openSaves();
                scrolllist.setVisibility(View.INVISIBLE);
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
                scrolllist.setVisibility(View.INVISIBLE);
            }
        });

        view.findViewById(R.id.rotateButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                rotate();
                scrolllist.setVisibility(View.INVISIBLE);
            }
        });

        view.findViewById(R.id.brushButton).setOnClickListener(new FloatingActionButton.OnClickListener() {
            @Override
            public void onClick(View view) {
                scrolllist.setVisibility(View.INVISIBLE);

                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.brushsize);

                final SeekBar input = new SeekBar(getContext());
                input.setMax(17);
                input.setProgress(canvas.brushSize-1);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
                params.setMarginStart(100);
                input.setLayoutParams(params);
                dialog.setView(input);

                dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        canvas.brushSize = input.getProgress()+1;
                    }
                });
                dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                dialog.show();
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

            final LinearLayout ll = new LinearLayout(getContext());
            ll.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginStart(100);
            params.setMarginEnd(100);
            ll.setLayoutParams(params);

            // add slider
            final SeekBar input = new SeekBar(getContext());
            input.setProgress(20);
            input.setMax(37);
            ll.addView(input);

            // add checkbox
            final CheckBox checkbox = new CheckBox(getContext());
            checkbox.setText(R.string.frontcamera);
            checkbox.setLayoutParams(params);
            ll.addView(checkbox);

            dialog.setView(ll);

            dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // get camera sensor orientation
                    int orientation = 0;
                    CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
                    try {
                        int length = manager.getCameraIdList().length;
                        String cameraId = manager.getCameraIdList()[0 + (checkbox.isChecked() && length > 1 ? 1 : 0)];
                        CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
                        orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
                    } catch (Exception e){}

                    // get picture
                    Bitmap photo = (Bitmap) data.getExtras().get("data");

                    // rotate photo based on picture orientation
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);
                    Bitmap rotatedPhoto = Bitmap.createBitmap(photo, 0, 0, photo.getWidth(), photo.getHeight(), matrix, true);

                    // resize photo
                    int rows = input.getProgress() + 3;
                    double aspectratio = ( ((double) rotatedPhoto.getHeight()) / rotatedPhoto.getWidth()) > 1 ?
                            ( ((double) rotatedPhoto.getHeight()) / rotatedPhoto.getWidth()) :
                            ( ((double) rotatedPhoto.getWidth()) / rotatedPhoto.getHeight());
                    int columns = (int) (rows * aspectratio);
                    Bitmap scaledPhoto = Bitmap.createScaledBitmap(rotatedPhoto, rows, columns, false);

                    // convert to color array
                    int[] pixels = new int[scaledPhoto.getWidth()*scaledPhoto.getHeight()];
                    scaledPhoto.getPixels(pixels, 0, scaledPhoto.getWidth(), 0, 0, scaledPhoto.getWidth(), scaledPhoto.getHeight());
                    generate(scaledPhoto.getHeight(),scaledPhoto.getWidth(), pixels);
                }
            });
            dialog.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            dialog.show();

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void rotate() {
        progressbar.setVisibility(View.VISIBLE);

        // rotate photo 90 degrees to the right
        new Thread(new Runnable() {
            public void run() {
                final TextView[][] rotated = canvas.getRotated();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        canvas.load(rotated);
                        progressbar.setVisibility(View.GONE);
                    }
                });
            }
        }).start();

    }

    public void generate(final int rows, final int columns) {
        generate(rows,columns,null);
    }

    public void generate(final int rows, final int columns, final int[] pixelColors) {
        // show loading spinner
        progressbar.setVisibility(View.VISIBLE);

        // remove old canvas
        if (body != null && canvas != null) {
            body.removeView(canvas);
            setColor(ColorUtils.setAlphaComponent(ContextCompat.getColor(getContext(), R.color.colorAccent), 255));
        }

        if (getActivity() == null) return;

        // make canvas
        final CanvasView canvas = new CanvasView(getActivity());
        canvas.setOrientation(LinearLayout.VERTICAL);
        canvas.setBackgroundColor(Color.WHITE);
        canvas.setElevation(8);


        new Thread(new Runnable() {
            public void run() {
                canvas.generate(body,rows,columns,pixelColors);

                if (getActivity() == null) return;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressbar.setVisibility(View.INVISIBLE);
                        body.addView(canvas, 0);


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
                                    int tempBrushSize = canvas.brushSize;
                                    canvas.brushSize = 1;
                                    List<View> pixels = canvas.findPixels(motionEvent);
                                    setColor( ((ColorDrawable) pixels.get(0).getBackground()).getColor() );
                                    canvas.brushSize = tempBrushSize;
                                    pickingColor = false;
                                    return true;
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
        tempSavedInstanceState = null;
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
