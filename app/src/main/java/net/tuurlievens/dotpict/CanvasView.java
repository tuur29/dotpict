package net.tuurlievens.dotpict;

import android.content.Context;
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

    public void reset() {
        pixels = null;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

}