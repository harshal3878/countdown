package com.harshal.countdown;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
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
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity  {
    private static final String TAG ="MainActivity";
    private TextView timerSec,timerText,warning;
    private Button start,pause;
    private Boolean countdownRunning=false,urlUp=false,permission=false;
    private int milliSecondsRemaining =5000;
    private ProgressBar progressBar;
    private EditText linkInput;
    private Switch connectionSwitch;
    private URL url;
    private HttpURLConnection conn;
    private Runtime runtime;
    private Process mIpAdarProcess;
    private String pingCommand,ipUrlStatus,dateNow,datetime;
    private int mode=0 ,mExitValue = 0;
    private File logFile;
    private FileWriter fw;
    private BufferedWriter bw;
    private PrintWriter pw;
    private LocalDateTime now;
    private DateTimeFormatter dtf;
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
        pause =findViewById(R.id.btnStop);
        warning = findViewById(R.id.warning);
        progressBar = findViewById(R.id.progressBar);
        linkInput =findViewById(R.id.linkInput);
//if user clicks start button
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isWriteStoragePermissionGranted();
                isReadStoragePermissionGranted();
                if(linkInput.getText().toString().matches("")) {
                    warning.setVisibility(View.VISIBLE);
                }
                else if(!countdownRunning){
                    if(isReadStoragePermissionGranted() && isWriteStoragePermissionGranted()){
                        createFolder();
                    }
                    else{
                        System.out.println("local logging disabled :: external storage permission inactive");
                    }
                    countdownRunning =true;
                    linkInput.setEnabled(false);
                    timerText.setText("Timer running");
                    warning.setVisibility(View.INVISIBLE);
                    hideKeyboard();
                }
                else {
                    Toast.makeText(MainActivity.this, "Timer already running!", Toast.LENGTH_SHORT).show();
                }
            }
        });
//if user clicks pause
        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(countdownRunning){
                    countdownRunning =false;
                    linkInput.setEnabled(true);
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

//first thread
        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (countdownRunning) {
                        progressBar.incrementProgressBy(1);

                        if (milliSecondsRemaining == 0) {

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    if (mode == 0) {
                                        if (mExitValue == 0) {

                                            Toast.makeText(MainActivity.this, "IP working!", Toast.LENGTH_SHORT).show();
                                        } else {

                                            Toast.makeText(MainActivity.this, "IP not working!", Toast.LENGTH_SHORT).show();
                                        }

                                    } else {
                                        if (urlUp) {
                                            Toast.makeText(MainActivity.this, "URL working", Toast.LENGTH_SHORT).show();

                                        } else {
                                            Toast.makeText(MainActivity.this, "URL not working", Toast.LENGTH_SHORT).show();

                                        }
                                    }
                                }
                            });
                            if (permission) {

                                String s = System.lineSeparator() + linkInput.getText().toString() + " , status :: " + ipUrlStatus + " , DATE:TIME ::" + datetime;
                                try {
                                    fw = new FileWriter(logFile.getPath(), true);
                                    bw = new BufferedWriter(fw);
                                    pw = new PrintWriter(bw);
                                    pw.println(s);
                                    System.out.println("log written successfully");
                                    pw.flush();

                                } catch (IOException e) {

                                }

                            }

                            milliSecondsRemaining = 5000;
                            progressBar.setProgress(0);
                        }
                        milliSecondsRemaining -= 50;
                        }

                        //System.out.println("milliseconds remaining:"+milliSecondsRemaining);
                        SystemClock.sleep(50);
                        timerSec.post(new Runnable() {
                            public void run() {
                                timerSec.setText(String.valueOf(milliSecondsRemaining / 1000));

                            }
                        });

                    }
                }

        });
        //second thread
        t = new Thread(new Runnable() {
            @Override
            public void run() {
               while(true){
                   if(countdownRunning && mode==0){
                       runtime = Runtime.getRuntime();
                       try
                       {
                           pingCommand = "/system/bin/ping -c " + linkInput.getText().toString();
                           mIpAdarProcess = runtime.exec(pingCommand);
                           mExitValue = mIpAdarProcess.waitFor();
                           if(mExitValue==0){
                               ipUrlStatus="ip reachable";
                           }
                           else {
                               ipUrlStatus="ip unreachable";
                           }
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
        //third thread
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
                                ipUrlStatus="url reachable ";
                                System.out.println("responseCode: good");
                            } else {
                                urlUp = false;
                                ipUrlStatus="url not reachable";
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
//creates a folder in external storage (phone storage)
    private void createFolder() {
        String storageState = Environment.getExternalStorageState();
        System.out.println("storage state : "+storageState );
        File directory = new File(Environment.getExternalStorageDirectory() + java.io.File.separator +"Countdown/logs");
        System.out.println("folder path : "+directory);
        if (!directory.exists()) {
            System.out.println(directory.mkdirs() ? "Directory has been created" : "Directory not created");

        }
        else{
            System.out.println("Directory already exists");
        }
        if(directory.exists()){
            permission=true;
            dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            now = LocalDateTime.now();
            datetime =dtf.format(now);
            dateNow=dtf.format(now);
            dateNow=dateNow.substring(0,10);
            dateNow=dateNow.replace("/","_");
            System.out.println("date now : "+dateNow);
            logFile = new File(Environment.getExternalStorageDirectory() + java.io.File.separator +"Countdown/logs/"+java.io.File.separator+dateNow+".txt");
            if(!logFile.exists()){

                try {
                    System.out.println("creating log file");
                    logFile.createNewFile();
                    System.out.println("log file created");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else{
                System.out.println("log file already exists");
            }

        }
        else{
            System.out.println("logging disabled due to inactive permission");
            permission=false;
        }

    }

    public  boolean isReadStoragePermissionGranted() {

        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;

        } else {

            System.out.println("Requesting read permission");
            Toast.makeText(this, "Requesting read permission", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 3);

        }
        if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        else{
            return false;
        }


    }

    public  boolean isWriteStoragePermissionGranted() {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {

                System.out.println("Requesting write permission");
                Toast.makeText(this, "Requesting write permission", Toast.LENGTH_SHORT).show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);

            }
        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }else{
            return false;
        }
        }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 2:
                Log.d(TAG, "External storage2");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    System.out.println("write permission is granted");
                    //resume tasks needing this permission
                }else{
                    System.out.println("write permission is denied");
                }
                break;

            case 3:
                Log.d(TAG, "External storage1");
                if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                    System.out.println("read permission is granted");
                }else{
                    System.out.println("read permission is denied");
                }
                break;
        }
    }
    private void hideKeyboard(){
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(linkInput.getWindowToken(), 0);
    }
}





