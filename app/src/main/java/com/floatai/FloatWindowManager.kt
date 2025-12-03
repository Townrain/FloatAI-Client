package com.floatai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.getactivity.easywindow.EasyWindow
import com.floatai.databinding.FloatWindowBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 悬浮窗管理器
 * 负责创建、显示和管理悬浮窗
 */
object FloatWindowManager {
    
    private const val TAG = "FloatWindowManager"
    
    private var floatWindow: EasyWindow? = null
    private var isShowing = false
    private lateinit var binding: FloatWindowBinding
    
    // UI组件
    private lateinit var tvStatus: TextView
    private lateinit var tvResult: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnCapture: Button
    
    /**
     * 显示悬浮窗
     */
    fun showFloatWindow(context: Context) {
        if (isShowing) {
            Log.d(TAG, "Float window is already showing")
            return
        }
        
        try {
            floatWindow = EasyWindow.with(context)
                .setContentView(R.layout.float_window)
                .setWidth(350)  // 宽度350dp
                .setHeight(450) // 高度450dp
                .setGravity(Gravity.START or Gravity.TOP) // 左上角开始
                .setOffsetX(50) // X轴偏移
                .setOffsetY(50) // Y轴偏移
                .setDraggable(true) // 可拖拽
                .setCancelable(false) // 不可通过返回键关闭
                .setTouchable(true) // 可触摸
                .setWindowType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY) // 悬浮窗类型
                .setWindowFormat(PixelFormat.TRANSLUCENT) // 透明背景
                .show()
            
            isShowing = true
            Log.d(TAG, "Float window shown successfully")
            
            // 初始化UI组件和事件
            initFloatWindowComponents(context)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show float window: ${e.message}", e)
            Toast.makeText(context, "Failed to show float window", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 初始化悬浮窗组件和事件
     */
    private fun initFloatWindowComponents(context: Context) {
        floatWindow?.let { window ->
            val rootView = window.rootView
            binding = FloatWindowBinding.bind(rootView)
            
            // 获取UI组件引用
            tvStatus = binding.tvStatus
            tvResult = binding.tvResult
            progressBar = binding.progressBar
            btnCapture = binding.btnCapture
            
            // 设置关闭按钮点击事件
            binding.ivClose.setOnClickListener {
                hideFloatWindow(context)
            }
            
            // 设置拖拽区域
            binding.ivDrag.setOnTouchListener { view, event ->
                window.updateTouch(event)
                true
            }
            
            // 设置截图按钮点击事件
            btnCapture.setOnClickListener {
                onCaptureButtonClicked(context)
            }
            
            // 初始状态
            updateStatus(context.getString(R.string.status_ready))
            tvResult.text = context.getString(R.string.ai_result_placeholder)
        }
    }
    
    /**
     * 截图按钮点击事件处理
     */
    private fun onCaptureButtonClicked(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. 更新状态为截图
                updateStatus(context.getString(R.string.status_capturing))
                progressBar.visibility = View.VISIBLE
                btnCapture.isEnabled = false
                
                // 2. 执行截图（在IO线程）
                val screenshot = withContext(Dispatchers.IO) {
                    ScreenCaptureHelper.captureScreen(context)
                }
                
                if (screenshot == null) {
                    updateStatus(context.getString(R.string.status_error))
                    tvResult.text = "Failed to capture screen"
                    return@launch
                }
                
                // 3. 更新状态为上传
                updateStatus(context.getString(R.string.status_uploading))
                
                // 4. 上传到服务器
                val result = withContext(Dispatchers.IO) {
                    ApiService.uploadImage(screenshot)
                }
                
                // 5. 处理结果
                if (result.success) {
                    updateStatus("AI Response Received")
                    tvResult.text = result.data ?: "No response content"
                    
                    // 显示通知
                    NotificationHelper.showNotification(
                        context,
                        "Float AI Result",
                        result.data ?: "AI has processed your screenshot"
                    )
                } else {
                    updateStatus(context.getString(R.string.status_error))
                    tvResult.text = "Error: ${result.error ?: "Unknown error"}"
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in capture process: ${e.message}", e)
                updateStatus(context.getString(R.string.status_error))
                tvResult.text = "Exception: ${e.message}"
            } finally {
                // 恢复UI状态
                progressBar.visibility = View.GONE
                btnCapture.isEnabled = true
            }
        }
    }
    
    /**
     * 更新状态文本
     */
    private fun updateStatus(status: String) {
        tvStatus.text = status
        Log.d(TAG, "Status updated: $status")
    }
    
    /**
     * 隐藏悬浮窗
     */
    fun hideFloatWindow(context: Context) {
        try {
            floatWindow?.dismiss()
            floatWindow = null
            isShowing = false
            Log.d(TAG, "Float window hidden")
            Toast.makeText(context, "Floating window closed", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hide float window: ${e.message}", e)
        }
    }
    
    /**
     * 检查悬浮窗是否正在显示
     */
    fun isFloatWindowShowing(): Boolean {
        return isShowing
    }
    
    /**
     * 更新AI结果到悬浮窗
     */
    fun updateAIResult(result: String) {
        if (isShowing) {
            tvResult.text = result
            tvStatus.text = "Result Updated"
        }
    }
    
    /**
     * 显示错误信息
     */
    fun showError(error: String) {
        if (isShowing) {
            tvResult.text = "Error: $error"
            tvStatus.text = "Error"
            progressBar.visibility = View.GONE
            btnCapture.isEnabled = true
        }
    }
}