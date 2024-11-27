package com.example.appmqtt;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    private TextView mensView; // Para mostrar mensajes de MQTT
    private EditText mensOut; // Para escribir mensajes a enviar
    private static final String BROKER_URL = "test.mosquitto.org:1883"; // Broker MQTT
    private static final String TOPIC = "test/app"; // Tema MQTT
    private static final String TAG = "MQTT"; // Tag para logs

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        mensView = findViewById(R.id.msg_view);
        mensOut = findViewById(R.id.mandarmsg_input);
        Button subscribeButton = findViewById(R.id.subscribe_btn);
        Button sendButton = findViewById(R.id.enviar_button);

        // Configurar cliente MQTT
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), BROKER_URL, clientId);

        // Conectar al broker MQTT
        connectToBroker();

        // Botón para suscribirse al tema MQTT
        subscribeButton.setOnClickListener(v -> subscribeToTopic());

        // Botón para enviar un mensaje al tema MQTT
        sendButton.setOnClickListener(v -> {
            String message = mensOut.getText().toString();
            if (!message.isEmpty()) {
                publishMessage(message);
            } else {
                Log.w(TAG, "Intento de enviar un mensaje vacío");
            }
        });
    }

    // Conexión al broker MQTT con opciones
    private void connectToBroker() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conexión exitosa");
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Error al conectar", exception);
                    runOnUiThread(() -> mensView.setText("Error al conectar al broker MQTT"));
                }
            });
        } catch (MqttException e) {
            Log.e("MQTT", "Error al intentar conectar", e);
        } catch (Exception e) {
            Log.e("MQTT", "Excepción inesperada", e);
        }
    }
    // Suscribirse al tema MQTT
    private void subscribeToTopic() {
        try {
            client.subscribe(TOPIC, 0);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Conexión perdida", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    runOnUiThread(() -> mensView.append("\nMensaje recibido: " + message.toString()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Mensaje entregado");
                }
            });
            Log.d("MQTT", "Suscrito al tema: " + TOPIC);
        } catch (MqttException e) {
            Log.e("MQTT", "Error al suscribirse al tema", e);
        } catch (Exception e) {
            Log.e("MQTT", "Excepción inesperada durante la suscripción", e);
        }
    }

    // Publicar un mensaje al tema MQTT
    private void publishMessage(String message) {
        try {
            if (client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage();
                mqttMessage.setPayload(message.getBytes()); // Convertir mensaje a bytes
                client.publish(TOPIC, mqttMessage);
                Log.d(TAG, "Mensaje enviado: " + message);
            } else {
                Log.w(TAG, "No se puede enviar el mensaje, cliente MQTT no conectado");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error al enviar el mensaje: " + message, e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.unregisterResources();
            }
        } catch (MqttException e) {
            Log.e(TAG, "Error al desconectar el cliente MQTT", e);
        }
    }
}
