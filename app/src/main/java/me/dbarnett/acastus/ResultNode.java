package me.dbarnett.acastus;

/**
 * Created by daniel on 7/24/16.
 */
public class ResultNode {

    public String name;
    public double lat;
    public double lon;

    public void printResult(){
        System.out.println("Name: " + name);
        System.out.println("Lattitude: " + lat);
        System.out.println("Longitude: " + lon);
    }

}
