package com.armatura.biomodule.thread

import android.annotation.SuppressLint
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.activity.UserManageActivity
import com.armatura.biomodule.databases.BioDataUtil
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class UpdateUserInfoTask(ctx: UserManageActivity, private val scrollTo: Int) {

    private val activityWeakReference: WeakReference<UserManageActivity> = WeakReference(ctx)

    @SuppressLint("NotifyDataSetChanged")
    fun execute() {
        val userManageActivity = activityWeakReference.get()
        userManageActivity?.let {
            //before
            userManageActivity.setUserListEnable(false)
            //task
            //BioDataUtil.instance().updateUsers()
            //after
            userManageActivity.lifecycleScope.launch {
                userManageActivity.updateUserSize(BioDataUtil.userFaces_all_List.size)
                userManageActivity.registerFaceAdapter.notifyDataSetChanged()
                userManageActivity.setUserListEnable(true)
                userManageActivity.binding.recyclerViewRegedFace.scrollToPosition(scrollTo)
            }
        }
    }
}