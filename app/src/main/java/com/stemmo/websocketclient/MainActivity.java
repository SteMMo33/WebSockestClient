package com.stemmo.websocketclient;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Applicativo per collegare un server websocket e generare una notifica.
 *
 */
public class MainActivity extends Activity {

    private WebSocketClient mWebSocketClient;
    private TextView tvStatus;
    private TextView tvMessages;
    private EditText edtServer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = findViewById(R.id.btnConnect);
        Button btnDisconnect = findViewById(R.id.btnDisconnect);
        Button btnSend = findViewById(R.id.btnSend);
        tvStatus = findViewById(R.id.tvStatus);
        tvMessages = findViewById(R.id.tvMessages);
        edtServer = findViewById(R.id.edtServer);

        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectWebSocket();
            }
        });

        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebSocketClient.close();
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebSocketClient.send("Ciao");
            }
        });

        edtServer.setText("192.168.5.69");
    }



    private void connectWebSocket() {

        String ip = edtServer.getText().toString();
        tvMessages.setText("Connecting to " + ip + "\n");

        URI uri;
        try {
            uri = new URI("ws://"+ip);
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        mWebSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

                tvStatus.setText("Status: Connected");

                Log.i("Websocket", "Opened");
                mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
            }

            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.tvMessages);
                        textView.setText(textView.getText() + "\n" + message);
                    }
                });
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                tvStatus.setText("Status: Disconnected");
                Log.i("Websocket", "Closed " + s);
            }

            @Override
            public void onError(Exception e) {
                Log.i("Websocket", "Error " + e.getMessage());
                tvMessages.setText(e.getMessage());
                tvStatus.setText("Status: Error");
            }
        };
        mWebSocketClient.connect();
    }
}
