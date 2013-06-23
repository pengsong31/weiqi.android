package com.weiqi.app.util.http;

import java.util.Map;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.weiqi.app.constant.WeiQiConstants;

public class AsyncHttpRequestUtils {

    private static AsyncHttpClient client   = new AsyncHttpClient();

    private static String getRequestUrl(String relativeUrl) {
        
        String requestUrl = WeiQiConstants.BASE_URL + relativeUrl;
        return requestUrl;
    }

    public static void get(String relativeUrl, Map<String, String> params, AsyncHttpResponseHandler responseHandler) {

        RequestParams requestParams = new RequestParams(params);

        client.get(getRequestUrl(relativeUrl), requestParams, responseHandler);
    }

    public static void post(String relativeUrl, Map<String, String> params, AsyncHttpResponseHandler responseHandler) {

        RequestParams requestParams = new RequestParams(params);

        client.post(getRequestUrl(relativeUrl), requestParams, responseHandler);
    }
    
}
