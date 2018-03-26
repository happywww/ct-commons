package com.constanttherapy.users;

import com.constanttherapy.util.CTTestBase;
import org.junit.Test;

import java.sql.SQLException;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

public class CTUserTest extends CTTestBase
{

    @Test
    public void changePassword() throws Exception
    {
        CTUser u = CTUser.getById(_sql, 11);
        assertNotNull(u);
        u.changePassword(_sql, "clinician1");
    }

    @Test
    public void test_updateClientInfo_1() throws IllegalArgumentException, SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);
        u.updateClientInfo(_sql, null, null, null, null, null, null, null);
    }

    @Test
    public void test_updateClientInfo_2() throws IllegalArgumentException, SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);
        u.updateClientInfo(_sql, "2.1", null, null, null, null, null, null);
    }

    @Test
    public void test_updateClientInfo_3() throws IllegalArgumentException, SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);
        u.updateClientInfo(_sql, null, 1, "EDT", null, null, null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_updateClientInfo_4() throws IllegalArgumentException, SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);
        u.updateClientInfo(_sql, "2.3", 555, null, null, null, null, null); // non-existent platform
    }

    @Test
    public void test_updateClientInfo_5() throws IllegalArgumentException, SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);
        u.updateClientInfo(_sql, "2.3", 1, "EDT", null, null, null, null);
    }

    @Test
    public void test_getFirstName() throws SQLException
    {
        CTUser u = CTUser.getById(_sql, 15);
        assertNotNull(u);

        // if firstName is null, return username
        u.firstName = null;

        String test = u.getFirstName();
        assertEquals(test, u.getUsername());

        // if firstname is single word, return it
        u.firstName = "John";

    }

    @Test
    public void test_updateUserInfo1() throws SQLException
    {
        final int USER_ID = 444;
        CTUser u = CTUser.getById(_sql, USER_ID);
        assertNotNull(u);
        assertNotNull(u);
        IdentifiableUserInfo userInfo = new IdentifiableUserInfo();

        if (u.getFirstName().equals("John"))
        {
            userInfo.firstName = "Paul";
            userInfo.lastName = "McCartney";
        }
        else
        {
            userInfo.firstName = "John";
            userInfo.lastName = "Lennon";
        }

        u.updateIdentifiableInfo(_sql, userInfo);

        u = CTUser.getById(_sql, USER_ID);
        assertNotNull(u);
        assertEquals(u.getFirstName(), userInfo.firstName);
        assertEquals(u.getLastName(), userInfo.lastName);
    }

    @Test
    public void test_updateUserInfo2() throws SQLException
    {
        final int USER_ID = 444;
        CTUser u = CTUser.getById(_sql, USER_ID);
        assertNotNull(u);assertNotNull(u);
        IdentifiableUserInfo userInfo = new IdentifiableUserInfo();

        if (u.getEmail().startsWith("madvani"))
        {
            userInfo.email = "mahendra.advani@constanttherapy.com";
        }
        else
        {
            userInfo.email = "madvani@gmail.com";
        }

        u.updateIdentifiableInfo(_sql, userInfo);

        u = CTUser.getById(_sql, USER_ID);
        assertNotNull(u);
        assertEquals(u.getEmail(), userInfo.email);
    }
}
