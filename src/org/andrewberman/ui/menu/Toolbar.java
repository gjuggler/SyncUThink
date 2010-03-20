/*******************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of PhyloWidget.
 * 
 * PhyloWidget is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 2 of the License, or (at your option) any later
 * version.
 * 
 * PhyloWidget is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * PhyloWidget. If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.ui.menu;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;

import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.Positionable;
import org.andrewberman.ui.ifaces.Sizable;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * A <code>Toolbar</code> is a menu that acts more or less like a
 * Windows-style toolbar. The two main options for the Toolbar class are:
 * <code>fullWidth</code>, which if set to true will draw itself across the
 * entire width of the screen, and <code>isModal</code>, which if set to true
 * will make the toolbar grab modal focus from the FocusManager when one of its
 * menus is opened.
 * 
 * @author Greg
 */
public class Toolbar extends Menu
{
	RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(0, 0, 0, 0,
			0, 0);
	Rectangle2D.Float buffRect = new Rectangle2D.Float(0, 0, 0, 0);

	/**
	 * If set to true, the toolbar will take up the entire width of the PApplet.
	 */
	public boolean fullWidth;
	/**
	 * If true, this option will cause the menu to grab modal focus from the
	 * FocusManager upon opening.
	 */
	public boolean isModal;

	public int orientation = HORIZONTAL;
	public static final int HORIZONTAL = 0;
	public static final int VERTICAL = 1;

	public Toolbar(PApplet app)
	{
		super(app);
		layout();
		open();
	}

	protected boolean isActive()
	{
		return hasOpenChildren();
	}

	public void setOptions()
	{
		super.setOptions();
		/*
		 * Override some default options from the Menu class.
		 */
		useCameraCoordinates = true;
		clickToggles = true;
		hoverNavigable = true;
		clickAwayBehavior = Menu.CLICKAWAY_COLLAPSES;
		// actionOnMouseDown = true;
		useHandCursor = true;
		autoDim = false;

		fullWidth = false;
		// isModal = true;
		/*
		 * Do some automatic positioning.
		 */

		float sw = style.getF("f.strokeWeight");

		if (fullWidth)
		{
			x = sw;
			y = sw;
		} else if (!useCameraCoordinates)
		{
			x = sw;
			y = sw;
		}
	}

	public void draw()
	{
		if (fullWidth)
			setFullWidth();
		super.draw();
	}

	@Override
	public void open(MenuItem i)
	{
		super.open(i);
		FocusManager.instance.setFocus(this);
	}

	@Override
	public void close(MenuItem item)
	{
		super.close(item);
		FocusManager.instance.removeFromFocus(this);
	}

	@Override
	public void close()
	{
		closeMyChildren();
		kbFocus = null;
	}

	public void drawBefore()
	{
		if (orientation == HORIZONTAL)
			MenuUtils.drawDoubleGradientRect(this, x, y, width, height);
		else
			MenuUtils.drawVerticalGradientRect(this, x, y, width, height);
	}

	public MenuItem create(String name)
	{
		ToolbarItem ti = new ToolbarItem();
		if (orientation == HORIZONTAL)
			ti.setLayoutMode(ToolbarItem.LAYOUT_BELOW);
		else
			ti.setLayoutMode(ToolbarItem.LAYOUT_RIGHT);
		ti.drawChildrenTriangle = true;
		ti.setName(name);
		return ti;
	}

	protected void clickaway()
	{
		super.clickaway();
		if (isModal)
		{
			FocusManager.instance.removeFromFocus(this);
		}
	}

	public void layout()
	{
		float px = style.getF("f.padX");
		float py = style.getF("f.padY");
		float xOffset = px;
		float yOffset = py;

		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			item.calcPreferredSize();
			float itemWidth = item.getWidth();
			float itemHeight = item.getHeight();
			if (item instanceof Positionable)
			{
				Positionable pos = (Positionable) item;
				pos.setPosition(x + xOffset, y + yOffset);
			}

			if (orientation == HORIZONTAL)
				xOffset += itemWidth;
			else
				yOffset += itemHeight;
		}
		
		/*
		 * Set this Toolbar's width and height.
		 */
		float maxHeight = getMaxHeight();
		float maxWidth = getMaxWidth();

		if (orientation == HORIZONTAL)
		{
			if (!fullWidth)
				setWidth(xOffset + px);
			else
				setFullWidth();
			setHeight(maxHeight + py * 2);
		} else
		{
			setHeight(yOffset + py);
			setWidth(maxWidth + px * 2);
		}
		
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = items.get(i);
			if (orientation == HORIZONTAL)
			{
				item.setHeight(maxHeight);
			} else
			{
				item.setWidth(maxWidth);
			}
		}
		
		/*
		 * Trigger the recursive layout.
		 */
		super.layout();
	}

	void setFullWidth()
	{
		float sw = style.getF("f.strokeWeight");

		if (orientation == HORIZONTAL)
		{
			width = canvas.width - sw * 2;
		} else
		{
			height = canvas.height - sw * 2;
		}
	}

	@Override
	public void setState(MenuItem i, int s)
	{
		if (!isActive())
			hoverNavigable = false;
		super.setState(i, s);
		if (i.parent == this && isActive() && s != MenuItem.UP)
		{
			closeMyChildren();
			open(i);
		}
		hoverNavigable = true;
	}

	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		super.getRect(rect, buff);
		if (isOpen())
		{
			buff.setRect(x, y, width, height);
			Rectangle2D.union(rect, buff, rect);
		}
	}

	protected void itemMouseEvent(MouseEvent e, Point pt)
	{
		super.itemMouseEvent(e, pt);
		if (isActive() && autoDim)
			aTween.continueTo(fullAlpha);
		// if (e.getID() == MouseEvent.MOUSE_PRESSED)
		// {
		// if (clickedInside)
		// System.out.println("HEY");
		// }
		// if (isActive() && isModal)
		// {
		// FocusManager.instance.setModalFocus(this);
		// }
	}

	protected void focusToItem(MenuItem i)
	{
		kbFocus = i;
		close(this);
		MenuItem parent = i.parent;
		while (parent != null)
		{
			open(parent);
			parent = parent.parent;
		}
		hoverNavigable = false;
		setState(i, MenuItem.OVER);
		hoverNavigable = true;
	}

	void focusWrap(MenuItem base, int dir)
	{
		int index = base.parent.items.indexOf(base);
		if (dir > 0)
			index++;
		else if (dir < 0)
			index--;
		if (index > base.parent.items.size() - 1)
			index = 0;
		else if (index < 0)
			index = base.parent.items.size() - 1;
		focusToItem(base.parent.items.get(index));
	}

	@Override
	public void keyEvent(KeyEvent e)
	{
		super.keyEvent(e);
		if (FocusManager.instance.getFocusedObject() != this)
			return;
		// System.out.println("EVENT!");
		if (kbFocus == null)
		{
			items.get(0).setState(MenuItem.OVER);
		}
		int code = e.getKeyCode();
		int type = e.getID();
		if (type != KeyEvent.KEY_PRESSED)
			return;

		MenuItem p = kbFocus.parent;
		switch (code)
		{
			case (KeyEvent.VK_ENTER):
				if (kbFocus.hasChildren())
				{
					focusToItem(kbFocus.items.get(0));
				} else
				{
					kbFocus.performAction();
				}
				break;
			case (KeyEvent.VK_RIGHT):
				if (p == this)
				{
					focusWrap(kbFocus, 1);
				} else
				{
					if (kbFocus.hasChildren())
					{
						focusToItem(kbFocus.items.get(0));
					} else
					{
						/*
						 * Finds the currently open top-level menuitem, and move
						 * one to the right. Should wrap around to the left if
						 * necessary.
						 */
						for (int i = 0; i < items.size(); i++)
						{
							MenuItem m = items.get(i);
							if (m.isAncestorOf(kbFocus))
							{
								focusWrap(m, 1);
//								System.out.println(kbFocus);
								focusToItem(kbFocus.items.get(0));
								break;
							}
						}
					}
				}
				break;
			case (KeyEvent.VK_LEFT):
				if (p == this)
				{
					focusWrap(kbFocus, -1);
				} else if (p.parent == this)
				{
					focusWrap(p, -1);
					focusToItem(kbFocus.items.get(0));
				} else
				{
					focusToItem(p);
				}
				break;
			case (KeyEvent.VK_DOWN):
				if (p == this)
				{
					focusToItem(kbFocus.items.get(0));
				} else
				{
					focusWrap(kbFocus, 1);
				}
				break;
			case (KeyEvent.VK_UP):
				if (p == this)
				{
					focusToItem(kbFocus.items.get(0));
				} else
				{
					focusWrap(kbFocus, -1);
				}
				break;
		}
		// System.out.println(kbFocus);
	}

	protected boolean containsPoint(Point pt)
	{
		// buffRect.setRect(x, y, width, height);
		// return buffRect.contains(pt);
		return false;
	}

	public int getOrientation()
	{
		return orientation;
	}

	public void setOrientation(int orientation)
	{
		this.orientation = orientation;
	}
}
