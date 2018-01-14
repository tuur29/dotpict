package net.tuurlievens.dotpict;

import android.graphics.Color;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.action.CoordinatesProvider;
import android.support.test.espresso.action.GeneralClickAction;
import android.support.test.espresso.action.Press;
import android.support.test.espresso.action.Tap;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DrawTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule(MainActivity.class);

    @Test
    public void colorPicker() {

        // select color picker
        onView(withId(R.id.colorButton)).perform(longClick());
        onView(withId(R.id.pickerButton)).perform(scrollTo(), click());

        // tap completely white canvas
        onView(withTagKey(R.id.CANVAS_TAG)).perform(clickXY(10,10));

        // check if color is indeed white
        CanvasView canvas = activityRule.getActivity().getCanvas();
        int color = canvas.getColor();
        assertTrue("color is white", color == Color.WHITE);
    }

    @Test
    public void resetCanvas() {

        // open reset dialog
        onView(withId(R.id.colorButton)).perform(longClick());
        onView(withId(R.id.clearButton)).perform(scrollTo(), click());

        // increase rows and remake canvas
        onView(withId(R.id.seekBarRows)).perform(clickXY(10,10));
        onView(withId(android.R.id.button1)).perform(click());

        // check if number of rows has decreased
        CanvasView canvas = activityRule.getActivity().getCanvas();
        assertTrue("number of rows is smaller than default", canvas.getRows() < 25);
    }

    // Helpers

    // source: https://stackoverflow.com/questions/33382344/espresso-test-click-x-y-coordinates
    public static ViewAction clickXY(final int x, final int y){
        return new GeneralClickAction(
            Tap.SINGLE,
            new CoordinatesProvider() {
                @Override
                public float[] calculateCoordinates(View view) {

                    final int[] screenPos = new int[2];
                    view.getLocationOnScreen(screenPos);

                    final float screenX = screenPos[0] + x;
                    final float screenY = screenPos[1] + y;
                    float[] coordinates = {screenX, screenY};

                    return coordinates;
                }
            },
            Press.FINGER
        );
    }

}
