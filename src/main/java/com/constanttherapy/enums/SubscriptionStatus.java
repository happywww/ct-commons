package com.constanttherapy.enums;

/**
 * Created by madvani on 4/30/15.
 */
public enum SubscriptionStatus
{
    active(1),
    trialing(2),
    grandfathered(3),
    veteran(4),
    scholarship(5),
    past_due(10),
    trialEnded(20),
    expired(30);

    private final int value;

    SubscriptionStatus(int val)
    {
        this.value = val;
    }

    public int getValue()
    {
        return this.value;
    }

    @Override
    public String toString()
    {
        switch (this)
        {
            case active:
                return "active";
            case trialing:
                return "trialing";
            case grandfathered:
                return "grandfathered";
            case veteran:
                return "veteran";
            case scholarship:
                return "scholarship";
            case past_due:
                return "past_due";
            case trialEnded:
                return "trialEnded";
            case expired:
                return "expired";
            default:
                return "";
        }
    }
}
