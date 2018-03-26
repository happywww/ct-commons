package com.constanttherapy.util;

import com.google.gson.JsonSyntaxException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;

/** email validator ... it's not static because we will probably use other stuff from the
 * cloud API besides just validity, so we will probably have some state information we keep
 * representing other results from the cloud API ... though right now we don't */
public class EmailValidator {

	private static final String URL_FOR_GET = "https://bpi.briteverify.com/emails.json";
	private static final String API_KEY = "d957e2fb-a49b-4873-972f-20c11681ec69"; // BriteVerify
	private String mEmail = null;
	private String mResponseRawString = null;
	private BriteVerifyPOJO mResponseObject = null;

	private List<AbstractMap.SimpleEntry<String, String>> params = new ArrayList<AbstractMap.SimpleEntry<String, String>>();

	public EmailValidator addParam(String key, String value) {
		assert((key != null) && (value != null));

		params.add(new AbstractMap.SimpleEntry<String, String>(key, value));
		return this;
	}

	public EmailValidator() {
		init();
	}

	private void init() {
		mEmail = null;
		mResponseRawString = null;
		mResponseObject = null;
	}

	public EmailValidator(String theEmail) {
		init();
		mEmail = theEmail;
	}


	public boolean checkValidity(String email) {
		mEmail = email;
		return checkValidity();
	}

	private class BriteVerifyPOJO {

		// Format of JSON response from BriteVerify...
		// {"address":"westpostedddd@gmail.com",
		//  "account":"westpostedddd",
		//  "domains":"gmail.com",
		//  "status":"invalid",
		//  "connected":null,
		//  "disposable":false,
		//  "role_address":false,
		//  "error_code":"email_account_invalid",
		//  "error":"Email account invalid",
		//  "duration":3.277712766}

		@SuppressWarnings("unused")
		public static final String BRITE_VERIFY_VALID_TOKEN = "valid";
		public static final String BRITE_VERIFY_INVALID_TOKEN = "invalid";

		public String status = null;

		@SuppressWarnings("unused")
		public String address = null;
		@SuppressWarnings("unused")
		public String account = null;
		@SuppressWarnings("unused")
		public String domain = null;
		@SuppressWarnings("unused")
		public String connected = null;
		@SuppressWarnings("unused")
		public boolean disposable = false;
		@SuppressWarnings("unused")
		public boolean role_address = false;
		@SuppressWarnings("unused")
		public String error_code = null;
		@SuppressWarnings("unused")
		public float duration;

	}

	public boolean checkValidity() {

		boolean returnValue = true;

		// simple check
		if (false == StringUtil.isEmailAddressValid(mEmail)) return false;

		// go to cloud API
		addParam("address", mEmail);
		addParam("apikey", API_KEY);
		mResponseRawString = HttpHelper.doSimpleHttpGetForJsonResponse(URL_FOR_GET, params);
		if (null != mResponseRawString) {
			CTLogger.debug(mResponseRawString);

			try {

				mResponseObject = GsonHelper.getGson().fromJson(mResponseRawString,  BriteVerifyPOJO.class);
				// for now, only barf if we get "invalid" ... ignore the other non-valid statuses e.g. "unknown" which
				// means we can't really tell ...
				returnValue = (0 != mResponseObject.status.compareTo(BriteVerifyPOJO.BRITE_VERIFY_INVALID_TOKEN));

			} catch (JsonSyntaxException e) {
				returnValue = false;
				e.printStackTrace();
			}
		}

		// done
		return returnValue;

	}

}
