/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */

package com.constanttherapy.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Random;

public class MathUtil
{
	/**
	 * Singleton Java Random object to avoid multiple instantiations.
	 * Might be good to shift to ThreadLocalRandom in the future.
	 */
	private static Random random;

	public static boolean randomBoolean()
	{
		if (random == null) random = new Random();
		return random.nextBoolean();
	}

	//	public static <T extends Comparable<T>> T clamp(T value, T min, T max)
	//	{
	//		return value.compareTo(max)>0 ? max : (value.compareTo(min)<0 ? min : value);
	//	}

	public static int clamp(int value, int min, int max)
	{
		return value > max ? max : (value < min ? min : value);
	}

	public static float clamp(float value, float min, float max)
	{
		return value > max ? max : (value < min ? min : value);
	}

	public static double clamp(double value, double min, double max)
	{
		return value > max ? max : (value < min ? min : value);
	}

	public static double gcd(double a, double b)
	{
		while (b > 0)
		{
			double temp = b;
			b = a % b; // % is remainder
			a = temp;
		}
		return a;
	}

	public static double gcd(double[] input)
	{
		double result = input[0];
		for (int i = 1; i < input.length; i++)
			result = gcd(result, input[i]);
		return result;
	}

	public static double lcm(double a, double b)
	{
		return a * (b / gcd(a, b));
	}

	public static double lcm(Object[] objects)
	{
		double result = (Double) objects[0];
		for (int i = 1; i < objects.length; i++)
			result = lcm(result, (Double) objects[i]);
		return result;
	}

	public static boolean isNumeric(String str)
	{
		try
		{
			@SuppressWarnings("unused")
			double d = Double.parseDouble(str);
		}
		catch (NumberFormatException nfe)
		{
			return false;
		}
		return true;
	}

	@Deprecated
	public static double roundTo(double input, int decimalPlaces)
	{
		double l = Math.pow(10, decimalPlaces);
		double scaled = Math.round(input * l);
		return scaled / l;
	}

	/*
	 * Attempts to convert specified string to Integer.  If unsuccessful, it returns null.
	 */
	public static Float tryParseFloat(String value)
	{
		return tryParseFloat(value, null);
	}

	/*
	 * Attempts to convert specified string to Integer.  If unsuccessful, it returns the default value.
	 */
	public static Float tryParseFloat(String value, Float defaultValue)
	{
		if (value == null) return defaultValue;

		try
		{
			return Float.parseFloat(value);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	/**
	 * Attempts to convert specified string to Integer.
	 * @param value
	 * @return Integer value, or null if invalid.
	 */
	public static Integer tryParseInt(String value)
	{
		return tryParseInt(value, null);
	}

	/**
	 * Attempts to convert specified string to Integer.
	 * @param value
	 * @return Integer value, or defaultValue if invalid.
	 */
	public static Integer tryParseInt(String value, Integer defaultValue)
	{
		if (value == null) return defaultValue;

		try
		{
			value = value.trim();
			return Integer.parseInt(value);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}

	/**
	 * Attempts to convert specified string to Long.
	 * @param value
	 * @return Long value, or null if invalid.
	 */
	public static Long tryParseLong(String value)
	{
		return tryParseLong(value, null);
	}

	/**
	 * Attempts to convert specified string to Long.
	 * @param value
	 * @return Long value, or defaultValue if invalid.
	 */
	public static Long tryParseLong(String value, Long defaultValue)
	{
		try
		{
			return Long.parseLong(value);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}

	public static double round(double unrounded, int precision)
	{
		try
		{
			BigDecimal bd = new BigDecimal(unrounded);
			BigDecimal rounded = bd.setScale(precision, BigDecimal.ROUND_HALF_UP);
			return rounded.doubleValue();
		}
		catch (Exception e)
		{
			CTLogger.error(e);
			return 0.0;
		}
	}

	public static boolean tryParseBoolean(String value)
	{
		if (value == null) return false;
		String val = value.toLowerCase();
		return (val.equals("t") || val.equals("true") || val.equals("1") || val.equals("y") || val.equals("yes"));
	}

	/**
	 * Converts range expression into an integer array
	 * @param rangeExpression Either a single value, multiple values separated by vertical bar (|), range (min-max), greater than (>min), less than (<max)
	 * @param minVal min value for range or null (in which case it defaults to 1)
	 * @param maxVal max value for range or null (in which case it defaults to 25)
	 * @return
	 */
	public static Integer[] toIntArray(String rangeExpression, Integer minVal, Integer maxVal)
	{
		// range expression can contain comma separated values
		// OR hyphen to specify range
		// OR < (less than)
		// OR > (greater than)
		Integer[] intArray = null;

		int min = (minVal == null) ? 1  : minVal;
		int max = (maxVal == null) ? 25 : maxVal;

		if (rangeExpression.contains("|")) // csv, example: 1|3|4
		{
			String[] s = rangeExpression.split("\\|");

			intArray = new Integer[s.length];
			for (int i = 0; i < s.length; i++)
				intArray[i] = MathUtil.tryParseInt(s[i]);

			return intArray;
		}
		else if (rangeExpression.contains("-")) // range, example: 2-4
		{
			String[] s = rangeExpression.split("-");

			// take the first two elements
			min = MathUtil.tryParseInt(s[0]);
			max = MathUtil.tryParseInt(s[1]);

		}
		else if (rangeExpression.startsWith(">"))
			min = MathUtil.tryParseInt(rangeExpression.substring(1)) + 1;
		else if (rangeExpression.startsWith("<"))
			max = MathUtil.tryParseInt(rangeExpression.substring(1)) - 1;
		else
		{
			// expression should be a single value
			min = MathUtil.tryParseInt(rangeExpression, min);
			max = min;
		}

		if (min > max) max = min;

		intArray = new Integer[max - min + 1];
		int j = 0;
		for (int i = min; i <= max; i++)
		{
			intArray[j] = i;
			j++;
		}

		return intArray;
	}

	public static <T> String toCSVString(T[] obj)
	{
		if (obj == null || obj.length == 0) return "";

		String s = obj[0].toString();

		for (int i = 1; i < obj.length; i++)
			s += "," + obj[i].toString();

		return s;
	}

	public static Integer[] toIntArray(List<Integer> counts)
	{
		Integer[] arr = new Integer[counts.size()];

		for (int i = 0; i < arr.length; i++)
			arr[i] = counts.get(i);

		return arr;
	}

	/**
	 * Converts a string version number with two periods into a float value.
	 * The minor version and build number get combined and placed to the right of the decimal point.
	 *
	 * If build number is single digit, it gets zero-padded. so 1.2.5 will become 1.205
	 * @param ver Version string
	 * @return Version number represented as a float
	 */
	public static float versionStringToFloat(String ver)
	{
		if (ver != null)
		{
			String[] s = ver.split("\\.");
			if (s.length > 2)
			{
				if (s[2].length() == 1)
					s[2] = "0" + s[2]; // pad with zero

				// combine the minor version + revision/build number
				ver = s[0] + "." + s[1] + s[2];
			}
			return tryParseFloat(ver, -1.0f);
		}
		else
			return -1.0f;
	}

	public static Integer randomInt(int min, int max)
	{
		Double r = (Math.random() * (max - min));
		Integer offset = (int) MathUtil.round(r, 0);
		return offset + min;
	}
}
