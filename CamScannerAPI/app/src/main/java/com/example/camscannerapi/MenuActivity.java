package com.example.camscannerapi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.facebook.Profile;
import com.facebook.login.LoginManager;
import com.intsig.csopen.sdk.CSOpenAPI;
import com.intsig.csopen.sdk.CSOpenApiFactory;
import com.intsig.csopen.sdk.CSOpenApiHandler;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import com.intsig.csopen.sdk.CSOpenAPIParam;
import com.intsig.csopen.sdk.ReturnCode;
import com.intsig.csopen.util.Log;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String Tag="MenuActivity";
    TextView name;
    Button logout;
    private final int REQ_CODE_PICK_IMAGE = 1;
    private final int REQ_CODE_CALL_CAMSCANNER = 2;

    private static final String APP_KEY = "KLPt0gTtYUyP0fTXV8aH44e7";

    private static final String DIR_IMAGE = Environment.getExternalStorageDirectory().getAbsolutePath()
            + "/CSOpenApiDemo";

    // three values for save instance;
    private static final String SCANNED_IMAGE = "scanned_img";
    private static final String SCANNED_PDF = "scanned_pdf";
    private static final String ORIGINAL_IMG = "ori_img";


    private String mSourceImagePath;
    private String mOutputImagePath;
    private String mOutputPdfPath;
    private String mOutputOrgPath;

    private ImageView mImageView;
    private Bitmap mBitmap;

    private static final int CAMERA_REQUEST = 1888;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;

    private CSOpenAPI mApi;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);
        name = findViewById(R.id.name);
        logout = findViewById(R.id.logout);

        if(isReadStoragePermissionGranted() && isWriteStoragePermissionGranted()){
            Util.checkDir(DIR_IMAGE);
        }
        mImageView=(ImageView) findViewById(R.id.image);
        findViewById(R.id.gallery).setOnClickListener(this);
        findViewById(R.id.camera).setOnClickListener(this);

        Log.setLevel(Log.LEVEL_DEBUG);
        mApi = CSOpenApiFactory.createCSOpenApi(this, APP_KEY, null);

        String loginStatus = getIntent().getStringExtra("DATA");
        if(loginStatus.equals("")){
            Profile profile = Profile.getCurrentProfile();
            URL profile_pic = null;
            try {
                profile_pic = new URL("https://graph.facebook.com/" + profile.getId() + "/picture?width=250&height=250");
                Picasso.with(this).load(profile_pic.toString()).into((ImageView) findViewById(R.id.avatar));
                name.setText("Welcome "+ profile.getFirstName() + "!");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }else{
            JSONObject data = null;
            try {
                data = new JSONObject(getIntent().getStringExtra("DATA"));
                try {
                    URL profile_pic = new URL("https://graph.facebook.com/" + data.getString("id") + "/picture?width=250&height=250");
                    Picasso.with(this).load(profile_pic.toString()).into((ImageView) findViewById(R.id.avatar));
                    name.setText("Welcome "+ data.getString("first_name") + "!");
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                LoginManager.getInstance().logOut();
                startActivity(new Intent(MenuActivity.this, MainActivity.class));
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
        this.finish();
    }

    private void go2Gallery() {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        i.setType("image/*");
        try {
            startActivityForResult(i, REQ_CODE_PICK_IMAGE);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mOutputImagePath=savedInstanceState.getString(SCANNED_IMAGE);
        mOutputPdfPath=savedInstanceState.getString(SCANNED_PDF);
        mOutputOrgPath=savedInstanceState.getString(ORIGINAL_IMG);
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString(SCANNED_IMAGE, mOutputImagePath);
        outState.putString(SCANNED_PDF, mOutputPdfPath);
        outState.putString(ORIGINAL_IMG, mOutputOrgPath);
        super.onSaveInstanceState(outState);
    }

    public  boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"Permission is granted1");
                return true;
            } else {

//                Log.v(TAG,"Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
//            Log.v(TAG,"Permission is granted1");
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==MY_CAMERA_PERMISSION_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
//                Toast.makeText(this, "camera permission granted", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
            else
            {
                Toast.makeText(this, "camera permission denied", Toast.LENGTH_LONG).show();
            }
        }
        switch (requestCode) {
            case 2:
//                Log.d(TAG, "External storage2");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
//                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    Util.checkDir(DIR_IMAGE);
                }else{
//                    progress.dismiss();
                }
                break;

            case 3:
//                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
//                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
//                    SharePdfFile();
                    Util.checkDir(DIR_IMAGE);
                }else{
//                    progress.dismiss();
                }
                break;
        }
    }

    public  boolean isWriteStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"Permission is granted2");
                return true;
            } else {

//                Log.v(TAG,"Permission is revoked2");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return false;
            }
        }
        else { //permission is automatically granted on sdk<23 upon installation
//            Log.v(TAG,"Permission is granted2");
            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(Tag, "requestCode:"+requestCode+" resultCode:"+resultCode);
        if(requestCode == REQ_CODE_CALL_CAMSCANNER){
            mApi.handleResult(requestCode, resultCode, data, new CSOpenApiHandler() {

                @Override
                public void onSuccess() {
                    new AlertDialog.Builder(MenuActivity.this)
                            .setTitle(R.string.a_title_success)
                            .setMessage(R.string.a_msg_api_success)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                    mBitmap = Util.loadBitmap(mOutputImagePath);
                }

                @Override
                public void onError(int errorCode) {
                    String msg = handleResponse(errorCode);
                    /*new AlertDialog.Builder(MenuActivity.this)
                            .setTitle(R.string.a_title_reject)
                            .setMessage(msg)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();*/
                }

                @Override
                public void onCancel() {
                    new AlertDialog.Builder(MenuActivity.this)
                            .setMessage(R.string.a_msg_cancel)
                            .setPositiveButton(android.R.string.ok, null)
                            .create().show();
                }
            });
        } else if (requestCode == REQ_CODE_PICK_IMAGE && resultCode == RESULT_OK) {	// result of go2Gallery
            if (data != null) {
                Uri u = data.getData();
                Cursor c = getContentResolver().query(u, new String[] { "_data" }, null, null, null);
                if (c == null || c.moveToFirst() == false) {
                    return;
                }
                mSourceImagePath = c.getString(0);
                c.close();
                go2CamScanner();
            }
        }

        else if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK)
        {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            // CALL THIS METHOD TO GET THE URI FROM THE BITMAP
            Uri tempUri = getImageUri(getApplicationContext(), photo);

            // CALL THIS METHOD TO GET THE ACTUAL PATH
            //File finalFile = new File(getRealPathFromURI(tempUri));
            mSourceImagePath = getRealPathFromURI(tempUri);
            go2CamScanner();
            mImageView.setImageBitmap(photo);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    public String getRealPathFromURI(Uri uri) {
        String path = "";
        if (getContentResolver() != null) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                path = cursor.getString(idx);
                cursor.close();
            }
        }
        return path;
    }

    private void go2CamScanner() {
        mOutputImagePath = DIR_IMAGE + "/scanned.jpg";
        mOutputPdfPath = DIR_IMAGE + "/scanned.pdf";
        mOutputOrgPath = DIR_IMAGE + "/org.jpg";
        try {
            FileOutputStream fos = new FileOutputStream(mOutputOrgPath);
            fos.write(3);
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CSOpenAPIParam param = new CSOpenAPIParam(mSourceImagePath,
                mOutputImagePath, mOutputPdfPath, mOutputOrgPath, 1.0f);
        boolean res = mApi.scanImage(this, REQ_CODE_CALL_CAMSCANNER, param);
        android.util.Log.d(Tag, "send to CamScanner result: " + res);
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if(id == R.id.gallery){
            go2Gallery();
        }
        else if(id==R.id.camera){
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkSelfPermission(Manifest.permission.CAMERA)
                        == PackageManager.PERMISSION_GRANTED) {
//                Log.v(TAG,"Permission is granted1");
                    Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, CAMERA_REQUEST);
                } else {

//                Log.v(TAG,"Permission is revoked1");
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
                }
            }
            else { //permission is automatically granted on sdk<23 upon installation
//            Log.v(TAG,"Permission is granted1");
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        }
    }

    private String handleResponse(int code){
        switch(code){
            case ReturnCode.OK:
                return getString(R.string.a_msg_api_success);
            case  ReturnCode.INVALID_APP:
                return getString(R.string.a_msg_invalid_app);
            case ReturnCode.INVALID_SOURCE:
                return getString(R.string.a_msg_invalid_source);
            case ReturnCode.AUTH_EXPIRED:
                return getString(R.string.a_msg_auth_expired);
            case ReturnCode.MODE_UNAVAILABLE:
                return getString(R.string.a_msg_mode_unavailable);
            case ReturnCode.NUM_LIMITED:
                return getString(R.string.a_msg_num_limit);
            case ReturnCode.STORE_JPG_ERROR:
                return getString(R.string.a_msg_store_jpg_error);
            case ReturnCode.STORE_PDF_ERROR:
                return getString(R.string.a_msg_store_pdf_error);
            case ReturnCode.STORE_ORG_ERROR:
                return getString(R.string.a_msg_store_org_error);
            case ReturnCode.APP_UNREGISTERED:
                return getString(R.string.a_msg_app_unregistered);
            case ReturnCode.API_VERSION_ILLEGAL:
                return getString(R.string.a_msg_api_version_illegal);
            case ReturnCode.DEVICE_LIMITED:
                return getString(R.string.a_msg_device_limited);
            case ReturnCode.NOT_LOGIN:
                return getString(R.string.a_msg_not_login);
            default:
                return "Return code = " + code;
        }
    }
}
