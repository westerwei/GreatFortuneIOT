'''
Created on 2014/12/11

@author: Wester
'''
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
from bson import json_util, BSON
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
import paho.mqtt.client as mqtt
import hashlib
import binascii
import os
import uuid
from werkzeug.security import gen_salt
import simplejson
import datetime
from datetime import timedelta
from flask_oauthlib.provider import OAuth2Provider
from flask.app import Flask
import sys
import urllib
from DataAccess.iotmodel import Client
from DataAccess.iotmodel import Token
from DataAccess.iotmodel import Grant
from DataAccess.iotmodel import User
from DataAccess import iotmodel
import importlib
from DataConsumption import ObserverLocator
# from demo.IOTDemo import cmdmqtt_on_connect, cmdmqtt_on_message
import pymongo
from bson.objectid import ObjectId
from passlib.context import CryptContext #@UnusedImport
# Passlib use lazy import for each hashing implementation class,
# therefore import pbkdf2_sha256 will cause PyDev showing "unresolved import error"
# just ignore error from this import statement line, but need to make sure you use
# correct hash algorithm name --- by Wester 
from passlib.hash import pbkdf2_sha256 #@UnresolvedImport


app = Flask(__name__)

remoteHostIP = "wester_macair.com"
# remoteHostIP = "172.20.10.7"
oauth = OAuth2Provider()

@app.route('/iotapp/restgettest', methods=['GET'])
def restgettest():
    print request.args.get('param')
    ws.send(request.args.get('param'))
    return jsonify({'Result':'Success to call REST GET API......'})

@app.route('/iotapp/restposttest', methods=['POST'])
def restposttest():
    jsonObj = request.get_json(force=True)
    return jsonify({'Result':'Success to call REST POST API......', 'data':jsonObj})

@app.route('/iotapp/resttest', methods=['GET'])
def resttest():
    headers = {
        'User-Agent': 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/39.0.2171.95 Safari/537.36'
    }
    
#     req = requests.request('GET', 'http://192.168.1.101:6190/iotapp/oauth/authorize?client_id=hGGRcSeD4nWHkMr9QQsz2zR5LpmoyKkfNQxeytRU&redirect_uri=http%3A%2F%2F192.168.1.101%3A6190%2Fiotapp%2Foauth%2Fredirect&response_type=code&scope=iot')
    
#     r = requests.get("http://" + remoteHostIP + ":1150/iotapp/oauth/authorize?client_id=hGGRcSeD4nWHkMr9QQsz2zR5LpmoyKkfNQxeytRU&redirect_uri=http%3A%2F%2F" + remoteHostIP + "%3A1150%2Fiotapp%2Foauth%2Fredirect&response_type=code&scope=iot", allow_redirects=False, headers=headers)
    r = requests.get("http://" + remoteHostIP + ":1150/iotapp/oauth/authorize?client_id=hGGRcSeD4nWHkMr9QQsz2zR5LpmoyKkfNQxeytRU&redirect_uri=http%3A%2F%2F" + remoteHostIP + "%3A1150%2Fiotapp%2Foauth%2Fredirect&response_type=code&scope=iot")
#     r = requests.get("http://" + remoteHostIP + ":6190/iotapp/resttestlocal", allow_redirects=False, headers=headers);
    app.logger.debug("resp status: " + str(r.status_code))
    
    
    return jsonify({'Result':'Success to call REST API......', 'auth_resp' : r.json()})

@app.route('/iotapp/customer/register', methods=['POST'])
def registerCustomer():
    app.logger.debug("Trigger customer registration")
    jsonObj = request.get_json(force=True)
    client = MongoClient('localhost',27017)
    db = client.iotdb
    custCol = db.customer
    clientAppCol = db.ClientApp
#     apiclientId = str(uuid.uuid4()).replace('-', '')
#     apisecr = str(uuid.uuid4()).replace('-', '')
    customerModel = custCol.find_and_modify({"CustID" : jsonObj['CustID']}, update=jsonObj, upsert=True, new=True)
    
    app.logger.debug("Create/Update customer info success!")
    
    client_id_val=gen_salt(40)
    client_secret_val=gen_salt(50)
    clientJson = {
        'client_id' : client_id_val,
        'client_secret' : client_secret_val,
        'client_type' : 'public' , # public or confidential
        'redirect_uris' : 'http://' + remoteHostIP + ':1150/iotapp/oauth/redirect',
        'default_redirect_uri' : 'http://' + remoteHostIP + ':1150/iotapp/oauth/redirect',
        'default_scopes' : ' iot', # defined by 
        'cust_id' : jsonObj['CustID'],
        'user_id' : jsonObj['CustID']
    }
    cnt = clientAppCol.find({"cust_id" : jsonObj['CustID']}).count()
    if cnt > 0:
        defaultClient = clientAppCol.find_one({"cust_id" : jsonObj['CustID']})
        clientJson['client_id'] = defaultClient['client_id']
        clientJson['client_secret'] = defaultClient['client_secret']
        
        
    clientApp = clientAppCol.find_and_modify({"cust_id" : jsonObj['CustID']}, update=clientJson, upsert=True, new=True)
    app.logger.debug("Create default client app success")
    
    customerModel['ClientApp'] = clientApp
    
    resultJson = json.dumps(customerModel, default=json_util.default)
    return Response(resultJson, mimetype='application/json')

@app.route('/iotapp/oauth/get_oauth_token')
def get_oauth_token():
#     call authorization API to get auth code
    client_id = request.args.get('client_id')
#     client_secret = request.args.get('client_secret')
    mongoClient = MongoClient('localhost',27017)
    db = mongoClient.iotdb
    clientAppCol = db.ClientApp
#     apiclientId = str(uuid.uuid4()).replace('-', '')
#     apisecr = str(uuid.uuid4()).replace('-', '')
    clientApp = clientAppCol.find_one({"client_id" : client_id})
    client_secret = clientApp["client_secret"]
    app.logger.debug("client_secret: " + client_secret)
    
#     app.logger.debug("http://" + remoteHostIP + ":1150/iotapp/oauth/authorize?client_id=" + client_id + "&redirect_uri=http%3A%2F%2F" + remoteHostIP + "%3A1150%2Fiotapp%2Foauth%2Fredirect&response_type=code&scope=iot")
    
    r = requests.get("http://" + remoteHostIP + ":1150/iotapp/oauth/authorize?client_id=" + client_id + "&redirect_uri=http%3A%2F%2F" + remoteHostIP + "%3A1150%2Fiotapp%2Foauth%2Fredirect&response_type=code&scope=iot")
    
    app.logger.debug("OAuth auth response: " + str(r.status_code) + " | " + r.text)
    code_str = r.json()['auth_code']
    app.logger.debug("OAuth auth code: " + code_str)
    r_token = requests.get("http://" + remoteHostIP + ":1150/iotapp/oauth/token?code=" + code_str + "&grant_type=authorization_code&client_id=" + client_id + "&client_secret=" + client_secret + "&redirect_uri=http%3A%2F%2F" + remoteHostIP + "%3A1150%2Fiotapp%2Foauth%2Fredirect")
    
    resultJson = json.dumps(r_token.json(), default=json_util.default)
    return Response(resultJson, mimetype='application/json')
#     return r_token.json()
# use auth code to call token API to get actual token for OAuth 2.0 spec
    
#     return None

@oauth.clientgetter
def load_client(client_id):
    app.logger.debug("Load App client info: " + client_id)
    client = MongoClient('localhost',27017)
    db = client.iotdb
    clientAppCol = db.ClientApp
    lookupClient = clientAppCol.find_one({"client_id" : client_id})
    app.logger.debug("lookupClient: " + lookupClient['client_secret'])
    
    if lookupClient:
        clientResult = Client()
        clientResult.client_id = lookupClient['client_id']
        clientResult.client_secret = lookupClient['client_secret']
        clientResult.client_type = lookupClient['client_type']
        clientResult.redirect_uris = lookupClient['redirect_uris'].split()
        clientResult.default_redirect_uri = lookupClient['default_redirect_uri']
        clientResult.default_scopes = lookupClient['default_scopes'].split()
        clientResult.cust_id = lookupClient['cust_id']
        clientResult.user_id = lookupClient['user_id']
        
        return clientResult
    else:
        return None

@oauth.grantgetter
def load_grant(client_id, code):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    grantCol = db.Grant
    lookupGrant = grantCol.find_one({"client_id" : client_id, "code" : code})
    if lookupGrant:
        user = User()
        user.user_id = lookupGrant['user']
        user.userName = lookupGrant['user']
        grantResult = Grant()
        grantResult.client_id = lookupGrant['client_id']
        grantResult.code = lookupGrant['code']
        grantResult.redirect_uri = lookupGrant['redirect_uri']
        grantResult.scopes = lookupGrant['scopes'].split()
        grantResult.user = user
        grantResult.expires = lookupGrant['expires']
        grantResult.objid = str(lookupGrant['_id'])
    
        return grantResult
    else:
        return None
#     return lookupGrant

@oauth.grantsetter
def save_grant(client_id, code, request, *args, **kwargs):
    # decide the expires time yourself
    expires = datetime.datetime.utcnow() + timedelta(seconds=100)
    grant = {
        'client_id' : client_id,
        'code' : code['code'],
        'redirect_uri' : request.redirect_uri,
        'scopes' : ' '.join(request.scopes),
        'user' : 'IOTAPP',
        'expires' : expires
    }
    user = User()
    user.user_id = 'IOTAPP'
    user.userName = 'IOTAPP'
    client = MongoClient('localhost',27017)
    db = client.iotdb
    grantCol = db.Grant
    grantCol.insert(grant)
    
    grantResult = Grant()
    grantResult.client_id = grant['client_id']
    grantResult.code = grant['code']
    grantResult.redirect_uri = grant['redirect_uri']
    grantResult.scopes = grant['scopes'].split()
    grantResult.user = user
    grantResult.expires = grant['expires']
    grantResult.objid = str(grant['_id'])
    
    return grantResult

@oauth.tokengetter
def load_token(access_token=None, refresh_token=None):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    tokenCol = db.Token
    tok = None
    
    if access_token:
        tok = tokenCol.find_one({"access_token" : access_token})
    elif refresh_token:
        tok = tokenCol.find_one({"refresh_token" : refresh_token})
    
    if tok:
        tokResult = Token()
        user = User()
        user.user_id = 'IOTAPP'
        user.userName = 'IOTAPP'
        tokResult.access_token = tok['access_token']
        tokResult.refresh_token = tok['refresh_token']
        tokResult.token_type = tok['token_type']
        tokResult.scopes = tok['scopes'].split()
        tokResult.expires = tok['expires']
        tokResult.client_id = tok['client_id']
        tokResult.user_id = tok['user_id']
        tokResult.user = user
        return tokResult
    else:
        return None

@oauth.tokensetter
def save_token(token, request, *args, **kwargs):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    tokenCol = db.Token
    # make sure that every client has only one token connected to a user
#     tokenCol.remove({"client_id" : request.client.client_id})
    # Default value of 'expires_in' is 3600 seconds -- one hour for token expiration
    expires_in = token.pop('expires_in') 
    expires = datetime.datetime.utcnow() + timedelta(seconds=expires_in)

    tok = {
        'access_token': token['access_token'],
        'refresh_token' : token['refresh_token'],
        'token_type': token['token_type'],
        'scopes' : token['scope'],
        'expires' : expires,
        'client_id' : request.client.client_id,
        'user_id' :request.user.user_id
    }
    tokenCol.insert(tok)
    
    tokResult = Token()
    user = User()
    user.user_id = 'IOTAPP'
    user.userName = 'IOTAPP'
    tokResult.access_token = token['access_token']
    tokResult.refresh_token = token['refresh_token']
    tokResult.token_type = token['token_type']
    tokResult.scopes = token['scope'].split()
    tokResult.expires = expires
    tokResult.client_id = request.client.client_id
    tokResult.user_id = request.user.user_id
    tokResult.user = user
    
    return tokResult

@app.route('/iotapp/api/test')
@oauth.require_oauth()
def me():
    return jsonify({'result' : 'Call api through OAuth'})

@app.route('/iotapp/device/register', methods=['POST'])
def registerDevice():
#     customerID = request.args.get('customerID')
#     deviceSerialNO = request.args.get('deviceSerialNO')
#     deviceLocation = request.args.get('deviceLocation')
#     deviceIP = request.args.get('deviceIP')
#     deviceVendor = request.args.get('deviceVendor')
#     deviceType = request.args.get('deviceType')
#     serviceType = request.args.get('serviceType')
#     notifyToken = request.args.get('notifyToken')
    secureToken = request.args.get('secureToken')
    if secureToken is None:
        secureToken = gen_salt(40)
    
    
#     parentObj = request.args.get('parentObj')
#     routerObj = request.args.get('routerObj')
#     status = 'enable'
    registerData = request.get_json(force=True)
    
    client = MongoClient('localhost',27017)
    db = client.iotdb
    deviceCol = db.DeviceMetadata
    
#     jsonObj = {
#        "customerID": customerID,
#        "deviceSerialNO" : deviceSerialNO,
#        "deviceLocation" : deviceLocation,
#        "deviceIP" : deviceIP,
#        "deviceVendor" : deviceVendor,
#        "deviceType" : deviceType,
#        "serviceType" : serviceType,
#        "notifyToken" : notifyToken,
#        "secureToken" : secureToken,
#        "parentObj" : parentObj,
#        "routerObj" : routerObj,
#        "status" : status
#     }
#     existDevice = deviceCol.find_one({"customerID" : customerID, "deviceSerialNO": deviceSerialNO, "deviceLocation": deviceLocation })
    
#     registerData['status'] = status
    registerData['secureToken'] = secureToken    
#     deviceModel = deviceCol.find_and_modify({"customerID" : customerID, "deviceSerialNO": deviceSerialNO, "deviceLocation": deviceLocation }, 
#                                             update=jsonObj, upsert=True, new=True)
    
    deviceModel = deviceCol.find_and_modify({"customerID" : registerData['customerID'], "deviceSerialNO": registerData['deviceSerialNO'], "deviceLocation": registerData['deviceLocation'] }, 
                                            update=registerData, upsert=True, new=True)
    
    return jsonify({'result' : 'success', 'secureToken': secureToken})

@app.route('/iotapp/device/data/<deviceSerialNO>/<secureToken>' , methods=['POST'])
def sendDeviceData(deviceSerialNO, secureToken):
    '''
    Temporary not to check status, just insert data
    '''
    client = MongoClient('localhost',27017)
    db = client.iotdb
    deviceCol = db.DeviceMetadata
    device = deviceCol.find_one({'secureToken':secureToken, 'deviceSerialNO':deviceSerialNO})
    serviceType = device["serviceType"]
    
    curTime = datetime.datetime.now()
    data = request.get_json(force=True)
    deviceData = {
      "deviceSerialNO" : deviceSerialNO,
      "secureToken" : secureToken,
      "DeviceData" : data,
      "timestamp" : curTime,
      "deviceObjID" : str(device["_id"]),
      "serviceType" : serviceType
    }
    
    ObserverLocator.locate_service(serviceType, deviceData, cmdClient)
    
    # Use WebSocket to notify client browser
    ws.send(payload=json.dumps(deviceData, default=json_util.default), binary=False)
    return jsonify({'result' : 'success'})

@app.route('/iotapp/testmqtt/<isBlink>/<delayTime>' , methods=['GET'])
def testmqtt(isBlink, delayTime):
    payload = str(isBlink) + '|' + str(delayTime)
    cmdClient.publish('iotcmd', payload=payload, qos=2)
    return jsonify({'mqtttest':'Pls check log.....'})

def cmdmqtt_on_connect(client, userdata, rc):
    print 'Command MQTT connected with result code:' + str(rc)
    client.subscribe('iotcmd', 2)
    

def cmdmqtt_on_message(client, userdata, msg):
    print msg.topic + ' : ' + str(msg.payload)

# @app.route('/iotapp/oauth/redirect')
# def oauth_redirect():
#     app.logger.debug("In OAuth redirect.....")
#     auth_code = request.args.get('code')
#     return jsonify({"auth_code": auth_code})
    
def reloadnotify():
    print "Tornado app is reloading............"

@app.route('/iotapp/admin/dashboard_test')
def testDashboardTemplate(name=None):  
    app.logger.debug('in admin_dashboard page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
    
    
    return render_template("dashboard_test.html")

@app.route('/index.html')
def showindexPage(name=None):  
    app.logger.debug('in index page..')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
    return render_template("index.html")

@app.route('/iotapp/admin/device')
def showDevice(name=None):  
    app.logger.debug('in device page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
#     return render_template("dashboard-page.html")
    return render_template("device.html")

@app.route('/iotapp/admin/dashboard')
def showDashboard(name=None):  
    app.logger.debug('in dashboard page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
#     return render_template("dashboard-page.html")
    return render_template("dashboard.html")

@app.route('/iotapp/admin/dashboard2')
def showDashboard2(name=None):  
    app.logger.debug('in dashboard page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
#     return render_template("dashboard.html")
    return render_template("dashboard.html")

@app.route('/iotapp/admin/loginSys')
def showLoginsys(name=None):  
    app.logger.debug('in login page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
#     return render_template("login.html")
    return render_template("login-page.html")

@app.route('/iotapp/admin/loginSys2')
def showLoginsys2(name=None):  
    app.logger.debug('in login page')  
#     client = MongoClient('localhost',27017)
#     db = client.iotdemo
#     return render_template("login.html")
    return render_template("login.html")

@app.route('/iotapp/deviceData/<device>')
def get_deviceLatestData(device):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    deviceDataCol = None
    latestValue = None
    if device == 'tempc':
        deviceDataCol = db.TEMDeviceData
        dData = deviceDataCol.find(sort=[("timestamp" , pymongo.DESCENDING)], limit=1)
#         print str(dData[0]["timestamp"])
        latestValue = dData[0]["DeviceData"]["Temperature_C"]
        
    if device == 'tempf':
        deviceDataCol = db.TEMDeviceData
        dData = deviceDataCol.find(sort=[("timestamp" , pymongo.DESCENDING)], limit=1)
#         print str(dData[0]["timestamp"])
        latestValue = dData[0]["DeviceData"]["Temperature_F"]
        
    elif device == 'humidity':
        deviceDataCol = db.HUMDeviceData
        dData = deviceDataCol.find(sort=[("timestamp" , pymongo.DESCENDING)], limit=1)
#         print str(dData[0]["timestamp"])
        latestValue = dData[0]["DeviceData"]["Humidity"]
     
    return jsonify({'value' : latestValue}) 

@app.route('/iotapp/device/<deviceObjID>/threshold', methods=['GET'])
def getThresholdData(deviceObjID):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    deviceCol = db.DeviceMetadata
    tmpObjId = ObjectId(deviceObjID)
    device = deviceCol.find_one({'_id':tmpObjId})
    
    thresholdCol = device["thresholdList"]
    
#     thresholdResult = thresholdCol.find({"deviceObjID": deviceObjID})
    if thresholdCol:
        return jsonify({"thresholdList" : thresholdCol})
    else:
        return jsonify({"thresholdList" : []})

@app.route('/iotapp/loginSys', methods=['POST'])
def loginSys():
    loginInfo = request.get_json(force=True)
    userID = loginInfo["userID"]
    passwd = loginInfo["passwd"]
    userModel = getUserModel(userID)
    loginResultCode = None
    loginResultMsg = None
    if userModel:
        hashPasswd = userModel["password"]
        loginResult = pbkdf2_sha256.verify(passwd, hashPasswd)
        if loginResult:
            loginResultCode = 'SUCCESS'
            loginResultMsg = 'Login successful!!!'
        else:
            loginResultCode = 'FAIL'
            loginResultMsg = 'Login failure!!! Password is not correct!!'
    else:
        loginResultCode = 'FAIL'
        loginResultMsg = 'Login failure!! User ID is not exist in current system!!'
        
    result = { 'LoginResultCode' : loginResultCode,
               'LoginResultMsg'  : loginResultMsg
              }
    
    return jsonify(result)

@app.route('/iotapp/user/<userID>', methods=['GET'])
def getUser(userID):    
    userModel = getUserModel(userID)
    if userModel:
        resultJson = json.dumps(userModel, default=json_util.default)
        returnObj = Response(resultJson, mimetype='application/json')
        return returnObj
    else:
        return jsonify({})

def getUserModel(userID):    
    client = MongoClient('localhost',27017)
    db = client.iotdb
    userCol = db.user
    userModel = userCol.find_one({'userID':userID})
    return userModel
    
@app.route('/iotapp/user/<userID>', methods=['PUT'])
def addUserData(userID):
    returnObj = None
    app.logger.debug("Trigger user registration")
    try:
        jsonObj = request.get_json(force=True)
        passwd = jsonObj["password"]
        hashPass = None
        client = MongoClient('localhost',27017)
        db = client.iotdb
        userCol = db.user
    
        if passwd and len(passwd) > 0:
            hashPass = pbkdf2_sha256.encrypt(passwd, rounds=200000, salt_size=16)
        else:
            existUser = userCol.find_one({"userID" : userID})
            if existUser:
                hashPass = existUser["password"]
            else:
                raise IOTAppError("Password is empty. If you want to register new user, pls input password information." + 
                                  "If you want to update specified user information, sorry, the system can not find the existing user with input user id." +
                                  "Pls verify your input data.")
        jsonObj["password"] = hashPass
        userModel = userCol.find_and_modify({"userID" : jsonObj['userID']}, update=jsonObj, upsert=True, new=True)    
    #     userModel = saveUserData(jsonObj)
        resultJson = json.dumps(userModel, default=json_util.default)
        returnObj = Response(resultJson, mimetype='application/json')
    
        
    except IOTAppError as e:
        print "Error occurs when executing user registration", e.value
        resultJson = json.dumps({'errorMsg':e.value}, default=json_util.default)
        returnObj = Response(resultJson, mimetype='application/json')
    
    return returnObj
    

@app.route('/iotapp/deviceData/<custID>/<userID>', methods=['GET'])
def getDeviceList(custID, userID):
    returnObj = None
    app.logger.debug("Get device list")
    try:
        
        client = MongoClient('localhost',27017)
        db = client.iotdb
        userModel = getUserModel(userID)
        if userModel:
            if userModel['custId'] != custID:
                raise IOTAppError("Don't have rights to get this customer account's device list.")
            else:
                deviceCol = db.DeviceMetadata
                deviceModelCursor = deviceCol.find({"customerID" : custID})
                deviceModelList = [];
                for dModel in deviceModelCursor:
                    deviceModelList.append(dModel)
                deviceModelListJson = json.dumps(deviceModelList, default=json_util.default)
                returnObj = Response(deviceModelListJson, mimetype='application/json')
        else:
            raise IOTAppError("Access user account is not exist! Don't have priviliege to get device list.")
        
    except IOTAppError as e:
        print "Error occurs when getting device list", e.value
        errorJson = json.dumps({'errorMsg':e.value}, default=json_util.default)
        returnObj = Response(errorJson, mimetype='application/json')
            
    return returnObj
    
def saveUserData(userData):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    userCol = db.user
    userModel = userCol.find_and_modify({"userID" : userData['userID']}, update=userData, upsert=True, new=True)
    
    userModelJson = json.dumps(userModel, default=json_util.default)
    returnObj = Response(userModelJson, mimetype='application/json')
        
    return returnObj    

@gen.engine
@app.route('/iotapp/mediadata', methods=['POST'])
def postDeviceMedia():
#   get POST JSON content   
    mediaJson = request.get_json(force=True)
#     app.logger.debug("Media JSON:" + json.dumps(mediaJson))
    curTime = datetime.datetime.now()
    mediaJson["timestamp"] = curTime
    client = MongoClient('localhost',27017)
    db = client.iotdb
    collection = db.DeviceMediaData
    app.logger.debug('deviceSerialNO:' + mediaJson["deviceSerialNO"])
#     deviceModel = collection.update({"phone" : deviceJson["phone"]}, {"$set" : deviceJson}, upsert=True)
#     deviceMediaModel = collection.find_and_modify({"phone" : mediaJson['phone']}, update=mediaJson, upsert=True, new=True)
    postId = str(collection.insert(mediaJson))
    
#     fileData = mediaJson["mediaData"]
#     file = base64.urlsafe_b64decode(fileData.encode('ascii'))
    
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
    refreshCmd = {
        'deviceSerialNO' : mediaJson["deviceSerialNO"],
        'mediaType' : mediaJson["mediaType"],
        'mediaFile' : mediaJson["mediaFile"],
    }
#     cmdClient.publish('refreshMediaData', payload=json.dumps(refreshCmd), qos=2)
    wsMediaAdd.send(payload=json.dumps(refreshCmd, default=json_util.default), binary=False)
    return jsonify(resp)

@app.route('/iotapp/mediadata/<deviceSerialNO>', methods=['GET'])
def getAllDeviceMediaFiles(deviceSerialNO):
    limitcnt = request.args.get('limit')
    client = MongoClient('localhost',27017)
    db = client.iotdb
    collection = db.DeviceMediaData
    mediaFileList = None
    
    if limitcnt is not None:
        mediaFileList = collection.find({"deviceSerialNO": deviceSerialNO},{"_id":False,"deviceSerialNO":True, "mediaType":True, "mediaFile":True}).sort('timestamp', pymongo.DESCENDING).limit(int(limitcnt))
    else:
        mediaFileList = collection.find({"deviceSerialNO": deviceSerialNO},{"_id":False,"deviceSerialNO":True, "mediaType":True, "mediaFile":True}).sort('timestamp', pymongo.DESCENDING)
        
    adjJson = json_util.dumps(mediaFileList)
    return Response(adjJson, mimetype='application/json')

@app.route('/iotapp/mediadata/<deviceSerialNO>/<mediaFile>', methods=['GET'])
def getMediaFile(deviceSerialNO, mediaFile):    
    client = MongoClient('localhost',27017)
    db = client.iotdb
    collection = db.DeviceMediaData
    mediaFile = collection.find_one({"deviceSerialNO": deviceSerialNO, "mediaFile":mediaFile})
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
    
class DeviceDataNotifyClient(TornadoWebSocketClient):
    def check_origin(self, origin):
        return True
    
    def opened(self):
        app.logger.debug("Send Ping from Python Client")
        self.send("ping")

    def received_message(self, m):
        app.logger.debug("Receive msg:" + str(m))

    def closed(self, code, reason=None):
        app.logger.debug("Close WS connection: " + str(code) + " | " + str(reason))

class MediaAddNotifyClient(TornadoWebSocketClient):
    def check_origin(self, origin):
        return True
    
    def opened(self):
        app.logger.debug("Send Ping from Python Client")
        self.send("ping")

    def received_message(self, m):
        app.logger.debug("Receive msg:" + str(m))

    def closed(self, code, reason=None):
        app.logger.debug("Close WS connection: " + str(code) + " | " + str(reason))
        
class IOTAppError(Exception):
    def __init__(self, value):
        self.value = value
    def __str__(self):
        return repr(self.value)

cmdClient = mqtt.Client()
cmdClient.on_connect = cmdmqtt_on_connect
cmdClient.on_message = cmdmqtt_on_message
ws = DeviceDataNotifyClient('ws://' + remoteHostIP + ':1150/iotapp/ws/devicedatanotify')
wsMediaAdd = MediaAddNotifyClient('ws://' + remoteHostIP + ':1150/iotapp/ws/addmedia')
ws.connect()
wsMediaAdd.connect()

if __name__ == '__main__':
    
    hashtmp = pbkdf2_sha256.encrypt("password", rounds=200000, salt_size=16)
    print hashtmp
    
    print 'IOT Application start to run........'
    cmdClient.connect(remoteHostIP, port=1883, keepalive=60, bind_address=remoteHostIP)
    cmdClient.loop_start()
    
    log = logging.getLogger('flask_oauthlib')
    log.addHandler(logging.StreamHandler(sys.stdout))
    log.setLevel(logging.DEBUG)
    
    filehandler = RotatingFileHandler('/Software/tmp/log/iotapp.log', maxBytes=100000, backupCount=1)
    filehandler.setLevel(logging.DEBUG)
    
    streamhandler = StreamHandler()
    streamhandler.setLevel(logging.DEBUG)
    
    app.logger.addHandler(filehandler)
    app.logger.addHandler(streamhandler)
    app.debug = True
    oauth.init_app(app)
    
    tornado_app = tornado.web.Application([
        (r'.*', tornado.web.FallbackHandler, {'fallback': tornado.wsgi.WSGIContainer(app)})
    ], debug=True)
#     tornado_app.listen(1150, '192.168.1.11')
    tornado_app.listen(6190, remoteHostIP)
#     tornado_app.settings["debug"] = True
    ioloop = tornado.ioloop.IOLoop.instance()
    tornado.autoreload.add_reload_hook(reloadnotify)
    tornado.autoreload.start(ioloop)
    print 'Start Tornado+Flask on 6190 port'
    ioloop.start()