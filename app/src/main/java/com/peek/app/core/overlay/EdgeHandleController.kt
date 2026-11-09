package com.peek.app.core.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import com.peek.app.core.diagnostics.PeekLog
import kotlin.math.abs

/**
 * Trigger 3: a slim handle on the right screen edge, available in any app.
 *
 * - Swipe it inward (or tap it) to peek at the latest conversation.
 * - Drag it up or down to move it; the position is remembered.
 *
 * The window is tiny, not focusable and not touch-modal, so it never blocks the app
 * underneath beyond its own few dp. It is excluded from the system back gesture so an
 * inward swipe reaches PEEK. Nothing runs while it is idle: no polling, no timers.
 */
class EdgeHandleController(
    context: Context,
    private val onActivate: () -> Unit,
    private val onMoved: (yFraction: Float) -> Unit,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val density = appContext.resources.displayMetrics.density
    private var view: HandleView? = null
    private var params: WindowManager.LayoutParams? = null

    var color: Int = 0x998E8E93.toInt()

    val isShowing: Boolean get() = view != null

    fun show(yFraction: Float) {
        if (!OverlayPermissionChecker.isGranted(appContext)) {
            hide()
            return
        }
        val screenHeight = appContext.resources.displayMetrics.heightPixels
        val y = (yFraction.coerceIn(0.05f, 0.9f) * screenHeight).toInt()
        val existing = view
        if (existing != null) {
            params?.let {
                it.y = y
                safeUpdate(existing, it)
            }
            return
        }
        val handle = HandleView(appContext)
        val lp = WindowManager.LayoutParams(
            (24 * density).toInt(),
            (96 * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.END or Gravity.TOP
            this.y = y
            title = "PEEK handle"
        }
        try {
            windowManager.addView(handle, lp)
            view = handle
            params = lp
        } catch (e: RuntimeException) {
            PeekLog.w("edge handle add failed", e)
        }
    }

    fun hide() {
        val current = view ?: return
        view = null
        params = null
        try {
            if (current.isAttachedToWindow) windowManager.removeViewImmediate(current)
        } catch (e: RuntimeException) {
            PeekLog.w("edge handle remove failed", e)
        }
    }

    private fun safeUpdate(v: View, lp: WindowManager.LayoutParams) {
        try {
            if (v.isAttachedToWindow) windowManager.updateViewLayout(v, lp)
        } catch (e: RuntimeException) {
            PeekLog.w("edge handle update failed", e)
        }
    }

    @SuppressLint("ViewConstructor")
    private inner class HandleView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val pill = RectF()
        private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        private val activateDistance = 28 * density

        private var downX = 0f
        private var downY = 0f
        private var startY = 0
        private var dragging = false
        private var triggered = false

        init {
            contentDescription = "PEEK handle. Double-tap to peek at the latest conversation."
            isClickable = true
        }

        override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
            super.onLayout(changed, left, top, right, bottom)
            // Keep inward swipes on the handle from being taken by the system back gesture.
            systemGestureExclusionRects = listOf(Rect(0, 0, width, height))
        }

        override fun onDraw(canvas: Canvas) {
            val w = 5 * density
            val h = 64 * density
            val right = width - 4 * density
            pill.set(right - w, (height - h) / 2f, right, (height + h) / 2f)
            paint.color = color
            canvas.drawRoundRect(pill, w / 2, w / 2, paint)
        }

        override fun performClick(): Boolean {
            super.performClick()
            activate()
            return true
        }

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouchEvent(event: MotionEvent): Boolean {
            val lp = params ?: return false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    startY = lp.y
                    dragging = false
                    triggered = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = downX - event.rawX // positive = inward from the right edge
                    val dy = event.rawY - downY
                    if (!dragging && !triggered && abs(dy) > touchSlop && abs(dy) > abs(dx)) dragging = true
                    if (dragging) {
                        lp.y = (startY + dy).toInt().coerceAtLeast(0)
                        safeUpdate(this, lp)
                    } else if (!triggered && dx > activateDistance) {
                        triggered = true
                        activate()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    if (dragging) {
                        val screenHeight = resources.displayMetrics.heightPixels.coerceAtLeast(1)
                        onMoved(lp.y.toFloat() / screenHeight)
                    } else if (!triggered && abs(event.rawX - downX) < touchSlop && abs(event.rawY - downY) < touchSlop) {
                        performClick()
                    }
                }
            }
            return true
        }

        private fun activate() {
            performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            onActivate()
        }
    }
}
