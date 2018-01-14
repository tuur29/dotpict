package net.tuurlievens.dotpict;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.tuurlievens.dotpict.saves.*;

import java.util.List;

public class SavesFragment extends Fragment {

    private SavesFragmentListener mListener;
    private SaveAdapter adapter;
    private SavesDatabase database;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.database = Room.databaseBuilder(getContext(), SavesDatabase.class, "saves").build();
        this.adapter = new SaveAdapter(getContext());

        new Thread(new Runnable() {
            public void run() {
                List<Save> saves = database.daoAccess().fetchAllData();
                for (Save save : saves)
                    adapter.add(save);
            }
        }).start();
    }

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

        ListView saveslist = view.findViewById(R.id.saveslist);
        saveslist.setAdapter(this.adapter);

        // load save
        saveslist.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, final int i, long l) {
                // confirm dialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.load);
                dialog.setMessage(R.string.loadmessage);

                dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onCloseSavesFragment();
                        mListener.onSaveLoad(adapter.getItem(i).getId());
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

        return view;
    }

    // add new save names to list
    public void addSave(final Save save) {
        new Thread(new Runnable() {
            public void run() {
                database.daoAccess().insert(save);
            }
        }).start();
        adapter.add(save);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        database.close();
        MainApplication.getRefWatcher(getActivity()).watch(this);
    }

    public interface SavesFragmentListener {
        void onSaveLoad(int id);
        void onCloseSavesFragment();
    }
}
