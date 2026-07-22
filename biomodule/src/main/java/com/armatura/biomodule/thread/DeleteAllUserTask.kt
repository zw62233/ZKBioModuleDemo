package com.armatura.biomodule.thread

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.UserManageActivity
import com.armatura.biomodule.databases.BioDataUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference

class DeleteAllUserTask(ctx: UserManageActivity) {
    private val activityWeakReference: WeakReference<UserManageActivity> = WeakReference(ctx)

    @SuppressLint("NotifyDataSetChanged")
    fun execute() {
        //before
        val userManageActivity = activityWeakReference.get()
        userManageActivity ?: return
        userManageActivity.lifecycleScope.launch {
            userManageActivity
                .showProgressDialog(
                    activityWeakReference.get(),
                    activityWeakReference.get()!!.getResources()
                        .getString(R.string.users_tip_deleting)
                )
            withContext(Dispatchers.IO) {
                //task execute
                userManageActivity.deleteUsers()
            }
            //after
            userManageActivity.dismissProgressDialog()
            userManageActivity.updateUserSize(BioDataUtil.userFaces_all_List.size)
            userManageActivity.registerFaceAdapter.notifyDataSetChanged()
        }
    }
}