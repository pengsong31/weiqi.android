package com.weiqi.app.util.http;

import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;
import com.weiqi.app.constant.WeiQiConstants;

public abstract class BaseAsyncHttpResponseHandler extends AsyncHttpResponseHandler {

    @Override
    public void onFailure(Throwable error, String message) {
        Log.e("request http service cause an exception : ", error.toString());
        Log.e("request http service error and return message as : ", message);
        failure(WeiQiConstants.HTTP_FAILURE);
    }

    @Override
    public void onSuccess(String message) {
        sucess(WeiQiConstants.HTTP_SUCCESS, message);
    }

    protected abstract void failure(int errorCode);

    protected abstract void sucess(int successCoder, String message);

    
}
