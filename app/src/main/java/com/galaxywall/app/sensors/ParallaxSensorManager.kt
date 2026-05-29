package com.galaxywall.app.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.PI
import kotlin.math.abs

/**
 * Converts device tilt into a normalized 2D offset in the range [-1, 1] for both axes.
 *
 * Prefers the fused game rotation vector (gyroscope + accelerometer, drift-free and smooth),
 * falls back to the plain rotation vector, then to the raw accelerometer. The first reading
 * after [start] becomes the neutral baseline, so the offset is always 0 wherever the user is
 * currently holding the phone. A light low-pass filter removes jitter; per-frame easing is the
 * consumer's responsibility (see ParallaxImageView).
 */
class ParallaxSensorManager(context: Context) : SensorEventListener {

    fun interface OffsetListener {
        fun onOffset(x: Float, y: Float)
    }

    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    val isAvailable: Boolean get() = sensor != null

    /** 0f..1f. Mapped onto an effective gain so even 0 keeps a subtle motion. */
    var sensitivity: Float = 0.5f

    var enabled: Boolean = true

    private var listener: OffsetListener? = null

    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)
    private val gravity = FloatArray(3)

    private var baseA = 0f
    private var baseB = 0f
    private var hasBaseline = false
    private var registered = false

    fun setListener(l: OffsetListener?) { listener = l }

    fun start() {
        if (registered || !enabled) return
        val s = sensor ?: return
        hasBaseline = false
        registered = sensorManager.registerListener(this, s, SAMPLING_PERIOD_US)
    }

    fun stop() {
        if (!registered) return
        sensorManager.unregisterListener(this)
        registered = false
        hasBaseline = false
        listener?.onOffset(0f, 0f)
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_GAME_ROTATION_VECTOR, Sensor.TYPE_ROTATION_VECTOR ->
                handleRotation(event.values)
            Sensor.TYPE_ACCELEROMETER -> handleAccelerometer(event.values)
        }
    }

    private fun handleRotation(values: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        val pitch = orientation[1] // front/back tilt (radians)
        val roll = orientation[2]  // left/right tilt (radians)
        emit(roll, pitch, ROTATION_RANGE_RAD)
    }

    private fun handleAccelerometer(values: FloatArray) {
        gravity[0] = LOW_PASS * gravity[0] + (1 - LOW_PASS) * values[0]
        gravity[1] = LOW_PASS * gravity[1] + (1 - LOW_PASS) * values[1]
        emit(gravity[0], gravity[1], ACCEL_RANGE)
    }

    private fun emit(rawA: Float, rawB: Float, range: Float) {
        if (!hasBaseline) {
            baseA = rawA
            baseB = rawB
            hasBaseline = true
            listener?.onOffset(0f, 0f)
            return
        }
        val gain = MIN_GAIN + sensitivity.coerceIn(0f, 1f) * (MAX_GAIN - MIN_GAIN)
        val x = (wrap(rawA - baseA) / range).coerceIn(-1f, 1f) * gain
        val y = (wrap(rawB - baseB) / range).coerceIn(-1f, 1f) * gain
        // Negative so layers shift toward the lifted edge, like real depth.
        listener?.onOffset(-x.coerceIn(-1f, 1f), -y.coerceIn(-1f, 1f))
    }

    /** Keeps angular deltas within [-PI, PI] so wrap-around at the seam doesn't snap. */
    private fun wrap(v: Float): Float {
        var x = v
        while (x > PI) x -= (2 * PI).toFloat()
        while (x < -PI) x += (2 * PI).toFloat()
        return if (abs(x) < DEAD_ZONE) 0f else x
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val SAMPLING_PERIOD_US = 16_000 // ~60 Hz
        private const val LOW_PASS = 0.8f
        private const val ROTATION_RANGE_RAD = 0.5f // ~28° of tilt = full deflection
        private const val ACCEL_RANGE = 4.0f        // m/s^2 for full deflection
        private const val DEAD_ZONE = 0.01f
        private const val MIN_GAIN = 0.45f
        private const val MAX_GAIN = 1.0f
    }
}
