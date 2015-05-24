package com.murraycole.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by User on 5/23/15.
 *
 *
 * A handler thread that takes in a token object
 */
public class ThumbnailDownloader<Token> extends HandlerThread {
    private static final String TAG = ThumbnailDownloader.class.getCanonicalName();
    private static final int MESSAGE_DOWNLOAD = 0;
    Handler mHandler;
    Map<Token, String> requestMap =
            Collections.synchronizedMap(new HashMap<Token, String>());
    Handler mResponseHandler;
    Listener<Token> mListener;

    public interface Listener<Token>{
        void onThumbnailDownloaded(Token token, Bitmap thumbnail);
    }

    public void setListener (Listener listener){
        mListener = listener;
    }

    public ThumbnailDownloader(Handler responseHandler){
        super(TAG);
        mResponseHandler = responseHandler;
    }

    @Override
    protected void onLooperPrepared() {
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg){
                if (msg.what == MESSAGE_DOWNLOAD){
                    Token token = (Token) msg.obj;
                    Log.i(TAG, "Got request for url: " + requestMap.get(token));
                    handleRequest(token);
                }
            }
        };

    }

    public void queueThumbnail(Token token, String url){
        Log.i(TAG, "Got a url: " + url);
        requestMap.put(token, url);

        mHandler.obtainMessage(MESSAGE_DOWNLOAD, token)
                .sendToTarget();

    }

    private void handleRequest(final Token token){
        try{
            final String url = requestMap.get(token);
            if (url == null){
                return;
            }

            byte[] bitmapbytes = new FlickrFetchr().getUrlBytes(url);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapbytes,0,bitmapbytes.length);

            mResponseHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(token) != url){
                        return;
                    }
                    requestMap.remove(token);
                    mListener.onThumbnailDownloaded(token,bitmap);
                }
            });

            Log.i(TAG, "bitmap created");
        } catch (IOException e){
            Log.i(TAG, "Error downloading image", e);
        }
    }

    public void clearQueue(){
        mHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}
