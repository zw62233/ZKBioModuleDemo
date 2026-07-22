package com.armatura.biomodule.handler;

import android.graphics.Bitmap;

/**
 * Created by Magic on 2020/9/18
 */
public interface AvatarListener {

    void onPhotoReady(Bitmap bitmap);

    void saveAvatar(boolean isHostAvatar, Bitmap bitmap);
}
