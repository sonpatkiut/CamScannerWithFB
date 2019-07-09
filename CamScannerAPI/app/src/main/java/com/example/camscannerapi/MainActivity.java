package com.example.camscannerapi;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Network;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.Profile;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    CallbackManager callbackManager;
    ProgressDialog mDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);

        callbackManager = CallbackManager.Factory.create();

        //printKeyHash();
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if(accessToken!=null){
            Intent intent = new Intent(this, MenuActivity.class);
            intent.putExtra("DATA", "");
            startActivity(intent);
            finish();
        }else{

            LoginButton button = findViewById(R.id.login_button);
            button.setReadPermissions(Arrays.asList("public_profile","email"));

            button.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    mDialog = new ProgressDialog(MainActivity.this);
                    mDialog.setMessage("Retrieving data...");
                    mDialog.show();

                    String accessToken = loginResult.getAccessToken().getToken();
                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            mDialog.dismiss();
                            getData(object);
                            Log.d("response", object.toString());
                        }
                    });

                    //Request Graph API
                    Bundle parameters = new Bundle();
                    parameters.putString("fields","id,email,first_name,last_name,gender");
                    request.setParameters(parameters);
                    request.executeAsync();
                }

                @Override
                public void onCancel() {

                }

                @Override
                public void onError(FacebookException error) {
                    if(!Util.isNetworkConnected(MainActivity.this)){
                        //show network error
                        Toast.makeText(MainActivity.this, "Please connect to internet to continue", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

    }

    private void getData(JSONObject object){
        /*try{
            URL profile_pic = new URL("https://graph.facebook.com/" + object.getString("id") + "/picture?width=250&height=250");
            Picasso.with(this).load(profile_pic.toString()).into((ImageView) findViewById(R.id.avatar));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

        //navigate to other Activity
        Intent intent = new Intent(this, MenuActivity.class);
        intent.putExtra("DATA", object.toString());
        startActivity(intent);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);

    }

    private void printKeyHash(){
        try{
            PackageInfo info = getPackageManager().getPackageInfo("com.example.camscannerapi", PackageManager.GET_SIGNATURES);
            for(Signature signature:info.signatures){
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}
