package com.example.wester.myapplication.restclient;

import java.util.List;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

/**
 * Created by Wester on 2014/11/4.
 */
public interface IotRESTInterface {

//    @GET("/user_device/{phone}/contacts")
//    public List<Contact> getContactList(@Path("phone") String phone);
//
//    @DELETE("/user_device/{phone}")
//    public UnregisterResp unregisterDevice(@Path("phone") String phone);
//
//    @POST("/user_device")
//    public RegisterResp registerDevice(@Body Device device);

    @POST("/iotapp/mediadata")
    public RegisterResp sendDeviceMediaData(@Body IOTDeviceMediaData deviceMedia);

}
