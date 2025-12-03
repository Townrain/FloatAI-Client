package com.floatai

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.floatai.network.ApiService

/**
 * 应用入口类，初始化全局组件
 */
class FloatAIApplication : Application() {
    
    companion object {
        lateinit var instance: FloatAIApplication
            private set
        
        lateinit var sharedPreferences: SharedPreferences
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        sharedPreferences = getSharedPreferences("float_ai_prefs", Context.MODE_PRIVATE)
        
        // 初始化API服务，从配置中读取服务器地址
        val serverUrl = sharedPreferences.getString("server_url", "http://192.168.1.100:8000") ?: "http://192.168.1.100:8000"
        ApiService.init(serverUrl)
        
        // 初始化悬浮窗管理器（延迟初始化）
        initFloatWindowManager()
    }
    
    /**
     * 初始化悬浮窗管理器
     */
    private fun initFloatWindowManager() {
        // 这里只是确保类被加载，实际初始化在需要时进行
        FloatWindowManager
    }
    
    /**
     * 更新服务器地址配置
     */
    fun updateServerUrl(url: String) {
        sharedPreferences.edit().putString("server_url", url).apply()
        ApiService.init(url)
    }
    
    /**
     * 获取当前服务器地址
     */
    fun getServerUrl(): String {
        return sharedPreferences.getString("server_url", "http://192.168.1.100:8000") ?: "http://192.168.1.100:8000"
    }
}