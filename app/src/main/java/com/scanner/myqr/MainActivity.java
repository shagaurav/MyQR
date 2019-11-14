package com.scanner.myqr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.scanner.myqr.Helper.GraphicOverlay;
import com.scanner.myqr.Helper.RectangleOverlay;
import com.wonderkiln.camerakit.CameraKitError;
import com.wonderkiln.camerakit.CameraKitEvent;
import com.wonderkiln.camerakit.CameraKitEventListener;
import com.wonderkiln.camerakit.CameraKitImage;
import com.wonderkiln.camerakit.CameraKitVideo;
import com.wonderkiln.camerakit.CameraView;

import java.util.List;

import dmax.dialog.SpotsDialog;

public class MainActivity extends AppCompatActivity {

    CameraView cameraView;
    Button button;
    AlertDialog waitingDialog;
    GraphicOverlay graphicOverlay;
    Button generateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        graphicOverlay = findViewById(R.id.graphic_overlay);
        cameraView = findViewById(R.id.camera);
        button = findViewById(R.id.scan_button);
        generateButton = findViewById(R.id.generate_button);

        generateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, GenerateActivity.class));
            }
        });


        //dialog box
        waitingDialog = new SpotsDialog.Builder()
                .setContext(this)
                .setMessage("Please Wait")
                .setCancelable(false)
                .build();


        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cameraView.start();
                cameraView.captureImage();
                graphicOverlay.clear();
            }
        });

        cameraView.addCameraKitListener(new CameraKitEventListener() {
            @Override
            public void onEvent(CameraKitEvent cameraKitEvent) {

            }

            @Override
            public void onError(CameraKitError cameraKitError) {

            }

            @Override
            public void onImage(CameraKitImage cameraKitImage) {
                //dialog box
                waitingDialog.show();
                Bitmap bitmap = cameraKitImage.getBitmap();
                bitmap = Bitmap.createScaledBitmap(bitmap, cameraView.getWidth(), cameraView.getHeight(), false);
                cameraView.stop();

                runDetector(bitmap);
            }

            private void runDetector(Bitmap bitmap) {
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder().setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_QR_CODE,
                        FirebaseVisionBarcode.FORMAT_PDF417 //or any type you want
                ).build();

                FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options);

                detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
                        processResult(firebaseVisionBarcodes);
                    }
                })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });

            }

            @Override
            public void onVideo(CameraKitVideo cameraKitVideo) {

            }
        });

    }

    private void processResult(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
        for (final FirebaseVisionBarcode item : firebaseVisionBarcodes) {

            // Draw  Rectangle
            Rect rectBounds = item.getBoundingBox();
            RectangleOverlay rectangleOverlay = new RectangleOverlay(graphicOverlay, rectBounds);
            graphicOverlay.add(rectangleOverlay);

            int value_type = item.getValueType();


            switch (value_type) {
                case FirebaseVisionBarcode.TYPE_TEXT: {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(item.getRawValue());
                    builder.setPositiveButton("Generate QR", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent sendIntent = new Intent(MainActivity.this, GenerateActivity.class);
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, item.getRawValue());
                            sendIntent.setType("text/plain");
                            getBaseContext().startActivity(sendIntent);
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;

                case FirebaseVisionBarcode.TYPE_URL: {
                    //Start browse url
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(item.getRawValue()));
                    startActivity(intent);
                }
                break;

                case FirebaseVisionBarcode.TYPE_CONTACT_INFO: {
                    final String info = new StringBuilder("Name: ").append(item.getContactInfo().getName().getFormattedName())
                            .append("\n").append("Address: ").append(item.getContactInfo().getAddresses().get(0).getAddressLines())
                            .append("\n").append("Email: ").append(item.getContactInfo().getEmails().get(0).getAddress()).toString();

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                    builder.setMessage(info);
                    builder.setPositiveButton("Generate QR", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent sendIntent = new Intent();
                            sendIntent.setAction(Intent.ACTION_SEND);
                            sendIntent.putExtra(Intent.EXTRA_TEXT, info);
                            sendIntent.setType("text/plain");
                            getBaseContext().startActivity(sendIntent);
                        }
                    });
                    android.app.AlertDialog dialog = builder.create();
                    dialog.show();
                }
                break;

            }
        }
        // dialog box
        waitingDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraView.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraView.stop();
    }

}
