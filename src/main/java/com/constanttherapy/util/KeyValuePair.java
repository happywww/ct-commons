/*
Constant Therapy LLC Confidential & Proprietary
Copyright 2012, All rights reserved
Author:
Date:
 */

package com.constanttherapy.util;

import java.util.Map;
import java.util.Map.Entry;

public class KeyValuePair<K, V>
{
	public KeyValuePair(K key, V value) {
		this.key = key;
		this.value = value;
	}

	public K key;
	public V value;

    public static Map<String, String> convertNullStringsToNull(Map<String, String> input_map)
    {
        for (Entry<String, String> pairs : input_map.entrySet())
        {
			if (pairs.getValue()==null){continue;}
            if (pairs.getValue().equals("null") || pairs.getValue().equals("NULL"))
            {
				pairs.setValue(null);
			}
		}
		return input_map;
	}
}
