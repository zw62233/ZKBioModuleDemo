package com.armatura.biomodule.activity.adapter;

import android.animation.ObjectAnimator;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.armatura.biomodule.R;
import com.armatura.biomodule.bean.CardInfo;
import com.armatura.biomodule.bean.IdentifyInfoBoard;
import com.armatura.biomodule.pojo.common.Label;
import com.armatura.biomodule.pojo.face.recognize.DrawFaceData;
import com.armatura.biomodule.pojo.palm.recognize.PalmInfo;
import com.armatura.biomodule.view.RotaterView;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class IdentifyInfoAdapter extends RecyclerView.Adapter<IdentifyInfoAdapter.IdentifyInfoViewHolder> {
    private int lastPosition = -1;

    private static final Object IDENTIFY_INFO_LOCK = new Object();
    private final List<IdentifyInfoBoard> displayIdentifyInfoBoardArrayList;
    private final List<IdentifyInfoBoard> newIdentifyInfoBoardList;

    public IdentifyInfoAdapter() {
        displayIdentifyInfoBoardArrayList = new ArrayList<>();
        newIdentifyInfoBoardList = new ArrayList<>();
    }

    public void addFaceIdentifyInfo(DrawFaceData drawFaceData) {
        synchronized (IDENTIFY_INFO_LOCK) {
            newIdentifyInfoBoardList.clear();
            if (drawFaceData != null) {
                List<IdentifyInfoBoard> identifyInfoBoardList = drawFaceData.toMultiIdentifyInfoBoard();
                newIdentifyInfoBoardList.addAll(identifyInfoBoardList);
            }

            DiffUtil.DiffResult diff = getDiff();

            displayIdentifyInfoBoardArrayList.clear();
            displayIdentifyInfoBoardArrayList.addAll(newIdentifyInfoBoardList);

            diff.dispatchUpdatesTo(this);
        }
    }

    public void addPalmIdentifyInfo(PalmInfo palmInfo) {
        synchronized (IDENTIFY_INFO_LOCK) {
            newIdentifyInfoBoardList.clear();
            if (palmInfo != null) {
                IdentifyInfoBoard identifyInfoBoard = palmInfo.toIdentifyInfoBoard();
                newIdentifyInfoBoardList.add(identifyInfoBoard);
            }

            DiffUtil.DiffResult diff = getDiff();

            displayIdentifyInfoBoardArrayList.clear();
            displayIdentifyInfoBoardArrayList.addAll(newIdentifyInfoBoardList);

            diff.dispatchUpdatesTo(this);
        }
    }

    public void clearData() {
        synchronized (IDENTIFY_INFO_LOCK) {
            displayIdentifyInfoBoardArrayList.clear();
            notifyDataSetChanged();
        }
    }

    public void addCardInfo(CardInfo cardInfo) {
        synchronized (IDENTIFY_INFO_LOCK) {
            newIdentifyInfoBoardList.clear();
            if (cardInfo != null) {
                IdentifyInfoBoard identifyInfoBoard = cardInfo.toIdentifyInfoBoard();
                newIdentifyInfoBoardList.add(identifyInfoBoard);
            }

            DiffUtil.DiffResult diff = getDiff();

            displayIdentifyInfoBoardArrayList.clear();
            displayIdentifyInfoBoardArrayList.addAll(newIdentifyInfoBoardList);

            diff.dispatchUpdatesTo(this);
        }
    }


    private DiffUtil.DiffResult getDiff() {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return displayIdentifyInfoBoardArrayList.size();
            }

            @Override
            public int getNewListSize() {
                return newIdentifyInfoBoardList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(displayIdentifyInfoBoardArrayList.get(oldItemPosition).userName
                        , newIdentifyInfoBoardList.get(newItemPosition).userName);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return displayIdentifyInfoBoardArrayList.get(oldItemPosition).identifyInfoSpannableStingBuilder.hashCode() ==
                        newIdentifyInfoBoardList.get(newItemPosition).identifyInfoSpannableStingBuilder.hashCode();
            }
        });
    }

    private DiffUtil.DiffResult getDrawFaceDiff() {
        return DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return displayIdentifyInfoBoardArrayList.size();
            }

            @Override
            public int getNewListSize() {
                return newIdentifyInfoBoardList.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return Objects.equals(displayIdentifyInfoBoardArrayList.get(oldItemPosition).userName
                        , newIdentifyInfoBoardList.get(newItemPosition).userName);
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return displayIdentifyInfoBoardArrayList.get(oldItemPosition).identifyInfoSpannableStingBuilder.hashCode() ==
                        newIdentifyInfoBoardList.get(newItemPosition).identifyInfoSpannableStingBuilder.hashCode();
            }
        });
    }

    @NonNull
    @Override
    public IdentifyInfoViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater mLayoutInflater = LayoutInflater.from(viewGroup.getContext());
        return new IdentifyInfoViewHolder(mLayoutInflater.inflate(R.layout.item_identify_info_layout, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull IdentifyInfoViewHolder identifyInfoViewHolder, int i) {
        IdentifyInfoBoard identifyInfoBoard = displayIdentifyInfoBoardArrayList.get(i);
        safeSetHtml(identifyInfoViewHolder.identifyInfoTextView, identifyInfoBoard.identifyInfoSpannableStingBuilder);
//        LogToFileAsyncManager.INSTANCE.saveRecord("identify_name.txt", identifyInfoBoard.userName);
        identifyInfoViewHolder.userNameTextView.setText(identifyInfoBoard.userName);
        identifyInfoViewHolder.avatarIv.setImageResource(
                AvatarAdapter.getAvatarDrawable(identifyInfoBoard.avatarIndex));
        if (identifyInfoBoard.identifyType == Label.LABEL_FACE) {
            if (identifyInfoBoard.hasFaceFeature) {
                doRotate(identifyInfoViewHolder.rotaterView);
            }
        } else {
            doRotate(identifyInfoViewHolder.rotaterView);
        }
    }


    /**
     * be avoid of "Abort message: 'ubsan: add-overflow' "
     */
    public static void safeSetHtml(TextView textView, SpannableStringBuilder html) {
        if (html == null) return;
        try {
            Log.i("safeSetHtml", "safeSetHtml: " + html);
            textView.setText(html, TextView.BufferType.SPANNABLE);
        } catch (Exception e) {
            Log.e("safeSetHtml", "HTML parsing failed", e);
            textView.setText("N/A");
        }
    }


    @Override
    public int getItemCount() {
        return displayIdentifyInfoBoardArrayList.size();
    }

    public final static class IdentifyInfoViewHolder extends RecyclerView.ViewHolder {
        final ShapeableImageView avatarIv;
        final TextView identifyInfoTextView;
        final TextView userNameTextView;
        final RotaterView rotaterView;

        IdentifyInfoViewHolder(View view) {
            super(view);
            identifyInfoTextView = view.findViewById(R.id.tv_identify_info);
            userNameTextView = view.findViewById(R.id.tv_user_name);
            avatarIv = view.findViewById(R.id.user_avatar_iv);
            rotaterView = view.findViewById(R.id.rotate_view);
        }
    }

    private void doRotate(RotaterView rotaterView) {
        rotaterView.setColour(0xff4ae8ab);
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(rotaterView,
                "progress", 0, 100);
        objectAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        objectAnimator.setDuration(300);
        objectAnimator.setAutoCancel(true);
        objectAnimator.start();
    }

}
