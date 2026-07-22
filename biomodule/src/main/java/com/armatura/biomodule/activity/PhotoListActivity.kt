package com.armatura.biomodule.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.databinding.DataBindingUtil
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.adapter.PhotoListAdapter
import com.armatura.biomodule.activity.base.BaseActivity
import com.armatura.biomodule.databinding.ActivityPhotoListBinding
import com.armatura.biomodule.view.ToolBar

class PhotoListActivity : BaseActivity() {
    companion object {
        @JvmStatic
        fun action(context: Context, list: ArrayList<String>) {
            Intent(context, PhotoListActivity::class.java).apply {
                putStringArrayListExtra("photo", list)
            }.also {
                context.startActivity(it)
            }
        }
    }

    private lateinit var binding: ActivityPhotoListBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_photo_list)

        intent.getStringArrayListExtra("photo")?.let {
            binding.photoListRecyclerView.adapter = PhotoListAdapter(supportFragmentManager, it)
        }
        binding.myToolBar.setToolBarClickListener(object : ToolBar.ToolBarClickListener {
            override fun onClickLeft() {
                finish()
            }

            override fun onClickRight() {
            }

        })

    }

}