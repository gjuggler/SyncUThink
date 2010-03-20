/**************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with PhyloWidget.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.ui.tween;

import java.lang.reflect.Field;

public class PropertyTween extends Tween
{
	Object o;
	Field field;
	Class fieldClass;
	String prop;
	
	public PropertyTween(Object object, String propertyName, TweenFunction function, int type,
			float start, float end, float duration)
	{
		super(null, function, type, start, end, duration);
		
		this.o = object;
		this.prop = propertyName;
		try
		{
			field = o.getClass().getField(prop);
//			field.setAccessible(true);
			fieldClass = field.getType();
		} catch (SecurityException e)
		{
			e.printStackTrace();
		} catch (NoSuchFieldException e)
		{
			e.printStackTrace();
		}
	}

	public float update()
	{
		float value = super.update();
		if (field == null) return value;
		try
		{
			if (fieldClass == float.class)
				field.setFloat(o, value);
			else if (fieldClass == int.class)
				field.setInt(o, (int)value);
		} catch (IllegalArgumentException e)
		{
			e.printStackTrace();
		} catch (IllegalAccessException e)
		{
			e.printStackTrace();
		}
		return value;
	}
}
