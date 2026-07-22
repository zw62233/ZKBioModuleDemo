package com.armatura.biomodule.activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.armatura.biomodule.R;
import com.armatura.biomodule.databinding.ItemPhotoBinding;
import com.armatura.biomodule.dialog.ShowImageDialog;
import com.armatura.biomodule.dialog.ShowPhotoDialog;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

/**
 * @author Magic
 */
public class PhotoListAdapter extends RecyclerView.Adapter<PhotoListAdapter.PhotoListViewHolder> {

    private final List<String> mPhotoPathList;
    private final FragmentManager fragmentManager;

    public PhotoListAdapter(FragmentManager fragmentManager, List<String> photoPathList) {
        mPhotoPathList = photoPathList;
        this.fragmentManager = fragmentManager;
    }

    @NonNull
    @Override
    public PhotoListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPhotoBinding binding =
                DataBindingUtil.inflate(LayoutInflater.from(parent.getContext()),
                        R.layout.item_photo, parent, false);
        return new PhotoListViewHolder(binding, binding.getRoot());
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoListViewHolder holder, int position) {
        final String path = mPhotoPathList.get(position);
        Glide.with(holder.itemView.getContext())
                .load(path)
                .skipMemoryCache(true)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .into(holder.binding.ivPhoto);
        holder.binding.ivPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ShowPhotoDialog showImageDialog = new ShowPhotoDialog(path);
                showImageDialog.show(fragmentManager, "");
            }
        });


    }


    @Override
    public int getItemCount() {
        return mPhotoPathList.size();
    }

    public static class PhotoListViewHolder extends RecyclerView.ViewHolder {

        public final ItemPhotoBinding binding;

        public PhotoListViewHolder(ItemPhotoBinding binding, @NonNull View itemView) {
            super(itemView);
            this.binding = binding;
        }
    }
}
