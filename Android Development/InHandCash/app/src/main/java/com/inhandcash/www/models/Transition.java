package com.inhandcash.www.models;

import java.util.Date;

public class Transition {
    double amount;
    String sender;
    String date;
    String status;
    String status_name;

    String receiver;


    public Transition(double amount, String sender, String receiver, String date, String status_name, String status) {
        this.amount = amount;
        this.sender = sender;
        this.date = date;
        this.status_name = status_name;
        this.status = status;
        this.receiver = receiver;
    }

    public Transition() {
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus_name() {
        return status_name;
    }

    public void setStatus_name(String status_name) {
        this.status_name = status_name;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
