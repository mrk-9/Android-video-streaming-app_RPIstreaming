package com.tom.rpistreaming;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;

public class SettingActivity extends AppCompatActivity {
    private String txtFileName = "rpistreaming.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        String ip_str="";
        try{
            Context context = this;
            String fpath = context.getFilesDir().getPath().toString()+"/"+txtFileName;
            File file = new File(fpath);
            if(!file.exists()){
                file.createNewFile();
                ip_str = "http://172.24.1.1:8080/stream/video.h264";
                FileWriter fw = new FileWriter(file.getAbsoluteFile());
                BufferedWriter bw = new BufferedWriter(fw);
                bw.write(ip_str);
                bw.close();
            }
            BufferedReader br = new BufferedReader(new FileReader(fpath));
            ip_str = br.readLine();
        }
        catch(IOException e){
            e.printStackTrace();
        }
        ((TextView)findViewById(R.id.txtRpiIP)).setText(ip_str);
    }

    public void onSave(View view){
        try{
            Context context = this;
            String fpath = context.getFilesDir().getPath().toString()+"/"+txtFileName;

            File file = new File(fpath);
            if(!file.exists()){
                file.createNewFile();
            }
            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            BufferedWriter bw = new BufferedWriter(fw);
            String ip_str = ((TextView)findViewById(R.id.txtRpiIP)).getText().toString();
            bw.write(ip_str);
            bw.close();
            AlertDialog alertDialog = new AlertDialog.Builder(SettingActivity.this).create();
            alertDialog.setTitle("Info");
            alertDialog.setMessage("Streaming url saved successfully.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startActivity(new Intent(SettingActivity.this, MainActivity.class));
                }
            });

            alertDialog.show();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }

    public void onBack(View view){
        startActivity(new Intent(SettingActivity.this, MainActivity.class));
    }
}
