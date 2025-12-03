package com.floatai

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.floatai.databinding.ActivityMainBinding
import com.guolindev.permissionx.PermissionX

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // 检查并申请必要权限
        checkAndRequestPermissions()
        
        // 设置按钮点击事件
        setupClickListeners()
    }
    
    private fun setupClickListeners() {
        // 启动悬浮窗按钮
        binding.btnStartFloatWindow.setOnClickListener {
            if (checkFloatPermission()) {
                startFloatWindow()
            } else {
                requestFloatPermission()
            }
        }
        
        // 打开设置按钮
        binding.btnOpenSettings.setOnClickListener {
            openSettings()
        }
        
        // 测试连接按钮
        binding.btnTestConnection.setOnClickListener {
            testServerConnection()
        }
    }
    
    /**
     * 检查并申请权限
     */
    private fun checkAndRequestPermissions() {
        PermissionX.init(this)
            .permissions(
                android.Manifest.permission.INTERNET,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    Toast.makeText(this, "All permissions granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Some permissions denied: $deniedList", Toast.LENGTH_SHORT).show()
                }
            }
    }
    
    /**
     * 检查悬浮窗权限
     */
    private fun checkFloatPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else {
            true
        }
    }
    
    /**
     * 请求悬浮窗权限
     */
    private fun requestFloatPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_CODE_FLOAT_PERMISSION)
        }
    }
    
    /**
     * 启动悬浮窗
     */
    private fun startFloatWindow() {
        FloatWindowManager.showFloatWindow(this)
        Toast.makeText(this, "Floating window started", Toast.LENGTH_SHORT).show()
        // 可以最小化或关闭当前Activity
        finish()
    }
    
    /**
     * 打开应用设置
     */
    private fun openSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
    
    /**
     * 测试服务器连接
     */
    private fun testServerConnection() {
        // 这里可以添加测试服务器连接的逻辑
        Toast.makeText(this, "Testing server connection...", Toast.LENGTH_SHORT).show()
        // 实际测试代码需要实现网络请求
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FLOAT_PERMISSION) {
            if (checkFloatPermission()) {
                startFloatWindow()
            } else {
                Toast.makeText(this, "Float permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    companion object {
        private const val REQUEST_CODE_FLOAT_PERMISSION = 1001
    }
}