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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;

import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;

import processing.core.PApplet;
import processing.core.PFont;

public class CheckBox extends MenuItem
{
	public final static float CHECKBOX_SIZE = .9f;
	
	private boolean value;

	private Field field;
	private Method method;
	private Object methodObj;
	private Object fieldObj;
	private boolean useReflection;

	private float tWidth, nWidth, nOffsetX, nHeight, nOffsetY;
	
	@Override
	public boolean getCloseOnAction()
	{
		return false;
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
		// setVal(defaultValue);
	}

	public void setMethodCall(Object obj, String meth)
	{
		try {
			method = obj.getClass().getMethod(meth,Boolean.TYPE);
			methodObj = obj;
			useReflection = true;
		} catch (Exception e)
		{
			e.printStackTrace();
			method = null;
			return;
		}
	}
	
	public void setValue(String s)
	{
		setVal(Boolean.parseBoolean(s));
	}

	void setVal(boolean value)
	{
		this.value = value;
		
		if (useReflection)
		{
			try
			{
				if (field != null)
					field.setBoolean(fieldObj, value);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
			try 
			{
				if (method != null)
					method.invoke(methodObj, value);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	public boolean getValue()
	{
		return value;
	}
	
	public void drawMyself()
	{
		super.drawMyself();

		float px = menu.style.getF("f.padX");
		float py = menu.style.getF("f.padY");
		
		float curX = x + px;
		MenuUtils.drawLeftText(this, getName() + ":", curX);
//		curX += tWidth;
		curX = getX() + getWidth() - px - nWidth;
		
		if (getState() == MenuItem.UP)
		{
			menu.buff.strokeWeight(0.5f);
			menu.buff.stroke(100);
			menu.buff.noFill();
			menu.buff.rect(curX, y+nOffsetY, nWidth, nHeight);
//			MenuUtils.drawBlankRect(this, curX, y+nOffsetY, nWidth, nHeight);
		} else
		{
			MenuUtils.drawSingleGradientRect(this, curX, y+nOffsetY, nWidth, nHeight,0);
		}
		/*
		 * update the "value" object using Reflection.
		 */
		try
		{
			if (useReflection)
				value = field.getBoolean(fieldObj);
		} catch (Exception e)
		{
			useReflection = false;
			e.printStackTrace();
		}
		
		/*
		 * Draw the check mark, if necessary.
		 */
		if (value)
		{
			drawCheckMark();
		}
		
		
	}

	private void drawCheckMark()
	{
		float w = nWidth * .75f;
		float h = nHeight * .75f;
		float x0 = x + nOffsetX + (nWidth - w)/2f;
		float y0 = y + nOffsetY + (nHeight - h)/2f;
		menu.canvas.strokeWeight(nHeight/8f);
		menu.canvas.stroke(0);
		menu.canvas.line(x0+w*.2f, y0+h*.6f,
				x0+w*.5f, y0+h*.9f);
		menu.canvas.line(x0+w*.5f, y0+h*.9f,
				x0+w*.8f,y0+h*.2f);
	}
	
	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
		
		PFont font = menu.style.getFont("font");
		float fontSize = menu.style.getF("f.fontSize");
		float px = menu.style.getF("f.padX");
		float py = menu.style.getF("f.padY");
		
		/*
		 * For the height, let's use the height of some capital letters.
		 */
		float tHeight = UIUtils.getTextHeight(menu.buff, font,
				fontSize, "XYZ", true);
		/*
		 * Calculate the text rectangle size.
		 */
		if (getName().length() > 0)
		{
			tWidth = UIUtils.getTextWidth(menu.buff, font,
					fontSize, getName() + ":", true);
			tWidth += px;
		}

		setHeight(tHeight + 2 * py);
		
		nOffsetX = getWidth() - px - nWidth;
		nHeight = tHeight * CHECKBOX_SIZE;
		nWidth = nHeight;
		nOffsetY = (getHeight() - nHeight)/2f;

		setWidth(px + tWidth + nWidth + px);
		
	}

	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		buff.setFrame(x, y, width, height);
		Rectangle2D.union(rect, buff, rect);
		super.getRect(rect, buff);
	}

	public void performAction()
	{
		setVal(!value);
		super.performAction();
	}

	protected void visibleMouseEvent(MouseEvent e, Point tempPt)
	{
		super.visibleMouseEvent(e, tempPt);

		if (mouseInside)
		{
			menu.setCursor(Cursor.HAND_CURSOR);
		}
		switch (e.getID())
		{
			case (MouseEvent.MOUSE_PRESSED):
				if (mouseInside)
				{
//					setVal(!value);
				}
				break;
			case (MouseEvent.MOUSE_DRAGGED):

				break;
			case (MouseEvent.MOUSE_RELEASED):
				break;
		}
	}

	static RoundRectangle2D.Float buffRoundRect = new RoundRectangle2D.Float(0,
			0, 0, 0, 0, 0);

	protected boolean containsPoint(Point p)
	{
		float ro = menu.style.getF("f.roundOff");
		buffRoundRect.setRoundRect(x, y, width, height, ro,
				ro);
//		buffRoundRect.setRoundRect(x + nOffsetX, y + nOffsetY, nWidth, nHeight,
//				menu.style.roundOff, menu.style.roundOff);
		return buffRoundRect.contains(p);
	}

	@Override
	public void keyEvent(KeyEvent e)
	{
		super.keyEvent(e);
		if (e.getID() != KeyEvent.KEY_PRESSED)
			return;
		switch (e.getKeyCode())
		{
			case (KeyEvent.VK_SPACE):
				performAction();
				break;
			case (KeyEvent.VK_ENTER):
				break;
		}
	}
	
}
