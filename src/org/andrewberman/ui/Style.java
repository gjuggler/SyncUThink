package org.andrewberman.ui;

import java.util.HashMap;
import java.util.Properties;

public class Style extends HashMap<Object, Object>
{
	/**
	 * The parental style, from which we inherit values that weren't 
	 * directly set within this style.
	 */
	Style parent;

	/**
	 * A set of defaults particular to this style's class.
	 */
	protected static Style defaults;

	@Override
	public Object get(Object key)
	{
		if (containsKey(key))
		{
			return super.get(key);
		} else if (parent != null)
		{
			return parent.get(key);
		} else if (defaults != null && defaults.containsKey(key))
		{
			return defaults.get(key);
		} else
		{
			System.err.println("No style found for: "+key);
			return null;
		}
	}

	public void set(Object key, Object value)
	{
		put(key, value);
	}

	public float getF(Object key)
	{
		float f;
		try{
			f = ((Float) get(key));
		} catch (Exception e){
			f = ((Integer)get(key));
		}
		return f;
	}

	public Color getC(Object key)
	{
		return ((Color) get(key));
	}

	public int getI(Object key)
	{
		return ((Integer) get(key));
	}

	public Object getO(Object key)
	{
		return get(key);
	}

	public void setParent(Style p)
	{
		parent = p;
	}

}
