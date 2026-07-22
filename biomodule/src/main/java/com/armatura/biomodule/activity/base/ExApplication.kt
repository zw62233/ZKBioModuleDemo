package com.armatura.biomodule.activity.base

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import com.armatura.biomodule.config.CrashHandler
import com.armatura.biomodule.register.RegisterStatus


fun getString(status: RegisterStatus) = ExApplication.instance().getString(status.resId)
fun getString(resId: Int) = ExApplication.instance().getString(resId)

class ExApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
        CrashHandler.getInstance().init(this)
        //        if (BuildConfig.DEBUG) {
//            StrictMode.setThreadPolicy(
//                    new StrictMode.ThreadPolicy.Builder()
//                            .detectAll()
//                            .penaltyLog()
//                            .build()
//            );
//            StrictMode.setVmPolicy(
//                    new StrictMode.VmPolicy.Builder()
//                            .detectAll()
//                            .penaltyLog()
//                            .build()
//            );
//        }
    }


    companion object {
        private var instance: ExApplication? = null


        @JvmStatic
        fun instance(): Context {
            return instance!!.applicationContext
        }

    }
}
