package com.programminghut.realtime_object

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import org.opencv.android.*
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat

class CameraActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {
    private var mRgba: Mat? = null
    private var mGray: Mat? = null
    private var mOpenCvCameraView: CameraBridgeViewBase? = null

    private lateinit var takePictureButton: ImageView
    private lateinit var retryButton: ImageView
    private lateinit var textView: TextView

    private lateinit var textRecognizer: TextRecognizer
    private var bitmap: Bitmap? = null

    // Callback for OpenCV loader
    private val mLoaderCallback: BaseLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(TAG, "OpenCV is loaded")
                    mOpenCvCameraView?.enableView()
                }
                else -> super.onManagerConnected(status)
            }
        }
    }

    init {
        Log.i(TAG, "Instantiated new ${this.javaClass}")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request camera permission if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), MY_PERMISSIONS_REQUEST_CAMERA)
        }

        setContentView(R.layout.activity_camera)

        // Initialize camera and text recognizer
        openCamera()
        textRecognizer = TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())

        // Initialize views
        textView = findViewById(R.id.textview)
        textView.visibility = View.GONE

        takePictureButton = findViewById(R.id.take_picture_button)
        takePictureButton.setOnTouchListener(onTakePictureTouchListener())

        retryButton = findViewById(R.id.retry_button)
        retryButton.visibility = View.GONE
        retryButton.setOnTouchListener(onRetryTouchListener())
    }

    private fun openCamera() {
        mOpenCvCameraView = findViewById(R.id.frame_Surface) as CameraBridgeViewBase
        mOpenCvCameraView?.visibility = SurfaceView.VISIBLE
        mOpenCvCameraView?.setCvCameraViewListener(this)
        mOpenCvCameraView?.enableView()
    }

    // Touch listener for the take picture button
    private fun onTakePictureTouchListener() = View.OnTouchListener { _, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                takePicture()
                true
            }
            else -> false
        }
    }

    private fun takePicture() {
        retryButton.visibility = View.VISIBLE
        takePictureButton.visibility = View.GONE

        // Process frame
        val rotatedMat = mRgba!!.t()
        Core.flip(rotatedMat, mRgba, 1)
        rotatedMat.release()

        bitmap = Bitmap.createBitmap(mRgba!!.cols(), mRgba!!.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(mRgba, bitmap)
        mOpenCvCameraView?.disableView()

        textView.visibility = View.VISIBLE
        val image = InputImage.fromBitmap(bitmap!!, 0)

        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                textView.text = visionText.text
                Log.d(TAG, "Detected Text: ${textView.text}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Text recognition failed", e)
            }
    }

    // Touch listener for the retry button
    private fun onRetryTouchListener() = View.OnTouchListener { _, event ->
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> true
            MotionEvent.ACTION_UP -> {
                retry()
                true
            }
            else -> false
        }
    }

    private fun retry() {
        textView.visibility = View.GONE
        retryButton.visibility = View.GONE
        takePictureButton.visibility = View.VISIBLE
        openCamera()
    }

    override fun onResume() {
        super.onResume()
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "OpenCV initialization is done")
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        } else {
            Log.d(TAG, "OpenCV is not loaded. Trying again")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0, this, mLoaderCallback)
        }
    }

    override fun onPause() {
        super.onPause()
        mOpenCvCameraView?.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        mOpenCvCameraView?.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        mRgba = Mat(height, width, CvType.CV_8UC4)
        mGray = Mat(height, width, CvType.CV_8UC1)
    }

    override fun onCameraViewStopped() {
        mRgba?.release()
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        mRgba = inputFrame.rgba()
        mGray = inputFrame.gray()

        // Rotate the frame to correct orientation
        val rotatedMat = Mat()
        Core.transpose(mRgba, rotatedMat)
        Core.flip(rotatedMat, mRgba, 1)
        rotatedMat.release()

        return mRgba!!
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val MY_PERMISSIONS_REQUEST_CAMERA = 0
    }
}
