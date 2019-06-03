package com.example.administrator.download;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;




import android.os.PowerManager;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.view.View;
import android.widget.Button;

import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    //  按钮
    private Button button;
    //  上下文
    //  进度条
    //  对话框
    private ProgressDialog mDownloadDialog;//进度条
    private static String[] PERMISSIONS_STORAGE = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE};    //请求状态码
    private static int REQUEST_PERMISSION_CODE = 2;
    //  判断是否停止
    //  请求链接
    private String url ="http://117.158.18.33:90/Public/data/app/SmartGenOR.apk";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)//进行权限的判断
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS_STORAGE, REQUEST_PERMISSION_CODE);
            }
        }

        mDownloadDialog =new ProgressDialog(MainActivity.this);//进度条
        mDownloadDialog.setMessage("正在下载...");
        mDownloadDialog.setIndeterminate(true);//进度条是否明确
        mDownloadDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//进度条的样式
        mDownloadDialog.setCancelable(true);

        button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onUpdateClick();//主要操作逻辑

            }
        });
    }



    /*
     * 显示正在下载对话框
     */


//升级下载按钮点击事件
        private void onUpdateClick(){

            final DownloadTask downloadTask = new DownloadTask(MainActivity.this);

            downloadTask.execute(url);
            mDownloadDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    downloadTask.cancel(true);
                }
            });
        }

        // 下载文件
       // downloadAPK();

    private  class DownloadTask extends AsyncTask<String,Integer,String> {//下载文件  AsyncTask线程 thred的框架的封装
        private Context context;
        private PowerManager.WakeLock mWakeLock;
        public DownloadTask(Context context) {
            this.context = context;
        }
        //onPreExecute(),在execute(Params... params)方法被调用后立即执行，执行在ui线程，
        // 一般用来在执行后台任务前会UI做一些标记
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mDownloadDialog.show();
        }
        // doInBackground这个方法在onPreExecute()完成后立即执行，
        // 用于执行较为耗时的操作，
        // 此方法接受输入参数
        // 和返回计算结果（返回的计算结果将作为参数在任务完成是传递到onPostExecute(Result result)中），
        // 在执行过程中可以调用publishProgress(Progress... values)来更新进度信息
        //后台任务的代码块
        @Override
        protected String doInBackground(String... url) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL urll=new URL(url[0]);
                Log.d("upgrade","url1:"+urll+"////url:"+url);
                connection = (HttpURLConnection) urll.openConnection();//打开网络链接
                connection.connect();
                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {//链接失败
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }
                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();//获取链接内容的长度
                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/sdcard/new.apk");//下载安装的安装包名后缀
                byte data[] = new byte[4096];//字节
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        //在调用这个方法后，执行onProgressUpdate(Progress... values)，
                        //运行在主线程，用来更新pregressbar
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }
        //onProgressUpdate(Progress... values),
        // 执行在UI线程，在调用publishProgress(Progress... values)时，此方法被执行。
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mDownloadDialog.setIndeterminate(false);
            mDownloadDialog.setMax(100);
            mDownloadDialog.setProgress(progress[0]);//设置进度
        }

        //onPostExecute(Result result),
        // 执行在UI线程，当后台操作结束时，此方法将会被调用。
        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mDownloadDialog.dismiss();
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
                Log.i("aaa",result);
            }
            else
            {Toast.makeText(context,"File downloaded", Toast.LENGTH_SHORT).show();}
//这里主要是做下载后自动安装的处理
            File file=new File("/sdcard/new.apk");
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {//安卓7.0进行的版本判断
                installIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);//传递权限
                Uri contentUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".fileProvider", file);
                installIntent.setDataAndType(contentUri, "application/vnd.android.package-archive");
            } else {
                installIntent.setDataAndType(Uri.fromFile(file), "application/vnd.andid.proackage-archiv");
                installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }


            startActivity(installIntent);
        }

    }

}//calss

