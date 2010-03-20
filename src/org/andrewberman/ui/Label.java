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
package org.andrewberman.ui;

import org.andrewberman.ui.ifaces.Malleable;

import processing.core.PApplet;

public class Label extends AbstractUIObject implements Malleable
{
	PApplet app;

	String label;
	
	Color color;
	float fontSize;
	float x, y;

	public Label(PApplet p)
	{
		this.app = p;
		UIUtils.loadUISinglets(p);
		EventManager.instance.add(this);

		color = new Color(Color.black);
		label = "";
		fontSize = 16;
		x = 0;
		y = 0;
	}

	public Label(PApplet p, String s)
	{
		this(p);
		setLabel(s);
	}

	public void setLabel(String s)
	{
		label = s;
	}

	public String getLabel()
	{
		return label;
	}

	public void draw()
	{
		app.smooth();
		app.fill(color.getRGB());
		app.textFont(FontLoader.instance.vera);
		app.textSize(fontSize);
		app.textAlign(PApplet.LEFT);
		app.text(label, x, y);
	}

	public float getX()
	{
		return x;
	}

	public float getY()
	{
		return y;
	}

	public void setFontSize(float f)
	{
		fontSize = f;
	}
	
	public void setPosition(float x, float y)
	{
		setPositionByCornerNW(x,y);
	}

	public void setPositionByBaseline(float x, float y)
	{
		this.x = x;
		this.y = y;
	}
	
	public void setPositionByCornerNW(float west, float north)
	{
		cache();
		x = west; // Nothing fancy here
		y = north + cacheA;
	}
	
//	public void setPositionByCornerSW(float west, float south)
//	{
//		cache();
//		x = west;
//		y = south - cacheD;
//	}
	
	public void setX(float f)
	{
		x = f;
	}

	public void setY(float f)
	{
		cache();
		y = f + cacheA;
	}

	public void setYBaseline(float f)
	{
		y = f;
	}
	
	public float getHeight()
	{
		cache();
		return cacheH;
	}

	void cache()
	{
		if (cacheS == label && cacheFS == fontSize)
			return;
		cacheS = label;
		cacheFS = fontSize;
		cacheH = UIUtils.getTextHeight(app.g, FontLoader.instance.vera, fontSize, label, true);
		cacheW = UIUtils.getTextWidth(app.g, FontLoader.instance.vera, fontSize, label, true);
		cacheA = UIUtils.getTextAscent(app.g, FontLoader.instance.vera, fontSize, true);
		cacheD = UIUtils.getTextDescent(app.g, FontLoader.instance.vera, fontSize, true);
	}
	
	float cacheFS;
	String cacheS;
	float cacheW;
	float cacheH;
	float cacheA;
	float cacheD;
	public float getWidth()
	{
		cache();
		return cacheW;
	}

	public void setHeight(float h)
	{
		setFontSize(getFontSize() * h/getHeight());
	}

	public void setWidth(float w)
	{
		setFontSize(getFontSize() * w/getWidth());
	}
	
	private float getFontSize()
	{
		return fontSize;
	}

	public void setColor(int r, int g, int b)
	{
		color = new Color(r,g,b);
	}
	
	public void setSize(float w, float h)
	{
	}

}
