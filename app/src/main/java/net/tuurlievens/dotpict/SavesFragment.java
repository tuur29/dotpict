package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SavesFragment extends Fragment {

    private SavesFragmentListener mListener;
    private ArrayList<String> saves = new ArrayList<>();
    private SaveAdapter adapter;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof SavesFragmentListener)
            mListener = (SavesFragmentListener) context;
        else
            throw new RuntimeException(context.toString() + " must implement SavesFragmentListener");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saves, container, false);

        SharedPreferences sharedPref = getActivity().getSharedPreferences("saves", Context.MODE_PRIVATE);
        saves = new ArrayList<>(sharedPref.getAll().keySet());
        adapter = new SaveAdapter(getContext(), saves);
        adapter.notifyDataSetChanged();

        ListView saveslist = view.findViewById(R.id.saveslist);
        saveslist.setAdapter(adapter);

        // load save
        saveslist.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                // confirm dialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.load);
                dialog.setMessage(R.string.loadmessage);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onCloseSavesFragment();
                        mListener.onSaveLoad(saves.get(i));
                    }
                });
                dialog.setNegativeButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialog.show();
            }
        });

        return view;
    }

    // add new save names to list
    public void addSave(String key) {
        if (!saves.contains(key)) {
            saves.add(key);
            adapter.notifyDataSetChanged();
        }
    }

    public interface SavesFragmentListener {
        void onSaveLoad(String key);
        void onCloseSavesFragment();
    }
}
