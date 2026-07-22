package com.armatura.biomodule.fragment

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.armatura.biomodule.bean.CardInfo
import com.armatura.biomodule.camera.CameraController
import com.armatura.biomodule.databases.BioDataUtil
import com.armatura.biomodule.databinding.FragmentRfidBinding
import com.armatura.biomodule.view.ToolBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by Jeremy on 2024/3/27.
 */
class RFIDFragment : BaseFragment() {

    private lateinit var binding: FragmentRfidBinding


    private var userId: String? = null
    private var mCardNum: String = ""
    private var mCardInfo: CardInfo? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentRfidBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
                        //SAVE
                        val cardInfo = if (mCardInfo == null) {
                            CardInfo().apply {
                                userId = this@RFIDFragment.userId
                                rawCard = mCardNum
                            }
                        } else {
                            mCardInfo?.apply {
                                rawCard = mCardNum
                            }
                        }
                        cardInfo.also {
                            val result = BioDataUtil.instance().saveCardInfo(it)
                            Log.i(TAG, "onClickRight: $result")
                        }
                        withContext(Dispatchers.Main) {
                            activity?.onBackPressedDispatcher?.onBackPressed()
                        }
                    }
                }
            })
        }

        lifecycleScope.launch(Dispatchers.IO) {
            mCardInfo = BioDataUtil.instance().getCardInfoByUserId(userId)
            mCardInfo?.let {
                mCardNum = it.rawCard
                withContext(Dispatchers.Main) {
                    binding.tvRfidValue.text = mCardNum
                }
            }

        }
    }


    override fun onPause() {
        super.onPause()
        lifecycleScope.launch(Dispatchers.IO) {
            CameraController.instance().pauseCam(this@RFIDFragment)
            enterStandByMode()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            CameraController.instance().resumeCam(this@RFIDFragment)
            exitStandByMode()
        }
    }

    override fun onImagePicked(uri: Uri?) {
    }

    override fun onFilePicked(uri: Uri?) {
    }


    override fun onCardInfo(cardInfo: CardInfo?) {
        lifecycleScope.launch {
            cardInfo?.let {
                if (mCardNum != cardInfo.rawCard) {
                    binding.tvRfidValue.text = it.rawCard
                    mCardNum = it.rawCard
                }
            }
        }
    }


    companion object {
        const val TAG = "RFIDFragment"
        private const val TITLE = "title"
        private const val USER_ID = "userId"
        private const val REGISTER_TYPE = "type"

        fun newInstance(title: String?, userId: String?, type: Int): RFIDFragment {
            val args = Bundle().apply {
                putString(TITLE, title)
                putString(USER_ID, userId)
                putInt(REGISTER_TYPE, type)
            }
            val fragment = RFIDFragment()
            fragment.arguments = args
            return fragment
        }
    }
}