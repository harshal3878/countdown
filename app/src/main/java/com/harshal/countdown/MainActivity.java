package com.harshal.countdown;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity  {
    private static final String TAG ="MainActivity";
    private TextView timerSec,timerText,warning;
    private Button start,stop;
    private Boolean countdownRunning=false,urlUp=false;
    private int milliSecondsRemaining =5000;
    private ProgressBar progressBar;
    private EditText linkInput;
    private Switch connectionSwitch;
    private URL url;
    private HttpURLConnection conn;
    private Context context;
    private Runtime runtime;
    private Process  mIpAddrProcess;
    private String pingCommand,ipUrlStatus;
    private int mode=0 ,mExitValue = 0;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                Log.d(TAG, "External storage2");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show();
                }
                break;

            case 3:
                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    Log.v(TAG,"Permission: "+permissions[0]+ "was "+grantResults[0]);
                    //resume tasks needing this permission
                    Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(this, "permission not granted", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    LocalDateTime now;
    File gpxfile;
    DateTimeFormatter dtf;
    Thread t = null;
    Thread thread1,thread2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        connectionSwitch = findViewById(R.id.connectionSwitch);
        timerSec =findViewById(R.id.timerSec);
        timerText = findViewById(R.id.timerText);
        start =findViewById(R.id.btnStart);
        stop =findViewById(R.id.btnStop);
        warning = findViewById(R.id.warning);
        progressBar = findViewById(R.id.progressBar);
        linkInput =findViewById(R.id.linkInput);

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            1);
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            2);
                }
                else if(linkInput.getText().toString().matches("")) {
                    warning.setVisibility(View.VISIBLE);
                }
                else if(!countdownRunning){
                    countdownRunning =true;
                    timerText.setText("Timer running");
                    warning.setVisibility(View.INVISIBLE);
                    InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(linkInput.getWindowToken(), 0);
                }
                else {
                    Toast.makeText(MainActivity.this, "Timer already running!", Toast.LENGTH_SHORT).show();
                }
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(countdownRunning){
                    countdownRunning =false;
                    timerText.setText("Timer paused");
                }
            }
        });
        connectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(countdownRunning){
                    Toast.makeText(MainActivity.this, "pause the timer first", Toast.LENGTH_SHORT).show();
                    if (connectionSwitch.isChecked()){
                        connectionSwitch.setChecked(false);
                    }
                    else if(!connectionSwitch.isChecked()){
                        connectionSwitch.setChecked(true);
                    }
                }
                else if(!isChecked){
                    mode = 0;
                    linkInput.setHint("enter IP");
                }
                else if(isChecked){
                    mode= 1;
                    linkInput.setHint("enter URL");
                }
        }
        });


        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (countdownRunning) {
                        progressBar.incrementProgressBy(1);

                        if (milliSecondsRemaining == 0) {
                            progressBar.setProgress(0);
                            runOnUiThread(new Runnable() {
                                public void run() {
                                        if(mode==0){
                                            if(mExitValue==0){
                                                ipUrlStatus ="reachable";
                                            Toast.makeText(MainActivity.this, "IP working!", Toast.LENGTH_SHORT).show();
                                        }
                                            else{
                                                ipUrlStatus ="not reachable";
                                            Toast.makeText(MainActivity.this, "IP not working!", Toast.LENGTH_SHORT).show();
                                        }

                                        }
                                        else{
                                            if(urlUp){
                                                Toast.makeText(MainActivity.this, "URL working", Toast.LENGTH_SHORT).show();
                                                ipUrlStatus ="reachable";
                                            }
                                            else{
                                                Toast.makeText(MainActivity.this, "URL not working", Toast.LENGTH_SHORT).show();
                                                ipUrlStatus ="not reachable";
                                            }
                                        }
                                }
                            });

                            milliSecondsRemaining = 5000;

                        }
                        milliSecondsRemaining -= 50;
                        System.out.println("milliseconds remaining:"+milliSecondsRemaining);
                        SystemClock.sleep(50);
                        timerSec.post(new Runnable() {
                            public void run() {
                                timerSec.setText(String.valueOf(milliSecondsRemaining / 1000));

                            }
                        });

                    }
                }
            }
        });
        t = new Thread(new Runnable() {
            @Override
            public void run() {
               while(true){
                   if(countdownRunning && mode==0){
                       runtime = Runtime.getRuntime();
                       try
                       {
                           pingCommand = "/system/bin/ping -c " + linkInput.getText().toString();
                           mIpAddrProcess = runtime.exec(pingCommand);
                           mExitValue = mIpAddrProcess.waitFor();
                       } catch (Exception ignore)
                       {
                           ignore.printStackTrace();
                           System.out.println(" Exception:"+ignore);
                       }
                       SystemClock.sleep(1000);
                   }
               }
            }
        });
        thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if (countdownRunning && mode==1) {
                        try {
                            url = new URL(linkInput.getText().toString());
                            conn = (HttpURLConnection) url.openConnection();
                            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                                urlUp = true;
                                System.out.println("responseCode: good");
                            } else {
                                urlUp = false;
                                System.out.println("responseCode: "+conn.getResponseCode());
                            }
                        } catch (SocketTimeoutException tout) {
                            System.out.println("socket exception" + tout);
                        } catch (IOException ioex) {
                            System.out.println("ioException" + ioex);
                        }catch(Exception e){
                            System.out.println(e);
                        }

                    }
                    SystemClock.sleep(1500);
                }
            }
        });
        thread1.start();
        t.start();
        thread2.start();
        }
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(linkInput.getWindowToken(), 0);
    }
        }





