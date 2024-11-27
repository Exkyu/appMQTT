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
    private TextView messageView; // Para mostrar mensajes de MQTT
    private EditText messageInput; // Para escribir mensajes a enviar
    private static final String TOPIC = "test/app"; // Tema MQTT

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialización de vistas
        messageView = findViewById(R.id.msg_view);
        messageInput = findViewById(R.id.mandarmsg_input);
        Button subscribeButton = findViewById(R.id.subscribe_btn);
        Button sendButton = findViewById(R.id.enviar_button);

        // Configurar cliente MQTT
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), "tcp://test.mosquitto.org:1883", clientId);

        // Conectar al broker MQTT
        connectToBroker();

        // Botón para suscribirse al tema MQTT
        subscribeButton.setOnClickListener(v -> subscribeToTopic());

        // Botón para enviar un mensaje al tema MQTT
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString();
            if (!message.isEmpty()) {
                publishMessage(message);
            }
        });
    }

    // Conexión al broker MQTT con opciones
    private void connectToBroker() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true); // Restablecer sesión para evitar mensajes no deseados
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MQTT", "Conexión exitosa");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.e("MQTT", "Error al conectar", exception);
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Suscribirse al tema MQTT
    private void subscribeToTopic() {
        try {
            client.subscribe(TOPIC, 0); // Nivel de QoS 0
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e("MQTT", "Conexión perdida", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // Mostrar mensaje recibido
                    runOnUiThread(() -> messageView.setText("Mensaje: " + message.toString()));
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d("MQTT", "Mensaje entregado");
                }
            });
            Log.d("MQTT", "Suscrito al tema: " + TOPIC);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    // Publicar un mensaje al tema MQTT
    private void publishMessage(String message) {
        try {
            MqttMessage mqttMessage = new MqttMessage();
            mqttMessage.setPayload(message.getBytes()); // Convertir mensaje a bytes
            client.publish(TOPIC, mqttMessage);
            Log.d("MQTT", "Mensaje enviado: " + message);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (client != null) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}