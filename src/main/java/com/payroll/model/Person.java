package com.payroll.model;

/**
 * Person — base class for Employee and User.
 * Updated: added performanceRating (new column via ALTER TABLE in payrolldbs_project.sql Section 2).
 */
public class Person {
    protected String nic            = "";
    protected String fName          = "";
    protected String lName          = "";
    protected String dob            = "";
    // Address from employee_addresses table
    protected String addressLine1   = "";
    protected String addressLine2   = "";
    protected String city           = "";
    protected String postalCode     = "";
    protected String gender         = "";
    // Contacts from employee_contact_numbers table
    protected String telHome        = "";
    protected String telMobile      = "";
    protected double salAmount      = 0.0;
    // Added by ALTER TABLE in payrolldbs_project.sql (Section 2 - Lab 1 ALTER)
    // DEFAULT 3, CHECK (performanceRating BETWEEN 1 AND 5)
    protected int performanceRating = 3;

    public String getNic()                    { return nic; }
    public void   setNic(String v)            { nic = v; }
    public String getFname()                  { return fName; }
    public void   setFname(String v)          { fName = v; }
    public String getLname()                  { return lName; }
    public void   setLname(String v)          { lName = v; }
    public String getDob()                    { return dob; }
    public void   setDob(String v)            { dob = v; }
    public String getAddressLine1()           { return addressLine1; }
    public void   setAddressLine1(String v)   { addressLine1 = v; }
    public String getAddressLine2()           { return addressLine2; }
    public void   setAddressLine2(String v)   { addressLine2 = v; }
    public String getCity()                   { return city; }
    public void   setCity(String v)           { city = v; }
    public String getPostalCode()             { return postalCode; }
    public void   setPostalCode(String v)     { postalCode = v; }
    public String getGender()                 { return gender; }
    public void   setGender(String v)         { gender = v; }
    public String getTelHome()                { return telHome; }
    public void   setTelHome(String v)        { telHome = v; }
    public String getTelMobile()              { return telMobile; }
    public void   setTelMobile(String v)      { telMobile = v; }
    public double getSalAmount()              { return salAmount; }
    public void   setSalAmount(double v)      { salAmount = v; }
    public int    getPerformanceRating()      { return performanceRating; }
    public void   setPerformanceRating(int v) { performanceRating = v; }
}
