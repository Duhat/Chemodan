package com.example.chemodan;

public class СurrencyItem {
    private String code; // Example: "USD"
    private double rate; // Rate relative to base currency
    private double value; // User input value
    private boolean isBase; // Is this the base currency

    public СurrencyItem(String code, double rate, double value, boolean isBase) {
        this.code = code;
        this.rate = rate;
        this.value = value;
        this.isBase = isBase;
    }

    // Getters and setters
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    
    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }
    
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    
    public boolean isBase() { return isBase; }
    public void setBase(boolean base) { isBase = base; }
}
