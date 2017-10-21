package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

public class SavesFragment extends Fragment {

    private SavesFragmentListener mListener;
    private ListView saveslist;
    private ArrayList<String> saves = new ArrayList<>();
    SaveAdapter adapter;

    public SavesFragment() { }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_saves, container, false);

        // load saves
        SharedPreferences sharedPref = getActivity().getSharedPreferences("saves", Context.MODE_PRIVATE);
        saves = new ArrayList<>(sharedPref.getAll().keySet());

        // make saves list
        saveslist = view.findViewById(R.id.saveslist);
        adapter = new SaveAdapter(getContext(), saves);
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SavesFragmentListener) {
            mListener = (SavesFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement SavesFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
    }
}
