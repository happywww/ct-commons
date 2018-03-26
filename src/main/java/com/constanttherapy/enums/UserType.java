package com.constanttherapy.enums;

/**
 * This enum is taken directly from the 'type' column of the users table.
 * It is used to distinguish between patients, clinicians, caretakers,
 * and administrators
 *
 * @author ehsan
 *
 */
public enum UserType
{
    patient(1),
    clinician(2),
    admin(3), 		// TODO
    caregiver(4),	// TODO
    other(5),
    educator(6),
    researcher(7),
    student(8);

    private int value;
    private UserType(int val) { this.value = val; }
    public int getValue() { return this.value; }

    @Override
    public String toString()
    {
        switch(this)
        {
            case patient:
                return "patient";
            case clinician:
                return "clinician";
            case admin:
                return "admin";
            case caregiver:
                return "caregiver";
            case educator:
                return "educator";
            case other:
                return "other";
            case researcher:
                return "researcher";
            case student:
                return "student";
            default:
                return "";
        }
    }
}
