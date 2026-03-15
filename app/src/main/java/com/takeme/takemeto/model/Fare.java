package com.takeme.takemeto.model;

/**
 * Fare details for a trip (req 6.1.1).
 * amountRand = distanceKm * ratePerKm.
 */
public class Fare {

    private double amountRand;
    private double distanceKm;
    private String currency;
    private double ratePerKm;

    /** No-arg constructor required for Firebase deserialization. */
    public Fare() {}

    public Fare(double amountRand, double distanceKm, String currency, double ratePerKm) {
        this.amountRand = amountRand;
        this.distanceKm = distanceKm;
        this.currency = currency;
        this.ratePerKm = ratePerKm;
    }

    public double getAmountRand() { return amountRand; }
    public void setAmountRand(double amountRand) { this.amountRand = amountRand; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public double getRatePerKm() { return ratePerKm; }
    public void setRatePerKm(double ratePerKm) { this.ratePerKm = ratePerKm; }
}
