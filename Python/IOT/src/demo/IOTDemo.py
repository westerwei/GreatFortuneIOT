
from flask import Flask, url_for, Response
from flask import render_template
from flask import Request
from flask import request
from flask import json
import logging
from logging.handlers import RotatingFileHandler
from logging import StreamHandler
# import pymongo
from pymongo import MongoClient
from flask.json import jsonify
from bson import json_util
import requests
import base64
# from fileinput import filename
from flask import send_file
from io import BytesIO
# import tornado
import tornado.websocket
import tornado.wsgi
# from tornado import autoreload
# from ws4py.client.threadedclient import WebSocketClient
from ws4py.client.tornadoclient import TornadoWebSocketClient
from tornado import ioloop
# from tornado.websocket import websocket_connect
from tornado import gen
# from tornado.testing import AsyncHTTPTestCase, gen_test
import cv2
import FaceRec
import paho.mqtt.client as mqtt


app = Flask(__name__)
remoteHostIP = "192.168.1.101"
# remoteHostIP = "172.20.10.7"

@app.route('/user_device', methods=['POST'])
def registerDevice():
#   get POST JSON content   
    deviceJson = request.get_json(force=True)
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDevice
    app.logger.debug('phone:' + deviceJson["phone"])
#     deviceModel = collection.update({"phone" : deviceJson["phone"]}, {"$set" : deviceJson}, upsert=True)
    deviceModel = collection.find_and_modify({"phone" : deviceJson['phone']}, update=deviceJson, upsert=True, new=True)
#     postId = str(collection.insert(deviceJson))
    app.logger.debug('postId:' + str(deviceModel["_id"]) + " | " + deviceModel["deviceId"])
    postId = str(deviceModel["_id"])
    resp={
          'postId': postId,
          'result': 'success'
    }
    
    return jsonify(resp)

@app.route('/user_device/<phoneNum>/contacts', methods=['GET'])
def getContactList(phoneNum):
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDevice
    device = collection.find_one({"phone": phoneNum})
    if device:    
        adjJson = json.dumps(device["contacts"], default=json_util.default)
        return Response(adjJson, mimetype='application/json')
    else:
        return jsonify({'Error':'Data not found'})

@app.route('/user_device/<phoneNum>', methods=['DELETE'])
def unregisterDevice(phoneNum):
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDevice
    removeDoc = collection.remove({"phone" : phoneNum})
#     app.logger.debug(str(removeDoc["_id"]) + ' is removed') 
    resp={
          'phone': phoneNum,
          'result': 'success to unregister'
    }
    
    return jsonify(resp)

@app.route('/user_device/<phoneNum>/add_contact', methods=['GET'])
def addContactCmd(phoneNum):
    contactName = request.args.get('contactName')
    contact = request.args.get('contact')
    cmd = {
      'cmdType' : 'add_contact',    
      'contactName' : contactName,
      'contact' : contact 
    }
#     sendCommand(phoneNum, cmd)
    sendMQTTCommand(cmd)
    
    resp={
          'phone': phoneNum,
          'contact' : cmd,
          'result': 'success to add one contact'
    }
    
    return jsonify(resp);

@app.route('/user_device/<phoneNum>/take_pic', methods=['GET'])
def takePicCmd(phoneNum):
    cmd = {
      'cmdType' : 'take_pic',    
      'contactName' : '',
      'contact' : '' 
    }
#     sendCommand(phoneNum, cmd)
    sendMQTTCommand(cmd)
    resp={
          'phone': phoneNum,
          'contact' : cmd,
          'result': 'Already send take pic command'
    }
    
    return jsonify(resp);

@app.route('/user_device/<phoneNum>/cmd/<command>', methods=['GET'])
def runCmd(phoneNum, command):
    cmd = {
      'cmdType' : command,    
      'contactName' : '',
      'contact' : '' 
    }
#     sendCommand(phoneNum, cmd)
    sendMQTTCommand(cmd)
    resp={
          'phone': phoneNum,
          'contact' : '',
          'result': 'Already send the command[' + command + '] to ' + phoneNum 
    }
    
    return jsonify(resp);

@gen.engine
@app.route('/user_device/mediadata', methods=['POST'])
def postDeviceMedia():
#   get POST JSON content   
    mediaJson = request.get_json(force=True)
#     app.logger.debug("Media JSON:" + json.dumps(mediaJson))
    
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDeviceMedia
    app.logger.debug('phone:' + mediaJson["phone"])
#     deviceModel = collection.update({"phone" : deviceJson["phone"]}, {"$set" : deviceJson}, upsert=True)
#     deviceMediaModel = collection.find_and_modify({"phone" : mediaJson['phone']}, update=mediaJson, upsert=True, new=True)
    postId = str(collection.insert(mediaJson))
    
    fileData = mediaJson["mediaData"]
    file = base64.urlsafe_b64decode(fileData.encode('ascii'))
    
    app.logger.debug('postId:' + postId + " | " + mediaJson["mediaFile"])
#     postId = str(deviceMediaModel["_id"])
    resp={
          'postId': postId,
          'result': 'Success. Post ' + mediaJson["mediaType"] + " file [" + mediaJson["mediaFile"] + " to server"
    }
    
#     wsMsg = mediaJson["phone"] + "|" + mediaJson["mediaType"] + "|" + mediaJson["mediaFile"] 
    
#     modelFile = '/Users/Wester/Pictures/tmp/Gender-Data/Model/GenderModel.xml'
#     testImg = open('/tmp/' + mediaJson["mediaFile"], 'wb')
#     testImg.write(file)
#     FaceRec.predictOnePic(modelFile, testImg)
    
    return jsonify(resp)
    

@app.route('/user_device/generate/medias/<phoneNum>', methods=['GET'])
def genMediaFiles(phoneNum):
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDeviceMedia
    mediaFiles = collection.find({"phone": phoneNum})
    folder = "/Users/Wester/tmp/"
    if mediaFiles:    
        for media in mediaFiles:
            fileName = folder + media["mediaFile"]
            fileData = media["mediaData"]
            file = base64.urlsafe_b64decode(fileData)
            with open(fileName, mode='wb') as f:
                f.write(file)
            app.logger.debug("file " + fileName + " has been created!")
        resp={
          'result': 'Success. to generate media files'
        }
                
    else:
        resp={
          'result': 'There are no media files registered by this phone:' + phoneNum
        }
    
    return jsonify(resp);

@app.route('/user_device/<phoneNum>/mediadata/<mediaFile>', methods=['GET'])
def getMediaFile(phoneNum, mediaFile):    
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDeviceMedia
    mediaFile = collection.find_one({"phone": phoneNum, "mediaFile":mediaFile})
    if mediaFile:
        app.logger.info("Media file found!")
        fileData = mediaFile["mediaData"]
        '''
          Since python 3, Base64 will handle utf-8 
        '''
#         file = base64.urlsafe_b64decode(fileData)
        file = base64.urlsafe_b64decode(fileData.encode('ascii'))
        mime = "image/jpeg"
        if mediaFile["mediaType"] == 'video':
            mime = "video/mp4"    
        return send_file(BytesIO(file), mimetype=mime)
    else:
        return jsonify({"result" : "Media file not found"})

@app.route('/user_device/<phoneNum>/mediadata', methods=['GET'])
def getMediaFileList(phoneNum):    
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDeviceMedia
    mediaFileList = collection.find({"phone": phoneNum},{"_id":False,"phone":True, "mediaType":True, "mediaFile":True})
    adjJson = json_util.dumps(mediaFileList)
#     adjJson = json.dumps(mediaFileList, default=json_util.default)
    print 'Wester Test Python 2.7.8 print func '
    FaceRec.testModuleCall()
    return Response(adjJson, mimetype='application/json')

@app.route('/mqttTest/<msg>', methods=['GET'])
def testMQTT(msg):
    client.publish('mymqtt', payload=str(msg), qos=2)
    return jsonify({"result" : "Send message to MQTT"})
    
'''
Content-Type:application/json
Authorization:key=AIzaSyAu1-fa7jFH7t4PgwEK6y7CYyMvSSU6IUM

{ "data": {
    "score": "5x1",
    "time": "15:10"
  },
  "registration_ids": ["4", "8", "15", "16", "23", "42"]
}
'''

def sendMQTTCommand(command):
    app.logger.info(json.dumps(command))
    cmdClient.publish('cmdJSON', payload=json.dumps(command), qos=2)
    

def sendCommand(phoneNum, command):
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    collection = db.userDevice
    device = collection.find_one({"phone": phoneNum})
    clientRegId = device["regId"];
    payload = {
        "data" : command,
        "registration_ids" : [clientRegId]
    }
    headers = {
        'content-type': 'application/json',
        'Authorization': 'key=AIzaSyAu1-fa7jFH7t4PgwEK6y7CYyMvSSU6IUM'
    }
    r = requests.post("https://android.googleapis.com/gcm/send", data=json.dumps(payload), headers=headers)
    app.logger.info(r.text)

def reloadnotify():
    app.logger.info("Tornado app is reloading............")
#     print("Tornado app is reloading........") 

@app.route('/user_device/admin')
def adminTemplate(name=None):  
    app.logger.debug('in admin page')  
    client = MongoClient('localhost',27017)
    db = client.iotdemo
    allDevices = db.userDevice.find()
    
    return render_template("admin.html", allDevices=allDevices)

@app.route('/user_device/testjquery')
def jqueryTemplate(name=None):  
    return render_template("index.html")

def mqtt_on_connect(client, userdata, rc):
    print 'Connected with result code:' + str(rc)
    client.subscribe('mymqtt', 2)
    

def mqtt_on_message(client, userdata, msg):
    print msg.topic + ' : ' + str(msg.payload)
    
def cmdmqtt_on_connect(client, userdata, rc):
    print 'Command MQTT connected with result code:' + str(rc)
    client.subscribe('cmdJSON', 2)
    

def cmdmqtt_on_message(client, userdata, msg):
    print msg.topic + ' : ' + str(msg.payload)    

'''
Tornado WebSocket class
'''
class TestWebSocket(tornado.websocket.WebSocketHandler):
    clients = []
 
    def open(self):
        app.logger.debug("WebSocket is open~~")
        TestWebSocket.clients.append(self)
 
    def on_message(self, message):
        app.logger.debug("Msg is coming in~~~ ")
        for client in TestWebSocket.clients:
            client.write_message(message)
 
    def on_close(self):
        app.logger.debug("WebSocket is closed~~")
        TestWebSocket.clients.remove(self) 

class MediaAddNotify(tornado.websocket.WebSocketHandler):
    clients = []
 
    def open(self):
        app.logger.debug("MediaAddNotify is open~~ ")
        MediaAddNotify.clients.append(self)
 
    def on_message(self, message):
        app.logger.debug("MediaAddNotify msg is coming in~~~")
        if message == 'ping':
            message = 'ping_ok'        
        for client in MediaAddNotify.clients:
            client.write_message(message)
 
    def on_close(self):
        app.logger.debug("MediaAddNotify is closed~~")
        MediaAddNotify.clients.remove(self) 

class MediaAddNotifyClient(TornadoWebSocketClient):
    def check_origin(self, origin):
        return True
    
    def opened(self):
        self.send("ping")

    def received_message(self, m):
        app.logger.debug("Receive msg:" + m)

    def closed(self, code, reason=None):
        app.logger.debug("Close WS connection: " + str(code) + " | " + str(reason))

client = mqtt.Client()
cmdClient = mqtt.Client()
client.on_connect = mqtt_on_connect
client.on_message = mqtt_on_message
cmdClient.on_connect = cmdmqtt_on_connect
cmdClient.on_message = cmdmqtt_on_message
        
if __name__ == '__main__':
    print 'start to connect MQTT broker (in Demo package)'
    client.connect(remoteHostIP, port=1883, keepalive=60, bind_address=remoteHostIP)
    cmdClient.connect(remoteHostIP, port=1883, keepalive=60, bind_address=remoteHostIP)
    client.loop_start()
    cmdClient.loop_start()
    filehandler = RotatingFileHandler('/Software/tmp/log/iotdemo.log', maxBytes=100000, backupCount=1)
    filehandler.setLevel(logging.DEBUG)
    
    streamhandler = StreamHandler()
    streamhandler.setLevel(logging.DEBUG)
    
    app.logger.addHandler(filehandler)
    app.logger.addHandler(streamhandler)
    app.debug = True
#     app.run(host='192.168.1.11',port=1150, debug=True)
    tornado_app = tornado.web.Application([
        (r'/websocket/test', TestWebSocket),
        (r'/websocket/addmedia', MediaAddNotify),
        (r'.*', tornado.web.FallbackHandler, {'fallback': tornado.wsgi.WSGIContainer(app)})
    ], debug=True)
#     tornado_app.listen(1150, '192.168.1.11')
    tornado_app.listen(1151, remoteHostIP)
#     tornado_app.settings["debug"] = True
    ioloop = tornado.ioloop.IOLoop.instance()
    tornado.autoreload.add_reload_hook(reloadnotify)
    tornado.autoreload.start(ioloop)
    print 'Start Tornado+Flask'
    ioloop.start()