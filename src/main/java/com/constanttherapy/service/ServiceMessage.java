package com.constanttherapy.service;

public class ServiceMessage
{
    /**
     * These are the different types of messages that the client should be able to handle
     */
    public enum MessageType
    {
        alert, // uses title, message, and button instance variables

        appRating,
        reloadPatients,
        reloadTaskTypes,
        validateResourceCache,
        updateResourceDomain,   // uses 'value' instance variable for new resource domain root
        updateUserData,
        showPaymentOptions,
    }

    /**
     * the message type, e.g. if "alert" an alert dialog will be shown
     */
    private MessageType type    = null;
    /**
     * title of dialog that displays message on client
     */
    private String title   = null;
    /**
     * actual body of message shown in dialog on client
     */
    private String message = null;
    /**
     * optional label text to display on the client dialog "OK" button instead of "OK"
     */
    private String button  = null;    // should be used for the action button
    private String button2 = null;    // should be used for the cancel button
    private Object audio   = null;
    /**
     * used by different types of messages to store any data
     */
    private Object value;

    /**
     * construct a new service message with the given attributes
     */
    public ServiceMessage(MessageType theType, String theTitle, String theMessage, String theButton)
    {
        setType(theType);
        setTitle(theTitle);
        setMessage(theMessage);
        setButton(theButton);
    }

    /**
     * construct a new service message with the given type and value
     */
    public ServiceMessage(MessageType theType, Object value)
    {
        setType(theType);
        setValue(value);
    }

    /**
     * construct a new service message with the given attributes - default to "OK" button
     */
    public ServiceMessage(MessageType theType, String theTitle, String theMessage)
    {
        setType(theType);
        setTitle(theTitle);
        setMessage(theMessage);
        // use default button
    }

    /**
     * construct a new service message with the given attributes - default to an alert message with "OK" button
     */
    public ServiceMessage(String theTitle, String theMessage)
    {
        setType(MessageType.alert);
        setTitle(theTitle);
        setMessage(theMessage);
        // use default button
    }

    /**
     * get message type, e.g. if "alert" then an alert dialog will be shown
     */
    public MessageType getType()
    {
        return type;
    }

    /**
     * get message type, e.g. if "alert" then an alert dialog will be shown
     */
    public void setType(MessageType type)
    {
        this.type = type;
    }

    /**
     * get the dialog title for an alert message
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * set the dialog title for an alert message
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * get the actual message content/body
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * set the actual message content/body
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * get the (optional) button label to use instead of "OK", for alert dialog (if any)
     */
    public String getButton()
    {
        return button;
    }

    /**
     * set the (optional) button label to use instead of "OK", for alert dialog (if any)
     */
    public void setButton(String button)
    {
        this.button = button;
    }

    /**
     * get the value
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * set the value
     */
    public void setValue(Object value)
    {
        this.value = value;
    }

    public String getButton2()
    {
        return button2;
    }

    public void setButton2(String button2)
    {
        this.button2 = button2;
    }

    public Object getAudio()
    {
        return audio;
    }

    public void setAudio(Object audio)
    {
        this.audio = audio;
    }
}
