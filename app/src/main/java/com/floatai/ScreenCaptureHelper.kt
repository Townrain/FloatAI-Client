package com.floatai

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * 屏幕截图辅助类
 * 使用MediaProjection API捕获屏幕内容
 */
object ScreenCaptureHelper {
    
    private const val TAG = "ScreenCaptureHelper"
    private const val REQUEST_CODE_SCREEN_CAPTURE = 1002
    
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    // 截图回调
    private var captureCallback: ((Bitmap?) -> Unit)? = null
    
    /**
     * 初始化截图服务
     */
    fun init(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mediaProjectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        }
    }
    
    /**
     * 请求屏幕截图权限
     */
    fun requestScreenCapturePermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && mediaProjectionManager != null) {
            val captureIntent = mediaProjectionManager!!.createScreenCaptureIntent()
            activity.startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE)
        } else {
            Log.e(TAG, "MediaProjection not supported on this device")
        }
    }
    
    /**
     * 处理权限请求结果
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun handlePermissionResult(resultCode: Int, data: Intent?, callback: (Bitmap?) -> Unit) {
        captureCallback = callback
        
        if (resultCode == Activity.RESULT_OK && data != null && mediaProjectionManager != null) {
            mediaProjection = mediaProjectionManager!!.getMediaProjection(resultCode, data)
            captureScreen()
        } else {
            Log.e(TAG, "Screen capture permission denied")
            captureCallback?.invoke(null)
            captureCallback = null
        }
    }
    
    /**
     * 捕获屏幕
     */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun captureScreen() {
        try {
            val windowManager = FloatAIApplication.instance.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi
            
            // 创建ImageReader
            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            
            // 创建虚拟显示
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
            
            // 等待一帧然后读取图像
            imageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage()
                if (image != null) {
                    val bitmap = imageToBitmap(image)
                    image.close()
                    
                    // 清理资源
                    cleanup()
                    
                    // 返回截图
                    captureCallback?.invoke(bitmap)
                    captureCallback = null
                }
            }, null)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error capturing screen: ${e.message}", e)
            cleanup()
            captureCallback?.invoke(null)
            captureCallback = null
        }
    }
    
    /**
     * 将Image转换为Bitmap
     */
    private fun imageToBitmap(image: Image): Bitmap? {
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            
            // 创建Bitmap
            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            // 裁剪掉多余的部分
            return Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting image to bitmap: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 清理资源
     */
    private fun cleanup() {
        virtualDisplay?.release()
        virtualDisplay = null
        
        imageReader?.close()
        imageReader = null
        
        mediaProjection?.stop()
        mediaProjection = null
    }
    
    /**
     * 简单的屏幕截图方法（需要悬浮窗权限）
     * 注意：这个方法只能在有悬浮窗权限的情况下使用
     */
    fun captureScreen(context: Context): Bitmap? {
        return try {
            // 这里使用最简化的方式，实际需要根据具体情况调整
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val display = windowManager.defaultDisplay
            val metrics = DisplayMetrics()
            display.getMetrics(metrics)
            
            // 创建一个空白Bitmap并返回（这里只是示例，实际需要实现真正的截图）
            Bitmap.createBitmap(metrics.widthPixels, metrics.heightPixels, Bitmap.Config.ARGB_8888)
        } catch (e: Exception) {
            Log.e(TAG, "Error in simple capture: ${e.message}", e)
            null
        }
    }
    
    /**
     * 将Bitmap转换为Base64字符串
     */
    fun bitmapToBase64(bitmap: Bitmap, quality: Int = 80): String {
        return try {
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            val byteArray = outputStream.toByteArray()
            android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting bitmap to base64: ${e.message}", e)
            ""
        }
    }
    
    /**
     * 压缩Bitmap
     */
    fun compressBitmap(bitmap: Bitmap, maxSizeKB: Int = 500): Bitmap {
        var quality = 100
        var outputStream: ByteArrayOutputStream
        
        do {
            outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.toByteArray().size / 1024 > maxSizeKB && quality > 10)
        
        // 重新解码为Bitmap（注意：这可能会降低质量）
        val byteArray = outputStream.toByteArray()
        return android.graphics.BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size) ?: bitmap
    }
    
    /**
     * 获取请求码
     */
    fun getRequestCode(): Int = REQUEST_CODE_SCREEN_CAPTURE
}