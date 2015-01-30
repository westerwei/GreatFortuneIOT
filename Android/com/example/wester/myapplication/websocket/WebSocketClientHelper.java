package com.example.wester.myapplication.websocket;

import android.util.Log;

import com.example.wester.myapplication.AppConstants;

import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by Wester on 2014/11/13.
 */
public class WebSocketClientHelper {

    private static String TAG = "WebSocket";

    private static WebSocketClient mClient;

    public static WebSocketClient getWebSocketClientInstance(){

        if(mClient != null && mClient.getConnection().isOpen()){
            return mClient;
        }

        URI uri;
        try {
//            uri = new URI("ws://192.168.1.11:1150/websocket/addmedia");
//            uri = new URI("ws://" + AppConstants.remoteHostIP + ":1150/websocket/addmedia");
            uri = new URI("ws://" + AppConstants.remoteHostIP + ":1150/iotapp/ws/addmedia");
        } catch (URISyntaxException e) {
            Log.e(TAG, "Create WebSocket connection fail....", e);
            return null;
        }

        mClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                Log.i(TAG, "Connect to media notification WebSocket");
                this.send("ping");
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "Get message from media notification WS: " + message);
//                final String message = s;
//                runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        TextView textView = (TextView)findViewById(R.id.messages);
//                        textView.setText(textView.getText() + "\n" + message);
//                    }
//                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                Log.d(TAG, "Close media notification WebSocket");
            }

            @Override
            public void onError(Exception ex) {
                Log.e(TAG, "Fail to run the WS client for media notification", ex);
            }
        };

        try {
            mClient.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return mClient;
    }

    public static void sendMediaNotification(String phone, String mediaType, String mediaFile){
        try{
            Log.i(TAG, "Prepare to connect media notification WS");
            String wsMsg = phone + "|" + mediaType + "|" + mediaFile;
            WebSocketClient client = getWebSocketClientInstance();
            Log.i(TAG, "Prepare to send message to media notification WS");
            client.send(wsMsg);
            Log.i(TAG, "Success to send message");
        }
        catch(Exception e){
            Log.e(TAG, "Send WS message fail....", e);
        }


    }
}
