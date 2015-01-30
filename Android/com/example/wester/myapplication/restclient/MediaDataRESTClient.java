package com.example.wester.myapplication.restclient;

import com.example.wester.myapplication.AppConstants;

import retrofit.RestAdapter;

/**
 * Created by Wester on 2014/11/9.
 */
public class MediaDataRESTClient {

    public static RegisterResp sendMediaData(DeviceMediaData mediaData){
//        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://192.168.1.11:1150").build();
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + AppConstants.remoteHostIP + ":1150").build();
        IotDemoInterface iotDemoService = restAdapter.create(IotDemoInterface.class);
        RegisterResp resp = iotDemoService.sendDeviceMediaData(mediaData);
        return resp;
    }

    public static RegisterResp sendMediaData2(IOTDeviceMediaData mediaData){
        RestAdapter restAdapter = new RestAdapter.Builder().setEndpoint("http://" + AppConstants.remoteHostIP + ":6190").build();
        IotRESTInterface iotRESTInterface = restAdapter.create(IotRESTInterface.class);
        RegisterResp resp = iotRESTInterface.sendDeviceMediaData(mediaData);
        return resp;
    }
}
