package com.peek.app.core.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.data.model.PeekConversation
import com.peek.app.data.model.SampleConversation
import com.peek.app.features.preview.PeekOverlayContent
import com.peek.app.ui.theme.PeekTheme

/**
 * Shows the peek as a full-screen overlay window, like an iOS context-menu preview.
 *
 * The window is modal on purpose: a stray tap must not reach the messaging app underneath
 * (that could open the chat and mark it read, which is exactly what PEEK exists to avoid).
 * It is only shown when the user asks for a peek, and one tap anywhere outside the card,
 * the Close action, or Back dismisses it. Where the device supports cross-window blur
 * (Android 12+), the app behind is blurred as well as dimmed.
 */
class PeekOverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private class Shown(val root: View, val owner: OverlayLifecycleOwner, val closing: MutableState<Boolean>)

    private var shown: Shown? = null

    val isShowing: Boolean get() = shown != null

    /** Returns false if the overlay could not be shown (e.g. permission missing). */
    fun show(conversation: PeekConversation): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(conversation) }
            return OverlayPermissionChecker.isGranted(appContext)
        }
        if (!OverlayPermissionChecker.isGranted(appContext)) return false

        dismissNow()

        val owner = OverlayLifecycleOwner()
        val closing = mutableStateOf(false)
        val root = OverlayRootView(appContext, onBack = { closing.value = true })
        val composeView = ComposeView(appContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                PeekTheme {
                    PeekOverlayContent(
                        conversation = conversation,
                        closing = closing.value,
                        onOpenApp = openAppAction(conversation),
                        onCloseRequested = { closing.value = true },
                        onClosed = ::dismissNow,
                    )
                }
            }
        }
        root.addView(composeView)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT,
        ).apply {
            title = "PEEK preview"
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) fitInsetsTypes = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && windowManager.isCrossWindowBlurEnabled) {
                flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                blurBehindRadius = (28 * appContext.resources.displayMetrics.density).toInt()
            }
        }

        return try {
            owner.start()
            windowManager.addView(root, params)
            shown = Shown(root, owner, closing)
            true
        } catch (e: RuntimeException) {
            // BadTokenException / SecurityException if permission was revoked in between.
            PeekLog.w("overlay add failed", e)
            owner.destroy()
            false
        }
    }

    /** Closes the preview with its exit animation. Safe from any thread. */
    fun dismiss() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { dismiss() }
            return
        }
        shown?.closing?.value = true
    }

    /** Removes the preview immediately, without animation. Safe from any thread. */
    fun dismissNow() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { dismissNow() }
            return
        }
        val current = shown ?: return
        shown = null
        try {
            if (current.root.isAttachedToWindow) windowManager.removeViewImmediate(current.root)
        } catch (e: RuntimeException) {
            PeekLog.w("overlay remove failed", e)
        } finally {
            current.owner.destroy()
        }
    }

    /**
     * "Open Messenger" action. Launches the app's main screen only: PEEK keeps no
     * notification PendingIntents, so it cannot deep-link into a specific conversation.
     */
    private fun openAppAction(conversation: PeekConversation): (() -> Unit)? {
        if (conversation.packageName == SampleConversation.PACKAGE) return null
        val launch = appContext.packageManager.getLaunchIntentForPackage(conversation.packageName)
            ?: return null
        return {
            dismissNow()
            try {
                appContext.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (e: RuntimeException) {
                PeekLog.w("open app failed", e)
            }
        }
    }

    /** Root container that turns Back into an animated dismissal. */
    @SuppressLint("ViewConstructor")
    private class OverlayRootView(
        context: Context,
        private val onBack: () -> Unit,
    ) : FrameLayout(context) {
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP && !event.isCanceled) onBack()
                return true
            }
            return super.dispatchKeyEvent(event)
        }
    }
}
