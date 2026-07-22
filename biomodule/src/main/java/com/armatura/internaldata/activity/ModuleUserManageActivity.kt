package com.armatura.internaldata.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.AMTResult
import com.armatura.biomodule.bean.RegisterPhotoInfo
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache
import com.armatura.biomodule.common.AddNewUserType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databinding.ActivityModuleUserManageBinding
import com.armatura.biomodule.databinding.ItemModuleUserInfoBinding
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.manager.ModuleHeartbeatManager
import com.armatura.biomodule.util.BatchAddUtil
import com.armatura.biomodule.util.UserInfoListDiffCallback
import com.armatura.biomodule.util.safeCollect
import com.armatura.biomodule.util.safeShowAlertDialog
import com.armatura.biomodule.util.showOKAlertDialog
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.PullLoadMoreRecyclerView
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.armatura.constant.ErrorCode
import com.armatura.constant.ParamIndex
import com.armatura.internaldata.activity.ModuleUserDetailActivity.Companion.DATA_NAME
import com.armatura.internaldata.util.ModuleBioDataUtil
import com.armatura.internaldata.viewmodel.ModuleUserMgnViewModel
import com.armatura.translib.AMTHidManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rosuh.filepicker.config.FilePickerManager
import java.io.File
import java.lang.ref.WeakReference
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean


/**
 * Created by Jeremy on 2022/11/3.
 * This page is used to demonstrate how to manage user data within the module.
 * Not all modules support this function, please consult business.
 */
class ModuleUserManageActivity : BaseActivity() {
    companion object {
        private val TAG = ModuleUserManageActivity::class.java.simpleName

        fun action(activity: Activity) {
            activity.startActivity(Intent(activity, ModuleUserManageActivity::class.java))
        }

        private const val PAGE_SIZE = 10

        private const val PICK_TEST_DB = 0x9001
        private const val UPDATE_USER_INFO = 0x9002
    }

    private lateinit var viewModel: ModuleUserMgnViewModel
    private lateinit var binding: ActivityModuleUserManageBinding
    private lateinit var registerUserRvAdapter: RegisterUserRvAdapter

    private var clickItemPosition = -1

    private val refreshLock = AtomicBoolean(false)

    @Volatile
    private var pageIndex = 0

    private lateinit var userDetailResultLauncher: ActivityResultLauncher<Intent>
    private lateinit var userAddResultLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = getAndroidViewModel(ModuleUserMgnViewModel::class.java)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_module_user_manage)

        initResultLauncher()

        binding.registerRv.apply {
            addItemDecoration(
                DividerItemDecoration(
                    this@ModuleUserManageActivity, DividerItemDecoration.VERTICAL
                )
            )
            setLinearLayout()
            pullRefreshEnable = false
            setOnPullLoadMoreListener(object :
                PullLoadMoreRecyclerView.PullLoadMoreListener {
                override fun onRefresh() {}

                override fun onLoadMore() {
                    if (binding.registerRv.isEnabled) {
                        loadMorePage()
                    }
                }

                override fun onItemVisibleRangeChanged(start: Int, end: Int) {}
            })
        }

        registerUserRvAdapter = RegisterUserRvAdapter(this@ModuleUserManageActivity)
        binding.registerRv.setAdapter(registerUserRvAdapter)

        binding.fabAddNewUser.setOnClickListener {
            userAddResultLauncher.launch(Intent().apply {
                putExtra(
                    ModuleAddNewUserActivity.ADD_TYPE,
                    AddNewUserType.LOCALE
                )
                setClass(this@ModuleUserManageActivity, ModuleAddNewUserActivity::class.java)
            })
        }

        binding.topToolBar.apply {
            showDeleteAllIcon()
            hideSaveIcon()
            setToolBarClickListener(object : ToolBarClickListener {
                override fun onClickLeft() {
                    finish()
                }

                override fun onClickRight() {
                    safeShowAlertDialog(
                        title = resources.getString(R.string.users_tip_delete_all_data),
                        iconRes = android.R.drawable.ic_dialog_info,
                        positiveAction = {
                            clearAllModuleUser()
                        }
                    )
                }
            })
        }



        binding.fabBatchAddUser.setOnClickListener {
            FilePickerManager.from(this).forResult(PICK_TEST_DB)
        }

        if (!Config.instance().isTestMode) {
            binding.fabBatchAddUser.visibility = View.GONE
        }


        binding.svSearch.apply {
            onActionViewExpanded()
            clearFocus()
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    search(newText.toString())
                    return true
                }

            })
        }

        loadFirstPage()
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.cancel()
    }

    private fun initResultLauncher() {
        userDetailResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val userPin =
                        result?.data?.getStringExtra(ModuleAddNewUserActivity.DATA_TAG_USER_PIN)
                    if (!userPin.isNullOrEmpty()) {
                        flow {
                            ModuleBioDataUtil.instance().getPerson(
                                userPin = userPin,
                                needFaceFeature = true,
                                needPalmFeature = true
                            ).also {
                                emit(it)
                            }
                        }.flowOn(Dispatchers.IO)
                            .safeCollect(lifecycle, lifecycleScope) {
                                if (it.code == ErrorCode.ERROR_NONE) {
                                    //update user info
                                    registerUserRvAdapter.updateUserInfo(it.result!!)
                                } else {
                                    showOKAlertDialog(it.message)
                                }
                            }
                    }
                }
            }

        userAddResultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val userPin =
                        result.data?.getStringExtra(ModuleAddNewUserActivity.DATA_TAG_USER_PIN)
                    if (!userPin.isNullOrEmpty()) {
                        flow {
                            ModuleBioDataUtil.instance().getPerson(
                                userPin = userPin,
                                needFaceFeature = true,
                                needPalmFeature = true
                            ).also {
                                emit(it)
                            }
                        }.flowOn(Dispatchers.IO)
                            .safeCollect(lifecycle, lifecycleScope) {
                                if (it.code == ErrorCode.ERROR_NONE) {
                                    loadFirstPage()
                                } else {
                                    showOKAlertDialog(it.message)
                                }
                            }
                    }
                }
            }

    }

    /**
     * clear all module user
     * clear all recognize record
     * clear all avatar if it exist
     * clear glide
     */
    private fun clearAllModuleUser() {
        showProgressDialog(
            this@ModuleUserManageActivity,
            resources.getString(R.string.users_tip_deleting)
        )
        flow {
            val result = ModuleBioDataUtil.instance().clearPersons()
            if (result.code == ErrorCode.ERROR_NONE) {
                Glide.get(this@ModuleUserManageActivity).clearDiskCache()
                clearUserList()
                //delete recognize cache to avoid of display when back to main page
                RecognizedBioDataCache.instance().clearRecordUserFace()
                RecognizedBioDataCache.instance().clearRecFaces()
                File(Config.getModuleAvatarPath()).apply {
                    if (exists()) {
                        val listFiles = this.listFiles()
                        listFiles?.let {
                            for (file in it) {
                                file.delete()
                            }
                        }
                    }
                }
            }
            emit(result)
        }.flowOn(Dispatchers.IO)
            .catch { exception ->
                emit(AMTResult(404, "${exception.message}", null))
            }
            .safeCollect(lifecycle, lifecycleScope) {
                Glide.get(this@ModuleUserManageActivity).clearMemory()
                if (it.code != ErrorCode.ERROR_NONE) {
                    showOKAlertDialog(it.message)
                } else {
                    flow {
                        emit(getModuleUserInfoSize())
                    }.flowOn(Dispatchers.IO).catch {
                        emit(AMTResult(404, "${it.message}", 0))
                    }.safeCollect(lifecycle, lifecycleScope) {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            updateToolBarUserInfoSize(it.result!!)
                        } else {
                            showOKAlertDialog(it.message)
                        }
                        dismissProgressDialog()
                    }
                }
            }
    }

    private fun setUserListEnable(enable: Boolean) {
        lifecycleScope.launch {
            binding.registerRv.isEnabled = enable
        }
    }

    private fun getModuleUserInfoSize() = ModuleBioDataUtil.instance().queryStatistics()

    private fun updateToolBarUserInfoSize(personCount: Int) {
        lifecycleScope.launch {
            binding.topToolBar.setTitle(
                "%s(%d)".format(
                    Locale.US,
                    getString(R.string.tool_panel_module_users),
                    personCount
                )
            )
        }
    }

    private fun updateModuleUserSize() {
        flow {
            emit(getModuleUserInfoSize())
        }.flowOn(Dispatchers.IO)
            .safeCollect(lifecycle, lifecycleScope) {
                if (it.code == ErrorCode.ERROR_NONE) {
                    updateToolBarUserInfoSize(it.result!!)
                } else {
                    showOKAlertDialog(it.message)
                }
            }
        if (registerUserRvAdapter.itemCount < PAGE_SIZE) {
            loadMorePage()
        }
    }


    private fun loadFirstPage() {
        lifecycleScope.launch {
            clearUserList()
            //show wait dialog
            showProgressDialog(
                this@ModuleUserManageActivity,
                resources.getString(R.string.loading_user)
            )
            //disable recyclerview action
            setUserListEnable(false)
            //get module user size
            val queryPersonCountFlow = flow {
                emit(getModuleUserInfoSize())
            }.flowOn(Dispatchers.IO)

            val loadFirstPageFlow = flow {
                ModuleBioDataUtil.instance().queryAllPerson(0, PAGE_SIZE).also {
                    emit(it)
                }
            }.flowOn(Dispatchers.IO)

            queryPersonCountFlow.safeCollect(lifecycle, lifecycleScope) { queryPersonCountResult ->
                if (queryPersonCountResult.code == ErrorCode.ERROR_NONE) {
                    updateToolBarUserInfoSize(queryPersonCountResult.result!!)
                    loadFirstPageFlow.safeCollect(lifecycle, lifecycleScope) {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            registerUserRvAdapter.replaceList(it.result!!.toMutableList())
                            //enable recyclerview action
                            setUserListEnable(true)
                            binding.registerRv.scrollToPosition(0)
                        } else {
                            showOKAlertDialog(it.message)
                        }
                        //dismiss dialog
                        dismissProgressDialog()
                    }
                } else {
                    //dismiss dialog
                    dismissProgressDialog()
                    showOKAlertDialog(queryPersonCountResult.message)
                }
            }

        }
    }

    private fun loadMorePage() {
        lifecycleScope.launch(Dispatchers.IO) {
            if (!refreshLock.get()) {
                pageIndex++
                refreshLock.set(true)
                withContext(Dispatchers.Main) {
                    setUserListEnable(false)
                }
                //load more user
                flow {
                    ModuleBioDataUtil.instance().queryAllPerson(pageIndex, PAGE_SIZE).also {
                        emit(it)
                    }
                }.flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            registerUserRvAdapter.appendUserInfo(it.result!!)
                        } else {
                            withContext(Dispatchers.Main) {
                                showOKAlertDialog(it.message)
                            }
                        }
                    }

                withContext(Dispatchers.Main) {
                    setUserListEnable(true)
                }
                refreshLock.set(false)
            }
            withContext(Dispatchers.Main) {
                binding.registerRv.setPullLoadMoreCompleted()
            }
        }
    }

    private fun clearUserList() {
        registerUserRvAdapter.clear()
    }

    private fun search(name: String) {
        if (!binding.registerRv.isEnabled) {
            toastMsg("Can't locate when update")
            return
        }
        if (TextUtils.isEmpty(name)) {
            loadFirstPage()
        } else {
            lifecycleScope.launch {
                flow {
                    ModuleBioDataUtil.instance().getPerson(
                        name, needFaceFeature = true, needPalmFeature = true
                    ).also {
                        emit(it)
                    }
                }.flowOn(Dispatchers.IO)
                    .safeCollect(lifecycle, lifecycleScope) {
                        if (it.code == ErrorCode.ERROR_NONE) {
                            val list = ArrayList<UserInfo>(1).apply {
                                this.add(it.result!!)
                            }
                            registerUserRvAdapter.replaceList(list)
                        } else {
                            toastAnywhere(it.message)
                        }
                    }
            }


        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_TEST_DB -> {
                    val filePathList = FilePickerManager.obtainData()
                    lifecycleScope.launch {
                        val importProgressDialog =
                            ProgressDialogFragment.show(
                                supportFragmentManager,
                                "Batch import...",
                                "",
                                false
                            )
                        withContext(Dispatchers.IO) {
                            ModuleHeartbeatManager.getInstance().heatBeatStop()
                            for (filePath in filePathList) {
                                BatchAddUtil.importUserInfoToModuleFromDBFile(
                                    filePath,
                                    object :
                                        BatchAddUtil.BatchAddListener {
                                        override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                        }

                                        override fun onBatchAddProgress(progressMsg: String?) {
                                            lifecycleScope.launch {
                                                importProgressDialog.setMessage(progressMsg)
                                            }
                                        }

                                        override fun onError(error: Int, message: String?) {
                                            message?.let { msg ->
                                                lifecycleScope.launch {
                                                    showOKAlertDialog(msg)
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                            withContext(Dispatchers.Main) {
                                importProgressDialog.dismiss(supportFragmentManager)
                            }
                        }

                        val reloadAlgorithmDialog = ProgressDialogFragment.show(
                            supportFragmentManager,
                            "",
                            "Reload algorithm",
                            false
                        )
                        withContext(Dispatchers.IO) {
                            //reload algorithm
                            val result = ByteArray(255)
                            val size = intArrayOf(255)
                            AMTHidManager.instance()
                                .setParam(ParamIndex.RELOAD_ALGORITHM, null, result, size)
                                .also {
                                    Log.i(TAG, "Reload algorithm: ret = $it")
                                    if (it == 0) {
                                        Log.i(
                                            TAG,
                                            "Reload algorithm: result=${
                                                String(
                                                    result,
                                                    0,
                                                    size[0]
                                                )
                                            }"
                                        )
                                    }
                                }
                        }
                        reloadAlgorithmDialog.dismiss(supportFragmentManager)

                        loadFirstPage()
                    }
                }
            }
        }
    }


    class RegisterUserRvAdapter(
        userManageActivity: ModuleUserManageActivity,
    ) : ListAdapter<UserInfo, RegisterUserRvAdapter.UserInfoViewHolder>(UserInfoListDiffCallback) {
        private val userManageActivityWrf: WeakReference<ModuleUserManageActivity> =
            WeakReference(userManageActivity)

        fun replaceList(userInfos: MutableList<UserInfo>) {
            submitList(ArrayList(userInfos))
        }

        fun appendUserInfo(userInfoList: List<UserInfo>) {
            val newList = mutableListOf<UserInfo>()
            newList.addAll(currentList)
            newList.addAll(userInfoList)
            submitList(newList)
        }

        fun updateUserInfo(userInfo: UserInfo) {
            val newList = currentList.toMutableList()
            val iterator = newList.listIterator()
            var index = 0
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.userId == userInfo.userId) {
                    iterator.remove()
                    break
                }
                index++
            }
            newList.add(index, userInfo)
            submitList(newList)
        }

        private fun removeUserInfo(userInfo: UserInfo) {
            val newList = currentList.toMutableList()
            val iterator = newList.iterator()
            while (iterator.hasNext()) {
                val info = iterator.next()
                if (info.userId == userInfo.userId) {
                    iterator.remove()
                    break
                }
            }
            submitList(newList)
            //delete recognize cache to avoid of display when back to main page
            RecognizedBioDataCache.instance().deleteFaceRecord(userInfo.userId)
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserInfoViewHolder {
            val binding = DataBindingUtil.inflate<ItemModuleUserInfoBinding>(
                LayoutInflater.from(parent.context),
                R.layout.item_module_user_info,
                parent,
                false
            )
            return UserInfoViewHolder(binding)
        }

        override fun onBindViewHolder(
            holderUserInfo: UserInfoViewHolder, @SuppressLint("RecyclerView") position: Int,
        ) {
            val userInfo = getItem(position)
            val userManageActivity = userManageActivityWrf.get()
            if (userManageActivity != null && !userManageActivity.isFinishing && !userManageActivity.isDestroyed) {
                Glide.with(userManageActivity).load(R.drawable.default_avatar)
                    .diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true)
                    .error(R.drawable.default_avatar)
                    .into(holderUserInfo.binding.imageViewItemImg)
            }
            with(holderUserInfo.binding) {
                textViewItemID.text = String.format(
                    Locale.getDefault(), "%s", userInfo.userId
                )
                textViewItemName.text = String.format(
                    Locale.getDefault(), "%s", userInfo.name
                )
                faceStatusIv.setImageResource(
                    if (userInfo.face > 0) R.mipmap.ic_face_show else R.mipmap.ic_face
                )
                palmStatusIv.setImageResource(
                    if (userInfo.palm > 0) R.drawable.ic_palm_green else R.drawable.ic_palm_gray
                )
                userInfoLl.setOnClickListener {
                    val activity = userManageActivityWrf.get() ?: return@setOnClickListener
                    activity.clickItemPosition = position
                    val intent =
                        Intent(activity, ModuleUserDetailActivity::class.java).apply {
                            putExtra(DATA_NAME, userInfo.userId)
                        }
                    activity.userDetailResultLauncher.launch(intent)
                }
                if (!Config.isSupportPalm) {
                    palmStatusIv.visibility = View.GONE
                }
                if (!Config.isSupportFace) {
                    faceStatusIv.visibility = View.GONE
                }
                imageButtonItemDelete.setOnClickListener {
                    userManageActivityWrf.get()?.also { activity ->
                        val userPin = userInfo.userId
                        activity.safeShowAlertDialog(
                            title = getString(R.string.tips_delete_user),
                            iconRes = android.R.drawable.ic_dialog_info,
                            positiveAction = {
                                if (!activity.binding.registerRv.isEnabled) {
                                    activity.toastMsg(R.string.users_tip_cannot_delete)
                                    return@safeShowAlertDialog
                                }
                                flow {
                                    val result =
                                        ModuleBioDataUtil.instance()
                                            .deletePerson(userPin.toString())
                                    emit(result)
                                }.flowOn(Dispatchers.IO)
                                    .catch { exception ->
                                        emit(AMTResult(404, "${exception.message}", null))
                                    }
                                    .safeCollect(activity.lifecycle, activity.lifecycleScope) {
                                        if (it.code == ErrorCode.ERROR_NONE) {
                                            removeUserInfo(userInfo)
                                            activity.toastMsg(R.string.users_tip_delete_ok)
                                            File(Config.getModuleAvatarPath() + userPin + ".jpg").apply {
                                                if (exists()) {
                                                    delete()
                                                }
                                            }
                                            activity.updateModuleUserSize()
                                        } else {
                                            activity.showOKAlertDialog(it.message)
                                        }
                                    }
                            }
                        )
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            return currentList.size
        }

        fun clear() {
            val newList = mutableListOf<UserInfo>()
            submitList(newList)
        }

        class UserInfoViewHolder internal constructor(
            val binding: ItemModuleUserInfoBinding,
        ) : RecyclerView.ViewHolder(binding.root)
    }

}