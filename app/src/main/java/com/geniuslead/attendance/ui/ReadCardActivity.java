package com.geniuslead.attendance.ui;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.view.SurfaceView;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.daililol.cameraviewlibrary.CameraView;
import com.geniuslead.attendance.R;
import com.geniuslead.attendance.events.AttendanceEvent;
import com.geniuslead.attendance.events.CamCaptureEvent;
import com.geniuslead.attendance.events.UserDetailsEvent;
import com.geniuslead.attendance.jobs.SubmitAttendanceJob;
import com.geniuslead.attendance.model.AttendanceResultSubmission;
import com.geniuslead.attendance.model.Students;
import com.geniuslead.attendance.model.UserDetails;
import com.geniuslead.attendance.utils.MyApplication;
import com.geniuslead.attendance.utils.MyException;
import com.geniuslead.attendance.utils.ReuseableClass;
import com.google.gson.Gson;

import org.json.JSONArray;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.greenrobot.event.EventBus;

public class ReadCardActivity extends AppCompatActivity implements CameraView.PhotoCaptureCallback {

    @Bind(R.id.textViewLastResult)
    TextView textViewLastResult;
    Camera mCamera;
    SurfaceView mPreview;
    @Bind(R.id.buttonCaptureImage)
    Button buttonCaptureImage;
    @Bind(R.id.buttonAttendanceDone)
    Button buttonAttendanceDone;
    String photoFunctionality = "";
    String uploadPhoto = "";
    AttendanceResultSubmission attendanceResultSubmission;
    ArrayList<Students> studentsArrayList = null;
    private NfcAdapter mAdapter;
    private PendingIntent mPendingIntent;
    private AlertDialog mDialog;
    private NdefMessage mNdefPushMessage;
    private int cameraId = 0;
    private CameraView cameraView;
    private ProgressDialog progressDialog;
    @Bind(R.id.webview)
    WebView webview;
    String decodedUrl = "";
    private String decodedSuffix = "";
    private String decodedStudentId;
    private boolean isRedirected = false;
    private Boolean processingDecoding = false;

    int attendanceCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_card);
        ButterKnife.bind(this);

        webview.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {

                if (!isRedirected) {
                    isRedirected = true;
                    decodedUrl = webview.getUrl();
                    if (decodedUrl != null) {
                        Log.d("URL", "Decoded URL: " + decodedUrl);

                        decodedUrl = decodedUrl.substring(decodedUrl.indexOf("//") + 2, decodedUrl.lastIndexOf("/"));
                        decodedStudentId = decodedUrl + decodedSuffix;

                        if (progressDialog != null) progressDialog.dismiss();
                        Log.d("URL", "Decoded ID: " + decodedStudentId);

                        processingDecoding = false;
                        EventBus.getDefault().post(new CamCaptureEvent.Success(decodedStudentId));
                    } else {
                        if (progressDialog != null) progressDialog.dismiss();
                        Toast.makeText(ReadCardActivity.this, "Unable to fetch your id. Try again please.", Toast.LENGTH_LONG).show();
                    }
                } else
                    isRedirected = false;
            }
        });

        attendanceResultSubmission = new AttendanceResultSubmission();
        studentsArrayList = new ArrayList<Students>();

        if (!ReuseableClass.getFromPreference("userDetailsObject", this).equalsIgnoreCase("")) {
            UserDetails userDetails = new Gson().fromJson(ReuseableClass.getFromPreference("userDetailsObject", this), UserDetails.class);
            attendanceResultSubmission.setDeviceId(userDetails.getDeviceId());
            attendanceResultSubmission.setUserName(userDetails.getUserName());
            attendanceResultSubmission.setPassword(userDetails.getPassword());
            attendanceResultSubmission.setAttendanceDateTime(new SimpleDateFormat("dd/MM/yyyy").format(new Date()));
            attendanceResultSubmission.setCourseId(ReuseableClass.getFromPreference("courseId", ReadCardActivity.this));
            attendanceResultSubmission.setSubjectId(ReuseableClass.getFromPreference("subjectId", ReadCardActivity.this));
            attendanceResultSubmission.setUserId(userDetails.getUserId());
            attendanceResultSubmission.setCollegeId(userDetails.getCollegeId());
        }
        photoFunctionality = ReuseableClass.getFromPreference("photoFunctionality", ReadCardActivity.this);
        uploadPhoto = ReuseableClass.getFromPreference("uploadPhoto", ReadCardActivity.this);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        cameraView = (CameraView) findViewById(R.id.cameraView);
        cameraView.setPhotoCaptureCallback(this);

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
                cameraView.setCamera(cameraView.getFrontCamera());
            }
        }

        if (!photoFunctionality.equalsIgnoreCase("") && photoFunctionality.equalsIgnoreCase("false"))
            cameraView.setVisibility(View.GONE);

        mDialog = new AlertDialog.Builder(this).setNeutralButton("Ok", null).create();
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mAdapter == null) {
            showMessage(R.string.error, R.string.no_nfc);
            finish();
            return;
        }
        mPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        mAdapter = NfcAdapter.getDefaultAdapter(this);
        mNdefPushMessage = new NdefMessage(new NdefRecord[]{newTextRecord("Message from NFC Reader :-)", Locale.ENGLISH, true)});
    }

    @OnClick(R.id.buttonCaptureImage)
    public void capturingImage() {
        cameraView.capture();
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
        // Toast.makeText(ReadCardActivity.this, "OnResume", Toast.LENGTH_LONG).show();
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                showWirelessSettingsDialog();
            }
            mAdapter.enableForegroundDispatch(this, mPendingIntent, null, null);
            mAdapter.enableForegroundNdefPush(this, mNdefPushMessage);
        }
        cameraView.reconnectCamera();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mAdapter != null) {
            mAdapter.disableForegroundDispatch(this);
            mAdapter.disableForegroundNdefPush(this);
        }
        cameraView.releaseCamera();
    }

    @Override
    public void onNewIntent(Intent intent) {
        setIntent(intent);
        resolveIntent(intent);
    }

    private void resolveIntent(Intent intent) {
        String type = intent.getType();
        String action = intent.getAction();

        if (!processingDecoding) {
            processingDecoding = true;
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
                            if (text.substring(text.length() - 2).equalsIgnoreCase("-N")) {
                                if (!photoFunctionality.equalsIgnoreCase("") && photoFunctionality.equalsIgnoreCase("true"))
                                    capturingImage();

                                EventBus.getDefault().post(new CamCaptureEvent.Success(text));
                            } else if (text.substring(text.length() - 2).equalsIgnoreCase("-Y")) {
                                if (!photoFunctionality.equalsIgnoreCase("") && photoFunctionality.equalsIgnoreCase("true"))
                                    capturingImage();

                                progressDialog = ProgressDialog.show(ReadCardActivity.this, "Getting your id wait please", getString(R.string.loading), true);

                                Log.d("URL", "url: " + text.substring(0, text.indexOf("-")));
                                Log.d("URL", "rest: " + text.substring(text.indexOf("-"), text.length()));

                                decodedSuffix = text.substring(text.indexOf("-"), text.length());
                                webview.loadUrl(text.substring(0, text.indexOf("-")));
                            } else
                                Log.i("TAG", "Some other type card");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    textViewLastResult.setText("Wrong mime type: " + type);
                }
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
        textViewLastResult.setText(event.getNfcValue());

        Students students = new Students();
        students.setUniqueIdentifier(event.getNfcValue());
        studentsArrayList.add(students);
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
    public void onPhotoCaptured(Bitmap bitmap) {
        //Here, we chose internal storage
        try {
            //Compressing the image 640*480
            //-----------------------------------------
            //Bitmap bmp = BitmapFactory.decodeByteArray(imgData, 0, imgData.length);
            Bitmap resizedBmp = Bitmap.createScaledBitmap(bitmap, 640, 480, false);

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
            attendanceCount += attendanceCount;
            Toast.makeText(ReadCardActivity.this, "New Image saved:" + filename + "\nTotal attendance - " + attendanceCount, Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private boolean writeBitmapToFile(Bitmap bitmap, String desFileUrl) {
        FileOutputStream outStream = null;

        try {
            outStream = new FileOutputStream(desFileUrl);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @OnClick(R.id.buttonAttendanceDone)
    public void submittingAttendanceResult(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(ReadCardActivity.this);
        builder.setTitle("Confirm");
        builder.setMessage("Are you sure all attendance are done?");
        builder.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                attendanceResultSubmission.setStudents(studentsArrayList);
                Log.d("TAG", "Submitting Json: " + attendanceResultSubmission.toString());
                progressDialog = ProgressDialog.show(ReadCardActivity.this, "", getString(R.string.loading), true);
                MyApplication.getInstance().getJobManager().addJob(new SubmitAttendanceJob(attendanceResultSubmission));
            }
        });
        builder.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void onEventMainThread(AttendanceEvent.Success event) {
        if (progressDialog != null) progressDialog.dismiss();

        try {
            JSONArray studentArray = new JSONArray(event.getResult());
            AlertDialog.Builder builder = new AlertDialog.Builder(ReadCardActivity.this);
            builder.setTitle("Confirmation");
            builder.setMessage("Attendance successful. No of student is - " + studentArray.length());
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    Toast.makeText(ReadCardActivity.this, "Thank you.", Toast.LENGTH_LONG).show();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();

        } catch (Throwable e) {
            EventBus.getDefault().post(new UserDetailsEvent.Fail(new MyException(e.getMessage())));
        }
    }

    public void onEventMainThread(AttendanceEvent.Fail event) {
        if (progressDialog != null) progressDialog.dismiss();
        if (event.getEx() != null) {
            new android.support.v7.app.AlertDialog.Builder(this)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setMessage(R.string.server_error)
                    .setPositiveButton("OK", null)
                    .show();
        }
    }
}
