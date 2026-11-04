package com.digitalpioneers.stocksystem.models;

public class Supplier {
    public long id;
    public String name;
    public String contactName;
    public String email;
    public String phone;
    public String createdAt;

    public Supplier() {}

    public Supplier(long id, String name, String contactName, String email, String phone, String createdAt) {
        this.id = id;
        this.name = name;
        this.contactName = contactName;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
    }
}