/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.kyselad;

/**
 *
 * @author dtk6
 */
public class Coordinate {

    double x, y;
    String name;

    public Coordinate(double x, double y, String name) {
        this.x = x;
        this.y = y;
        this.name = name;
    }

    String coordToString() {
        String str = name + "\t" + x + "\t" + y + "\n";
        return str;
    }
    
    public void setX (double x) {
        this.x = x;
    }
    
    public void setY (double y) {
        this.y = y;
    }
}
