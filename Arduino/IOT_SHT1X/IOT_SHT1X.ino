#include <Process.h>
#include <SHT1x.h>
#include <Bridge.h>
#include <YunServer.h>
#include <YunClient.h>
#include <PubSubClient.h>

#define dataPin  10
#define clockPin 11
SHT1x sht1x(dataPin, clockPin);
byte server[] = { 192, 168, 1, 37 };
//byte server[] = { 172, 20, 10, 7 };
boolean isBlink = false;
int blinkDelay = 500;
char* mqttName = "ArduinoIOTClient";

void callback(char* topic, byte* payload, unsigned int length) {
  // handle message arrived
  //Serial.println("--------Get mqtt command notification:------------");
  String payloadStr = String((char*)payload);
  Serial.println("mqtt payload: " + payloadStr);
  int pos = payloadStr.indexOf(String(mqttName));
  String cmd = payloadStr.substring(0,pos);
  //Serial.println("iot cmd: " + cmd);
  int pipePos = cmd.indexOf("|");
  int secPipe = cmd.lastIndexOf("|");
  String isBlinkStr = cmd.substring(0,pipePos);
  //Serial.println("isBlinkStr:" + isBlinkStr);
  String delayStr = cmd.substring(pipePos+1, secPipe);
  Serial.println("delayStr:" + delayStr);
  blinkDelay = delayStr.toInt();
  if(isBlinkStr.equalsIgnoreCase("true")){
    isBlink = true;
  }
  else{
    isBlink = false;
  }
  
  //Serial.println(delayStr.toInt());
  //Serial.println("-----------------------------");
}

YunClient yunc;

PubSubClient mqttClient(server, 1883, callback, yunc);

void setup() {
  // put your setup code here, to run once:
  Bridge.begin();
  Serial.begin(9600);
  pinMode(13, OUTPUT);
  while(!Serial){
    ;
  }
  
  Serial.println("Hello IOT Client!");
  
  if(mqttClient.connect(mqttName)){
    mqttClient.subscribe("iotcmd");
    Serial.println("Subscribe mqtt success~");
  }
  //runCurl();  
}

void mqttConnect() {
  if (!mqttClient.connected()) {
    Serial.println("Connecting to broker");
    if (mqttClient.connect(mqttName)){
       mqttClient.subscribe("iotcmd");
       Serial.println("Subscribe mqtt again~");
    }else{
      Serial.println("Failed to connect!");
    }
  }
}

void loop() {
  mqttConnect();
  mqttClient.loop();
  checkBlink();
  // put your main code here, to run repeatedly:
  //digitalWrite(13, HIGH);   // turn the LED on (HIGH is the voltage level)
  //delay(1000);              // wait for a second
  //digitalWrite(13, LOW);    // turn the LED off by making the voltage LOW
  //delay(500);
  //sendLEDData();
  
  float temp_c;
  float temp_f;
  float humidity;

  // Read values from the sensor
  temp_c = sht1x.readTemperatureC();
  temp_f = sht1x.readTemperatureF();
  humidity = sht1x.readHumidity();

  // Print the values to the serial port
  Serial.print("Temperature: ");
  Serial.print(temp_c, 1);
  Serial.print("C / ");
  Serial.print(temp_f, 1);
  Serial.print("F. Humidity: ");
  Serial.print(humidity);
  Serial.println("%");
  
  sendHUMData(String(humidity,2));
  sendTEMData(temp_c, temp_f);
  
  delay(500);
}

void checkBlink(){
  Serial.println(isBlink);
  
  if(isBlink){
    for(int i = 0;i<10;i++){
      digitalWrite(13, HIGH);   // turn the LED on (HIGH is the voltage level)
      delay(blinkDelay);              // wait for a second
      digitalWrite(13, LOW);    // turn the LED off by making the voltage LOW
      delay(blinkDelay);
      Serial.println(i);
      Serial.println("Blink...");
    }
    
    sendLEDData();
    
    isBlink = false;
  }
  
}

void sendLEDData(){
  Process p;
  //p.begin("kkk");
  //p.addParameter(" LEDService LED_Light_Testing_Arduino");
  //p.run();
  p.runShellCommand("python /root/iotclient.py LEDService LED_Light_Arduino_1225");
  Serial.println("Python process to send LED Data has been triggered");
  
  //while(p.available()){
  //  char c = p.read();
  //  Serial.print(c);
  //}
  //Serial.println(p.available());
  Serial.flush();
}

void sendHUMData(String humData){
  Process p;
  //p.begin("kkk");
  //p.addParameter(" LEDService LED_Light_Testing_Arduino");
  //p.run();
  p.runShellCommand("python /root/iotclient.py HUMService " + String(humData));
  Serial.println("Send humidity data:" + String(humData));
  
//  while(p.available()){
//    char c = p.read();
//    Serial.print(c);
//  }
//  Serial.println(p.available());
  Serial.flush();
}

void sendTEMData(float temp_c, float temp_f){
  Process p;
  //p.begin("kkk");
  //p.addParameter(" LEDService LED_Light_Testing_Arduino");
  //p.run();
  p.runShellCommand("python /root/iotclient.py TEMService " + String(temp_c,5) + " " + String(temp_f,5));
  Serial.println("Send temperature data:" + String(temp_c,5) + " " + String(temp_f,5));
  
//  while(p.available()){
//    char c = p.read();
//    Serial.print(c);
//  }
//  Serial.println(p.available());
  Serial.flush();
}

void runCurl(){
  
  
  Process p;
  p.begin("curl");
  p.addParameter("http://arduino.cc/asciilogo.txt");
  p.run();
  
  while(p.available() > 0){
    char c = p.read();
    Serial.print(c);
  }
  
  Serial.flush();
  
}
