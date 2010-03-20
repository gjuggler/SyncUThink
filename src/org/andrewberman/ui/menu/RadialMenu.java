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

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;

import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIRectangle;

import processing.core.PApplet;
import processing.core.PConstants;

public class RadialMenu extends Menu
{

	public static final float FADE_DIST_MULTIPLIER = .5f;

	public float thetaLo = 0;
	public float thetaHi = PConstants.TWO_PI;
	float innerRadius;
	float radius;

	private VisibleDepthComparator visComp = new VisibleDepthComparator();

	UIRectangle myRect = new UIRectangle(0, 0, 0, 0);
	UIRectangle buffRect = new UIRectangle(0, 0, 0, 0);

	AffineTransform buffTransform, mouseTransform;

	protected int maxLevelOpen;

	public RadialMenu(PApplet p)
	{
		super(p);
	}

	public void setOptions()
	{
		super.setOptions();

		setRadii(10, 30);
		clickAwayBehavior = Menu.CLICKAWAY_HIDES;
		hoverNavigable = false;
		clickToggles = true;
		autoDim = true;
		useCameraCoordinates = true;
		modalFocus = true;
		useHandCursor = true;
	}

	public void setRadius(float r)
	{
		this.radius = r;
	}

	public void setRadii(float inner, float outer)
	{
		this.innerRadius = inner;
		this.radius = outer;
		layout();
	}

	public MenuItem create(String s)
	{
		/*
		 * Attempt to automatically find a good hint character to use with the
		 * new RadialMenuItem
		 */
		boolean foundGoodChar = false;
		int charInd = 0;
		for (int i = 0; i < s.length(); i++)
		{
			char c = s.charAt(i);
			if (!alreadyContainsChar(c) && Character.isLetter(c))
			{
				foundGoodChar = true;
				charInd = i;
				break;
			}
		}
		if (!foundGoodChar)
		{
			charInd = 0; // Oh well, let's just use the first char again.
		}
		return create(s, s.charAt(charInd));
	}

	protected boolean alreadyContainsChar(char c)
	{
		for (int i = 0; i < items.size(); i++)
		{
			RadialMenuItem rmi = (RadialMenuItem) items.get(i);
			if (rmi.alreadyContainsChar(c))
				return true;
		}
		return false;
	}

	public RadialMenuItem create(String s, char c)
	{
		RadialMenuItem rmi = new RadialMenuItem();
		rmi.setName(s);
		rmi.setHint(String.valueOf(c));
		return rmi;
	}

	public void setArc(float thetaLo, float thetaHi)
	{
		this.thetaLo = thetaLo;
		this.thetaHi = thetaHi;
		layout();
	}

	public void layout()
	{
		if (items.size() == 0)
			return;

		float dTheta = thetaHi - thetaLo;
		float thetaStep = dTheta / items.size();
		float start = thetaLo; // -PConstants.HALF_PI;
		for (int i = 0; i < items.size(); i++)
		{
			RadialMenuItem seg = (RadialMenuItem) items.get(i);
			seg.setPosition(x, y);
			float curTheta = start + i * thetaStep;
			seg.layout(innerRadius, radius, curTheta, curTheta + thetaStep);
		}
		findMaxLevelOpen();
	}

	public void findMaxLevelOpen()
	{
		MenuItem item = this;
		maxLevelOpen = 0;
		boolean shouldContinue = true;
		while (shouldContinue)
		{
			maxLevelOpen++;
			shouldContinue = false;
			for (int i = 0; i < item.items.size(); i++)
			{
				MenuItem item2 = (MenuItem) item.items.get(i);
				if (item2.isOpen())
				{
					item = item2;
					shouldContinue = true;
					break;
				}
			}
		}

	}

	//
	// public void draw()
	// {
	// super.draw();
	// }
	//
	// protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	// {
	// super.getRect(rect, buff);
	// }

	public void setPosition(float x, float y)
	{
		this.x = x;
		this.y = y;
		layout();
	}

	public void itemMouseEvent(MouseEvent e, Point pt)
	{
		super.itemMouseEvent(e, pt);
		myRect.setRect(x, y, 0, 0);
		myRect.setRect(x, y, 0, 0);
		getRect(myRect, buffRect);
		float dist = myRect.distToPoint(pt);
		float fadeDist = Math.max(myRect.width, myRect.height)
				* FADE_DIST_MULTIPLIER;
		fadeDist = Math.max(fadeDist, radius);
		if (dist < fadeDist)
		{
			if (autoDim)
			{
				float normalized = 1f - (dist / fadeDist);
				aTween.continueTo(normalized);
				aTween.fforward();
			}
		} else
		{
			close();
		}
	}

	public void keyEvent(KeyEvent e)
	{
		if (!isOpen())
			return;
		if (e.getID() == KeyEvent.KEY_TYPED)
		{
			ArrayList temp = (ArrayList) items.clone();
			Collections.sort(temp, visComp);
			for (int i = 0; i < temp.size(); i++)
			{
				if (!e.isConsumed())
				{
					RadialMenuItem item = (RadialMenuItem) temp.get(i);
					item.keyHintEvent(e);
				}
			}
		}
		e.consume();
	}
}
