package com.constanttherapy.util;

import org.apache.commons.validator.routines.EmailValidator;

import java.util.*;

public class StringUtil
{
    private static String key = "SXGWLZPDOKFIVUHJYTQBNMACERxswgzldpkoifuvjhtybqmncare";

    /**
     * Keeps non-supported characters from db.
     */
    public static boolean checkPrintableAscii(String userInput)
    {
        String regex = "^[\\!-\\~|\\s]*{1,32}$";
        return userInput != null && userInput.matches(regex);
    }

    public static String decodeStr(String coded)
    {
        String uncoded = "";
        char chr;
        for (int i = coded.length() - 1; i >= 0; i--)
        {
            chr = coded.charAt(i);

            if (chr >= 'a' && chr <= 'z')
                uncoded += StringUtil.fromCharCode(97 + key.indexOf(chr) % 26);
            else if (chr >= 'A' && chr <= 'Z')
                uncoded += StringUtil.fromCharCode(65 + key.indexOf(chr) % 26);
            else
                uncoded += chr;
        }
        return uncoded;
    }

    public static String encodeStr(String uncoded)
    {
        String coded = "";
        char chr;
        for (int i = uncoded.length() - 1; i >= 0; i--)
        {
            chr = uncoded.charAt(i);

            if (chr >= 65 && chr <= 90)
                coded += key.charAt((int) (chr - 65 + 26 * Math.floor(Math.random())));
            else if (chr >= 97 && chr <= 122)
                coded += key.charAt((int) (chr - 97 + 26 + 26 * Math.floor(Math.random())));
            else
                coded += StringUtil.fromCharCode(chr);
        }
        return coded;
    }

    public static String fromCharCode(int... codePoints)
    {
        return new String(codePoints, 0, codePoints.length);
    }

    public static boolean isEmailAddressValid(final String email)
    {
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }


    public static String listToCSV(List<String> list, boolean andLast)
    {
        StringBuilder sb = new StringBuilder();

        int count = list.size();

        for (int i = 0; i < count; i++)
        {
            String s = list.get(i);
            if (i == 0)
                sb.append(s);
            else if ((i == count - 1) && andLast) // put an "and" before the last value
                sb.append(" and ").append(s);
            else
                sb.append(", ").append(s);
        }

        return sb.toString();
    }

    /**
     * Takes a Map<String, String> and converts into a CSV string: key1,val1,key2,val2...
     *
     * @param map
     * @return
     */
    public static String mapToCSV(Map<String, String> map)
    {
        StringBuilder sb = new StringBuilder();

        boolean first = true;

        for (String key : map.keySet())
        {
            if (first)
                first = false;
            else
                sb.append(",");

            sb.append(key).append(",").append(map.get(key));
        }

        return sb.toString();
    }

    public static Map<String, String> csvToMap(String csv)
    {
        Map<String, String> map = null;
        if (csv != null)
        {

            String[] toksArray = csv.split(",");

            // verify that the toksArray has even number of elements
            int len = toksArray.length;
            if (len % 2 != 0)
                throw new IllegalArgumentException("csv should contain even number of comma-separated elements");

            map = new HashMap<String, String>();
            for (int j = 0; j < len; j += 2) // iterate two at a time
            {
                map.put(toksArray[j], toksArray[j + 1]);
            }
        }

        return map;
    }

    /**
     * Since we should not be logging passwords, use this utility function to strip out
     * the part of the string that may contain password information and return the
     * sanitized string for logging to file.
     *
     * @param str
     * @return String truncated at the first occurrence of password
     */
    public static String truncatePasswordFromString(String str)
    {
        String str2;
        if (str.toLowerCase().contains("password"))
            str2 = str.split("password")[0];
        else if (str.toLowerCase().contains("pw"))
            str2 = str.split("pw")[0];
        else
            str2 = str;

        return str2;
    }

    public static String listToString(List<String> list, String separator)
    {
        StringBuilder sb = new StringBuilder();

        int count = list.size();

        for (int i = 0; i < count; i++)
        {
            String s = list.get(i);
            if (i == 0)
                sb.append(s);
            else
                sb.append(separator).append(s);
        }

        return sb.toString();
    }

	public static List<String> stringToList(String str)
	{
		// default to comma separated string
		return stringToList(str, ",");
	}

	public static List<String> stringToList(String str, String separatorRegex)
	{
		List<String> list = new ArrayList<String>();
		if (str != null)
		{
			String[] arr = str.split(separatorRegex);
			Collections.addAll(list, arr);
		}
		return list;
	}

    public static String padRight(String s, int n)
    {
        return String.format("%1$-" + n + "s", s);
    }

    public static String padLeft(String s, int n)
    {
        return String.format("%1$" + n + "s", s);
    }
}
