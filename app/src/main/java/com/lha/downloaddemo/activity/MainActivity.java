package com.lha.downloaddemo.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.lha.downloaddemo.R;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private ProgressDialog progressDialog;
    private DownloadTask downloadTask;
    private long last_second_download;
    private long current_download;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btn_download:
                EditText editText = (EditText) findViewById(R.id.address_edit);
                String s = editText.getText().toString();
                if (s.length() > 0){
                    progressDialog.show();
                    downloadTask = new DownloadTask();
                    downloadTask.execute(s);
                    final DecimalFormat decimalFormat = new DecimalFormat(".00");
                    TimerTask timerTask = new TimerTask() {
                        @Override
                        public void run() {
                            final float speed = (float) (current_download - last_second_download) / 1024;

                            final String s1 = decimalFormat.format(speed);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.setMessage(s1 + "KB/S");
                                }
                            });
                            last_second_download = current_download;
                        }
                    };
                    timer = new Timer();
                    timer.schedule(timerTask,0,1000);
                }
                break;
        }
    }

    class DownloadTask extends AsyncTask<String,Integer,String>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            HttpURLConnection connection = null;
            try {
                String u = params[0];
                Log.d("TAG",u);
                URL url = new URL(u);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK){
                    return "网络错误";
                }
                long length = connection.getContentLength();
                String fileName = u.substring(u.lastIndexOf("/") + 1,u.length());
                inputStream = connection.getInputStream();
                outputStream = new FileOutputStream("/sdcard/download/" + fileName);

                byte data[] = new byte[4096];
                int count;
                last_second_download = 0;
                current_download = 0;

                if (length > 0){
                    while ((count = inputStream.read(data)) != -1){
                        current_download = current_download + count;
                        float i = (float)current_download / length * 100;
                        publishProgress((int)i);
                        outputStream.write(data,0,count);
                    }
                }

            } catch (Exception e){
                return e.toString();
            } finally {
                try {
                    if (outputStream != null){
                        outputStream.close();
                    }
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (connection != null){
                    connection.disconnect();
                }
            }

            return "下载成功...";
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            //Log.d("TAG",values[0] + "");
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Toast.makeText(MainActivity.this,s,Toast.LENGTH_SHORT).show();
            progressDialog.cancel();
        }
    }


    public void initView(){
        findViewById(R.id.btn_download).setOnClickListener(this);
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("下载中...");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (downloadTask != null){
                    downloadTask.cancel(true);
                    if (timer != null){
                        timer.cancel();
                    }
                }
            }
        });
    }
}
