package com.tom.rpistreaming;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public void onConnectTest(View view)    {
        boolean test_result = pingCamera("192.168.0.234");
    }

    public void showStreamingView(View view)    {
        startActivity(new Intent(MainActivity.this, StreamingActivity.class));
    }

    public void showSettingView(View view)    {
        startActivity(new Intent(MainActivity.this, SettingActivity.class));
    }
    public static boolean pingCamera(String rpi_ip ) {
        Log.e("CAMERA", "pingCamera");
        String str;
        try {
            Process process = Runtime.getRuntime().exec( "/system/bin/ping -c 1 " + rpi_ip);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            int i;
            char[] buffer = new char[4096];
            StringBuffer output = new StringBuffer();
            while ((i = reader.read(buffer)) > 0)
                output.append(buffer, 0, i);
            reader.close();

            str = output.toString();
            if(str.contains("100% packet loss")){
                return false;
            } else {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

    }
}
