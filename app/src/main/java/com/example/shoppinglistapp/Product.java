package com.example.shoppinglistapp;

public class Product {
    public int id;
    public String name;
    public double price;
    public boolean bought;
    public String description;

    public Product(int id, String name, double price, boolean bought, String description) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.bought = bought;
        this.description = description;
    }
}