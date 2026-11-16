package com.guard.superisland.camera

import android.content.Context
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.Image
import android.media.ImageReader
import android.media.MediaActionSound
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import com.guard.superisland.config.GuardConfig
import com.guard.superisland.util.MediaStoreSaver
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 前置摄像头引擎（Camera2）：
 * - 连续抓拍：按 intervalSec 间隔抓 JPEG，直到 maxCount 或 maxDurationSec；
 * - 录像：MediaRecorder 无音频录像，直到 maxDurationSec。
 *
 * 所有操作都在后台 HandlerThread 上，结束后立刻释放摄像头与唤醒锁（省电）。
 */
class CaptureEngine(private val context: Context) {

    interface Callback {
        fun onPhoto(count: Int, seconds: Int)
        fun onFinished(totalCount: Int, totalSeconds: Int, ok: Boolean, err: String?)
    }

    private val cameraManager: CameraManager =
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var recorder: MediaRecorder? = null
    private var running = false

    @Volatile
    private var frontId: String? = null

    private val sound by lazy { MediaActionSound() }

    fun isRunning() = running

    fun start(context: Context, cb: Callback) {
        if (running) return
        running = true
        thread = HandlerThread("capture-engine").also { it.start() }
        handler = Handler(thread!!.looper)
        val cfg = loadConfig()
        handler?.post {
            try {
                frontId = findFrontCamera()
                if (frontId == null) {
                    finish(cb, 0, 0, false, "未找到前置摄像头")
                    return@post
                }
                if (cfg.mode == GuardConfig.MODE_VIDEO) {
                    runVideo(cfg, cb)
                } else {
                    runPhotoBurst(cfg, cb)
                }
            } catch (t: Throwable) {
                finish(cb, 0, 0, false, t.message)
            }
        }
    }

    fun stop() {
        running = false
        closeResources()
        thread?.quitSafely()
        thread = null
        handler = null
    }

    // ---------- photo burst ----------

    private fun runPhotoBurst(cfg: GuardConfig.Config, cb: Callback) {
        val id = frontId ?: run { finish(cb, 0, 0, false, "无前置摄像头"); return }
        imageReader = ImageReader.newInstance(1280, 720, android.graphics.ImageFormat.JPEG, 2)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val bytes = jpegBytes(image)
            image.close()
            if (bytes != null) {
                if (cfg.saveToGallery) {
                    MediaStoreSaver.saveJpeg(context, bytes, name("jpg"))
                }
            }
        }, handler)

        val surfaces = listOf(imageReader!!.surface)
        cameraManager.openCamera(id, object : CameraDevice.StateCallback() {
            override fun onOpened(c: CameraDevice) {
                device = c
                c.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(s: CameraCaptureSession) {
                        session = s
                        photoLoop(cfg, cb)
                    }
                    override fun onConfigureFailed(s: CameraCaptureSession) {
                        finish(cb, 0, 0, false, "会话配置失败")
                    }
                }, handler)
            }
            override fun onDisconnected(c: CameraDevice) { finish(cb, 0, 0, false, "摄像头断开") }
            override fun onError(c: CameraDevice, e: Int) { finish(cb, 0, 0, false, "摄像头错误 $e") }
        }, handler)
    }

    private fun photoLoop(cfg: GuardConfig.Config, cb: Callback) {
        val start = System.currentTimeMillis()
        var count = 0
        while (running && count < cfg.maxCount) {
            val elapsed = ((System.currentTimeMillis() - start) / 1000).toInt()
            if (elapsed >= cfg.maxDurationSec) break

            if (cfg.shutterSound) sound.play(MediaActionSound.SHUTTER_CLICK)
            captureOnce()
            count++
            cb.onPhoto(count, elapsed)

            if (count >= cfg.maxCount) break
            sleepSec(cfg.intervalSec)
        }
        val total = ((System.currentTimeMillis() - start) / 1000).toInt()
        finish(cb, count, total, true, null)
    }

    private fun captureOnce() {
        val dev = device ?: return
        val s = session ?: return
        try {
            val req = dev.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader!!.surface)
                set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
            }
            s.capture(req.build(), null, handler)
        } catch (_: Throwable) {
        }
    }

    private fun jpegBytes(image: Image): ByteArray? {
        return try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)
            bytes
        } catch (_: Throwable) {
            null
        }
    }

    // ---------- video ----------

    private fun runVideo(cfg: GuardConfig.Config, cb: Callback) {
        val id = frontId ?: run { finish(cb, 0, 0, false, "无前置摄像头"); return }
        val out = File(context.cacheDir, "guard_video_${System.currentTimeMillis()}.mp4")
        val rec = if (android.os.Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
        recorder = rec
        try {
            rec.setVideoSource(MediaRecorder.VideoSource.CAMERA)   // 无音频，明示且更省电
            rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            rec.setVideoSize(1280, 720)
            rec.setVideoFrameRate(30)
            rec.setVideoEncodingBitRate(4_000_000)
            rec.setOutputFile(out.absolutePath)
            rec.prepare()
            rec.start()
        } catch (t: Throwable) {
            finish(cb, 0, 0, false, "录像启动失败: ${t.message}")
            return
        }

        val start = System.currentTimeMillis()
        while (running && ((System.currentTimeMillis() - start) / 1000).toInt() < cfg.maxDurationSec) {
            sleepSec(1)
        }
        try { recorder?.stop() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        recorder = null

        val total = ((System.currentTimeMillis() - start) / 1000).toInt()
        if (cfg.saveToGallery) {
            MediaStoreSaver.saveVideo(context, out, name("mp4"))
        }
        out.delete()
        finish(cb, 1, total, true, null)
    }

    // ---------- helpers ----------

    private fun loadConfig(): GuardConfig.Config {
        return GuardConfig.Config(
            mode = GuardConfig.mode(context),
            intervalSec = GuardConfig.intervalSec(context),
            maxCount = GuardConfig.maxCount(context),
            maxDurationSec = GuardConfig.maxDurationSec(context),
            saveToGallery = GuardConfig.saveToGallery(context),
            shutterSound = GuardConfig.shutterSound(context),
        )
    }

    private fun findFrontCamera(): String? {
        return try {
            for (id in cameraManager.cameraIdList) {
                val ch = cameraManager.getCameraCharacteristics(id)
                val facing = ch.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) return id
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun sleepSec(sec: Int) {
        try { Thread.sleep((sec * 1000).toLong()) } catch (_: InterruptedException) {}
    }

    private fun name(ext: String): String {
        val fmt = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        return "Guard_${fmt.format(Date())}_${System.currentTimeMillis() % 1000}.$ext"
    }

    private fun closeResources() {
        try { session?.close() } catch (_: Throwable) {}
        try { imageReader?.close() } catch (_: Throwable) {}
        try { device?.close() } catch (_: Throwable) {}
        try { recorder?.release() } catch (_: Throwable) {}
        session = null
        imageReader = null
        device = null
        recorder = null
    }

    private fun finish(cb: Callback, count: Int, seconds: Int, ok: Boolean, err: String?) {
        val wasRunning = running
        running = false
        closeResources()
        if (wasRunning) {
            cb.onFinished(count, seconds, ok, err)
        }
    }
}
