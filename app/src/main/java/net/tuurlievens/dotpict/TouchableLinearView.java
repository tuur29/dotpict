package net.tuurlievens.dotpict;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class TouchableLinearView extends LinearLayout {

    public TouchableLinearView(Context context) {
        super(context);
    }

    public TouchableLinearView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchableLinearView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return true;
    }

}