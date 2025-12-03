package com.floatai

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

/**
 * 通知帮助类
 * 用于显示AI处理结果的通知
 */
object NotificationHelper {
    
    private const val CHANNEL_ID = "float_ai_channel"
    private const val CHANNEL_NAME = "Float AI Notifications"
    private const val NOTIFICATION_ID = 1001
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Float AI results"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 100, 200, 300)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 显示通知
     */
    fun showNotification(context: Context, title: String, content: String) {
        // 创建通知渠道（如果需要）
        createNotificationChannel(context)
        
        // 创建点击通知后的意图（打开主Activity）
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // 构建通知
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setDefaults(Notification.DEFAULT_SOUND or Notification.DEFAULT_VIBRATE)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .build()
        
        // 显示通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    /**
     * 清除所有通知
     */
    fun clearAllNotifications(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }
    
    /**
     * 显示进度通知（用于长时间操作）
     */
    fun showProgressNotification(context: Context, title: String, progress: Int, maxProgress: Int = 100) {
        createNotificationChannel(context)
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Processing...")
            .setProgress(maxProgress, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    /**
     * 更新进度通知
     */
    fun updateProgressNotification(context: Context, progress: Int, maxProgress: Int = 100) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Float AI Processing")
            .setContentText("Processing screenshot...")
            .setProgress(maxProgress, progress, false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }
    
    /**
     * 完成进度通知并显示结果
     */
    fun completeProgressNotification(context: Context, result: String) {
        // 先取消进度通知
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID + 1)
        
        // 显示结果通知
        showNotification(context, "Float AI Result", result)
    }
}