package com.armatura.biomodule.activity.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.armatura.biomodule.R;

import java.util.List;

/**
 * create：2020/7/2 on 13:44
 * remark:
 */
public class AboutInfoAdapter extends RecyclerView.Adapter<AboutInfoAdapter.ViewHolder> {
    private final LayoutInflater mLayoutInflater;
    private final List<String> titles;
    private final List<String> contents;


    public AboutInfoAdapter(Context context, List<String> titles, List<String> contents) {
        mLayoutInflater = LayoutInflater.from(context);
        this.titles = titles;
        this.contents = contents;
    }

    @NonNull
    @Override
    public AboutInfoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(mLayoutInflater.inflate(R.layout.layout_about_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AboutInfoAdapter.ViewHolder viewHolder, int i) {
        String title = titles.get(i);
        String content = contents.get(i);
        viewHolder.titleTextView.setText(title);
        viewHolder.contentTextView.setText(content);

    }

    @Override
    public int getItemCount() {
        return titles.size();
    }

    public final static class ViewHolder extends RecyclerView.ViewHolder {

        final TextView titleTextView;
        final TextView contentTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.tv_title);
            contentTextView = itemView.findViewById(R.id.tv_content);
        }
    }
}
