'''
Created on 2014/12/26

@author: Wester
'''
from pymongo.mongo_client import MongoClient
import importlib
import json
import smtplib
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from line import LineClient

class EmailControl(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
    def execute(self, deviceData, mqttClient=None):
        print 'Run EmailControl.......'
        d_Data = deviceData['DeviceData']
        temp_c = float(d_Data['Temperature_C'])
        msg = MIMEMultipart('alternative')
#         msgPlainTxt = MIMEText("Warning message from IOT platform: current temperature is over threshold:" + str(temp_c))
        msg['Subject'] = 'Device Warning!!!!!!'
        msg['From'] = 'wester.wei@gmail.com'
        msg['To'] = 'wester.wei@gmail.com'
        
        text = """Warning message from IOT platform: 
            current temperature is over threshold: %(temp_c)s .\nURL to IOT platform: http://192.168.1.37:6190/iotapp/admin/dashboard"""
        html = """
            <html><head></head><body>
            <p><H3>Hi!<br>
                Warning message from IOT platform: current temperature is over threshold: %(temp_c)s . 
                </h3><br><h3>Please check the dashboard page: 
                <a href='http://192.168.1.37:6190/iotapp/admin/dashboard'>
                    http://192.168.1.37:6190/iotapp/admin/dashboard</a>
                    </h3>.</p></body>""" % {'temp_c':str(temp_c)}
        
        # Record the MIME types of both parts - text/plain and text/html.
        part1 = MIMEText(text, 'plain')
        part2 = MIMEText(html, 'html')

        # Attach parts into message container.
        # According to RFC 2046, the last part of a multipart message, in this case
        # the HTML message, is best and preferred.
        msg.attach(part1)
        msg.attach(part2)
        
        s = smtplib.SMTP('localhost')
        s.sendmail('wester.wei@gmail.com', 'wester.wei@gmail.com', msg.as_string())
        s.quit()
        
        return None

class LineControl(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
    def execute(self, deviceData, mqttClient=None):
        print 'Run LineControl.......'
        d_Data = deviceData['DeviceData']
        temp_c = float(d_Data['Temperature_C'])
        msgPlainTxt = "Warning message from IOT platform: current temperature is over threshold:" + str(temp_c)
#       Initial Line client  
        client = LineClient(authToken='DOFM6cNhM8VIuKmyOXK2.azad0AlpxQy6iJzh2pQLqG.By8oDIYL8bQ8KNscNnygUVN7HngF0vweWFwHG1lGyII=')
        friend = client.getContactFromName('Wester')
        friend.sendMessage(msgPlainTxt);
        return None

class RecordingControl(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
    def execute(self, deviceData, mqttClient=None):
        print 'Run RecordingControl.......'
#         d_Data = deviceData['DeviceData']
#         temp_c = float(d_Data['Temperature_C'])
#         if temp_c >= 25 and temp_c < 35:
        cmd = {
               'cmdType' : 'recording',    
               'contactName' : '',
               'contact' : '' 
        }
        mqttClient.publish('cmdJSON', payload=json.dumps(cmd), qos=2)
        print 'send command:' + json.dumps(cmd)
            
#         else:
#             print 'Nothing to do in RecordingControl'
        
        return None

class LEDControl(object):
    '''
    classdocs
    '''

    def __init__(self):
        '''
        Constructor
        '''
    def execute(self, deviceData, mqttClient=None):
        print 'Run LEDControl.......'
        d_Data = deviceData['DeviceData']
        temp_c = float(d_Data['Temperature_C'])
#         if temp_c < 25:
#             payload = 'false|500|'
#             mqttClient.publish('iotcmd', payload=payload, qos=2)
#             print 'send command:' + payload
        if temp_c >= 25 and temp_c < 35:
            payload = 'true|500|'
            mqttClient.publish('iotcmd', payload=payload, qos=2)
            
#             cmd = {
#               'cmdType' : 'recording',    
#               'contactName' : '',
#               'contact' : '' 
#             }
#             mqttClient.publish('cmdJSON', payload=json.dumps(cmd), qos=2)
#             print 'send command:' + json.dumps(cmd)
            print 'send command:' + payload 
        else:
            payload = 'true|250|'
            mqttClient.publish('iotcmd', payload=payload, qos=2)
            print 'send command:' + payload
        return None

class LOGControl(object):
    '''
    classdocs
    '''
    def __init__(self):
        '''
        Constructor
        '''
    def execute(self, deviceData, mqttClient=None):
        print "LOGControl print device data:" + str(deviceData["DeviceData"])
        
def locate_command(cmdName, deviceData, mqttClient):
    client = MongoClient('localhost',27017)
    db = client.iotdb
    cmdMolduleCol = db.CommandModuleConfig
    cmdScript = cmdMolduleCol.find_one({"commandName":cmdName})
    scriptKlass = cmdScript["moduleScript"]
    locatorModule = importlib.import_module('command.CommandLocator');
    klass_ = getattr(locatorModule, scriptKlass)
    klassInstance = klass_()
    klassInstance.execute(deviceData, mqttClient)        