package com.weiqi.app;

import java.util.HashMap;
import java.util.Map;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;
import com.weiqi.app.constant.WeiQiConstants;
import com.weiqi.app.util.LazyImageLoader;
import com.weiqi.app.util.http.AsyncHttpRequestUtils;
import com.weiqi.app.util.http.BaseAsyncHttpResponseHandler;

public class MainActivity extends Activity {

    private TextView  tv;

    private ImageView imageView;

    private TextView  companyName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView) findViewById(R.id.txt);
        companyName = (TextView) findViewById(R.id.companyName);
        imageView = (ImageView) findViewById(R.id.image);
        new MainPageLoader().doInBackground();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private class MainPageLoader {

        
        protected void doInBackground(String... params) {

            Map<String, String> param = new HashMap<String, String>() {
                {
                    this.put("appuuid", 11 + "");
                    this.put("ddd", 2 + "");
                }
            };

            AsyncHttpRequestUtils.get("/app/page", param, new BaseAsyncHttpResponseHandler() {

                @Override
                protected void failure(int errorCode) {
                    tv.setText("sorry,error!");
                }

                @Override
                protected void sucess(int successCode, String message) {
                    try {
                        JSONObject json = new JSONObject(message);
                        tv.setText(json.getString("introduceDetail"));
                        companyName.setText(json.getString("companyName"));
                        String url = WeiQiConstants.BASE_URL + "/"+ json.getString("introduceImage");
                        LazyImageLoader.getInstance(MainActivity.this).displayImage(url, imageView, 800, 800, R.drawable.ic_launcher);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                }

            });
        }

    }

}
