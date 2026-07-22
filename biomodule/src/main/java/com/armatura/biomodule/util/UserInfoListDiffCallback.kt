package com.armatura.biomodule.util

import androidx.recyclerview.widget.DiffUtil
import com.armatura.biomodule.bean.UserInfo

object UserInfoListDiffCallback : DiffUtil.ItemCallback<UserInfo>() {
    override fun areItemsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: UserInfo, newItem: UserInfo): Boolean {
        return (oldItem.face == newItem.face) &&
                (oldItem.palm == newItem.palm)
    }
}