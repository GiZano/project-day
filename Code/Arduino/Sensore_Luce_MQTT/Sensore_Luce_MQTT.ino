#include <WiFiS3.h>
#include <PubSubClient.h>
#include "arduino_secrets.h"

// Define the values "SSID" and "SECRET_PASS" and "SECRET_IP" in the file "arduino_secrets.h"

const char* ssid = SECRET_SSID;
const char* password = SECRET_PASS;
const char* ip = SECRET_IP;

WiFiClient arduinoClient;
PubSubClient client(arduinoClient);
unsigned long lastMsg = 0;
#define MSG_BUFFER_SIZE	(50)
char msg[MSG_BUFFER_SIZE];
char vuoto[MSG_BUFFER_SIZE];

int value = 0;
int analogPIN = A3;
int ledPin = 5;
int onPin = 1;
int autoPin = 2;
int buzzerPin = 8;
int autoFlagPin = 0;

bool autoTurn = false;
bool led = false;

int valLight;

void setup_wifi() {

  delay(10);
  // We start by connecting to a WiFi network
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(ssid);

  WiFi.begin(ssid, password);

  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }

  randomSeed(micros());

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
  Serial.println();

  // Switch on the LED if an 1 was received as first character
  if ((char)payload[0] == '1') {
    digitalWrite(LED_BUILTIN, LOW);   // Turn the LED on (Note that LOW is the voltage level
    // but actually the LED is on; this is because
    // it is active low on the ESP-01)
  } else {
    digitalWrite(LED_BUILTIN, HIGH);  // Turn the LED off by making the voltage HIGH
  }

}

void reconnect() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection...");
    // Create a random client ID
    String clientId = "ArduinoBe";
    // Attempt to connect
    if (client.connect(clientId.c_str(), "LightSensor", "Arduino123")) {
      Serial.println("connected");
      // Once connected, publish an announcement...
      client.publish("connection/arduino", "hello world");
      // ... and resubscribe
      client.subscribe("inTopic");
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void setup() {
  pinMode(LED_BUILTIN, OUTPUT);     // Initialize the LED_BUILTIN pin as an output
  pinMode(ledPin, OUTPUT);
  pinMode(buzzerPin, OUTPUT);
  pinMode(autoFlagPin, OUTPUT);
  pinMode(onPin, INPUT);
  pinMode(autoPin, INPUT);

  Serial.begin(115200);
  setup_wifi();
  client.setServer(ip, 1883);
  
  client.setCallback(callback);
}

void loop() {

  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  for(int i = 0; i < MSG_BUFFER_SIZE; i++){
    msg[i] = vuoto[i];
  }

  // reads how much light there is in the room/environment
  valLight = analogRead(analogPIN);

  // creates the message to send via MQTT
  msg[0] = '{';
  msg[1] = 'l';
  msg[2] = 'i';
  msg[3] = 'g';
  msg[4] = 'h';
  msg[5] = 't';
  msg[6] = ':';

  String num = String(valLight);
  for(int i = 7; i < (num.length()+7); i++){
    msg[i] = num.charAt(i-7);
  }

  msg[7+num.length()] = '}';

  // send the message via MQTT
  unsigned long now = millis();
  if (now - lastMsg > 2000) {
    lastMsg = now;
    ++value;
    Serial.println("Publish message: ");
    Serial.println(msg);
    client.publish("v1/devices/me/telemetry", msg);
  }

  // if the "Auto" button is pressed, switch auto on/off
  if(digitalRead(autoPin)){
    digitalWrite(buzzerPin, HIGH);
    changeAuto();
  }

  // if "Led" button is pressed, switch leds on/off
  if(digitalRead(onPin)){
    changeLed();
    if(led){
      digitalWrite(ledPin, HIGH);
    }
    else{
      digitalWrite(ledPin, LOW);
    }
  }

  // if light value is higher than 450 and the auto mode is active, switch the leds off
  if(valLight > 450 && autoTurn){
    digitalWrite(ledPin, LOW);
  }

  delay(1000);
  digitalWrite(buzzerPin, LOW);
}

void changeAuto(){
  // switch auto between on and off
  if(autoTurn){
    autoTurn = false;
    digitalWrite(autoFlagPin, LOW);
    Serial.print("Switch auto off");
  }
  else{
    autoTurn = true;
    digitalWrite(autoFlagPin, HIGH);
    Serial.print("Switch auto on");
  }
}

void changeLed(){
  // switch leds between on and off
  if(led){
    led = false;
    Serial.print("Switch led off");
  }
  else{
    led = true;
    Serial.print("Switch led on");
  }
}
