package com.focuszen.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.focuszen.app.R
import kotlin.math.abs

/**
 * Servicio encargado de mostrar la píldora flotante translúcida sobre la aplicación supervisada.
 * Muestra el propósito consciente del usuario y ofrece un botón de salida directa al escritorio.
 */
class FloatingGoalOverlayService : Service() {

    companion object {
        const val ACTION_START_OVERLAY = "com.focuszen.action.START_OVERLAY"
        const val ACTION_STOP_OVERLAY = "com.focuszen.action.STOP_OVERLAY"
        const val ACTION_SET_VISIBILITY = "com.focuszen.action.SET_VISIBILITY"

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_PURPOSE_TEXT = "extra_purpose_text"
        const val EXTRA_IS_VISIBLE = "extra_is_visible"

        private const val NOTIFICATION_CHANNEL_ID = "focus_zen_overlay_channel"
        private const val NOTIFICATION_ID = 2001

        private var instance: FloatingGoalOverlayService? = null

        fun isRunning(): Boolean = instance != null

        fun updateVisibilityForPackage(context: Context, currentPackage: String?) {
            val service = instance ?: return
            val activePkg = SessionManager.currentActivePackage
            val shouldBeVisible = (currentPackage != null && currentPackage == activePkg)
            service.setVisibility(shouldBeVisible)
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingGoalOverlayService::class.java).apply {
                action = ACTION_STOP_OVERLAY
            }
            context.stopService(intent)
        }
    }

    private var windowManager: WindowManager? = null
    private var pillView: View? = null
    private var targetPackage: String = ""
    private var purposeText: String = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_OVERLAY -> {
                targetPackage = intent.getStringExtra(EXTRA_PACKAGE_NAME) ?: ""
                purposeText = intent.getStringExtra(EXTRA_PURPOSE_TEXT) ?: "Mi Objetivo"
                createOrUpdateFloatingPill()
            }
            ACTION_SET_VISIBILITY -> {
                val isVisible = intent.getBooleanExtra(EXTRA_IS_VISIBLE, true)
                setVisibility(isVisible)
            }
            ACTION_STOP_OVERLAY -> {
                removeFloatingPill()
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun createOrUpdateFloatingPill() {
        if (!Settings.canDrawOverlays(this)) return

        if (pillView == null) {
            val inflater = LayoutInflater.from(this)
            val view = inflater.inflate(R.layout.layout_floating_pill, null)

            val tvPurpose = view.findViewById<TextView>(R.id.tv_purpose)
            val btnDone = view.findViewById<TextView>(R.id.btn_done)

            tvPurpose.text = purposeText

            // Botón "Objetivo Cumplido" -> Cierra la app y vuelve a Home
            btnDone.setOnClickListener {
                onGoalCompleted()
            }

            // Configurar parámetros de la ventana flotante
            val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 60
                y = 220
            }

            // Soporte para arrastrar la píldora con el dedo a cualquier parte de la pantalla
            view.setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                    if (event == null) return false
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = params.x
                            initialY = params.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - initialTouchX).toInt()
                            val dy = (event.rawY - initialTouchY).toInt()
                            params.x = initialX + dx
                            params.y = initialY + dy
                            try {
                                windowManager?.updateViewLayout(view, params)
                            } catch (_: Exception) {}
                            return true
                        }
                        MotionEvent.ACTION_UP -> {
                            // Si apenas se movió, permitir que pase el toque a los hijos si fuera necesario
                            val totalDelta = abs(event.rawX - initialTouchX) + abs(event.rawY - initialTouchY)
                            if (totalDelta < 15) {
                                v?.performClick()
                            }
                            return true
                        }
                    }
                    return false
                }
            })

            pillView = view
            try {
                windowManager?.addView(view, params)
            } catch (_: Exception) {}
        } else {
            // Actualizar texto si ya existe
            pillView?.findViewById<TextView>(R.id.tv_purpose)?.text = purposeText
            pillView?.visibility = View.VISIBLE
        }
    }

    fun setVisibility(visible: Boolean) {
        pillView?.let { view ->
            val targetVisibility = if (visible) View.VISIBLE else View.GONE
            if (view.visibility != targetVisibility) {
                view.visibility = targetVisibility
            }
        }
    }

    private fun onGoalCompleted() {
        // 1. Expulsar inmediatamente al usuario al escritorio del teléfono (Home)
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)

        // 2. Revocar la sesión activa para que la app vuelva a estar protegida
        if (targetPackage.isNotBlank()) {
            SessionManager.revokeSession(targetPackage)
        }

        // 3. Quitar la ventana flotante y detener el servicio
        removeFloatingPill()
        stopSelf()
    }

    private fun removeFloatingPill() {
        pillView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (_: Exception) {}
            pillView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        removeFloatingPill()
        instance = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "FocusZen - Recordatorio Flotante",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Mantiene activo el recordatorio flotante de tu objetivo"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("FocusZen Activo")
            .setContentText("Recordatorio de objetivo en curso")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }
}
