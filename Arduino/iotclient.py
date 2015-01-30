import json
import urllib2
import sys

remoteHostIP = '192.168.1.37'
RESTPrefix = 'http://' + remoteHostIP + ':6190'
if __name__ == "__main__":
  serviceType = sys.argv[1]
  print 'type: ' + serviceType
  dataVal = sys.argv[2]
  print 'dataVal: ' + dataVal
  url = None
  if serviceType == 'LEDService':
    url = '/iotapp/device/data/LED0000001/sfrxPhUGsIClyRqRyUyxm4SXH1YnM4XkPkKFTkeJ'
  elif serviceType == 'HUMService':
    url = '/iotapp/device/data/HUM0000001/AJEYghHS7qkLioFdYvRMLfEl8S2806JwLo8qQi9F'
  else:
    url = '/iotapp/device/data/TEM0000001/mOlHOtxqnOnXR1pvEWaw502XRFKO8hxm9MB0KQQ5'

  if serviceType == 'LEDService':
    data={
      'LEDData': dataVal
    }
  elif serviceType == 'HUMService':
    data={
      'Humidity': dataVal
    }
  else:
    temp_f = sys.argv[3]
    data={
      'Temperature_C': dataVal,
      'Temperature_F': temp_f
    }

  jsonData = json.dumps(data)
  req = urllib2.Request(RESTPrefix+url, jsonData, {'Content-Type': 'application/json'})
  uo = urllib2.urlopen(req)
  resp = uo.read()
  uo.close()