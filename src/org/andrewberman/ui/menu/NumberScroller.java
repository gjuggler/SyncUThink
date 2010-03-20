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
package org.andrewberman.ui.menu;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import java.text.DecimalFormat;

import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;

import processing.core.PApplet;
import processing.core.PFont;

public class NumberScroller extends MenuItem
{
	private float value = 0, defaultValue = 0, increment = 1, scrollSpeed = 1;
	private float min = -Float.MAX_VALUE, max = Float.MAX_VALUE;
	// private int minDigits, maxDigits;
	private String stringValue;
	private DecimalFormat df;

	private Field field;
	private Object fieldObj;
	private boolean useReflection;

	private float tWidth, nWidth, nOffset;
	boolean scrolling;
	float startY, startVal;

	public NumberScroller()
	{
		super();

		df = new DecimalFormat("#######0.0#");
		df.setDecimalSeparatorAlwaysShown(false);

		setIncrement(increment);
		setDefault(defaultValue);
		setValue(value);
		setScrollSpeed(scrollSpeed);
		
		stringValue = new String();
	}

	public float getValue()
	{
		float oldValue = value;
		try
		{
			if (useReflection)
				value = field.getFloat(fieldObj);
		} catch (Exception e)
		{
			useReflection = false;
			e.printStackTrace();
		}
		if (value != oldValue)
			updateString();
		return value;
	}

	void updateString()
	{
		stringValue = df.format(value);
	}

	public void setDefault(float def)
	{
		defaultValue = def;
		setValue(defaultValue);
	}

	public void setMin(float min)
	{
		this.min = min;
	}

	public void setMax(float max)
	{
		this.max = max;
	}

	public void setProperty(Object obj, String prop)
	{
		try
		{
			field = obj.getClass().getField(prop);
			fieldObj = obj;
			useReflection = true;
		} catch (Exception e)
		{
			e.printStackTrace();
			field = null;
			return;
		}
		setValue(defaultValue);
	}

	public void setIncrement(float inc)
	{
		increment = inc;
		/*
		 * Try and auto-detect number of decimal places from the increment.
		 */
		int numDecimals = (int) Math.ceil((float) -Math.log10(increment));
		// System.out.println(Math.log10(increment)+ " "+getName() + " " +
		// increment + " " + numDecimals);
		df.setMinimumFractionDigits(numDecimals);
		df.setMaximumFractionDigits(numDecimals);

		setValue(value);
	}

	public void setScrollSpeed(float changePerPixel)
	{
		scrollSpeed = changePerPixel;
	}

	protected void drawMyself()
	{
		super.drawMyself();
		getValue();
		if (scrolling)
		{
			/*
			 * Cause the menu to re-layout in case we've changed preferred size.
			 */
			menu.layout();
		}
		
		float px = menu.getStyle().getF("f.padX");
		float py = menu.getStyle().getF("f.padY");

		float curX = x + px;
		MenuUtils.drawLeftText(this, getName() + ":", curX);
		curX += tWidth;

		curX = getX() + getWidth() - px - nWidth;
		MenuUtils.drawSingleGradientRect(this, curX, y, nWidth, height);
		/*
		 * update the "value" object using Reflection.
		 */
		MenuUtils.drawText(this, stringValue, true, true, curX, y, nWidth,
				height);
	}

	public void setValue(float val)
	{
		float oldValue = value;
		value = PApplet.constrain(val, min, max);
		if (useReflection)
		{
			try
			{
				field.setFloat(fieldObj, value);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		updateString();
	}

	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
		
		PFont font = menu.getStyle().getFont("font");
		float fs = menu.getStyle().getF("f.fontSize");
		float px = menu.getStyle().getF("f.padX");
		float py = menu.getStyle().getF("f.padY");
		
		/*
		 * For the height, let's use the height of some capital letters.
		 */
		float tHeight = UIUtils.getTextHeight(menu.buff, font,
				fs, "XYZ", true);
		/*
		 * Calculate the text rectangle size.
		 */
		if (getName().length() > 0)
		{
			tWidth = UIUtils.getTextWidth(menu.buff, font,
					fs, getName() + ":", true);
			tWidth += px;
		}

		String s = stringValue;

		/*
		 * Store the beginning point for the number area.
		 */
		nWidth = 0;
		nWidth += UIUtils.getTextWidth(menu.buff, font,
				fs, s, true);
		nWidth += 2 * px;

		nOffset = getWidth() - px - nWidth;

		setWidth(px + tWidth + nWidth + px);
		setHeight(tHeight + 2 * py);
	}

	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		buff.setFrame(x, y, width, height);
		Rectangle2D.union(rect, buff, rect);
		super.getRect(rect, buff);
	}

	public void performAction()
	{
		// super.performAction();
	}

	protected void visibleMouseEvent(MouseEvent e, Point tempPt)
	{
		super.visibleMouseEvent(e, tempPt);

		if (mouseInside)
		{
			menu.setCursor(Cursor.N_RESIZE_CURSOR);
		}
		switch (e.getID())
		{
			case (MouseEvent.MOUSE_PRESSED):
				if (mouseInside)
				{
					if (e.getClickCount() > 1)
					{
						setValue(defaultValue);
					}
					startY = tempPt.y;
					startVal = getValue();
					scrolling = true;
					FocusManager.instance.setModalFocus(this.menu);
				}
				break;
			case (MouseEvent.MOUSE_DRAGGED):
				if (scrolling)
				{
					float dy = startY - tempPt.y;
					float dVal = dy * increment * scrollSpeed;
					value = startVal + dVal;
					setValue(value);
					e.consume();
				}
				break;
			case (MouseEvent.MOUSE_RELEASED):
				if (scrolling)
				{
					e.consume();
					scrolling = false;
					FocusManager.instance.removeFromFocus(this.menu);
				}
				break;
		}
	}

	static RoundRectangle2D.Float buffRoundRect = new RoundRectangle2D.Float(0,
			0, 0, 0, 0, 0);

	protected boolean containsPoint(Point p)
	{
		if (scrolling)
			return true;
		float ro = menu.getStyle().getF("f.roundOff");
		buffRoundRect.setRoundRect(x, y, width, height, ro,
				ro);
		// buffRoundRect.setRoundRect(x + nOffset, y, nWidth, height,
		// menu.getStyle().roundOff, menu.getStyle().roundOff);
		return buffRoundRect.contains(p);
	}

}
