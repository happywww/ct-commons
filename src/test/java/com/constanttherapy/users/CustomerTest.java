package com.constanttherapy.users;

import com.constanttherapy.enums.UserType;
import com.constanttherapy.util.CTTestBase;
import com.constanttherapy.util.GeoLocation;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomerTest extends CTTestBase
{
	@Test
	public void test_Create() throws SQLException, java.lang.UnsupportedOperationException
	{
        ////
        // Account creation tests.
        ////

		int userId = 1111;
		Customer cust = new Customer();

		cust.userId = userId;
		cust.firstName = "Delete";
		cust.lastName = "Me";
		cust.birthYear = 1901;
		cust.email = "mcaddu@gmail.com";

		String ipAddress = "72.74.163.25";
		cust.setIpAddress(_sqlr, ipAddress);

		cust.gender = "male";
		cust.education = "bachelor";
		cust.leadSource = "HLTH";
		cust.leadSourceDetails = "St. Elizabeth's";

		cust.create(_sql);
		Customer cust2 = Customer.getCustomerByUserId(_sql, userId);

		assertTrue(cust.firstName.equals(cust2.firstName));
		assertTrue(cust.gender.equals(cust2.gender));
		assertTrue(cust.education.equals(cust2.education));
		assertTrue(cust.leadSource.equals(cust2.leadSource));
		assertTrue(cust.leadSourceDetails.equals(cust2.leadSourceDetails));

        ////
        // Account update tests.
        ////

		cust2.comments = "Test";
		cust2.disorders = "1,2,3,4";

        cust2.gender = "female";
        cust2.education = "professional";
        cust2.leadSource = "tradeshow";
        cust2.leadSourceDetails = "ASHA";

		cust2.update(_sql);
		Customer cust3 = Customer.getCustomerByUserId(_sql, userId);

		assertTrue(cust2.comments.equals(cust3.comments));
        assertTrue(cust2.gender.equals(cust3.gender));
        assertTrue(cust2.education.equals(cust3.education));
        assertTrue(cust2.leadSource.equals(cust3.leadSource));
        assertTrue(cust2.leadSourceDetails.equals(cust3.leadSourceDetails));

        ////
        // Account deletion tests.
        ////

		cust.delete(_sql);
		Customer cust4 = Customer.getCustomerByUserId(_sql, userId);
		assertTrue(cust4 == null);
	}

	@Test
	public void test_BatchCreateMissingCustomerRecords() throws SQLException
	{
		Customer cust = new Customer();
		cust.firstName = "Allison";
		cust.username = "akc701";
		cust.email = "allisonpetrey@gmail.com";
		cust.userType = UserType.clinician;
		cust.whatPromptedYou = "To use in therapy";
		cust.role = "SLP";
		cust.facilityName = "VHA";
		cust.setting = "OPC";
		cust.leadSource = "INET";
		cust.signupLandingPage = "/ct_signupform_v2/ipad";
		cust.ipAddress = "65.50.80.18";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "ipad";
		cust.userId = 31643;
		cust.create(_sql);


		cust = new Customer();
		cust.username = "DPC";;
		cust.conditionSince = "2y";
		cust.birthYear = 1989;
		cust.userType = UserType.patient;
		cust.ipAddress = "65.50.80.18";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.leadSource = "Clinician_Setup";
		cust.userId = 31644;
		cust.create(_sql);

		cust = new Customer();
		cust.username = "Jim.P";
		cust.conditionSince = "1y";
		cust.birthYear = 1950;
		cust.userType = UserType.patient;
		cust.ipAddress = "65.50.80.18";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.leadSource = "Clinician_Setup";
		cust.userId = 31645;
		cust.create(_sql);


		cust = new Customer();
		cust.firstName = "Linda";
		cust.lastName = "Dinucci";
		cust.username = "lindadinucci";
		cust.email = "lindadinucci@gmail.com";
		cust.userType = UserType.clinician;
		cust.whatPromptedYou = "looking to provide carry over for stoke survivors";
		cust.role = "SLP";
		cust.facilityName = "REACH Program";
		cust.setting = "PP";
		cust.leadSource = "PAT";
		cust.phoneNumber = "6509496960";
		cust.signupLandingPage = "/signup/";
		cust.ipAddress = "107.194.154.203";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "browser";
		cust.userId = 31646;
		cust.create(_sql);

		cust = new Customer();
		cust.firstName = "Darla Plunkett";
		cust.username = "djplunkett39";
		cust.email = "adplunkett@wavecable.com";
		cust.userType = UserType.patient;
		cust.conditionSince = "1y";
		cust.setting = "OPC";
		cust.birthYear = 1938;
		cust.leadSource = "INET";
		cust.disorders = "STRK,APHA";
		cust.deficits = "READ,WRIT,NAME,ATTN,MEM,COMP,VIS,PROB";
		cust.signupLandingPage = "/ct_signupform_v2/android";
		cust.ipAddress = "76.14.190.156";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "android";
		cust.userId = 31647;
		cust.create(_sql);


		cust = new Customer();
		cust.username = "Smitheye";
		cust.conditionSince = "6m";
		cust.birthYear = 1944;
		cust.userType = UserType.patient;
		cust.ipAddress = "24.196.30.2";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.leadSource = "Clinician_Setup";
		cust.setting = "HOME";
		cust.userId = 31648;
		cust.create(_sql);

		cust = new Customer();
		cust.firstName = "Jess";
		cust.username = "jessrocks619";
		cust.email = "jess.graham85@yahoo.com";
		cust.userType = UserType.patient;
		cust.conditionSince = "6m";
		cust.setting = "OPC";
		cust.birthYear = 1985;
		cust.leadSource = "HLTH";
		cust.disorders = "APHA";
		cust.deficits = "READ,WRIT,NAME,ATTN,MEM,COMP,PROB";
		cust.signupLandingPage = "/ct_signupform_v2/ipad";
		cust.ipAddress = "76.241.9.174";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "ipad";
		cust.userId = 31649;
		cust.create(_sql);



		cust = new Customer();
		cust.firstName = "Laurence";
		cust.username = "Vaughan";
		cust.email = "mrlevaughan@gmail.com";
		cust.userType = UserType.patient;
		cust.conditionSince = ">10y";
		cust.birthYear = 1976;
		cust.leadSource = "Google play";
		cust.phoneNumber = "7203417156";
		cust.disorders = "DYSL";
		cust.deficits = "READ,WRIT,ATTN,MEM,COMP,PROB";
		cust.signupLandingPage = "/ct_signupform_v2/android";
		cust.ipAddress = "71.237.44.124";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "android";
		cust.userId = 31650;
		cust.create(_sql);



		cust = new Customer();
		cust.firstName = "Sara Woodruff";
		cust.username = "SLWoodruff";
		cust.email = "woodruff_sara@hotmail.com";
		cust.userType = UserType.clinician;
		cust.whatPromptedYou = "A student recommended this app";
		cust.role = "SLP";
		cust.facilityName = "Fairmont Rehabilitation Center";
		cust.setting = "OPC";
		cust.leadSource = "COLL";
		cust.phoneNumber = "3043633167";
		cust.signupLandingPage = "/ct_signupform_v2/ipad";
		cust.ipAddress = "24.123.159.142";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.signupLocation = "ipad";
		cust.userId = 31651;
		cust.create(_sql);

		cust = new Customer();
		cust.username = "FGHRehab1";
		cust.disorders = "All trial";
		cust.conditionSince = "6m";
		cust.birthYear = 1990;
		cust.userType = UserType.patient;
		cust.ipAddress = "24.123.159.142";
		cust.location = GeoLocation.getCityStateFromIpAddress(_sqlr, cust.ipAddress);
		cust.leadSource = "Clinician_Setup";
		cust.userId = 31652;
		cust.create(_sql);
	}

	@Test
	public void test_updateIdentifiableInfo() throws SQLException
	{
		final int USER_ID = 444;
		Customer cust = Customer.getCustomerByUserId(_sqlr, USER_ID);

		IdentifiableUserInfo userInfo = new IdentifiableUserInfo();

		if (cust.firstName.equals("John"))
		{
			userInfo.firstName = "Paul";
			userInfo.lastName = "McCartney";
		}
		else
		{
			userInfo.firstName = "John";
			userInfo.lastName = "Lennon";
		}

		cust.updateIdentifiableInfo(_sql, userInfo);

		cust = Customer.getCustomerByUserId(_sqlr, USER_ID);

		assertEquals(userInfo.firstName, cust.firstName);
	}

	@Test
	public void test_updateIdentifiableInfo_2() throws SQLException
	{
		final int USER_ID = 444;
		Customer cust = Customer.getCustomerByUserId(_sqlr, USER_ID);

		IdentifiableUserInfo userInfo = new IdentifiableUserInfo();

		if (cust.email.startsWith("mahendra"))
		{
			userInfo.email = "madvani@gmail.com";
		}
		else
		{
			userInfo.email = "mahendra.advani@constanttherapy.com";
		}

		cust.updateIdentifiableInfo(_sql, userInfo);

		cust = Customer.getCustomerByUserId(_sqlr, USER_ID);

		assertEquals(userInfo.email, cust.email);
	}
}
