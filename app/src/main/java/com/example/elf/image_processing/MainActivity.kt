package com.example.elf.image_processing

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer

import android.graphics.*

import android.graphics.Bitmap
import android.net.Uri
import android.util.DisplayMetrics
import android.util.Log
import com.google.android.gms.vision.text.TextBlock

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var imageUri: Uri? = null

    private val CAMERA_REQUEST_CODE = 12345
    private val REQUEST_GALLERY_CAMERA = 54654

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener(this)


    }

    override fun onClick(v: View?) {
            when(v){
                button->{
                    if(Build.VERSION.SDK_INT >=23){
                        if(ContextCompat.checkSelfPermission
                                (this,android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED
                            || ContextCompat.checkSelfPermission
                                (this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(
                                this,
                                arrayOf(
                                    android.Manifest.permission.CAMERA,
                                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ), REQUEST_GALLERY_CAMERA
                            )
                        }else{
                            openCamera()
                        }
                    }else{
                        openCamera()
                    }
                }

            }
    }

    private fun openCamera() {
        val filename = System.currentTimeMillis().toString() + ".jpg"

        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        imageUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

        val intent = Intent()
        intent.action = MediaStore.ACTION_IMAGE_CAPTURE
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_GALLERY_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this@MainActivity, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode== Activity.RESULT_OK){
            when(requestCode){
                CAMERA_REQUEST_CODE->{
                 inspect(imageUri)
                                    }

              /*  CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE->
                {
                    var result = CropImage.getActivityResult(data)
                    val resultUri = result.uri
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, resultUri)
                    inspectFromBitmap(bitmap)

                }*/

            }

        }
    }



    private fun inspect(uri: Uri?) {
        var `is`: InputStream? = null
        var bitmap: Bitmap? = null
        try {
            `is` = contentResolver.openInputStream(uri!!)
            val options = BitmapFactory.Options()
            options.inPreferredConfig = Bitmap.Config.ARGB_8888
            options.inSampleSize = 2
            options.inScreenDensity = DisplayMetrics.DENSITY_LOW
            bitmap = BitmapFactory.decodeStream(`is`, null, options)
            inspectFromBitmap(bitmap)
            barcode(bitmap)
            imageView.setImageBitmap(null)
        } catch (e: FileNotFoundException) {
            Log.w("tag", "Failed to find the file: " + uri!!, e)
        } finally {
            bitmap?.recycle()
            if (`is` != null) {
                try {
                    `is`.close()
                } catch (e: IOException) {
                    Log.w("tag", "Failed to close InputStream", e)
                }

            }
        }
    }


    private fun inspectFromBitmap(bitmap: Bitmap?) {
        val textRecognizer = TextRecognizer.Builder(this).build()
        try {
            if (!textRecognizer.isOperational) {
                AlertDialog.Builder(this).setMessage("Text recognizer could not be set up on your device").show()
                return
            }

            val frame = Frame.Builder().setBitmap(bitmap!!).build()
            val origTextBlocks = textRecognizer.detect(frame)
            val textBlocks = ArrayList<TextBlock>()
            for (i in 0 until origTextBlocks.size()) {
                val textBlock = origTextBlocks.valueAt(i)
                textBlocks.add(textBlock)
            }
            textBlocks.sortWith(Comparator { o1, o2 ->
                val diffOfTops = o1.boundingBox.top - o2.boundingBox.top
                val diffOfLefts = o1.boundingBox.left - o2.boundingBox.left
                if (diffOfTops != 0) {
                    diffOfTops
                } else diffOfLefts
            })

            val detectedText = StringBuilder()
            for (textBlock in textBlocks) {
                if (textBlock.value != null) {
                    detectedText.append(textBlock.value)
                    detectedText.append("\n")
                }
            }

            textView3.text = detectedText
        } finally {
            textRecognizer.release()
        }
    }

    private fun barcode(imageBitmap: Bitmap) {

        val detector = BarcodeDetector.Builder(applicationContext)
            .setBarcodeFormats(Barcode.ALL_FORMATS)
            .build()

        if (!detector.isOperational) {
            textView.setText("Could not set up the detector!")
            return
        }

        if (detector!!.isOperational && imageBitmap != null) {
            val frame = Frame.Builder().setBitmap(imageBitmap).build()
            val barcodes = detector.detect(frame)

            textView.text=""

            for (index in 0 until barcodes.size()) {
                val code = barcodes.valueAt(index)
                textView.text = code.displayValue



            }


        }
    }


/*
    private fun beginCrop(source: Uri) {
        val destination = Uri.fromFile(File(cacheDir, "cropped"))
        Crop.of(source, destination).asSquare().start(this)
    }

    private fun handleCrop(resultCode: Int, result: Intent) {
        if (resultCode == Activity.RESULT_OK) {
            imageView.setImageURI(Crop.getOutput(result))
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).message, Toast.LENGTH_SHORT).show()
        }
    }
*/




}
