package net.tuurlievens.dotpict.adapters;

import android.arch.persistence.room.Room;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import net.tuurlievens.dotpict.R;
import net.tuurlievens.dotpict.models.Save;
import net.tuurlievens.dotpict.persistency.*;

import java.util.ArrayList;

public class SaveAdapter extends ArrayAdapter<Save> {

    final private Context context;

    public SaveAdapter(Context context) {
        super(context, R.layout.saveitem, new ArrayList<Save>());
        this.context = context;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.saveitem, parent, false);

            holder = new ViewHolder();
            holder.text = convertView.findViewById(R.id.savename);
            holder.button = convertView.findViewById(R.id.deletesave);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // remove saved drawing
        holder.text.setText(this.getItem(position).getName());
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // confirm dialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.delete);
                dialog.setMessage(R.string.deletemessage);

                dialog.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final Save save = getItem(position);
                        remove(save);
                        notifyDataSetChanged();
                        final SavesDatabase database = Room.databaseBuilder(getContext(), SavesDatabase.class, "saves").build();
                        new Thread(new Runnable() {
                            public void run() {
                                database.daoAccess().delete(save);
                            }
                        }).start();
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

        return convertView;
    }

    static class ViewHolder {
        TextView text;
        ImageView button;
    }
}
