package com.example.accessibilityplay;

public class Configurations {
    public static Configurations configuration = new Configurations();
    private int statusBarHeight;

    public int getStatusBarHeight() {
        return statusBarHeight;
    }

    public void setStatusBarHeight(int statusBarHeight) {
        this.statusBarHeight = statusBarHeight;
    }
}
