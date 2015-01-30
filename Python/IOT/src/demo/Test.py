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
from passlib.hash import pbkdf2_sha256 #@UnresolvedImport
import smtplib
from email.mime.text import MIMEText

client = mqtt.Client()
cmdClient = mqtt.Client()
client.on_connect = mqtt_on_connect
client.on_message = mqtt_on_message
# cmdClient.on_connect = cmdmqtt_on_connect
# cmdClient.on_message = cmdmqtt_on_message
def mqtt_on_connect(client, userdata, rc):
    print 'Connected with result code:' + str(rc)
    client.subscribe('testmqtt', 2)
    client.publish(topic='testmqtt', payload="{'aaa':'bbb','123':'456'}", qos=2)
    

def mqtt_on_message(client, userdata, msg):
    print msg.topic + ' : ' + str(msg.payload)
    
if __name__ == '__main__':
#     generated security token for API usage
    randomStr = binascii.hexlify(os.urandom(26))
    randomStr2 = binascii.hexlify(os.urandom(26))
    print randomStr
    print randomStr2
    
    print str(uuid.uuid4()).replace('-', '')
    print urllib.urlencode({'redirect_uri':'http://192.168.1.100:6190/iotapp/oauth/redirect'})
    
#     hashPass = pbkdf2_sha256.encrypt("abcd1234", rounds=200000, salt_size=16)
    passHash="$pbkdf2-sha256$200000$HMMY4xwjREipNeZ8710rxQ$iDBdI876rvbFOKLpID5U8cuvg28BbrNcVH219sPHags"
    passVerify = pbkdf2_sha256.verify('anock0511', passHash)
    print "Pass Hash verify result: " + str(passVerify)
#     print "abcd1234 hashing result: " + hashPass
    
#     client.connect("192.168.1.37", port=1883, keepalive=60, bind_address="192.168.1.37")
    
#     client.loop_forever()
    msg = MIMEText("Wester Test in Python")
    msg['Subject'] = 'Trigger from python unit test'
    msg['From'] = 'wester.wei@gmail.com'
    msg['To'] = 'wester.wei@gmail.com'
    
    s = smtplib.SMTP('localhost')
    s.sendmail('wester.wei@gmail.com', 'wester.wei@gmail.com', msg.as_string())
    s.quit()
    
    
    pass