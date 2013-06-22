package com.weiqi.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.weiqi.app.util.HttpClientUtils;
import com.weiqi.app.util.LazyImageLoader;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends Activity implements Callback {

	private TextView tv;

	private Handler handler;

	private String result;
	
	private ImageView imageView;
	
	private TextView companyName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		handler = new Handler(this);
		tv = (TextView) findViewById(R.id.txt);
		companyName = (TextView) findViewById(R.id.companyName);
		imageView = (ImageView)findViewById(R.id.image);
		new MainPageLoader().execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private class MainPageLoader extends AsyncTask<String, String, Integer> {

		@Override
		protected Integer doInBackground(String... params) {

			try {
				Map<String,String> param = new HashMap<String,String>(){
					{
						this.put("appuuid", 11+"");
						this.put("ddd", 2+"");
					}
				};
				result = HttpClientUtils.getHttpUrl("http://10.17.218.9/weiqi/app/page", param);
				return 0;
			} catch (Exception e) {
				Log.e("", e.toString());
			}
			return 1;
		}
		
		@Override
		protected void onPostExecute(Integer result) {
			handler.sendEmptyMessage(result);
		}

	}

	@Override
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case 0:
			try {
				JSONObject json = new JSONObject(result);
				tv.setText(json.getString("introduceDetail"));
				companyName.setText(json.getString("companyName"));
				String url = "http://10.17.218.9/weiqi/" + json.getString("introduceImage");
				LazyImageLoader.getInstance(this).displayImage(url,
						imageView, 800, 800,
						R.drawable.ic_launcher);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			break;
		case 1:

			tv.setText("sorry,error!");
			break;
		}
		return false;
	}
	
	
}
