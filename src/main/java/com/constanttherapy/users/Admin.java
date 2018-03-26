package com.constanttherapy.users;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Admin extends Clinician
{
    public Admin(ResultSet rs) throws SQLException
    {
        super(rs);
    }
}
