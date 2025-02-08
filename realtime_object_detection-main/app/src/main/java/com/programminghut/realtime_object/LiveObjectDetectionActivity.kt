package com.programminghut.realtime_object

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class LiveObjectDetectionActivity : AppCompatActivity() {
    // Variables for labels, colors, paint, and TensorFlow Lite model
    private lateinit var labels: List<String>
    private val colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )
    private val paint = Paint()
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var bitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var model: SsdMobilenetV11Metadata1
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        get_permission()

        // Load labels and initialize the image processor and model
        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()
        model = SsdMobilenetV11Metadata1.newInstance(this)

        // Start a background handler thread for camera operations
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        // Initialize views
        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        textView = findViewById(R.id.textview)

        // Set up TextureView listener to handle camera operations
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                open_camera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                performObjectDetection()
            }
        }

        // Initialize CameraManager
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    // Function to open the camera and start the capture session
    @SuppressLint("MissingPermission")
    private fun open_camera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = textureView.surfaceTexture
                val surface = Surface(surfaceTexture)
                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    addTarget(surface)
                }

                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {}

            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    // Function to handle object detection using TensorFlow Lite model
    private fun performObjectDetection() {
        bitmap = textureView.bitmap!!
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)

        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val height = mutableBitmap.height
        val width = mutableBitmap.width
        paint.textSize = height / 15f
        paint.strokeWidth = height / 85f

        val detectedObjects = mutableListOf<String>()
        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val locationIndex = index * 4
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.STROKE
                canvas.drawRect(
                    RectF(
                        locations[locationIndex + 1] * width,
                        locations[locationIndex] * height,
                        locations[locationIndex + 3] * width,
                        locations[locationIndex + 2] * height
                    ), paint
                )
                paint.style = Paint.Style.FILL
                canvas.drawText(
                    "${labels[classes[index].toInt()]} $score",
                    locations[locationIndex + 1] * width,
                    locations[locationIndex] * height, paint
                )
                detectedObjects.add(labels[classes[index].toInt()])
            }
        }

        textView.text = detectedObjects.toString()
        imageView.setImageBitmap(mutableBitmap)
    }

    // Function to request camera permission if not granted
    private fun get_permission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            get_permission()
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}
