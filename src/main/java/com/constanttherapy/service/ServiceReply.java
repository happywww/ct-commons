package com.constanttherapy.service;

import com.constanttherapy.util.GsonHelper;
import com.constanttherapy.util.HttpHelper;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** a reply sent from a CT endpoint, including data and messages */
public class ServiceReply {

	/** the data ... could be many things, an object of any type, a generic, a collection... */
	private Object mData = null;
	/** type, in case the mData is a generic and needs to have its type represented explicitly to Gson */
	private transient Type mDataType = null;
	/** messages (optional) */
	private List<ServiceMessage> mMessages = null;
	
	private void init() {
		mData = null;
		mDataType = null;
		mMessages = null;
	}
	
	public ServiceReply() {
		init();
	}
	
	public ServiceReply addMessage(ServiceMessage theMessage) {
		
		if (null == mMessages) mMessages = new ArrayList<ServiceMessage>();
		mMessages.add(theMessage);
		// done
		return this;
	}
	
	/** set the data portion of the reply - use this for top level types 
	 * that are generic e.g. for data that is List<Object> */
	public ServiceReply setData(Object theData, Type theType) {
		
		assert((theData != null) && (theType != null));
		
		mData = theData;
		mDataType = theType;
		
		// done
		return this;
	}
	
	/** set the data portion of the reply - use this for top level types
	 * that are _not_ generic e.g. not collections */
	public ServiceReply setData(Object theData) {
		assert((theData != null));
		
		mData = theData;
		
		// done
		return this;

	}
	
	/** set the data portion of the reply - create a simple response
	 * consisting of an object with one key-value pair */
	public ServiceReply setData(String key, String value) {
		Map<String,String> theObject = new HashMap<String,String>();
		theObject.put(key, value);
		setData(theObject);
		return this;
	}
			
	/** converts to JSON, but output depends on whether the requestor is one of our
	 * servers, or a mobile client - our servers don't like the messaging format,
	 * at least not for now */
	public String toJson(HttpServletRequest theRequest) {
		
		if (HttpHelper.isFromCTServer(theRequest)) {
			
			// old school format without messages
			if (null == mDataType) return GsonHelper.toJson(mData);
			else return GsonHelper.toJson(mData, mDataType);
			
		} else {
			
			// full format with messages
			return toJson();
			
		}
	}
	
	/** converts to JSON */
	public String toJson() {
		
		assert(mData != null);
		
		// we are trying to return a JSON object that contains
		// two key value pairs at the top level ... the first
		// is key "ctdata" and has as its value the JSON for
		// our mData member variable.  The second is "ctmsgs",
		// which has as its value an array that contains
		// one or more message objects
		
		// the tricky part is that our mData may be a generic
		// ... in which case we will need the data type provided 
		// by the user ... we will assume that iff the data type 
		// has been provided we need to use it
		
		// ctdata
		String dataValue = null;
		if (null == mDataType) dataValue = GsonHelper.toJson(mData);
		else dataValue = GsonHelper.toJson(mData, mDataType);
		
		StringBuilder out = new StringBuilder();
		out.append("{\"ctdata\":");
		out.append(dataValue);
		
		// ctmsgs
		if (null != mMessages) {
			String msgsValue = GsonHelper.toJson(mMessages);
			out.append(",\"ctmsgs\":");
			out.append(msgsValue);
		}
		
		// end the object
		out.append("}");
		
		// done
		return out.toString();
	}
}