package com.zb.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.net.HttpURLConnection;

public class listLinks extends AppCompatActivity
{
    private List<String> episodeList;
	private TabLayout episodes;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_links_tablayout);
        episodes = (TabLayout) findViewById(R.id.test);
		
        try {
            initView();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void initView() throws IOException {
        Intent intent = getIntent();
        String name = intent.getStringExtra("pos");
        assert name != null;

        // 获取所选剧集的链接
		//AssetManager mg = getAssets();
		//InputStream in = mg.open(name+".txt");
		
        FileInputStream in = openFileInput(name+".txt");
        byte [] temp = new byte[1024];
        int count = in.read(temp);
        String content = new String(temp, 0, count, "ascii");
        in.close();
        // 存储链接
        episodeList = Arrays.asList(content.split("\n"));
        Log.i("tv links",episodeList.get(0));

		initEpisodesTablayout();

    }


    private void initEpisodesTablayout() {

        for (int i = 1; i < episodeList.size(); i++) {
            episodes.addTab(episodes.newTab().setText(String.valueOf((i))));
        }
        //用来循环适配器中的视图总数
        for (int i = 0; i < episodes.getTabCount(); i++) {
            //获取每一个tab对象
            TabLayout.Tab tabAt = episodes.getTabAt(i);
            //将每一个条目设置我们自定义的视图
            tabAt.setCustomView(R.layout.tab_video_episodes);
            //通过tab对象找到自定义视图的ID
            TextView textView = tabAt.getCustomView().findViewById(R.id.tab_video_episodes_tv);
            //设置tab上的文字
            textView.setText(episodes.getTabAt(i).getText());

        }

        episodes.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                //定义方法，判断是否选中
                int pos = Integer.parseInt(tab.getText().toString());
				updateTabView(tab, true);
				// 播放视频
               Intent intent = new Intent( listLinks.this , TvActivity.class);
               String url = episodeList.get(0)+episodeList.get(pos)+"/index.m3u8";

                try {
                    System.out.println(getUrl(url));
                    intent.putExtra("url",getUrl(url));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //grid.setSelection(pos);
                startActivity(intent);
                //
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //定义方法，判断是否选中
                updateTabView(tab, false);
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        

		
    }

    //用来改变tabLayout选中后的字体大小及颜色
    private void updateTabView(TabLayout.Tab tab, boolean isSelect) {
        //找到自定义视图的控件ID
        TextView tv_tab = tab.getCustomView().findViewById(R.id.tab_video_episodes_tv);
        if (isSelect) {
            //设置标签选中
            tv_tab.setSelected(true);
            //选中后字体
            tv_tab.setTextColor(getResources().getColor(R.color.ThemeColor));
        } else {
            //设置标签取消选中
            tv_tab.setSelected(false);
            //恢复为默认字体
            tv_tab.setTextColor(getResources().getColor(R.color.font_color));
        }
    }

    private String getUrl(String urls) throws IOException {
        String result = null;
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        Log.i("test",urls);
        try {
            URL url = new URL(urls);
            conn = (HttpURLConnection) url.openConnection();
//            conn.setRequestMethod("GET");
//            conn.setRequestProperty("User-agent", userAgent);
//            conn.setUseCaches(false);
//            conn.setConnectTimeout(DEF_CONN_TIMEOUT);
//            conn.setInstanceFollowRedirects(false);
            conn.connect();
            InputStream is = conn.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "utf-8"));

            String strRead = null;

            while ((strRead = reader.readLine()) != null) {
                result = strRead;
            }
            return urls.replace("index.m3u8",result);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return urls.replace("index.m3u8","1800k/hls/mixed.m3u8");
    }

}