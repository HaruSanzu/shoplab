package com.shoplab.order;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * {@code @Embeddable}: not its own table. Its fields become columns of the "orders" table
 * (ship_full_name, ship_city ...). Lets us group related fields without an extra table.
 */
@Embeddable
public class ShippingAddress {

    @Column(name = "ship_full_name")
    private String fullName;
    @Column(name = "ship_address")
    private String address;
    @Column(name = "ship_city")
    private String city;
    @Column(name = "ship_zip")
    private String zip;
    @Column(name = "ship_country")
    private String country;
    @Column(name = "ship_phone")
    private String phone;

    protected ShippingAddress() {
    }

    public ShippingAddress(String fullName, String address, String city, String zip, String country, String phone) {
        this.fullName = fullName;
        this.address = address;
        this.city = city;
        this.zip = zip;
        this.country = country;
        this.phone = phone;
    }

    public String getFullName() { return fullName; }
    public String getAddress() { return address; }
    public String getCity() { return city; }
    public String getZip() { return zip; }
    public String getCountry() { return country; }
    public String getPhone() { return phone; }
}
