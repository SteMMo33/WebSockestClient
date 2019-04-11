package com.stemmo.websocketclient;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Applicativo per collegare un server websocket, gestire una risposta JSON e generare una notifica.
 *
 * Aggiunto     implementation "org.java-websocket:Java-WebSocket:1.3.0" nel file app\build.gradle
 *
 * Per sincronizzare GIT remoto ho dato il comando 'git pull origin master --allow-unrelated-histories'
 *
 * Notifiche: https://developer.android.com/guide/topics/ui/notifiers/notifications.html
 * https://www.androidauthority.com/how-to-create-android-notifications-707254/
 *
 */
public class MainActivity extends Activity {

    private WebSocketClient mWebSocketClient;
    private TextView tvStatus;
    private TextView tvMessages;
    private EditText edtServer;
    Button btnConnect;
    Button btnDisconnect;
    Button btnSend;
    private View thisView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = findViewById(R.id.btnConnect);
        btnDisconnect = findViewById(R.id.btnDisconnect);
        btnSend = findViewById(R.id.btnSend);
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
        btnDisconnect.setEnabled(false);

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mWebSocketClient.send("firmware");
            }
        });
        btnSend.setEnabled(false);

        edtServer.setText("192.168.5.169");
        tvMessages.setText("");
    }


    private void AddToLog(String msg)
    {
        tvMessages.setText(tvMessages.getText() + msg);
    }

    private void connectWebSocket() {

        String ip = edtServer.getText().toString();
        tvMessages.setText("Connecting to " + ip + "..\n");

        URI uri;
        try {
            uri = new URI("ws://"+ip+":7681");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            AddToLog(e.getMessage());
            return;
        }

        // Creazione del websocket e definizione delle relative callbacks
        mWebSocketClient = new WebSocketClient(uri) {

            /**
             * Callback su apertura connessione
             * @param serverHandshake
             */
            @Override
            public void onOpen(ServerHandshake serverHandshake) {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btnConnect.setEnabled(false);
                        btnDisconnect.setEnabled(true);
                        btnSend.setEnabled(true);


                        AddToLog("Connected!\n");
                        tvStatus.setText("Status: Connected");

                        Log.i("Websocket", "Opened");
                        mWebSocketClient.send("Hello from " + Build.MANUFACTURER + " " + Build.MODEL);
                    }
                });
            }

            /**
             * Callback per la ricezione di un messaggio
             * @param s
             */
            @Override
            public void onMessage(String s) {
                final String message = s;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView textView = (TextView)findViewById(R.id.tvMessages);
                        textView.setText(textView.getText() + "\n" + message);

                        // Instantiate a JSON object from the request response
                        try {
                            JSONObject jsonObject = new JSONObject(message);
                            jsonObject.getString("v");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });

                sendNotification(null);
            }

            /**
             * Callback sulla chiusura del websocket
             * @param i
             * @param s
             * @param b
             */
            @Override
            public void onClose(int i, String s, boolean b) {
                Log.i("Websocket", "Closed: " + s);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvStatus.setText("Status: Disconnected");
                        AddToLog(".. disconnected");

                        btnConnect.setEnabled(true);
                        btnDisconnect.setEnabled(false);
                        btnSend.setEnabled(false);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                Log.e("Websocket", "Error: " + e.getMessage());
                final Exception ex = e;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AddToLog(ex.getMessage());
                        tvStatus.setText("Status: Error");

                        btnConnect.setEnabled(true);
                        btnDisconnect.setEnabled(false);
                        btnSend.setEnabled(false);
                    }
                });
            }
        };
        mWebSocketClient.connect();
    }

    /**
     * Funzione per generare una notifica
     * @param view
     */
    public void sendNotification(View view) {

        // prepare intent which is triggered if the
        // notification is selected

        Intent intent = new Intent(this, NotificationReceiver.class);
        // use System.currentTimeMillis() to have a unique ID for the pending intent
        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);

        // build notification
        // the addAction re-use the same intent to keep the example short
        Notification n  = new Notification.Builder(this)
                .setContentTitle("Notifica importante")
                .setContentText("Notifica da applicazione websocket")
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.icon, "Call", pIntent)
                .addAction(R.drawable.icon, "More", pIntent)
                .addAction(R.drawable.icon, "And more", pIntent).build();

        // Gets an instance of the NotificationManager service//
        NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // When you issue multiple notifications about the same type of event,
        // it’s best practice for your app to try to update an existing notification
        // with this new information, rather than immediately creating a new notification.
        // If you want to update this notification at a later date, you need to assign it an ID.
        // You can then use this ID whenever you issue a subsequent notification.
        // If the previous notification is still visible, the system will update this existing notification,
        // rather than create a new one. In this example, the notification’s ID is 001//
        // NotificationManager.notify().
        mNotificationManager.notify(001, n);
    }

}
