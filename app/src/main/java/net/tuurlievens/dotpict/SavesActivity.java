package net.tuurlievens.dotpict;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

public class SavesActivity extends FragmentActivity implements SavesFragment.SavesFragmentListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SavesFragment fragment = new SavesFragment();
        getSupportFragmentManager().beginTransaction().add(android.R.id.content, fragment).commit();
    }

    @Override
    public void onSaveLoad(String key) {
        Intent i = new Intent();
        i.putExtra("key", key);
        setResult(RESULT_OK, i);
        finish();
    }

}
