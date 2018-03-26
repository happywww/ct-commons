package com.constanttherapy.share.tasks.plain;

import java.util.ArrayList;
import java.util.List;

/** the possible responses that a user can choose from */
public class PlainTaskPresentedResponses {

	/** how long user has to choose a response, if zero they are allowed infinite time */
	public Integer itemTimeout = 0;
	public List<PlainTaskChoice> choices = new ArrayList<PlainTaskChoice>();

}
