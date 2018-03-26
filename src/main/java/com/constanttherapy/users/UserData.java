package com.constanttherapy.users;

import com.constanttherapy.enums.ClientPlatform;
import com.constanttherapy.enums.UserType;

import java.sql.Timestamp;

/**
 * Created by madvani on 4/6/16.
 */
public class UserData extends IndentifiableUserData
{
    protected UserType       type;
    protected Timestamp      createDate;
    protected String         clientVersion;
    protected ClientPlatform clientPlatform;
    protected Timestamp      lastLogin;
    protected Timestamp      lastResponseTime;
    protected String         clientTimeZone;
    protected String         clientOSVersion;
    boolean isDemo;
    boolean emailIsValidated;
    String  clientHardwareType;
    String  clientIPAddress;
    String  clientDeviceIdentifier;
    boolean needsAssessment;
    transient Timestamp websiteAccessTokenTimeout;
    private   boolean   acceptedEULA;
    private boolean promptChangePassword = false;
    private boolean isLeftHanded;
    private boolean isAccountActive;
    private int     doNotContact;
    private String  createDateString;
    private boolean isSubscribed = true;  // default value
    private String pushNotificationToken;

    public UserType getType()
    {
        return this.type;
    }

    public boolean isAcceptedEULA()
    {
        return this.acceptedEULA;
    }

    public void setAcceptedEULA(boolean acceptedEULA)
    {
        this.acceptedEULA = acceptedEULA;
    }

    public boolean isDemo()
    {
        return this.isDemo;
    }

    boolean isPromptChangePassword()
    {
        return this.promptChangePassword;
    }

    public void setPromptChangePassword(boolean promptChangePassword)
    {
        this.promptChangePassword = promptChangePassword;
    }

    public boolean isLeftHanded()
    {
        return this.isLeftHanded;
    }

    public void setLeftHanded(boolean leftHanded)
    {
        isLeftHanded = leftHanded;
    }

    public boolean isAccountActive()
    {
        return this.isAccountActive;
    }

    public void setAccountActive(boolean accountActive)
    {
        isAccountActive = accountActive;
    }

    public Timestamp getCreateDate()
    {
        return this.createDate;
    }

    public int getDoNotContact()
    {
        return this.doNotContact;
    }

    public void setDoNotContact(int doNotContact)
    {
        this.doNotContact = doNotContact;
    }

    public String getClientVersion()
    {
        return this.clientVersion;
    }

    public ClientPlatform getClientPlatform()
    {
        return this.clientPlatform;
    }

    public boolean isSubscribed()
    {
        return this.isSubscribed;
    }

    public void setSubscribed(boolean subscribed)
    {
        this.isSubscribed = subscribed;
    }

    public Timestamp getLastLogin()
    {
        return this.lastLogin;
    }

    public Timestamp getLastResponseTime()
    {
        return this.lastResponseTime;
    }

    public String getClientTimeZone()
    {
        return this.clientTimeZone;
    }

    String getClientHardwareType()
    {
        return this.clientHardwareType;
    }

    public String getClientOSVersion()
    {
        return this.clientOSVersion;
    }

    public boolean isEmailIsValidated()
    {
        return this.emailIsValidated;
    }

    String getClientIPAddress()
    {
        return this.clientIPAddress;
    }

    String getClientDeviceIdentifier()
    {
        return this.clientDeviceIdentifier;
    }

    boolean isNeedsAssessment()
    {
        return this.needsAssessment;
    }

    Timestamp getWebsiteAccessTokenTimeout()
    {
        return this.websiteAccessTokenTimeout;
    }

    public String getCreateDateString()
    {
        return this.createDateString;
    }

    public void setCreateDateString(String createDateString)
    {
        this.createDateString = createDateString;
    }

    public String getPushNotificationToken() { return this.pushNotificationToken; }

    public void setPushNotificationToken(String pushNotificationToken) { this.pushNotificationToken = pushNotificationToken; }

}
