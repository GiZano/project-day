import random
import time

from paho.mqtt import client as mqtt_client

server = "yourThingsBoardServerIp"
port = 1883
topic = "v1/devices/me/telemetry"
ClientID = "yourDeviceID"
username = "yourDeviceUsername"
password = "yourDevicePass"

def connect_mqtt():
    def on_connect(client, userdata, flags, rc, properties):
        if(rc == 0):
            print("Connected to MQTT Broker!")
        else:
            print("Failed to connect, reutnr code %d\n", rc)

    client = mqtt_client.Client(callback_api_version=mqtt_client.CallbackAPIVersion.VERSION2, client_id=ClientID)
    client.username_pw_set(username, password)
    client.on_connect = on_connect
    client.connect(server, port)
    return client

def publish(client):
    msg_count = 1
    while True:
        time.sleep(1)
        value = random.randint(15, 30)
        msgValue = "{temperature:" + str(value) + "}"
        msg = f"message number {msg_count}: {msgValue}"
        result = client.publish(topic, msgValue)
        status = result[0]
        if status == 0:
            print(f"Send '{msg}' to topic '{topic}'")
        else:
            print(f"Failed to send message to topic {topic}")
        msg_count += 1
        if msg_count > 1000:
            break

def run():
    client = connect_mqtt()
    client.loop_start()
    publish(client)
    client.loop_stop()

if __name__ == '__main__':
    run()










