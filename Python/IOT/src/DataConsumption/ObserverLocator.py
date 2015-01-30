'''
Created on 2014/12/24

@author: Wester
'''
from pymongo.mongo_client import MongoClient
import importlib
from flask import json
from command import CommandLocator
from bson.objectid import ObjectId

class BaseObserver(object):
    '''
    classdocs
    '''
    def __init__(self):
        '''
        Constructor
        '''
    
    def runCommand(self, deviceData, mqttClient):
        devId = deviceData["deviceObjID"]
        client = MongoClient('localhost',27017)
        db = client.iotdb
        deviceCol = db.DeviceMetadata
        gotDevice = deviceCol.find_one({'_id':ObjectId(devId)})
        if gotDevice :
            thresholdList = gotDevice["thresholdList"]
            if thresholdList :
                for thresholdObj in thresholdList:
                    key = thresholdObj["thresholdKey"]
                    val = thresholdObj["thresholdValue"]
                    d_Data = deviceData['DeviceData']
                    deviceVal = d_Data[key]
                    dt  = thresholdObj["dataType"]
                    if dt == 'Number':
                        val = float(val)
                        deviceVal = float(deviceVal)
                    comp  = thresholdObj["thresholdComparator"]
                    compResult = False
                    if comp == '>':
                        compResult = (deviceVal > val)
                    elif comp == '>=':
                        compResult = (deviceVal >= val)
                    elif comp == '<':
                        compResult = (deviceVal < val)
                    elif comp == '<=':
                        compResult = (deviceVal <= val)
                    elif comp == '=':
                        compResult = (deviceVal == val)
                    
                    if compResult:
                        commList = thresholdObj["commandScript"]
                        if commList:
                            for commObj in commList:
                                CommandLocator.locate_command(commObj["commScript"], deviceData, mqttClient)



class TestObserver(object):
    '''
    classdocs
    '''


    def __init__(self):
        '''
        Constructor
        '''
    
    def execute(self, deviceData, mqttClient=None):
        client = MongoClient('localhost',27017)
        db = client.iotdb
        deviceDataCol = db.TestDeviceData
        deviceDataCol.insert(deviceData)
        return None   

class Test2Observer(object):
    '''
    classdocs
    '''


    def __init__(self):
        '''
        Constructor
        '''
    
    def execute(self, deviceData, mqttClient=None):
        client = MongoClient('localhost',27017)
        db = client.iotdb
        deviceDataCol = db.Test2DeviceData
        deviceDataCol.insert(deviceData)
        return None    

class LEDObserver(BaseObserver):
    '''
    classdocs
    '''


    def __init__(self):
        '''
        Constructor
        '''
    
    def execute(self, deviceData, mqttClient=None):
        client = MongoClient('localhost',27017)
        db = client.iotdb
        deviceDataCol = db.LEDDeviceData
        deviceDataCol.insert(deviceData)
        self.runCommand(deviceData, mqttClient)
        return None

class HUMObserver(BaseObserver):
    '''
    classdocs
    '''


    def __init__(self):
        '''
        Constructor
        '''
    
    def execute(self, deviceData, mqttClient=None):
        client = MongoClient('localhost',27017)
        db = client.iotdb
        deviceDataCol = db.HUMDeviceData
        deviceDataCol.insert(deviceData)
        self.runCommand(deviceData, mqttClient)
        return None

class TEMObserver(BaseObserver):
    '''
    classdocs
    '''


    def __init__(self):
        '''
        Constructor
        '''
    
    def execute(self, deviceData, mqttClient=None):
        client = MongoClient('localhost',27017)
        db = client.iotdb
#         d_Data = deviceData['DeviceData']
#         temp_c = float(d_Data['Temperature_C'])
        deviceDataCol = db.TEMDeviceData
        deviceDataCol.insert(deviceData)
        self.runCommand(deviceData, mqttClient)
#         devId = deviceData["deviceObjID"]
#         CommandLocator.locate_command('LEDControl', deviceData, mqttClient)
#         CommandLocator.locate_command('RecordingControl', deviceData, mqttClient)
#         if temp_c < 25:
#             payload = 'false|500|'
#             mqttClient.publish('iotcmd', payload=payload, qos=2)
#             print 'send command:' + payload
#         elif temp_c >= 25 and temp_c < 35:
#             payload = 'true|100|'
#             mqttClient.publish('iotcmd', payload=payload, qos=2)
#             
#             cmd = {
#               'cmdType' : 'recording',    
#               'contactName' : '',
#               'contact' : '' 
#             }
#             mqttClient.publish('cmdJSON', payload=json.dumps(cmd), qos=2)
#             print 'send command:' + json.dumps(cmd)
#             print 'send command:' + payload 
#         else:
#             payload = 'true|50|'
#             mqttClient.publish('iotcmd', payload=payload, qos=2)
#             print 'send command:' + payload
        
        return None

                                
def locate_service(serviceType, deviceData, mqttClient):
#     print 'locate service dynamically'
    client = MongoClient('localhost',27017)
    db = client.iotdb
    serviceMolduleCol = db.ServiceMOduleCOnfig
    srvModule = serviceMolduleCol.find_one({"serviceType":serviceType})
    scriptKlass = srvModule["moduleScript"]
    locatorModule = importlib.import_module('DataConsumption.ObserverLocator');
    klass_ = getattr(locatorModule, scriptKlass)
    klassInstance = klass_()
    klassInstance.execute(deviceData, mqttClient) 