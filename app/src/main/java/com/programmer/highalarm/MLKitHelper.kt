package com.programmer.highalarm

import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

object MLKitHelper {

    fun scanBarcode(
        inputImage: InputImage,
        onSuccess: (List<Barcode>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        val scanner = BarcodeScanning.getClient(options)
        scanner.process(inputImage)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    fun detectObjects(
        inputImage: InputImage,
        onSuccess: (List<DetectedObject>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
        val detector = ObjectDetection.getClient(options)
        detector.process(inputImage)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    fun labelImage(
        inputImage: InputImage,
        onSuccess: (List<ImageLabel>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        labeler.process(inputImage)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }

    fun detectPose(
        inputImage: InputImage,
        onSuccess: (Pose) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        val detector = PoseDetection.getClient(options)
        detector.process(inputImage)
            .addOnSuccessListener(onSuccess)
            .addOnFailureListener(onFailure)
    }
}
