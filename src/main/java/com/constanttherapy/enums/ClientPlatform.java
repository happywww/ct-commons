package com.constanttherapy.enums;

/**
 * Created by Mahendra on 4/30/2015.
 */

public enum ClientPlatform
{
    iOS(1),
    Android(2),
    Browser(3);

    private int value;
    ClientPlatform(int val) { this.value = val; }
    public int getValue() { return this.value; }

    @Override
    public String toString()
    {
        switch(this)
        {
            case iOS:
                return "iOS";
            case Android:
                return "Android";
            case Browser:
                return "Browser";
            default:
                return "";
        }
    }

    public static boolean isIOS(Integer x) {
        return ClientPlatform.iOS.equals(ClientPlatform.fromInteger(x));
    }

    public static boolean isAndroid(Integer x) {
        return ClientPlatform.Android.equals(ClientPlatform.fromInteger(x));
    }

    public static boolean isBrowser(Integer x) {
        return ClientPlatform.Browser.equals(ClientPlatform.fromInteger(x));
    }

    public static ClientPlatform fromInteger(Integer x)
    {
        switch(x)
        {
            case 1:
                return iOS;
            case 2:
                return Android;
            case 3:
                return Browser;
        }
        return null;
    }

    public static ClientPlatform fromString(String hardwareType)
    {
        return (hardwareType.toLowerCase().contains("ipad") || hardwareType.toLowerCase().contains("iphone") || hardwareType.contentEquals("Simulator")
                ? ClientPlatform.iOS : ClientPlatform.Android);
    }
}
