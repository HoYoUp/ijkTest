package com.zb.myapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class MainActivity extends Activity
{
    private ListView list;
    private EditText ip;
    private Button update;

    private int SUCCESS = 1;
    private int FAILED = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 下载数据并显示
        try {
            initView();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Handler handler = new Handler()
    {

        public void handlerMessage(Message msg)
        {
            if(msg.what == SUCCESS)
            {
                tip("下载成功");
            }
            else
            {
                tip("下载失败");
            }
        }
    };

    @SuppressLint("JavascriptInterface")
    private void initView() throws IOException {

        list = (ListView) findViewById(R.id.names);
        // 获取所有剧集的名称
        String [] names = loadData();
        list.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // 展示剧集具体信息
                Intent intent = new Intent(MainActivity.this,listLinks.class);
                intent.putExtra("pos",String.valueOf(position));
                startActivity(intent);
            }
        });

        update = findViewById(R.id.update);
        // 更新资源文件
        update.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                reView();
            }
        });

        ArrayAdapter<String>  adapter = new ArrayAdapter<String>(this,R.layout.item_name,names);
        list.setAdapter(adapter);

    }

    // 更新资源文件
    private void reView(){
        if(ip.getText().equals("")){
            Toast.makeText(this,"请输入ip地址",Toast.LENGTH_LONG);
        }else
            getData();

    }

    // 获取资源文件
    private void getData()
    {
        new Thread()
        {
            HttpURLConnection conn;
            public void run()
            {
                URL url = null;
                try {
                    url = new URL("http://" + ip.getText() + ":8002/tvUpdate");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    if(conn.getResponseCode() == 200)
                    {
                        InputStream in = conn.getInputStream();
                        // 获取数据并解压
                        BufferedReader reader = new BufferedReader(new InputStreamReader(
                                in, conn.getContentEncoding()));
                        char[] buffer = new char[4096];
                        StringBuilder content = new StringBuilder();
                        int downloadedBytes = 0;
                        int len1 = 0;
                        while ((len1 = reader.read(buffer)) > 0)
                        {
                            downloadedBytes += len1;
                            content.append(buffer);
                        }
                        FileOutputStream out = openFileOutput("data.zip",MODE_PRIVATE);

                        out.write(content.toString().getBytes());

                        out.close();
                        releaseZip();
                        Message msg = new Message();
                        msg.what = SUCCESS;
                        handler.sendMessage(msg);
                    }
                    else
                    {
                        Message msg = new Message();
                        msg.what = FAILED;
                        handler.sendMessage(msg);
                    }
                } catch (MalformedURLException | ProtocolException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };
    }

    // 释放初始化数据
    public void release() throws IOException {
        AssetManager mg = getAssets();
        String[] srcFiles = mg.list("");
        String desPath = getFilesDir().getPath();
        for (String srcFileName : srcFiles) {
            String outFileName = desPath + File.separator + srcFileName;
            Log.e("tag","=========  filename: "+srcFileName +" outFile: "+outFileName);
            try {
                InputStream in = mg.open(srcFileName);
                FileOutputStream out = new FileOutputStream(outFileName);
                byte[] buffer = new byte[1024];
                int n = 0;
                while(-1 != (n = in.read(buffer))){
                    out.write(buffer,0,n);
                }
                in.close();
                out.close();
            } catch (IOException e) {//if directory fails exception
                e.printStackTrace();
                new File(outFileName).mkdir();
                //doCopy(context,srcFileName, outFileName);
            }
        }

    }

    // 解压数据压缩包
    public void releaseZip() throws IOException {
        File data = new File(getFilesDir()+"data.zip");
        InputStream in = new FileInputStream(data);
        ZipInputStream zipIn = new ZipInputStream(in);
        ZipEntry zip = zipIn.getNextEntry();
        String outputDirectory = getFilesDir().getPath();
        byte[] buffer = new byte[1024*1024];

        // 解压时字节计数
        int count = 0;
        // 如果进入点为空说明已经遍历完所有压缩包中文件和目录
        String TAG = "file";
        while (zip != null) {
            Log.i(TAG,"解压文件 入口 1： " +zip );
            if (!zip.isDirectory()) {  //如果是一个文件
                // 如果是文件
                String fileName = zip.getName();
                Log.i(TAG,"解压文件 原来 文件的位置： " + fileName);
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);  //截取文件的名字 去掉原文件夹名字
                Log.i(TAG,"解压文件 的名字： " + fileName);
                File file = new File(outputDirectory + File.separator + fileName);  //放到新的解压的文件路径

                file.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(file);
                while ((count = zipIn.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, count);
                }
                fileOutputStream.close();

            }

            // 定位到下一个文件入口
            zip = zipIn.getNextEntry();
            Log.i(TAG,"解压文件 入口 2： " + zip );
        }

        // 删除压缩包
        zipIn.close();
        data.delete();
    }

    // 提示信息
    public void tip(String tip)
    {
        Toast toast = new Toast(this);
        toast.setText(tip);
        toast.show();
    }

    // 加载数据
    public String[] loadData() throws IOException {
        //AssetManager mg = getAssets();
        //InputStream in = mg.open("names.txt");
        File file = new File(getFilesDir().getPath() + "names.txt");
        if(!file.exists())
            release();
        FileInputStream in = openFileInput("names.txt");

        byte [] temp = new byte[52];
        int count = in.read(temp);
        String content = new String(temp,0,count,"UTF-8");
        Log.i("tv names ",content);
        in.close();
        return content.split(" ");

    }

}

