package com.armatura.biomodule;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.executor.GlideExecutor;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Created by Magic on 2023/6/27
 * Description:
 */
@GlideModule
public final class MyGlideModule extends AppGlideModule {

    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        final GlideExecutor.UncaughtThrowableStrategy uncaughtThrowableStrategy
                = new GlideExecutor.UncaughtThrowableStrategy() {
            @Override
            public void handle(Throwable t) {
                //nothing
            }
        };
        builder.setLogLevel(Log.ERROR);
        builder.setDiskCacheExecutor(GlideExecutor
                .newDiskCacheBuilder()
                .setUncaughtThrowableStrategy(uncaughtThrowableStrategy)
                .build());
    }


    @Override
    public boolean isManifestParsingEnabled() {
        return false;
    }
}
