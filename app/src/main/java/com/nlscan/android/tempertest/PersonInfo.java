package com.nlscan.android.tempertest;

public class PersonInfo {

    private String name;

    private String temperVal;

    private String certificate;

    private String date;

    public PersonInfo(){

    }

    public PersonInfo(String name,  String certificate, String temperVal,String date) {
        this.name = name;
        this.temperVal = temperVal;
        this.certificate = certificate;
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTemperVal() {
        return temperVal;
    }

    public void setTemperVal(String temperVal) {
        this.temperVal = temperVal;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
