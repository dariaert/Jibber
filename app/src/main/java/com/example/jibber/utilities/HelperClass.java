package com.example.jibber.utilities;

public class HelperClass {

    String Name;
    String Surname;
    String Phone;
    String UserName;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getSurname() {
        return Surname;
    }

    public void setSurname(String surname) {
        Surname = surname;
    }

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

    public HelperClass(String name, String surname, String phone, String userName) {
        Name = name;
        Surname = surname;
        Phone = phone;
        UserName = userName;
    }

    public HelperClass() {
    }
}
