package app.filtro.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import app.filtro.core.NumberInfo
import app.filtro.core.Tone
import app.filtro.data.Store
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Tarjeta que aparece sobre la pantalla de llamada entrante con el tipo de número.
 * Se cierra sola al contestar, al cortar o a los 35 segundos. Se puede arrastrar
 * hacia arriba o abajo (recuerda la posición) y se cierra con un toque.
 */
object CallerOverlay {

    private val main = Handler(Looper.getMainLooper())
    private var current: View? = null
    private var watcher: Runnable? = null

    fun show(context: Context, info: NumberInfo, name: String?, silenced: Boolean) {
        val app = context.applicationContext
        if (!Settings.canDrawOverlays(app)) return
        main.post {
            runCatching { attach(app, info, name, silenced) }
                .onFailure { android.util.Log.w(ScreeningService.TAG, "No se pudo mostrar el identificador", it) }
        }
    }

    fun hide(context: Context) {
        val app = context.applicationContext
        main.post { detach(app) }
    }

    private fun detach(app: Context) {
        watcher?.let { main.removeCallbacks(it) }
        watcher = null
        val v = current ?: return
        current = null
        runCatching { app.getSystemService(WindowManager::class.java).removeView(v) }
    }

    private class Palette(
        val surface: Int,
        val stroke: Int,
        val text: Int,
        val muted: Int,
        val accent: Int,
    )

    private fun palette(night: Boolean, tone: Tone): Palette {
        val accent = when (tone) {
            Tone.NEUTRAL -> if (night) 0xFF8FC9B5 else 0xFF2E5E4E
            Tone.INFO -> if (night) 0xFFA9C2E6 else 0xFF35598C
            Tone.CAUTION -> if (night) 0xFFE6BC74 else 0xFF9A6312
            Tone.WARNING -> if (night) 0xFFF2A39B else 0xFFB3261E
        }.toInt()
        return if (night) {
            Palette(0xFF1B1E1D.toInt(), 0xFF2E3331.toInt(), 0xFFECEFED.toInt(), 0xFFA6ADA9.toInt(), accent)
        } else {
            Palette(0xFFFFFFFF.toInt(), 0xFFE2E5E2.toInt(), 0xFF191C1B.toInt(), 0xFF5D6460.toInt(), accent)
        }
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun attach(app: Context, info: NumberInfo, name: String?, silenced: Boolean) {
        detach(app)
        val wm = app.getSystemService(WindowManager::class.java)
        val dm = app.resources.displayMetrics
        fun px(v: Float): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, dm).roundToInt()

        val night = (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val p = palette(night, info.tone)
        val medium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        fun label(text: CharSequence, sp: Float, color: Int, face: Typeface? = null) = TextView(app).apply {
            this.text = text
            setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
            setTextColor(color)
            if (face != null) typeface = face
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        val root = FrameLayout(app).apply {
            setPadding(px(14f), px(8f), px(14f), px(18f))
            clipToPadding = false
            clipChildren = false
        }
        val card = LinearLayout(app).apply {
            orientation = LinearLayout.HORIZONTAL
            background = GradientDrawable().apply {
                cornerRadius = px(26f).toFloat()
                setColor(p.surface)
                setStroke(px(1f), p.stroke)
            }
            elevation = px(12f).toFloat()
            setPadding(px(18f), px(16f), px(8f), px(16f))
        }

        val bar = View(app).apply {
            background = GradientDrawable().apply {
                cornerRadius = px(3f).toFloat()
                setColor(p.accent)
            }
        }
        card.addView(
            bar,
            LinearLayout.LayoutParams(px(4f), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                marginEnd = px(14f)
            },
        )

        val texts = LinearLayout(app).apply { orientation = LinearLayout.VERTICAL }
        val tag = if (silenced) "FILTRO · SILENCIADA" else "FILTRO"
        texts.addView(label(tag, 11f, p.accent, medium).apply { letterSpacing = 0.14f })
        texts.addView(
            label(name ?: info.title, 19f, p.text, medium).apply { setPadding(0, px(4f), 0, 0) }
        )
        if (name != null) texts.addView(label(info.title, 13f, p.muted))
        texts.addView(
            label(info.display, 15f, p.text).apply {
                letterSpacing = 0.02f
                setPadding(0, px(2f), 0, 0)
            }
        )
        texts.addView(label(info.detail, 13f, p.muted).apply { setPadding(0, px(4f), 0, 0) })
        card.addView(texts, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val close = label("✕", 15f, p.muted).apply {
            setPadding(px(10f), px(2f), px(10f), px(10f))
            contentDescription = "Cerrar"
        }
        card.addView(close, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(card, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT))

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val saved = Store.overlayY
            y = if (saved >= 0) saved else (dm.heightPixels * 0.26f).roundToInt()
            windowAnimations = android.R.style.Animation_Toast
        }

        root.setOnTouchListener(object : View.OnTouchListener {
            private var downY = 0f
            private var startY = 0
            private var dragging = false

            override fun onTouch(v: View, e: MotionEvent): Boolean {
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downY = e.rawY
                        startY = params.y
                        dragging = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dy = e.rawY - downY
                        if (!dragging && abs(dy) > px(8f)) dragging = true
                        if (dragging) {
                            params.y = (startY + dy).roundToInt().coerceIn(0, max(0, dm.heightPixels - v.height))
                            runCatching { wm.updateViewLayout(v, params) }
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        if (dragging) {
                            Store.overlayY = params.y
                        } else {
                            v.performClick()
                            detach(app)
                        }
                    }
                }
                return true
            }
        })

        wm.addView(root, params)
        current = root
        watchCall(app)
    }

    /** Sin pedir permisos de teléfono: mira el modo de audio para saber si contestaron o cortaron. */
    private fun watchCall(app: Context) {
        val am = app.getSystemService(AudioManager::class.java) ?: return
        val started = SystemClock.elapsedRealtime()
        val initial = am.mode
        val busyAtStart = initial == AudioManager.MODE_IN_CALL || initial == AudioManager.MODE_IN_COMMUNICATION
        var sawRinging = initial == AudioManager.MODE_RINGTONE
        val limit = if (busyAtStart) 15_000L else 35_000L

        val r = object : Runnable {
            override fun run() {
                val mode = am.mode
                if (mode == AudioManager.MODE_RINGTONE) sawRinging = true
                val answered = !busyAtStart &&
                    (mode == AudioManager.MODE_IN_CALL || mode == AudioManager.MODE_IN_COMMUNICATION)
                val ended = sawRinging && mode == AudioManager.MODE_NORMAL
                val expired = SystemClock.elapsedRealtime() - started > limit
                if (answered || ended || expired) detach(app) else main.postDelayed(this, 400)
            }
        }
        watcher = r
        main.postDelayed(r, 700)
    }
}
