package com.geniuslead.attendance.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.geniuslead.attendance.R;
import com.geniuslead.attendance.events.CamCaptureEvent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class ReadCardActivity extends AppCompatActivity implements Camera.PictureCallback, Camera.ShutterCallback, SurfaceHolder.Callback {

    @Bind(R.id.textViewLastResult)
    TextView textViewLastResult;
    Camera mCamera;
    SurfaceView mPreview;
    @Bind(R.id.buttonCaptureImage)
    Button buttonCaptureImage;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private AlertDialog mDialog;
    private NdefMessage mNdefPushMessage;
    private int cameraId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_card);
        ButterKnife.bind(this);

        mPreview = (SurfaceView) findViewById(R.id.preview);
        mPreview.getHolder().addCallback(this);
        mPreview.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();

        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
            finish();
            return;
        }

        // do we have a camera?
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Toast.makeText(this, "No camera on this device", Toast.LENGTH_LONG).show();
            finish();
        } else {
            cameraId = findFrontFacingCamera();
            //cameraId = findBackFacingCamera();
            if (cameraId < 0) {
                Toast.makeText(this, "Sorry you don't have secondary camera", Toast.LENGTH_LONG).show();
                finish();
            } else {
                releaseCameraAndPreview();
                mCamera = Camera.open(cameraId);
            }
        }

        mPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mAdapter = NfcAdapter.getDefaultAdapter(this);

        mNdefPushMessage = new NdefMessage(new NdefRecord[]{newTextRecord(
                "Message from NFC Reader :-)", Locale.ENGLISH, true)});
    }


    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mCamera.release();
        mAdapter.disableForegroundDispatch(this);
        Log.d("CAMERA", "Destroy");
    }

    @OnClick(R.id.buttonCaptureImage)
    public void capturingImage() {
        mCamera.takePicture(null, null, ReadCardActivity.this);
    }

    private void showMessage(int title, int message) {
        mDialog.setTitle(title);
        mDialog.setMessage(getText(message));
        mDialog.show();
    }

    private NdefRecord newTextRecord(String text, Locale locale, boolean encodeInUtf8) {
        byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));

        Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
        byte[] textBytes = text.getBytes(utfEncoding);

        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        char status = (char) (utfBit + langBytes.length);

        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);

        return new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Toast.makeText(ReadCardActivity.this, "OnResume", Toast.LENGTH_LONG).show();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
        mCamera.stopPreview();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String type = intent.getType();
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            if ("text/plain".equals(type)) {
                Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (rawMsgs != null) {
                    NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                    for (int i = 0; i < rawMsgs.length; i++) {
                        msgs[i] = (NdefMessage) rawMsgs[i];
                    }
                    NdefMessage msg = msgs[0];
                    try {
                        byte[] payload = msg.getRecords()[0].getPayload();
                        String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
                        int languageCodeLength = payload[0] & 0077;

                        Log.d("TAG", "textEncoding: " + textEncoding);
                        String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
                        //Get the Text
                        String text = new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
                        if (text.substring(text.length() - 2).equalsIgnoreCase("-Y")
                                || text.substring(text.length() - 2).equalsIgnoreCase("-N")) {
                            EventBus.getDefault().post(new CamCaptureEvent.Success(text));

                        } else
                            Log.i("TAG", "Some other type card");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                //textViewLastResult.setText("Wrong mime type: " + type);
            }
        }
    }

    private void showWirelessSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.nfc_disabled);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.create().show();
        return;
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                Log.d("TAG", "Front Facing Camera found");
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    public void onEventMainThread(final CamCaptureEvent.Success event) {
        releaseCameraAndPreview();
        textViewLastResult.setText(event.getNfcValue());
        capturingImage();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onPictureTaken(byte[] imgData, Camera camera) {
        //Here, we chose internal storage
        try {
            //Compressing the image 640*480
            //-----------------------------------------
            Bitmap bmp = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
            Bitmap resizedBmp = Bitmap.createScaledBitmap(bmp, 640, 480, false);

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            resizedBmp.compress(Bitmap.CompressFormat.PNG, 0, bos);
            byte[] data = bos.toByteArray();
            //-----------------------------------------

            File pictureFileDir = new File(Environment.getExternalStorageDirectory(), "Attendance Image");
            if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {
                Toast.makeText(ReadCardActivity.this, "Can't create directory to save image.", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd_mm_yyyy_hh_mm_ss");
            String date = dateFormat.format(new Date());
            String photoFile = "student_" + textViewLastResult.getText().toString() + "_" + date + ".JPG";

            String filename = pictureFileDir.getPath() + File.separator + photoFile;
            File pictureFile = new File(filename);

            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(data);
            fos.close();
            Toast.makeText(ReadCardActivity.this, "New Image saved:" + filename, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        camera.startPreview();
    }

    @Override
    public void onShutter() {
        //Toast.makeText(this, "Click!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Camera.Parameters params = mCamera.getParameters();
        List<Camera.Size> sizes = params.getSupportedPreviewSizes();
        Camera.Size selected = sizes.get(0);
        params.setPreviewSize(selected.width, selected.height);
        mCamera.setParameters(params);

        mCamera.setDisplayOrientation(90);
        mCamera.startPreview();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.i("PREVIEW", "surfaceDestroyed");
    }
}
