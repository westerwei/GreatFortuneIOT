'''
Created on 2014/12/23

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

remoteHostIP = "wester_macair.com"
# remoteHostIP = "172.20.10.7"

app = Flask(__name__)
oauth = OAuth2Provider()
    
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

@app.route('/iotapp/oauth/token')
@oauth.token_handler
def access_token():
    return None

@app.route('/iotapp/oauth/authorize', methods=['GET', 'POST'])
@oauth.authorize_handler
def authorize(*args, **kwargs):
    client_id = kwargs.get('client_id')
#     kwargs['redirect_uri'] = urllib.urlencode(kwargs.get('redirect_uri'))
    app.logger.debug("Get client id: " + client_id)
    client = MongoClient('localhost',27017)
    db = client.iotdb
    clientAppCol = db.ClientApp
    clientAppCol.find_one({"client_id" : client_id})
    lookupClient = clientAppCol.find_one({"client_id" : client_id})
    
    kwargs['client'] = lookupClient
    kwargs['user'] = 'iotapp'
#     user = current_user()
#     if not user:
#         return redirect('/')
#     if request.method == 'GET':
#         client_id = kwargs.get('client_id')
#         client = Client.query.filter_by(client_id=client_id).first()
#         kwargs['client'] = client
#         kwargs['user'] = user
#         return render_template('authorize.html', **kwargs)
# 
#     confirm = request.form.get('confirm', 'no')
#     return confirm == 'yes'
    return True

@app.route('/iotapp/oauth/redirect')
def oauth_redirect():
    app.logger.debug("In OAuth redirect.....")
    auth_code = request.args.get('code')
    return jsonify({"auth_code": auth_code})
    
def reloadnotify():
    print "Tornado app is reloading............"

class DeviceDataNotify(tornado.websocket.WebSocketHandler):
    clients = []
 
    def open(self):
        app.logger.debug("DeviceDataNotify is open~~ ")
        DeviceDataNotify.clients.append(self)
 
    def on_message(self, message):
        app.logger.debug("DeviceDataNotify msg is coming in~~~" + message)
        if message == 'ping':
            message = 'ping_ok'        
        for client in DeviceDataNotify.clients:
            client.write_message(message)
 
    def on_close(self):
        app.logger.debug("DeviceDataNotify is closed~~")
        DeviceDataNotify.clients.remove(self) 
    
    def check_origin(self, origin):  
        return True
    
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
    
    def check_origin(self, origin):  
        return True

if __name__ == '__main__':
    print 'OAuth Application start to run........'
    
    
    log = logging.getLogger('flask_oauthlib')
    log.addHandler(logging.StreamHandler(sys.stdout))
    log.setLevel(logging.DEBUG)
    
    sha256 = hashlib.sha256()
    
    filehandler = RotatingFileHandler('/Software/tmp/log/oauthapp.log', maxBytes=100000, backupCount=1)
    filehandler.setLevel(logging.DEBUG)
    
    streamhandler = StreamHandler()
    streamhandler.setLevel(logging.DEBUG)
    
    app.logger.addHandler(filehandler)
    app.logger.addHandler(streamhandler)
    app.debug = True
    oauth.init_app(app)
    
    tornado_app = tornado.web.Application([
        (r'/iotapp/ws/devicedatanotify',DeviceDataNotify),
        (r'/iotapp/ws/addmedia', MediaAddNotify),                                           
        (r'.*', tornado.web.FallbackHandler, {'fallback': tornado.wsgi.WSGIContainer(app)})
        
    ], debug=True)
#     tornado_app.listen(1150, '192.168.1.11')
    tornado_app.listen(1150, remoteHostIP)
#     tornado_app.settings["debug"] = True
    ioloop = tornado.ioloop.IOLoop.instance()
    tornado.autoreload.add_reload_hook(reloadnotify)
    tornado.autoreload.start(ioloop)
    print 'Start Tornado+Flask on 1150 port'
    ioloop.start()