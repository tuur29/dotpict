package net.tuurlievens.dotpict;

import android.support.design.widget.TextInputEditText;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.*;
import static android.support.test.espresso.action.ViewActions.*;
import static android.support.test.espresso.assertion.ViewAssertions.*;
import static android.support.test.espresso.matcher.ViewMatchers.*;
import static junit.framework.Assert.*;
import static org.hamcrest.Matchers.*;

@RunWith(AndroidJUnit4.class)
public class SaveTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule(MainActivity.class);

    @Test
    public void saveDrawing() {
        // open saves fragment and record number of saves
        onView(withId(R.id.colorButton)).perform(longClick());
        onView(withId(R.id.saveButton)).perform(scrollTo(),longClick());

        ListView rv = activityRule.getActivity().findViewById(R.id.saveslist);
        int countBefore = rv.getChildCount();

        try {
            // return to drawing fragment if necessary
            onView(withId(R.id.closebutton)).perform(click());
        } catch (Exception e){}

        // open save dialog
        onView(withId(R.id.colorButton)).perform(longClick());
        onView(withId(R.id.saveButton)).perform(scrollTo(),click());

        // fill in name and save
        onView(withClassName(endsWith("EditText"))).perform(typeText("TestName"));
        Espresso.closeSoftKeyboard();
        onView(withId(android.R.id.button1)).perform(click());

        // open saves fragment
        onView(withId(R.id.colorButton)).perform(longClick());
        onView(withId(R.id.saveButton)).perform(scrollTo(), longClick());

        // check if extra save has been made
        ListView rv2 = activityRule.getActivity().findViewById(R.id.saveslist);
        int countAfterSave = rv2.getChildCount();
        assertTrue("an extra save has been made", countAfterSave -1 == countBefore);
    }

}
