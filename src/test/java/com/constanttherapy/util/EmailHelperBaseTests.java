package com.constanttherapy.util;

import org.junit.Test;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class EmailHelperBaseTests extends CTTestBase
{
	@Test
	public void test_shouldSendEmail()
	{
		boolean retval = EmailHelperBase.canSendEmail(_sqlr, 19990, "%homework%", 0);
		org.junit.Assert.assertTrue(retval);

        retval = EmailHelperBase.canSendEmail(_sqlr, 19990, "%homework%", 9999);
        org.junit.Assert.assertTrue(!retval);
	}

	@Test
	public void test_generateHomeworkHighlightTable() throws SQLException
	{
		Map<String,String> tokens = new TreeMap<>();
		EmailHelperBase.generateHomeworkHighlightTable(_sql, 126937, tokens);
		System.out.println(tokens.size());
	}

	@Test
	public void test_generateClinicianEmailPatientHighlightTable() throws SQLException
	{
		String highlight = EmailHelperBase.generateClinicianEmailPatientHighlightTable(_sql, 78);
		System.out.println(highlight);
	}

	@Test
	public void test_getClinicianPatientDashboardSummary() throws SQLException
	{
		Map<String,String> tokens = new HashMap<>();
		EmailHelperBase.getClinicianPatientDashboardSummary(_sql, tokens, 11);

		for (Map.Entry<String, String> entry : tokens.entrySet()) {
			System.out.println(entry.getKey()+" : "+entry.getValue());
		}
	}
}
