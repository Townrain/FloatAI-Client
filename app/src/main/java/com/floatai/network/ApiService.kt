package com.floatai.network

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.floatai.ScreenCaptureHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 网络API服务
 * 负责与服务器通信，上传图片并获取AI结果
 */
object ApiService {
    
    private const val TAG = "ApiService"
    private const val DEFAULT_TIMEOUT = 60L // 秒
    
    private lateinit var retrofit: Retrofit
    private lateinit var api: FloatAIApi
    private var baseUrl = "http://192.168.1.100:8000" // 默认服务器地址
    
    /**
     * 初始化API服务
     */
    fun init(serverUrl: String = baseUrl) {
        baseUrl = serverUrl
        
        // 创建OkHttpClient，设置超时时间
        val client = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("User-Agent", "FloatAI-Android-Client/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
        
        // 创建Retrofit实例
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        api = retrofit.create(FloatAIApi::class.java)
        Log.d(TAG, "API Service initialized with base URL: $baseUrl")
    }
    
    /**
     * 上传图片并获取AI结果
     */
    suspend fun uploadImage(bitmap: Bitmap): ApiResponse {
        return try {
            // 方法1：使用Base64编码上传
            uploadImageBase64(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Base64 upload failed, trying multipart: ${e.message}")
            try {
                // 方法2：使用Multipart表单上传
                uploadImageMultipart(bitmap)
            } catch (e2: Exception) {
                Log.e(TAG, "Multipart upload also failed: ${e2.message}")
                ApiResponse(
                    success = false,
                    error = "Upload failed: ${e2.message ?: "Unknown error"}"
                )
            }
        }
    }
    
    /**
     * 使用Base64编码上传图片
     */
    private suspend fun uploadImageBase64(bitmap: Bitmap): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                // 压缩图片
                val compressedBitmap = ScreenCaptureHelper.compressBitmap(bitmap, 500)
                
                // 转换为Base64
                val base64Image = ScreenCaptureHelper.bitmapToBase64(compressedBitmap, 80)
                
                if (base64Image.isEmpty()) {
                    return@withContext ApiResponse(success = false, error = "Failed to encode image")
                }
                
                // 创建请求
                val request = ImageRequest(
                    image = base64Image,
                    format = "base64",
                    timestamp = System.currentTimeMillis()
                )
                
                // 发送请求
                val response = api.uploadImageBase64(request)
                
                if (response.isSuccessful) {
                    response.body() ?: ApiResponse(success = false, error = "Empty response body")
                } else {
                    ApiResponse(
                        success = false,
                        error = "Server error: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Base64 upload error: ${e.message}", e)
                ApiResponse(success = false, error = "Network error: ${e.message}")
            }
        }
    }
    
    /**
     * 使用Multipart表单上传图片
     */
    private suspend fun uploadImageMultipart(bitmap: Bitmap): ApiResponse {
        return withContext(Dispatchers.IO) {
            try {
                // 将Bitmap保存为临时文件
                val tempFile = createTempFile(bitmap)
                
                // 创建Multipart请求
                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaType())
                val body = MultipartBody.Part.createFormData("image", "screenshot.jpg", requestFile)
                
                // 发送请求
                val response = api.uploadImageMultipart(body)
                
                // 清理临时文件
                tempFile.delete()
                
                if (response.isSuccessful) {
                    response.body() ?: ApiResponse(success = false, error = "Empty response body")
                } else {
                    ApiResponse(
                        success = false,
                        error = "Server error: ${response.code()} - ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Multipart upload error: ${e.message}", e)
                ApiResponse(success = false, error = "Network error: ${e.message}")
            }
        }
    }
    
    /**
     * 创建临时文件
     */
    private fun createTempFile(bitmap: Bitmap): File {
        val tempFile = File.createTempFile("float_ai_", ".jpg")
        val outputStream = FileOutputStream(tempFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.flush()
        outputStream.close()
        return tempFile
    }
    
    /**
     * 测试服务器连接
     */
    suspend fun testConnection(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.healthCheck()
                response.isSuccessful && response.body()?.status == "healthy"
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * 获取当前服务器地址
     */
    fun getBaseUrl(): String = baseUrl
}

/**
 * API接口定义
 */
interface FloatAIApi {
    
    /**
     * 健康检查
     */
    @GET("/health")
    suspend fun healthCheck(): retrofit2.Response<HealthResponse>
    
    /**
     * Base64方式上传图片
     */
    @POST("/api/process-image")
    @Headers("Content-Type: application/json")
    suspend fun uploadImageBase64(
        @Body request: ImageRequest
    ): retrofit2.Response<ApiResponse>
    
    /**
     * Multipart方式上传图片
     */
    @Multipart
    @POST("/api/process-image")
    suspend fun uploadImageMultipart(
        @Part image: MultipartBody.Part
    ): retrofit2.Response<ApiResponse>
}

/**
 * 图片上传请求
 */
data class ImageRequest(
    val image: String, // Base64编码的图片或图片URL
    val format: String = "base64",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * API响应
 */
data class ApiResponse(
    val success: Boolean = true,
    val data: String? = null,
    val error: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 健康检查响应
 */
data class HealthResponse(
    val status: String
)