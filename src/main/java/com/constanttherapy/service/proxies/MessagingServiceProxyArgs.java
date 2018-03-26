package com.constanttherapy.service.proxies;

public class MessagingServiceProxyArgs
{
	public String recipients;
	public String templateType;
	public String subject = null;
	public String body = null;
	public String replacementTokensJson = null;
	public boolean ccSupport = false;
	public String fromName = null;
	public String fromEmail = null;
	public boolean showStats = false;
	public String serverType = null;
}