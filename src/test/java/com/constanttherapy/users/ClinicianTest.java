package com.constanttherapy.users;

import com.constanttherapy.users.Clinician;
import com.constanttherapy.util.CTTestBase;
import org.junit.Test;

import java.sql.SQLException;

/**
 * Created by madvani on 4/6/16.
 */
public class ClinicianTest extends CTTestBase
{
    @Test
    public void test_isBot() throws SQLException
    {
        Clinician clinician = (Clinician) Clinician.getById(10);
    }
}
