package net.tuurlievens.dotpict;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class SaveAdapter extends ArrayAdapter<String> {

    private ArrayList<String> data_text;
    final private Context context;

    SaveAdapter(Context context, ArrayList<String> text) {
        super(context, R.layout.saveitem, text);
        this.context = context;
        data_text = text;
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
        final String text = data_text.get(position);
        holder.text.setText(text);
        holder.button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // confirm dialog
                AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                dialog.setTitle(R.string.delete);
                dialog.setMessage(R.string.deletemessage);

                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        data_text.remove(position);
                        SaveAdapter.this.notifyDataSetChanged();

                        SharedPreferences sharedPref = context.getSharedPreferences("saves", Context.MODE_PRIVATE);
                        sharedPref.edit().remove(text).apply();
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

        return convertView;
    }

    static class ViewHolder {
        TextView text;
        ImageView button;
    }
}
