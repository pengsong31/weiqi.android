package com.weiqi.app.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;


public class HttpClientUtils {

	public static final int CONNECTION_TIMEOUT = 3000;// 连接超时时间 单位：毫秒
	private static final int SOCKET_TIMEOUT = 5000;
	public static final int REQUEST_TIMEOUT = 3000;// 请求响应超时时间 单位：毫秒
	public static String CHARSET = "UTF-8";
	
	public static String getHttpUrl(String url,
			Map<String, String> sendMapContents,int longTime) throws Exception {
		return callHttpService(url, longTime, 0, "GET", sendMapContents);
	}

	public static String getHttpUrl(String url, Map<String, String> sendMapContents)
			throws Exception {
		return callHttpService(url, SOCKET_TIMEOUT, 0, "GET",
				sendMapContents);
	}

	public static String postHttpUrl(String url, Map<String, String> sendMapContents)
			throws Exception {
		return callHttpService(url, SOCKET_TIMEOUT, 0, "POST",
				sendMapContents);
	}

	private static String callHttpService(String url, int socketTimeout, int retry,
			String method, Map<String, String> sendMapContents)
			throws Exception {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT,
				CONNECTION_TIMEOUT);
		if (socketTimeout > 0) {
			params.setParameter(CoreConnectionPNames.SO_TIMEOUT, socketTimeout);
		} else {
			params.setParameter(CoreConnectionPNames.SO_TIMEOUT,
					REQUEST_TIMEOUT);
		}
		httpClient.setParams(params);

		if (retry > 0) {
			final int fretryCount = retry;
			HttpRequestRetryHandler myRetryHandler = new HttpRequestRetryHandler() {
				public boolean retryRequest(IOException exception,
						int executionCount, HttpContext context) {
					if (executionCount >= fretryCount) {
						// 如果超过最大重试次数，那么就不要继续了
						return false;
					} else {
						return true;
					}
				}
			};

			httpClient.setHttpRequestRetryHandler(myRetryHandler);
		}

		HttpResponse response;

		List<NameValuePair> formParams = new ArrayList<NameValuePair>(); // 构建POST请求的表单参数
		if(sendMapContents != null){
			for (Map.Entry<String, String> entry : sendMapContents.entrySet()) {
				formParams.add(new BasicNameValuePair(entry.getKey(), entry
						.getValue()));
			}
		}
		
		// 根据method选择GET或POST请求
		if ("GET".equals(method)) {
			StringBuilder getUrl = new StringBuilder(url);
			getUrl.append("?");
			getUrl.append(URLEncodedUtils.format(formParams, CHARSET));
			HttpGet httpGet = new HttpGet(getUrl.toString());
			response = httpClient.execute(httpGet);
		} else {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setEntity(new UrlEncodedFormEntity(formParams, CHARSET));
			response = httpClient.execute(httpPost);
		}

		if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
			throw new HttpException("status code not 200,but "
					+ response.getStatusLine().getStatusCode());
		}
		String result = EntityUtils.toString(response.getEntity(), CHARSET);
		
		return result;
	}
}
