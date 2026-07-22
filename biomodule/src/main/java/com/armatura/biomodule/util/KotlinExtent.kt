package com.armatura.biomodule.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.app.Activity
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.Animation
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.RotateAnimation
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import com.armatura.biomodule.R
import com.armatura.biomodule.activity.base.ExApplication
import com.armatura.biomodule.activity.base.getString
import com.armatura.biomodule.register.RegisterStatus
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import org.json.JSONException
import org.json.JSONObject

fun toastAnywhere(msg: CharSequence) {
    Toast.makeText(ExApplication.instance(), msg, Toast.LENGTH_SHORT).show()
}

fun toastAnywhere(msg: String) {
    Toast.makeText(ExApplication.instance(), msg, Toast.LENGTH_SHORT).show()
}

fun toastAnywhere(@StringRes stringId: Int) {
    Toast.makeText(ExApplication.instance(), getString(stringId), Toast.LENGTH_SHORT).show()
}

fun stringFormatD(status: RegisterStatus, ret: Int): String {
    val format = getString(status)
    return String.format(format, ret)
}

fun <T> stringFormatSTT(status: RegisterStatus, userId: String, t1: T, t2: T): String {
    val format = getString(status)
    return String.format(format, userId, t1, t2)
}


fun String.getStatus(): Int {
    return JSONObject(this).getStatus()
}

fun String.getDetail(): String {
    return JSONObject(this).getDetail()
}

fun JSONObject.getStatus(): Int {
    if (this.has("status")) {
        return this.getInt("status")
    }
    return 404
}

fun JSONObject.getDetail(): String {
    if (this.has("detail")) {
        return "(${getStatus()})${this.getString("detail")}"

    }
    return "unknown"
}

fun JSONObject.getDataJSONObject(): JSONObject {
    if (!this.has("data")) {
        return JSONObject()
    }
    return this.getJSONObject("data")
}

fun JSONObject.getDataJSONObject(key: String): JSONObject {
    if (!this.has("data")) {
        return JSONObject()
    }
    val dataJSONObject = this.getJSONObject("data")
    if (!dataJSONObject.has(key)) {
        return JSONObject()
    }

    return try {
        dataJSONObject.getJSONObject(key)
    } catch (e: JSONException) {
        Log.e("JSON", "Exception: $this", e)
        JSONObject()
    }
}

fun FragmentActivity.showOKAlertDialog(msg: String, onConfirm: () -> Unit) {
    AlertDialog.Builder(this)
        .setTitle(msg)
        .setIcon(android.R.drawable.ic_dialog_info).setPositiveButton(
            R.string.txt_ok
        ) { dialog, _ ->
            onConfirm.invoke()
            dialog.dismiss()
        }
        .create().also {
            it.setCanceledOnTouchOutside(false)
            it.show()
        }
}


fun FragmentActivity.showOKAlertDialog(msg: String) {
    if (isFinishing || isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(msg)
        .setIcon(android.R.drawable.ic_dialog_info).setPositiveButton(
            this.resources.getString(R.string.txt_ok)
        ) { dialog, _ ->
            dialog.dismiss()
        }
        .create().also {
            it.setCanceledOnTouchOutside(false)
            it.show()
        }
}

fun FragmentActivity.showCancelableAlertDialog(title: String, msg: String) {
    if (isFinishing || isDestroyed) return
    AlertDialog.Builder(this)
        .setTitle(title)
        .setMessage(msg)
        .setIcon(android.R.drawable.ic_dialog_info)
        .create().also {
            it.setCancelable(true)
            it.setCanceledOnTouchOutside(true)
            it.show()
        }
}

fun View.flipAnim(animEnd: () -> Unit) {
    val startScaleX = this.scaleX
    Log.i("", "flipAnim: startScaleX=$startScaleX")
    val flipOut = ObjectAnimator.ofFloat(this, "scaleX", startScaleX, 0F)
    flipOut.duration = 300
    flipOut.interpolator = AccelerateInterpolator()


    val targetScaleX = if (startScaleX > 0) -1F else 1F
    Log.i("", "flipAnim: targetScaleX=$targetScaleX")
    val flipIn = ObjectAnimator.ofFloat(this, "scaleX", 0F, targetScaleX)
    flipIn.duration = 300
    flipIn.interpolator = DecelerateInterpolator()

    flipOut.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            flipIn.start()
        }
    })

    flipIn.addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            animEnd.invoke()
            this@flipAnim.scaleX = startScaleX
        }
    })
    flipOut.start()
}


fun View.rotate90Anim(animEnd: () -> Unit) {
    ObjectAnimator.ofFloat(this, "rotation", 0F, 90F).apply {
        duration = 300
        addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                this@rotate90Anim.rotation = 0F
                animEnd.invoke()
            }
        })
    }.also {
        it.start()
    }
}

fun View.rotate90Anim() {
    this.startAnimation(
        RotateAnimation(
            0F, 90F,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 300
            interpolator = LinearInterpolator()
        })
}


fun <T> Flow<T>.safeCollect(
    lifecycle: Lifecycle, lifeScope: CoroutineScope,
    func: suspend (value: T) -> Unit,
) {
//    this.flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
//        .onEach {
//            if (currentCoroutineContext().isActive) {
//                func(it)
//            }
//        }.launchIn(lifeScope)
    lifeScope.launch {
        this@safeCollect.collect {
            func(it)
        }
    }
}

fun Activity.safeShowAlertDialog(
    message: String = "",
    title: String? = null,
    cancelable: Boolean = true,
    iconRes: Int = 0,
    positiveText: String = "OK",
    positiveAction: (() -> Unit)? = null,
    negativeText: String? = null,
    negativeAction: (() -> Unit)? = null,
) {
    if (isFinishing || isDestroyed) return

    val builder = AlertDialog.Builder(this)
        .setMessage(message)
        .setCancelable(cancelable)
        .setIcon(iconRes)

    if (title != null) {
        builder.setTitle(title)
    }

    builder.setPositiveButton(positiveText) { dialog, _ ->
        positiveAction?.invoke()
        dialog.dismiss()
    }

    if (negativeText != null) {
        builder.setNegativeButton(negativeText) { dialog, _ ->
            negativeAction?.invoke()
            dialog.dismiss()
        }
    }

    builder.show()
}

fun Fragment.safeShowAlertDialog(
    message: String = "",
    title: String? = null,
    cancelable: Boolean = true,
    iconRes: Int = 0,
    positiveText: String = "OK",
    positiveAction: (() -> Unit)? = null,
    negativeText: String? = null,
    negativeAction: (() -> Unit)? = null,
) {
    if (isDetached || isRemoving || isHidden) {
        return
    }
    val builder = AlertDialog.Builder(requireContext())
        .setMessage(message)
        .setCancelable(cancelable)
        .setIcon(iconRes)

    if (title != null) {
        builder.setTitle(title)
    }

    builder.setPositiveButton(positiveText) { dialog, _ ->
        positiveAction?.invoke()
        dialog.dismiss()
    }

    if (negativeText != null) {
        builder.setNegativeButton(negativeText) { dialog, _ ->
            negativeAction?.invoke()
            dialog.dismiss()
        }
    }

    builder.show()
}