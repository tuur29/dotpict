package net.tuurlievens.dotpict;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public class CanvasView extends LinearLayout {

    public TextView[][] pixels = null;
    public int pixelRadius = 0;

    public CanvasView(Context context) {
        super(context);
    }
    public CanvasView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public CanvasView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    public void fill(int color) {
        for (TextView[] row : pixels)
            for (TextView button: row)
                button.setBackgroundColor(color);
    }

    public int[] toArray() {
        int[] list = new int[pixels.length*pixels[0].length];
        int i = 0;
        for (TextView[] row : pixels) {
            for (TextView pixel : row) {
                list[i] = ((ColorDrawable) pixel.getBackground()).getColor();
                i++;
            }
        }

        return list;
    }

    @Override
    public String toString() {
        String list = pixels.length+";"+pixels[0].length+";";
        for (TextView[] row : pixels) {
            for (TextView pixel : row) {
                list += ((ColorDrawable) pixel.getBackground()).getColor()+",";
            }
        }

        return list.substring(0, list.length() - 1);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

}