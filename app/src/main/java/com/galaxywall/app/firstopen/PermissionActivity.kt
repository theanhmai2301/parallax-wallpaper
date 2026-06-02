package com.galaxywall.app.firstopen

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.galaxywall.app.databinding.ActivityPermissionBinding
import com.galaxywall.app.ui.MainActivity

/**
 * Notification-permission screen. Mirrors the source app's PermissionActivity: asks for
 * POST_NOTIFICATIONS (Android 13+), reflects the granted state on the button/icon, and moves on to
 * the app. Ad loading is left as the [loadNativeAd] hook (frAds is the container).
 */
class PermissionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPermissionBinding
    private var requestTimes = 0

    companion object {
        private const val REQUEST_CODE = 101

        fun start(context: Context) {
            if (isNotificationPermissionGranted(context)) {
                MainActivity.startMain(context)
            } else {
                context.startActivity(Intent(context, PermissionActivity::class.java))
            }
        }

        fun isNotificationPermissionGranted(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        }

        /** True if the screen will be shown (permission not granted yet) — also the ad preload spot. */
        fun preloadAds(activity: Activity): Boolean {
            if (!isNotificationPermissionGranted(activity)) {
                // TODO: ADS — preload the permission native ad here.
                return true
            }
            return false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        createView()
    }

    private fun createView() {
        reflectGranted(isNotificationPermissionGranted(this))

        binding.imageIcPer.setOnClickListener { requestNotificationPermission() }
        binding.button3.setOnClickListener {
            if (isNotificationPermissionGranted(this)) {
                MainActivity.startMain(this)
                finish()
            } else {
                requestNotificationPermission()
            }
        }
        loadNativeAd()
    }

    private fun reflectGranted(granted: Boolean) {
        binding.button3.isActivated = granted
        binding.button3.setTextColor(if (granted) Color.WHITE else Color.parseColor("#666666"))
        binding.imageIcPer.isActivated = granted
        binding.imageIcPer.isEnabled = !granted
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !isNotificationPermissionGranted(this)
        ) {
            requestTimes++
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            val granted = grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            reflectGranted(granted)
            if (!granted) {
                Toast.makeText(this, "Notification permission is denied", Toast.LENGTH_SHORT).show()
                if (requestTimes >= 3) {
                    MainActivity.startMain(this)
                    finish()
                }
            }
        }
    }

    /** Placeholder: load + show the permission native ad into binding.frAds. */
    private fun loadNativeAd() {
        // showNativeAd(this, frAds = binding.frAds, ...)
    }
}
