package com.constanttherapy.enums;

/**
 * Created by madvani on 4/30/15.
 */
public enum EmailValidationResult
{
    EmailInvalidFormat,        // not a correctly formatted email
    EmailFake,                    // not a real email
    EmailExists,                // the email is already in use by another user
    EmailValid;                    // the email is valid

    @Override
    public String toString()
    {
        switch (this)
        {
            case EmailInvalidFormat:
                return "Please provide a valid email address.";
            case EmailFake:
                return "Please provide a valid email address.";
            case EmailExists:
                return "Your selected email address is already in use.  Please try a different email address.";
            case EmailValid:
                return "The email address you provided is valid. Thanks!";
            default:
                return "";
        }
    }

    public boolean isValid()
    {
        return (this == EmailValid);
    }
}
