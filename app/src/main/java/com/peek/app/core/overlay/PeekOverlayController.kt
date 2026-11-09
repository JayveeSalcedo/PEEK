package com.peek.app.core.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.peek.app.core.diagnostics.PeekLog
import com.peek.app.data.model.PeekConversation
import com.peek.app.features.preview.PeekPreviewCard
import com.peek.app.ui.theme.PeekTheme

/**
 * Shows one floating preview card above whatever app is in the foreground.
 *
 * - The window is not focusable and not touch-modal: touches outside the card reach the app
 *   underneath, and the keyboard/back button stay with that app.
 * - A touch outside the card dismisses it (the touch still reaches the underlying app).
 * - All window operations run on the main thread and tolerate the permission being revoked
 *   or the window already being gone.
 */
class PeekOverlayController(context: Context) {

    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: View? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null

    val isShowing: Boolean get() = overlayView != null

    /** Returns false if the overlay could not be shown (e.g. permission missing). */
    fun show(conversation: PeekConversation): Boolean {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(conversation) }
            return OverlayPermissionChecker.isGranted(appContext)
        }
        if (!OverlayPermissionChecker.isGranted(appContext)) return false

        dismiss()

        val owner = OverlayLifecycleOwner()
        val root = OverlayRootView(appContext, onOutsideTouch = ::dismiss)
        val composeView = ComposeView(appContext).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                PeekTheme {
                    PeekPreviewCard(
                        conversation = conversation,
                        onDismiss = ::dismiss,
                    )
                }
            }
        }
        root.addView(composeView)
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.CENTER
            title = "PEEK preview"
        }

        return try {
            owner.start()
            windowManager.addView(root, params)
            overlayView = root
            lifecycleOwner = owner
            true
        } catch (e: RuntimeException) {
            // BadTokenException / SecurityException if permission was revoked in between.
            PeekLog.w("overlay add failed", e)
            owner.destroy()
            false
        }
    }

    /** Removes the preview if present. Safe to call at any time, from any thread. */
    fun dismiss() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { dismiss() }
            return
        }
        val view = overlayView ?: return
        overlayView = null
        try {
            if (view.isAttachedToWindow) windowManager.removeViewImmediate(view)
        } catch (e: RuntimeException) {
            PeekLog.w("overlay remove failed", e)
        } finally {
            lifecycleOwner?.destroy()
            lifecycleOwner = null
        }
    }

    /** Root container that turns an outside touch into a dismissal. */
    @SuppressLint("ViewConstructor")
    private class OverlayRootView(
        context: Context,
        private val onOutsideTouch: () -> Unit,
    ) : FrameLayout(context) {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            if (event.actionMasked == MotionEvent.ACTION_OUTSIDE) {
                onOutsideTouch()
                return true
            }
            return super.dispatchTouchEvent(event)
        }
    }
}
