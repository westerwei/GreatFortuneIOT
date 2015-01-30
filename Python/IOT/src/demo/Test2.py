'''
Created on 2014/12/15

@author: Wester
'''
import binascii
import os
import uuid
import urllib
import paho.mqtt.client as mqtt
from demo.IOTDemo import mqtt_on_connect, mqtt_on_message

client = mqtt.Client()
cmdClient = mqtt.Client()
client.on_connect = mqtt_on_connect
client.on_message = mqtt_on_message
# cmdClient.on_connect = cmdmqtt_on_connect
# cmdClient.on_message = cmdmqtt_on_message
def mqtt_on_connect(client, userdata, rc):
    print 'Connected with result code:' + str(rc)
    client.subscribe('testmqtt', 2)
    
    

def mqtt_on_message(client, userdata, msg):
    print msg.topic + ' : ' + str(msg.payload)
    
if __name__ == '__main__':
#     generated security token for API usage
    
    client.connect("192.168.1.37", port=1883, keepalive=60, bind_address="192.168.1.37")
    client.publish(topic='testmqtt', payload="{'aaa':'bbb','123':'456'}", qos=2)
    
    client.loop_forever()
    
    
    
    pass