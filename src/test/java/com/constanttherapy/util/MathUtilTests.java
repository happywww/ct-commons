package com.constanttherapy.util;

import com.constanttherapy.util.MathUtil;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class MathUtilTests
{
	@Test
	public void test_toIntArray()
	{
		Integer[] test = MathUtil.toIntArray("1-10", null, null);
		assertTrue (test.length == 10);

		// min, max are ignored since we have specific min and max in range
		test = MathUtil.toIntArray("1-10", 2, 9);
		assertTrue (test.length == 10);

		test = MathUtil.toIntArray(">10", null, null);
		assertTrue (test.length == 15);

		// min value should be ignored
		test = MathUtil.toIntArray(">10", 12, null);
		assertTrue (test.length == 15);

		// max value should be honored
		test = MathUtil.toIntArray(">10", 12, 20);
		assertTrue (test.length == 10);

		test = MathUtil.toIntArray("<10", null, null);
		assertTrue (test.length == 9);

		// min value should be honored
		test = MathUtil.toIntArray("<10", 4, null);
		assertTrue (test.length == 6);

		// max value should be ignored
		test = MathUtil.toIntArray("<10", null, 20);
		assertTrue (test.length == 9);

		test = MathUtil.toIntArray("11", null, null);
		assertTrue (test.length == 1);

		// min and max both ignored
		test = MathUtil.toIntArray("11", 100, 200);
		assertTrue (test.length == 1);

		test = MathUtil.toIntArray("11 | 15|18", null, null);
		assertTrue (test.length == 3);
	}

	@Test
	public void test_versionStringToFloat()
	{
		float test = MathUtil.versionStringToFloat("1.2.3");
		assertTrue(test == 1.203f);

		test = MathUtil.versionStringToFloat("1.2.3.4");
		assertTrue(test == 1.203f);

		test = MathUtil.versionStringToFloat("1.2333.4444");
		assertTrue(test == 1.23334444f);

		test = MathUtil.versionStringToFloat("1.2");
		assertTrue(test == 1.2f);

		test = MathUtil.versionStringToFloat("1");
		assertTrue(test == 1.0f);
	}

	@Test
	public void test_randomInt()
	{
		int i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(1, 2);
		assertTrue(i < 3 && i > 0);

		i = MathUtil.randomInt(10, 15);
		assertTrue(i >= 10 && i <= 15);

		i = MathUtil.randomInt(10, 15);
		assertTrue(i >= 10 && i <= 15);

		i = MathUtil.randomInt(10, 15);
		assertTrue(i >= 10 && i <= 15);

		i = MathUtil.randomInt(10, 15);
		assertTrue(i >= 10 && i <= 15);

		i = MathUtil.randomInt(10, 15);
		assertTrue(i >= 10 && i <= 15);

		i = MathUtil.randomInt(100, 200);
		assertTrue(i >= 100 && i <= 200);

		i = MathUtil.randomInt(100, 200);
		assertTrue(i >= 100 && i <= 200);

		i = MathUtil.randomInt(100, 200);
		assertTrue(i >= 100 && i <= 200);

		i = MathUtil.randomInt(100, 200);
		assertTrue(i >= 100 && i <= 200);

		i = MathUtil.randomInt(100, 200);
		assertTrue(i >= 100 && i <= 200);

	}

	@Test
	public void test_round()
	{
		Integer i = (int) MathUtil.round(3.134124, 0);
		System.out.println(i);
	}
}
