import paho.mqtt.client as mqtt_client

def on_message(client, userdata, message, properties=None):
    print(" Received message " + str(message.payload) + " on topic '" + message.topic + "' with QoS " + str(message.qos))
