package com.example.camera_native_android

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.content.Context
import android.graphics.Camera
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture


class CameraViewController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    camera: PreviewView,
) {

    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val cameraPreview: PreviewView = camera
    private var cameraDevice: Camera? = null
    private var isInitialized = false
    private  var videoFilePath:String=""
    private var currentLensFacing: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var videoQuality : Quality= Quality.HIGHEST



    /**
     * To start camera preview and capture
     */
    lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var preview: Preview
    @SuppressLint("RestrictedApi")
    fun startCamera(quality:Int?) {
        if(quality !=null){
        videoQuality = when (quality) {
            1 -> Quality.LOWEST
            2 -> Quality.SD
            3 -> Quality.HD
            4 -> Quality.FHD
            5 -> Quality.UHD
            6 -> Quality.HIGHEST
            else -> Quality.HIGHEST
        } }
        cameraProviderFuture = ProcessCameraProvider.getInstance(this.context)

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            cameraProvider = cameraProviderFuture.get()

            // Preview
            preview = Preview.Builder()
                .build( )
                .also {
                    it.setSurfaceProvider(cameraPreview.surfaceProvider)
                }
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(videoQuality))
                .build()
            videoCapture =  VideoCapture.Builder(recorder).setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY).build()
//            videoCapture = VideoCapture.Builder(recorder).setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY).build()

            // Select back camera as a default
            val cameraSelector = currentLensFacing

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, videoCapture)

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this.context))
    }

    /**
     * To stop camera preview and capture
     */
    fun stopCamera(onStop: (result: Boolean?) -> Unit) {
        // if (isInitialized) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context.applicationContext)
        try {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            cameraDevice = null
            isInitialized = false
            onStop.invoke(true)
        } catch (e: Exception) {
            e.printStackTrace()
            onStop.invoke(false)
        }
        //   }
    }
    fun changeCamera(onCameraChange: (result: Boolean?) -> Unit) {
        // if (isInitialized) {

        try {
            currentLensFacing = if(currentLensFacing == CameraSelector.DEFAULT_BACK_CAMERA){
                CameraSelector.DEFAULT_FRONT_CAMERA
            }else{
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            onCameraChange.invoke(true)
        } catch (e: Exception) {
            throw(Exception("Camera can't change. Exception is: $e"))

        }
        //   }
    }
    fun captureVideo(fileName: String, filePath: String) {
        val videoCapture = this.videoCapture ?: return
        val curRecording = recording
        if (curRecording != null) {
            // A recording is already in progress
            return
        }

        // create and start a new recording session
        videoFilePath = filePath

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, videoFilePath)
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
                .Builder(this.context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                .setContentValues(contentValues)
                .build()

        recording = videoCapture.output
                .prepareRecording(this.context, mediaStoreOutputOptions)
                .apply {
                    if (PermissionChecker.checkSelfPermission(
                                    context,
                                    android.Manifest.permission.RECORD_AUDIO
                            ) == PermissionChecker.PERMISSION_GRANTED
                    ) {
                        withAudioEnabled()
                    }
                }
                .start(ContextCompat.getMainExecutor(this.context)) { recordEvent ->
                    // lambda event listener
                    when (recordEvent) {
                        is VideoRecordEvent.Start -> {
                            Log.d(TAG, "Video recording started")
                        }
                        is VideoRecordEvent.Finalize -> {
                            Log.d(TAG, "Video recording finalize event received")
                            if (!recordEvent.hasError()) {
                                Log.d(TAG, "Video capture succeeded: ${recordEvent.outputResults.outputUri}")
                            } else {
                                recording?.close()
                                recording = null
                                Log.e(TAG, "Video capture ends with error: ${recordEvent.error}")
                            }
                        }
                    }
                }
    }

     fun stopVideoRecording():String{
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
            return videoFilePath
        }
         else{
             throw Exception("Currently video recording is not active")
        }
    }
    fun changeCamera(){
        currentLensFacing = if(currentLensFacing == CameraSelector.DEFAULT_BACK_CAMERA){
            CameraSelector.DEFAULT_FRONT_CAMERA
        }else{
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        val curRecording = recording
        if (curRecording != null) {
            curRecording.stop()
            recording = null
        }
        startCamera(null)
    }


}