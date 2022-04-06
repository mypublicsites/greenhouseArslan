package com.example.greenhouseapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MQTT";
    final static String MQTT_HOST = "tcp://192.168.180.84:1883";  // your MQTT broker. in this example http://www.mqtt-dashboard.com/index.html was used
    final static String TOPIC = "A00243808/group";

    private MqttAndroidClient mqttAndroidClient;
    private String clientId;
    private TextView isConnectedTv, incomingMessageTv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        isConnectedTv = findViewById(R.id.is_connected_tv);
        incomingMessageTv = findViewById(R.id.latest_message_incoming_tv);

       // messageET = findViewById(R.id.message_et);

        clientId = MqttClient.generateClientId();  // set randomly generated client identifier

        mqttAndroidClient = new MqttAndroidClient(this, MQTT_HOST, clientId); // MqttAndroidClient that can be used to communicate with an MQTT server
        mqttAndroidClient.setCallback(mqttCallback); // Sets a callback listener to use for events such as : new message arrived,connection has been lost,..

        Button myButton = findViewById(R.id.cloud_btn);


    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume fired");
        connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy fired");
        try {
            mqttAndroidClient.unregisterResources();
            mqttAndroidClient.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    private void connect() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);                 // Sets whether the client will automatically attempt to reconnect to the server if the connection is lost.
        mqttConnectOptions.setCleanSession(true);                       // Sets whether the client and server should remember state across restarts and reconnects
        mqttConnectOptions.setConnectionTimeout(3);                     // Sets the connection timeout value in seconds
        mqttConnectOptions.setKeepAliveInterval(60);                    // defines the maximum connection open time in seconds interval between messages sent or received

        try {
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    //DisconnectedBufferOptions holds the set of options that govern the behaviour of Offline (or Disconnected) buffering of messages
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);

                    Log.d(TAG, "onSuccess: Connected");

                    isConnectedTv.setText("Connected");

                    subscribe(TOPIC);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "onFailure: " + exception.getMessage(),exception);
                    isConnectedTv.setText("Failed to connect:\n"+exception.getMessage());
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
            isConnectedTv.setText("Exception on connect:\n"+ex.getMessage());
        }
    }

    private void subscribe(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "onSuccess subscribed to : "+topic);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e(TAG, "onFailure subscribing to : "+exception.getMessage(),exception);
                }
            });
        } catch (MqttException ex) {
            Log.e(TAG, "subscribe: "+ex.getMessage(),ex);
            ex.printStackTrace();
        }
    }

    private MqttCallbackExtended mqttCallback = new MqttCallbackExtended() {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {

        }

        @Override
        public void connectionLost(Throwable cause) {

        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            String[] messageInTv = incomingMessageTv.getText().toString().split(topic);
            String temp = "";
            String stringPayload = new String(message.getPayload(), StandardCharsets.UTF_8);
//            do{
//                temp += message + "\n";
//            }while(!TextUtils.isEmpty((CharSequence) message));

            StringBuilder sb = new StringBuilder(messageInTv.length >1 ? messageInTv[1] : "");
            sb.append('\n');
            sb.append(stringPayload);

            incomingMessageTv.setText(String.format("Topic : %s%s",topic,sb.toString()));
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {

        }
    };

    public void goCloud (View view) {
        goToUrl ( "https://studentait-my.sharepoint.com/:f:/g/personal/a00243808_student_ait_ie/Enuj2MkxtBFFtPkAL9jsSnoBe209EZyliaAc4BrCD8KIbw?e=cCjdD5");
    }

    private void goToUrl (String url) {
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }
}