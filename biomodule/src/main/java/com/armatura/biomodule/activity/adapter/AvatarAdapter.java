package com.armatura.biomodule.activity.adapter;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.armatura.biomodule.R;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Magic on 2022/2/24
 * 描述:
 */
public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    public static final List<AvatarItem> avatarSampleList = Arrays.asList(
            AvatarItem.newInstance(R.drawable.avatar01, false),
            AvatarItem.newInstance(R.drawable.avatar02, false),
            AvatarItem.newInstance(R.drawable.avatar03, false),
            AvatarItem.newInstance(R.drawable.avatar04, false),
            AvatarItem.newInstance(R.drawable.avatar05, false),
            AvatarItem.newInstance(R.drawable.avatar06, false),
            AvatarItem.newInstance(R.drawable.avatar07, false),
            AvatarItem.newInstance(R.drawable.avatar08, false),
            AvatarItem.newInstance(R.drawable.avatar09, false));

    public static int getAvatarDrawable(int index){
        if(index == -1){
            return R.drawable.default_avatar;
        }
        return avatarSampleList.get(index).drawableId;
    }

    private OnItemClickListener onItemClickListener;

    /**
     * get avatar index,if no item selected,use default 0
     *
     * @return avatar index
     */
    public int getAvatarIndex() {
        for (int i = 0; i < avatarSampleList.size(); i++) {
            if (avatarSampleList.get(i).isChecked) {
                return i;
            }
        }
        return 0;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_avatar, parent, false);
        int parentHeight = parent.getHeight();
        AvatarViewHolder itemHolder = new AvatarViewHolder(view);
        ViewGroup.LayoutParams params = itemHolder.itemView.getLayoutParams();
        params.height = (parentHeight / 3);
        return itemHolder;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, @SuppressLint("RecyclerView") int position) {

        final AvatarItem avatarItem = avatarSampleList.get(position);
        holder.imageView.setImageDrawable(ContextCompat.getDrawable(holder.imageView.getContext(), avatarItem.drawableId));
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0, avatarSampleListSize = avatarSampleList.size(); i < avatarSampleListSize; i++) {
                    AvatarItem item = avatarSampleList.get(i);
                    item.isChecked = item.drawableId == avatarItem.drawableId;
                }
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(position);
                }
            }
        });
        holder.imageView.setOnTouchListener(new ViewClickAnim());
    }

    @Override
    public int getItemCount() {
        return avatarSampleList.size();
    }

    public interface OnItemClickListener {
        void onItemClick(int index);
    }

    private static class ViewClickAnim implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                v.setAlpha(0.5F);
                v.setScaleX(1.1F);
                v.setScaleY(1.1F);
            } else if (event.getAction() == MotionEvent.ACTION_UP
                    || event.getAction() == MotionEvent.ACTION_CANCEL) {
                v.setAlpha(1.0F);
                v.setScaleX(1.0F);
                v.setScaleY(1.0F);
            }
            return false;
        }
    }

    private static boolean isPointInView(View view, float x, float y) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int viewX = location[0];
        int viewY = location[1];
        int viewWidth = view.getWidth();
        int viewHeight = view.getHeight();
        return x >= viewX && x < viewX + viewWidth && y >= viewY && y < viewY + viewHeight;
    }

    protected static class AvatarViewHolder extends RecyclerView.ViewHolder {
        public final ShapeableImageView imageView;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.avatar_sample);
        }
    }

    public static class AvatarItem {
        public int drawableId;
        public boolean isChecked;

        public AvatarItem(int drawableId, boolean isChecked) {
            this.drawableId = drawableId;
            this.isChecked = isChecked;
        }

        public static AvatarItem newInstance(int drawableId, boolean isChecked) {
            return new AvatarItem(drawableId, isChecked);
        }
    }
}
