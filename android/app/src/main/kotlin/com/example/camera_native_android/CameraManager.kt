
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.camera_native_android.CameraViewController
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

class CameraManager {
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun handle(call: MethodCall, result: MethodChannel.Result, controller: CameraViewController) {
        when {
            call.method.equals("startCamera") -> {
                controller.startCamera()
                result.success(true)

            }
            call.method.equals("startVideoRecording") -> {
                controller.captureVideo(call.argument<String>("fileName")!!,call.argument<String>("filePath")!!)
            } call.method.equals("stopVideoRecording") -> {
                try {
                    val filePath =  controller.stopVideoRecording()
                    result.success(filePath)
                }catch (exception:Exception){
                    result.error("Not Found",exception.message,exception.stackTraceToString())
                }

            }
            call.method.equals("changeCamera") -> {
                try {
                       controller.changeCamera()
                    result.success(true)
                }catch (exception:Exception){
                    result.error("Not Found",exception.message,exception.stackTraceToString())
                }

            }
        }
    }
}