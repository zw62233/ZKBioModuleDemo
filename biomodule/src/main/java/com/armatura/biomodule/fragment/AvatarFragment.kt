package com.armatura.biomodule.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.activity.adapter.AvatarAdapter
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.databinding.FragmentAvatarBinding
import com.armatura.biomodule.view.ToolBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Jeremy on 2024/3/27.
 */
class AvatarFragment : BaseFragment() {

    private lateinit var binding: FragmentAvatarBinding

    private var avatarAdapter: AvatarAdapter? = null

    private var userId: String? = null

    private var selectPos = -1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAvatarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        avatarAdapter = AvatarAdapter()
        binding.avatarRecyclerView.adapter = avatarAdapter
        avatarAdapter?.setOnItemClickListener { pos ->
            val avatarItem = AvatarAdapter.avatarSampleList[pos]
            binding.userAvatarIv.setImageResource(avatarItem.drawableId)
            selectPos = pos
        }

        var title: String? = null

        arguments?.also {
            userId = it.getString(USER_ID).toString()
            title = it.getString(TITLE).toString()
        }

        binding.myToolBar.apply {
            setTitle(title)
            setToolBarClickListener(object : ToolBar.ToolBarClickListener {
                override fun onClickLeft() {
                    activity?.onBackPressedDispatcher?.onBackPressed()
                }

                override fun onClickRight() {

                    lifecycleScope.launch(Dispatchers.IO) {
                        saveAvatar(userId)
                        withContext(Dispatchers.Main) {
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            })
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
            withContext(Dispatchers.Main) {
                binding.userAvatarIv.setImageResource(userInfo.avatarDrawable)
            }
        }
    }

    private fun saveAvatar(userId: String?) {
        if (selectPos == -1) return
        val userInfo = BioDataUtil.findUserInfoFromDatabasesByUserPin(userId)
        userInfo.avatarIndex = avatarAdapter?.avatarIndex ?: 0
        BioDataUtil.instance().updateUserInfo(userInfo)
    }

    override fun onImagePicked(uri: Uri?) {
    }

    override fun onFilePicked(uri: Uri?) {
    }

    companion object {
        const val TAG = "AvatarFragment"
        private const val TITLE = "title"
        private const val USER_ID = "userId"
        private const val AVATAR_TYPE = "avatar"

        fun newInstance(
            title: String?, userId: String?, isHostAvatar: Boolean
        ): AvatarFragment {
            val args = Bundle().apply {
                putString(TITLE, title)
                putString(USER_ID, userId)
                putBoolean(AVATAR_TYPE, isHostAvatar)
            }
            val fragment = AvatarFragment()
            fragment.arguments = args
            return fragment
        }
    }
}