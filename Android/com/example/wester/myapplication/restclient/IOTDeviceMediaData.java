package com.example.wester.myapplication.restclient;

/**
 * Created by Wester on 2014/11/9.
 */
public class IOTDeviceMediaData {

    private String deviceSerialNO;

    private String mediaType;

    private String mediaFile;

    private String mediaData;


    public String getDeviceSerialNO() {
        return deviceSerialNO;
    }

    public void setDeviceSerialNO(String deviceSerialNO) {
        this.deviceSerialNO = deviceSerialNO;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaFile() {
        return mediaFile;
    }

    public void setMediaFile(String mediaFile) {
        this.mediaFile = mediaFile;
    }

    public String getMediaData() {
        return mediaData;
    }

    public void setMediaData(String mediaData) {
        this.mediaData = mediaData;
    }
}
