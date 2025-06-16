package com.example.chemodan;

public class WeatherItem {
    private String city;
    private double temp;
    private String state;
    private double wind;
    private int humidity;

    public WeatherItem(String city, double temp, String state, double wind, int humidity) {
        this.city = city;
        this.temp = temp;
        this.state = state;
        this.wind = wind;
        this.humidity = humidity;
    }

    public String getCity() { return city; }
    public double getTemp() { return temp; }
    public String getState() { return state; }
    public double getWind() { return wind; }
    public int getHumidity() { return humidity; }

    public void setTemp(double temp) { this.temp = temp; }
    public void setState(String state) { this.state = state; }
    public void setWind(double wind) { this.wind = wind; }
    public void setHumidity(int humidity) { this.humidity = humidity; }
} 