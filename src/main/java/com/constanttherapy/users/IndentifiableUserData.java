package com.constanttherapy.users;

/**
 * Created by madvani on 4/6/16.
 */
public class IndentifiableUserData
{
    protected Integer id;
    private String username;
    public String firstName;
    private String lastName;
    private String email;
    protected String phoneNumber;
    protected transient String accessToken;
    protected transient CTUser.PasswordTuple password;
    transient String websiteAccessToken;
    transient String deviceToken;

    public Integer getId()
    {
        return this.id;
    }

    public String getUsername()
    {
        return this.username;
    }

    public String getFirstName()
    {
        return this.firstName;
    }

    public String getLastName()
    {
        return this.lastName;
    }

    public String getEmail()
    {
        return this.email;
    }

    public String getPhoneNumber()
    {
        return this.phoneNumber;
    }

    public String getAccessToken()
    {
        return this.accessToken;
    }

    public CTUser.PasswordTuple getPassword()
    {
        return this.password;
    }

    public String getWebsiteAccessToken()
    {
        return this.websiteAccessToken;
    }

    public String getDeviceToken()
    {
        return this.deviceToken;
    }

    public void setUsername(String username)
    {
        this.username = username.trim();
    }

    public void setLastName(String lastName)
    {
        if (lastName != null)
            this.lastName = lastName.trim();
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
}
