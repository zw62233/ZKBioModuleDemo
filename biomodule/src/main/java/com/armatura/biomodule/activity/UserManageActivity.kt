package com.armatura.biomodule.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.UserDetailActivity.Companion.action
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.bean.RegisterPhotoInfo
import com.armatura.biomodule.bean.UserInfo
import com.armatura.biomodule.camera.biodata.RecognizedBioDataCache
import com.armatura.biomodule.common.AddNewUserType
import com.armatura.biomodule.config.Config
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.databinding.ActivityUserManageBinding
import com.armatura.biomodule.databinding.ItemUserinfoBinding
import com.armatura.biomodule.dialog.ProgressDialogFragment
import com.armatura.biomodule.pojo.setting.VLPalmSetting
import com.armatura.biomodule.register.RegisterHelper
import com.armatura.biomodule.thread.UpdateUserInfoTask
import com.armatura.biomodule.util.BatchAddUtil
import com.armatura.biomodule.util.BatchAddUtil.BatchAddListener
import com.armatura.biomodule.util.safeShowAlertDialog
import com.armatura.biomodule.util.showCancelableAlertDialog
import com.armatura.biomodule.util.toastAnywhere
import com.armatura.biomodule.view.ToolBar.ToolBarClickListener
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.rosuh.filepicker.bean.FileItemBeanImpl
import me.rosuh.filepicker.config.AbstractFileFilter
import me.rosuh.filepicker.config.FilePickerManager
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserManageActivity : BaseActivity() {
    var faceRedActivityCtxtWeakRef: WeakReference<Context>? = null
    private val registerHelper by lazy { RegisterHelper.instance() }
    val registerFaceAdapter by lazy {
        RegedFaceRecyclerViewAdapter(
            this,
            BioDataUtil.userFaces_all_List
        )
    }

    private var bDelData = false
    private var mainHandler: Handler? = null
    private var addFaceHandler: Handler? = null
    private var addFaceHandlerThread: HandlerThread? = null
    private var clickItemPosition = -1
    private var batchAddListener: BatchBatchAddListener? = null

    lateinit var binding: ActivityUserManageBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_manage)
        faceRedActivityCtxtWeakRef = WeakReference(this)
        registerHelper.setCallback {
            UpdateUserInfoTask(
                this@UserManageActivity,
                BioDataUtil.userFaces_all_List.size
            ).execute()
        }
        batchAddListener = BatchBatchAddListener(this)
        binding.fabAddUser.apply {
            setOnClickListener {
                AddNewUserActivity.action(
                    this@UserManageActivity,
                    AddNewUserType.LOCALE
                )
            }
            setOnLongClickListener { v ->
                if (Config.instance().isTestMode) {
                    batchAddFace(v)
                }
                true
            }
        }
        binding.fabBatchAddPalm.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnLongClickListener { v ->
                batchAddPalmFromFile(v)
                true
            }
            setOnClickListener { v -> batchAddPalmFromPicture(v) }
        }

        binding.fabBatchAddPalmAndVein.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnLongClickListener { v ->
                batchAddPalmAndVeinFromFile(v)
                true
            }
        }

        binding.fabImportFaceFromNewDb.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnClickListener { v ->
                batchImportFaceFromNewFile(v)
            }
        }

        binding.fabImportFaceFromOldDb.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnClickListener { v ->
                batchImportFaceFromOldFile(v)
            }
        }

        binding.fabImportMx.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnClickListener { v ->
                batchImportFaceFromCSVFile(v)
            }
        }

        binding.recyclerViewRegedFace.apply {
            addItemDecoration(
                DividerItemDecoration(
                    this@UserManageActivity,
                    DividerItemDecoration.VERTICAL
                )
            )
            adapter = registerFaceAdapter
        }
        binding.myToolBar.apply {
            showDeleteAllIcon()
            hideSaveIcon()
            setToolBarClickListener(object : ToolBarClickListener {
                override fun onClickLeft() {
                    finish()
                }

                override fun onClickRight() {
                    safeShowAlertDialog(
                        message = resources.getString(R.string.users_tip_delete_all_data),
                        iconRes = android.R.drawable.ic_dialog_info,
                        positiveAction = {
                            lifecycleScope.launch {
                                showProgressDialog(
                                    this@UserManageActivity,
                                    resources.getString(R.string.users_tip_deleting)
                                )
                                withContext(Dispatchers.IO) {
                                    deleteUsers()
                                }
                                dismissProgressDialog()
                                updateUserSize(BioDataUtil.userFaces_all_List.size)
                                Glide.get(context.applicationContext).clearMemory()
                                withContext(Dispatchers.IO) {
                                    Glide.get(applicationContext).clearDiskCache()
                                }
                                registerFaceAdapter.notifyDataSetChanged()
                            }
                        }
                    )
                }
            })
        }
        binding.svSearch.apply {
            onActionViewExpanded()
            clearFocus()
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String): Boolean {
                    return false
                }

                override fun onQueryTextChange(newText: String): Boolean {
                    search(newText)
                    return true
                }
            })
            setOnQueryTextFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    mainHandler!!.removeCallbacks(hideSystemUIRunnable)
                    mainHandler!!.postDelayed(hideSystemUIRunnable, 100)
                }
            }
        }
        addFaceHandlerThread = HandlerThread("AddFaceHandlerThread")
        addFaceHandlerThread!!.start()
        addFaceHandler = Handler(addFaceHandlerThread!!.getLooper(), Handler.Callback { msg ->
            when (msg.what) {
                SINGLE_REGISTER_FACE -> {
                    val inputStream: InputStream? = try {
                        contentResolver.openInputStream((msg.obj as Uri))
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        mainHandler!!.obtainMessage(SHOW_TOAST, "Picture data input error")
                            .sendToTarget()
                        return@Callback false
                    }
                    val bitmap = BitmapFactory.decodeStream(inputStream) ?: return@Callback false
                    registerHelper.registerByBitmap(
                        bitmap,
                        RegisterHelper.LOCAL_IMAGE,
                        faceRedActivityCtxtWeakRef!!.get(),
                        null,
                        null
                    )
                }

                BATCH_REGISTER_PALM -> {
                    mainHandler!!.obtainMessage(
                        SHOW_PROGRESS,
                        getString(R.string.batch_adding_palms)
                    ).sendToTarget()
                    val registerType = msg.obj as Int
                    when (registerType) {
                        REGISTER_TYPE_FILE -> {
                            BatchAddUtil.batchAddPalmFromFile(
                                "palmFeatures.db",
                                batchAddListener
                            )
                        }

                        else -> {
                            mainHandler!!.obtainMessage(DISMISS_PROGRESS).sendToTarget()
                            return@Callback false
                        }
                    }
                    mainHandler!!.obtainMessage(DISMISS_PROGRESS).sendToTarget()
                    UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                }

                else -> {}
            }
            false
        })
        mainHandler = Handler(Looper.getMainLooper()) { msg ->
            when (msg.what) {
                SHOW_TOAST -> {
                    val tip = msg.obj as String
                    toastAnywhere(tip)
                }

                UPDATE_PROGRESS -> setProgressText(msg.obj as String)
                SHOW_PROGRESS -> showProgressDialog(this@UserManageActivity, msg.obj as String)
                DISMISS_PROGRESS -> dismissProgressDialog()
                else -> {}
            }
            false
        }
        bDelData = false
        Log.w(TAG, "onCreate")
        updateUserSize(BioDataUtil.userFaces_all_List.size)

        binding.fabExportUser.apply {
            visibility = if (Config.instance().isTestMode) View.VISIBLE else View.GONE
            setOnClickListener {
                FilePickerManager.from(this@UserManageActivity)
                    .maxSelectable(1)
                    .filter(object : AbstractFileFilter() {
                        override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
                            return java.util.ArrayList(listData.filter { item ->
                                item.isDir
                            })
                        }
                    })
                    .skipDirWhenSelect(false)
                    .forResult(PICK_EXPORT_PATH)
            }
        }
    }


    override fun onResume() {
        Log.w(TAG, "onResume")
        super.onResume()
        if (clickItemPosition != -1) {
            val clickedItem = BioDataUtil.findUserInfoFromDatabasesByUserPin(
                registerFaceAdapter.userFaceArrayList[clickItemPosition].userId
            )
            if (clickedItem != null) {
                registerFaceAdapter.updateUserInfo(clickedItem)
            }
            registerFaceAdapter.notifyItemChanged(clickItemPosition)
            clickItemPosition = -1
        }
        UpdateUserInfoTask(this, 0).execute()
        RegisterHelper.instance().setFragmentManager(supportFragmentManager)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                PICK_EXPORT_PATH -> {
                    val filePath = FilePickerManager.obtainData()[0]
                    if (filePath.trim().isBlank()) {
                        toastAnywhere("Invalid Path")
                        return
                    }
                    lifecycleScope.launch {
                        val dialog = ProgressDialogFragment.show(
                            supportFragmentManager,
                            "",
                            "Exporting...",
                            false,
                        )
                        withContext(Dispatchers.IO) {
                            val userCount = binding.recyclerViewRegedFace.adapter?.itemCount
                            if (userCount == 0) {
                                return@withContext
                            }
                            val timestamp =
                                SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                            val exportFile = File(filePath, "export_$timestamp _$userCount.db")
                            if (exportFile.exists()) {
                                exportFile.delete()
                                exportFile.createNewFile()
                            } else {
                                exportFile.createNewFile()
                            }
                            var fos: FileOutputStream? = null
                            var bos: BufferedOutputStream? = null
                            try {
                                fos = FileOutputStream(exportFile)
                                bos = BufferedOutputStream(fos)
                                val pageSize = 500L
                                var pageIndex = 0L
                                while (true) {
                                    val userInfoList =
                                        BioDataUtil.queryUserInfoByPage(pageIndex, pageSize)
                                    if (userInfoList.isNullOrEmpty()) {
                                        break
                                    }
                                    for (userInfo in userInfoList) {
                                        saveUserInfoToFile(userInfo, bos)
                                    }
                                    pageIndex++
                                }
                            } finally {
                                fos?.close()
                                bos?.close()
                            }
                        }
                        dialog.dismiss()
                    }
                }

                PICK_FACE_PHOTO_PATH -> {
                    val facePhotoDirPath = FilePickerManager.obtainData()[0]
                    if (facePhotoDirPath.trim().isBlank()) {
                        toastAnywhere("Invalid Path")
                        return
                    }
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Face By Photo"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.batchAddFace(
                                facePhotoDirPath,
                                object :
                                    BatchAddListener {
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                        registerPhotoInfo?.let {
                                            registerHelper
                                                .registerByBitmap(
                                                    it.bitmap, RegisterHelper.LOCAL_IMAGE,
                                                    this@UserManageActivity,
                                                    it.fileName, registerPhotoInfo.filePath
                                                )
                                        }
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        lifecycleScope.launch {
                                            this@UserManageActivity.setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                    }

                                }
                            )
                        }
                        dismissProgressDialog()
                    }
                }

                PICK_PALM_TEST_DB -> {
                    val palmTestDBPath = FilePickerManager.obtainData()[0]
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Palm By Repository"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.batchAddPalmFromFile(
                                palmTestDBPath,
                                object :
                                    BatchAddListener {
                                    val mainScope = CoroutineScope(Dispatchers.Main)
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        mainScope.launch {
                                            setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                    }

                                }
                            )
                        }
                        dismissProgressDialog()
                        UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                    }

                }

                PICK_PALM_AND_VEIN_TEST_DB -> {
                    val palmTestDBPath = FilePickerManager.obtainData()[0]
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Palm And Vein By Repository"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.batchAddPalmAndPalmVeinFromFile(
                                palmTestDBPath,
                                object :
                                    BatchAddListener {
                                    val mainScope = CoroutineScope(Dispatchers.Main)
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        mainScope.launch {
                                            setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                    }

                                }
                            )
                        }
                        dismissProgressDialog()
                        UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                    }

                }


                PICK_PALM_PHOTO_PATH -> {
                    val palmPhotoPath = FilePickerManager.obtainData()[0]
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Palm By Pictures"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.batchAddPalmFromPictures(palmPhotoPath,
                                object : BatchAddListener {
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        lifecycleScope.launch {
                                            setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                        lifecycleScope.launch {
                                            message?.let {
                                                showCancelableAlertDialog("Error:$error", it)
                                            }
                                        }
                                    }

                                })
                        }
                        dismissProgressDialog()
                        UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                    }

                }

                PICK_FACE_FROM_FACE_MX_CSV -> {
                    val faceTestDBPath = FilePickerManager.obtainData()[0]
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Face By FaceMX CSV File"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.importUserInfoToAppDBFromFaceMXFile(
                                faceTestDBPath,
                                object : BatchAddListener {
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        lifecycleScope.launch {
                                            setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                        lifecycleScope.launch {
                                            message?.let {
                                                showCancelableAlertDialog("Error:$error", it)
                                            }
                                        }
                                    }

                                }
                            )
                        }
                        dismissProgressDialog()
                        UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                    }
                }

                PICK_FACE_OLD_TEST_DB, PICK_FACE_NEW_TEST_DB -> {
                    val faceTestDBPath = FilePickerManager.obtainData()[0]
                    lifecycleScope.launch {
                        showProgressDialog(
                            this@UserManageActivity,
                            "Import Face By Repository"
                        )
                        withContext(Dispatchers.IO) {
                            BatchAddUtil.importUserInfoToAppDBFromDBFile(
                                requestCode == PICK_FACE_OLD_TEST_DB,
                                faceTestDBPath,
                                object : BatchAddListener {
                                    override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo?) {
                                    }

                                    override fun onBatchAddProgress(progressMsg: String?) {
                                        lifecycleScope.launch {
                                            setProgressText(progressMsg)
                                        }
                                    }

                                    override fun onError(error: Int, message: String?) {
                                        lifecycleScope.launch {
                                            message?.let {
                                                showCancelableAlertDialog("Error:$error", it)
                                            }
                                        }
                                    }

                                }
                            )
                        }
                        dismissProgressDialog()
                        UpdateUserInfoTask(this@UserManageActivity, 0).execute()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        Log.w(TAG, "onDestroy")
        super.onDestroy()
        if (bDelData) {
            Log.w(TAG, "onDestroy: deleted some face, clear cache")
            Glide.get(this).clearMemory()
            Thread { Glide.get(this@UserManageActivity).clearDiskCache() }.start()
        }
        registerHelper!!.setCallback(null)
        addFaceHandlerThread!!.quitSafely()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun search(name: String) {
        if (!binding.recyclerViewRegedFace.isEnabled) {
            toastMsg("Can't locate when update")
            return
        }
        if (TextUtils.isEmpty(name)) {
            registerFaceAdapter.changeList(BioDataUtil.userFaces_all_List)
            registerFaceAdapter.notifyDataSetChanged()
        } else {
            val userInfos = BioDataUtil.instance().fuzzyQueryByName(name)
            registerFaceAdapter.changeList(userInfos)
            registerFaceAdapter.notifyDataSetChanged()
        }
    }

    private fun batchAddFace(view: View) {
        Snackbar.make(view, "Click Go to batch add face", Snackbar.LENGTH_LONG)
            .setAction("Go!") {
                FilePickerManager.from(this)
                    .filter(object : AbstractFileFilter() {
                        override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
                            return java.util.ArrayList(listData.filter { item ->
                                item.isDir
                            })
                        }
                    })
                    .skipDirWhenSelect(false)
                    .forResult(PICK_FACE_PHOTO_PATH)
            }
            .show()
    }

    fun setUserListEnable(enable: Boolean) {
        runOnUiThread { binding.recyclerViewRegedFace.setEnabled(enable) }
    }

    fun updateUserSize(size: Int) {
        lifecycleScope.launch {
            binding.myToolBar.setTitle(
                getString(R.string.tool_panel_users) + String.format(
                    Locale.US,
                    "(%d)",
                    size
                )
            )
        }
    }

    /**
     * typedef struct _PalmDB_ {
     *         char Palmid[128];
     *         int size;
     *         int type;
     *         char template[PALM_DB_FEATURE_SIZE_MAX];
     * }__attribute__((packed)) TPalmDB, *PPalmDB;
     */
    private fun saveUserInfoToFile(userInfo: UserInfo, bos: BufferedOutputStream) {
        with(bos) {
            val userPinBytes =
                "${userInfo.userId}_${userInfo.name}_${userInfo.avatarIndex}".toByteArray()
            val id = ByteArray(128)
            System.arraycopy(userPinBytes, 0, id, 0, userPinBytes.size)
            write(id)



            if (userInfo.faceFeature != null) {
                val sizeBytes = int2Bytes(userInfo.faceFeature.size)
                write(sizeBytes)

                val type = int2Bytes(9)
                write(type)

                val templateBytes = ByteArray(2048)
                val feature = userInfo.faceFeature
                System.arraycopy(feature, 0, templateBytes, 0, feature.size)

                write(templateBytes)
            } else {
                if (userInfo.palmFeature1 != null) {
                    val sizeBytes = int2Bytes(userInfo.palmFeature1.size)
                    write(sizeBytes)
                    val type = int2Bytes(6)
                    write(type)
                    val templateBytes = ByteArray(2048)
                    val feature = userInfo.palmFeature1
                    System.arraycopy(feature, 0, templateBytes, 0, feature.size)
                    write(templateBytes)
                }
            }
            flush()
        }
    }


    /**
     * typedef struct _PalmDB_ {
     *         char Palmid[128];
     *         int color_size;
     *         int color_index;
     *         char color_feature[2048];
     *         int ir_size;
     *         int ir_index;
     *         char ir_feature[2048];
     * }__attribute__((packed)) TPalmDB, *PPalmDB;
     */
    private fun saveDualPalmUserInfoToFile(userInfo: UserInfo, bos: BufferedOutputStream) {
        with(bos) {
            val userPinBytes =
                "${userInfo.userId}_${userInfo.name}_${userInfo.avatarIndex}".toByteArray()
            val id = ByteArray(128)
            System.arraycopy(userPinBytes, 0, id, 0, userPinBytes.size)
            write(id)

            if (userInfo.palmFeature1 != null) {
                val sizeBytes = int2Bytes(userInfo.palmFeature1.size)
                write(sizeBytes)
                val index = int2Bytes(0)
                write(index)
                val templateBytes = ByteArray(2048)
                val feature = userInfo.palmFeature1
                System.arraycopy(feature, 0, templateBytes, 0, feature.size)
                write(templateBytes)

            }
            if (userInfo.palmFeature2 != null) {
                val sizeBytes = int2Bytes(userInfo.palmFeature2.size)
                write(sizeBytes)
                val index = int2Bytes(0)
                write(index)
                val templateBytes = ByteArray(2048)
                val feature = userInfo.palmFeature2
                System.arraycopy(feature, 0, templateBytes, 0, feature.size)
                write(templateBytes)
            }
            flush()
        }
    }

    private fun int2Bytes(src: Int): ByteArray {
        val dstBytes = ByteArray(4)
        dstBytes[0] = (src and 0xFF).toByte()
        dstBytes[1] = (src shr 8 and 0xFF).toByte()
        dstBytes[2] = (src shr 16 and 0xFF).toByte()
        dstBytes[3] = (src shr 24).toByte()
        return dstBytes
    }

    fun deleteUsers() {
        registerHelper!!.deleteAllUser()
        //delete record
        RecognizedBioDataCache.instance().clearRecordUserFace()
    }

    class RegedFaceRecyclerViewAdapter(
        userManageActivity: UserManageActivity,
        var userFaceArrayList: MutableList<UserInfo>,
    ) : RecyclerView.Adapter<RegedFaceRecyclerViewAdapter.ViewHolder>() {
        private val activityWeakReference: WeakReference<UserManageActivity> =
            WeakReference(userManageActivity)

        fun changeList(userInfos: MutableList<UserInfo>) {
            userFaceArrayList = userInfos
        }

        fun deleteUserInfoInAdapter(userPin: String?) {
            val iterator = userFaceArrayList.iterator()
            while (iterator.hasNext()) {
                val userFace = iterator.next()
                if (userFace.userId == userPin) {
                    iterator.remove()
                    break
                }
            }
        }

        fun updateUserInfo(userInfo: UserInfo) {
            val iterator: Iterator<UserInfo> = userFaceArrayList.iterator()
            while (iterator.hasNext()) {
                val dstUser = iterator.next()
                if (dstUser.userId == userInfo.userId) {
                    dstUser.name = userInfo.name
                    dstUser.userId = userInfo.userId
                    dstUser.faceFeature = userInfo.faceFeature
                    dstUser.palmFeature1 = userInfo.palmFeature1
                    dstUser.palmFeature2 = userInfo.palmFeature2
                    dstUser.age = userInfo.age
                    dstUser.gender = userInfo.gender
                    dstUser.avatarIndex = userInfo.avatarIndex
                    break
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val binding: ItemUserinfoBinding = DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_userinfo,
                parent,
                false
            )
            return ViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val binding = holder.binding
            val userInfo = userFaceArrayList[position]
            activityWeakReference.get()?.let {
                Glide.with(it)
                    .load(userInfo.avatarDrawable)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .error(R.drawable.ic_app)
                    .into(binding.imageViewItemImg)
            }
            binding.textViewItemID.text = String.format(Locale.getDefault(), "%s", userInfo.userId)
            binding.textViewItemName.text = String.format(Locale.getDefault(), "%s", userInfo.name)
            if (Config.isSupportFace) {
                binding.faceStatusIv.setImageResource(if (userInfo.faceFeature != null) R.mipmap.ic_face_show else R.mipmap.ic_face)
            } else {
                binding.faceStatusIv.setVisibility(View.GONE)
            }
            if (Config.isSupportPalm) {
                if (Config.palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_VL ||
                    Config.palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR
                ) {
                    binding.palmStatusIv.setImageResource(
                        if (userInfo.palmFeature1 != null)
                            R.drawable.ic_palm_green
                        else
                            R.drawable.ic_palm_gray
                    )
                } else {
                    binding.palmStatusIv.visibility = View.GONE
                }

                if (Config.palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_IR ||
                    Config.palmTemplateMode == VLPalmSetting.PALM_TEMPLATE_MODE_VL_AND_IR
                ) {
                    binding.palmVeinStatusIv.setImageResource(
                        if (userInfo.palmFeature2 != null)
                            R.mipmap.ic_palm_vein
                        else
                            R.mipmap.ic_palm_vein_gray
                    )
                } else {
                    binding.palmVeinStatusIv.visibility = View.GONE
                }

            } else {
                binding.palmStatusIv.setVisibility(View.GONE)
                binding.palmVeinStatusIv.setVisibility(View.GONE)
            }

            if (Config.isSupportRFID) {
                binding.rfidStatusIv.setImageResource(
                    if (BioDataUtil.isUserInfoHasCard(userInfo.userId))
                        R.drawable.ic_card_green
                    else
                        R.drawable.ic_card_gray
                )
            } else {
                binding.rfidStatusIv.setVisibility(View.GONE)
            }

            binding.userInfoLl.setOnClickListener {
                activityWeakReference.get()!!.clickItemPosition = holder.adapterPosition
                action(
                    activityWeakReference.get()!!,
                    userInfo.userId
                )
            }
            binding.imageButtonItemDelete.setOnClickListener(object : View.OnClickListener {
                override fun onClick(it: View) {
                    activityWeakReference.get()?.let { activity ->
                        activity.safeShowAlertDialog(
                            title = getString(R.string.tips_delete_user),
                            iconRes = android.R.drawable.ic_dialog_info,
                            positiveAction = {
                                if (!activity.binding.recyclerViewRegedFace.isEnabled) {
                                    activity.toastMsg(R.string.users_tip_cannot_delete)
                                    return@safeShowAlertDialog
                                }
                                if (activity.registerHelper!!.deleteUserFromDatabasesAndMemory(
                                        userInfo.userId
                                    )
                                ) {
                                    activity.registerFaceAdapter.deleteUserInfoInAdapter(
                                        userInfo.userId
                                    )
                                    //delete face record
                                    RecognizedBioDataCache.instance()
                                        .deleteFaceRecord(userInfo.userId)
                                    activity.toastMsg(R.string.users_tip_delete_ok)
                                    activity.updateUserSize(BioDataUtil.userFaces_all_List.size)
                                    activity.bDelData = true
                                } else {
                                    activity.toastMsg(R.string.users_tip_delete_fail)
                                }
                                activity.registerFaceAdapter.notifyDataSetChanged()
                            }
                        )
                    }
                }
            })
        }


        override fun getItemCount(): Int {
            return userFaceArrayList.size
        }

        class ViewHolder internal constructor(val binding: ItemUserinfoBinding) :
            RecyclerView.ViewHolder(binding.root)
    }

    private fun batchAddPalmFromFile(view: View) {
        Snackbar.make(view, R.string.tips_batch_enroll_palm_from_file, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this).forResult(PICK_PALM_TEST_DB)
            }
            .show()
    }

    private fun batchAddPalmAndVeinFromFile(view: View) {
        Snackbar.make(view, R.string.tips_batch_enroll_palm_from_file, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this).forResult(PICK_PALM_AND_VEIN_TEST_DB)
            }
            .show()
    }

    private fun batchImportFaceFromNewFile(view: View) {
        Snackbar.make(view, R.string.tips_batch_import_face_from_db, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this).forResult(PICK_FACE_NEW_TEST_DB)
            }
            .show()
    }

    private fun batchImportFaceFromOldFile(view: View) {
        Snackbar.make(view, R.string.tips_batch_import_face_from_db, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this).forResult(PICK_FACE_OLD_TEST_DB)
            }
            .show()
    }

    private fun batchImportFaceFromCSVFile(view: View) {
        Snackbar.make(view, R.string.tips_batch_import_face_from_face_mx_csv, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this).forResult(PICK_FACE_FROM_FACE_MX_CSV)
            }
            .show()
    }


    private fun batchAddPalmFromPicture(view: View) {
        Snackbar.make(view, R.string.tips_batch_enroll_palm_by_photo, Snackbar.LENGTH_LONG)
            .setAction(R.string.txt_ok) {
                FilePickerManager.from(this)
                    .filter(object : AbstractFileFilter() {
                        override fun doFilter(listData: ArrayList<FileItemBeanImpl>): ArrayList<FileItemBeanImpl> {
                            return ArrayList(listData.filter { item ->
                                item.isDir
                            })
                        }

                    })
                    .skipDirWhenSelect(false)
                    .forResult(PICK_PALM_PHOTO_PATH)
            }
            .show()
    }

    private class BatchBatchAddListener(userManageActivity: UserManageActivity) :
        BatchAddListener {
        private val activityWeakReference: WeakReference<UserManageActivity> =
            WeakReference(userManageActivity)

        override fun OnFacePhotoAvailable(registerPhotoInfo: RegisterPhotoInfo) {
            val userManageActivity = activityWeakReference.get() ?: return
            val bitmap = registerPhotoInfo.bitmap
            val name = registerPhotoInfo.fileName
            userManageActivity.registerHelper
                .registerByBitmap(
                    bitmap, RegisterHelper.LOCAL_IMAGE,
                    userManageActivity.faceRedActivityCtxtWeakRef!!.get(),
                    name, registerPhotoInfo.filePath
                )
        }

        override fun onBatchAddProgress(progressMsg: String) {
            val userManageActivity = activityWeakReference.get()
            if (userManageActivity != null) {
                userManageActivity.mainHandler!!.obtainMessage(UPDATE_PROGRESS, progressMsg)
                    .sendToTarget()
            }
        }

        override fun onError(error: Int, message: String?) {
        }
    }

    companion object {
        private const val TAG = "UserManageActivity"
        private const val SINGLE_REGISTER_FACE = 1002
        private const val BATCH_REGISTER_PALM = 1003
        private const val SHOW_TOAST = 2003
        private const val UPDATE_PROGRESS = 2004
        private const val SHOW_PROGRESS = 2005
        private const val DISMISS_PROGRESS = 2006
        private const val REGISTER_TYPE_FILE = 0
        private const val PICK_PALM_TEST_DB = 0x9001
        private const val PICK_EXPORT_PATH = 0x9002
        private const val PICK_FACE_PHOTO_PATH = 0x9003
        private const val PICK_PALM_PHOTO_PATH = 0x9004
        private const val PICK_FACE_NEW_TEST_DB = 0x9005
        private const val PICK_FACE_OLD_TEST_DB = 0x9006
        private const val PICK_PALM_AND_VEIN_TEST_DB = 0x9007
        private const val PICK_FACE_FROM_FACE_MX_CSV = 0x9008

        fun action(context: Context) {
            val intent = Intent(context, UserManageActivity::class.java)
            context.startActivity(intent)
        }

        fun actionTransition(activity: Activity) {
            val intent = Intent(activity, UserManageActivity::class.java)
            activity.startActivity(intent)
        }
    }
}
