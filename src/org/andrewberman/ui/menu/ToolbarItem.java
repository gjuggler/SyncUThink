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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D.Float;

import org.andrewberman.ui.Color;
import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.Positionable;
import org.andrewberman.ui.ifaces.Sizable;

import processing.core.PApplet;
import processing.core.PFont;

/**
 * The <code>ToolbarItem</code> class is a MenuItem that belongs to a Toolbar
 * object. It represents the "base" MenuItem of a Toolbar, i.e. the actual item
 * that says "File", "Edit", and so on.
 * 
 * @author Greg
 */
public class ToolbarItem extends MenuItem
{
	static AffineTransform at = new AffineTransform();
	static RoundRectangle2D.Float buffRoundRect = new RoundRectangle2D.Float(0,
			0, 0, 0, 0, 0);
	protected static final int LAYOUT_BELOW = 0;
	protected static final int LAYOUT_LEFT = 2;

	protected static final int LAYOUT_RIGHT = 1;
	static RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float(0, 0,
			0, 0, 0, 0);

	static final float shortcutTextSize = .75f;

	static Area tri;

	static float triWidth;

	boolean drawChildrenTriangle;

	protected int layoutMode;

	Rectangle2D.Float subItemRect = new Rectangle2D.Float();

	float tWidth, shortcutWidth;

	public ToolbarItem()
	{
		super();
	}
	protected void calcPreferredSize()
	{
		super.calcPreferredSize();
		
		PFont font = getFont();
		float fs = getFontSize();
		float px = getPadX();
		float py = getPadY();
		
		/*
		 * Calculate the text rectangle size.
		 */
		tWidth = UIUtils.getTextWidth(menu.canvas.g, font,
				fs, getName(), true);
		/*
		 * For the height, let's use the height of some capital letters.
		 */
		float tHeight = UIUtils.getTextHeight(menu.canvas.g, font,
				fs, "XYZ", true);

		float triangleWidth = 0;
		if (drawChildrenTriangle && items.size() > 0
				&& layoutMode != LAYOUT_BELOW)
		{
			/*
			 * Calculate the width of the "submenu" triangle shape.
			 */
			at = AffineTransform.getScaleInstance(tHeight / 2f, tHeight / 2f);
			Area tri = (Area) menu.style.get("subTriangle");
			Area a = tri.createTransformedArea(at);
			ToolbarItem.tri = a;
			ToolbarItem.triWidth = (float) a.getBounds2D().getWidth();
			triangleWidth = triWidth + px;
		}
		shortcutWidth = 0;
		if (shortcut != null)
		{
			shortcutWidth = px
					+ UIUtils.getTextWidth(menu.buff, font,
							fs * shortcutTextSize,
							shortcut.label, true);
		}

		setWidth(tWidth + triangleWidth + shortcutWidth + 2 * px);
		setHeight(tHeight + 2 * py);
	}
	protected boolean containsPoint(Point p)
	{
		float ro = menu.style.getF("f.roundOff");
		buffRoundRect.setRoundRect(x, y, width, height, ro,
				ro);
		return buffRoundRect.contains(p);
	}
	/**
	 * Normally, the MenuItem's create() method just defers back to the nearest
	 * Menu it can use to create an item, but here we want to change some
	 * options, so let's override it.
	 */
	public MenuItem create(String label)
	{
		ToolbarItem ti = new ToolbarItem();
		ti.setLayoutMode(ToolbarItem.LAYOUT_RIGHT);
		ti.drawChildrenTriangle = true;
		ti.setName(label);
		return ti;
	}

	protected void drawBefore()
	{
		if (isOpen() && items.size() > 0)
			MenuUtils.drawBackgroundRoundRect(this, subItemRect.x,
					subItemRect.y, subItemRect.width, subItemRect.height);
	}

	protected void drawMyself()
	{
		float ro = menu.style.getF("f.roundOff");
		Color strokeC = getStrokeColor();
		Stroke stroke = getStroke();
		float px = menu.style.getF("f.padX");
		float py = menu.style.getF("f.padY");
		
		roundRect.setRoundRect(x, y, width, height, ro,
				ro);
		Graphics2D g2 = menu.buff.g2;

		/*
		 * Set the correct fill gradient
		 */
		if (isOpen() && parent == menu)
		{
			g2.setPaint(menu.style.getGradient(MenuItem.DOWN, y, y + height));
		} else if (!hasChildren() && getState() == MenuItem.DOWN)
		{
			g2.setPaint(menu.style.getGradient(MenuItem.DOWN, y, y + height));
		} else
			g2.setPaint(menu.style.getGradient(getState(), y, y + height));

		/*
		 * Only perform the fill if the mood is right.
		 */
		if (getState() != MenuItem.UP || isOpen())
		{
			if (getState() == MenuItem.DISABLED)
			{
				g2.fill(roundRect);
				g2.setPaint(strokeC);
				g2.setStroke(stroke);
				g2.draw(roundRect);
			} else if (menu.hovered != null && menu.hovered != this
					&& !isAncestorOfHovered())
			{
				;
			} else
			{
				g2.fill(roundRect);
				g2.setPaint(strokeC);
				g2.setStroke(stroke);
				g2.draw(roundRect);
			}
		}

		/*
		 * Draw the text, triangle, and shortcut.
		 */
		float curX = x + px;
		MenuUtils.drawLeftText(this, getName(), curX);
		curX += tWidth;
		if (shortcut != null)
		{
			PFont font = menu.style.getFont("font");
			float fs = menu.style.getF("f.fontSize");
			
			float rightX = getX() + getWidth();
			curX = rightX - shortcutWidth;
			// curX += menu.style.padX;
			float shortSize = fs * shortcutTextSize;
			float descent = UIUtils.getTextDescent(menu.buff, font,
					shortSize, true);
			g2.setFont(font.font.deriveFont(shortSize));
			g2.setPaint(strokeC.brighter(100));
			float ht = UIUtils.getTextHeight(menu.canvas.g, font,
					shortSize, shortcut.label, true);
			float yOffset = (height - ht) / 2f + descent;
			yOffset += ht / 2;
			g2.drawString(shortcut.label, curX, y + yOffset);
		}
		curX += shortcutWidth;
		if (drawChildrenTriangle && items.size() > 0)
		{
			if (layoutMode == LAYOUT_BELOW && getState() != MenuItem.UP
					&& !isOpen())
			{
				curX = x + width / 2;
				at.setToIdentity();
				at.translate(curX, y + height + py / 2);
				at.rotate(PApplet.HALF_PI);
				Area a2 = tri.createTransformedArea(at);
				g2.setPaint(strokeC);
				g2.fill(a2);
			} else if (layoutMode != LAYOUT_BELOW)
			{
				curX = x + width - triWidth - px;
				at.setToIdentity();
				at.translate(curX, y + height / 2);
				Area a2 = tri.createTransformedArea(at);
				g2.setPaint(strokeC);
				g2.fill(a2);
			}
		}
	}

	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		buff.setFrame(x, y, width, height);
		Rectangle2D.union(rect, buff, rect);
		super.getRect(rect, buff);
	}

	protected void itemMouseEvent(MouseEvent e, Point pt)
	{
		/*
		 * I'm doing this actionOnMouseDown stuff so that the top-level menus
		 * are activated on a mouse press, to be more toolbar-like (I'm looking
		 * to match Eclipse-like functionality). Basically, I'm overriding the
		 * default values if we're in a top-level menu.
		 */
		super.itemMouseEvent(e, pt);
		// if (parent == menu && mouseInside)
		// {
		// if (e.getID() == MouseEvent.MOUSE_RELEASED)
		// {
		// if (!isOpen())
		// {
		// menuTriggerLogic();
		// } else
		// {
		// close();
		// }
		// }
		// // System.out.println("Hey!");
		// }
	}

	// protected void setState(int state)
	// {
	// super.setState(state);
	// if (this.state == state)
	// return;
	// if (menu == parent && menu instanceof Toolbar)
	// {
	// boolean oldHov = menu.hoverNavigable;
	// menu.hoverNavigable = false;
	// super.setState(state);
	// Toolbar tb = (Toolbar) menu;
	// if (state != MenuItem.UP)
	// {
	// if (tb.isActive())
	// {
	// tb.closeMyChildren();
	// open();
	// }
	// }
	// menu.hoverNavigable = oldHov;
	// } else
	// super.setState(state);
	// }

	public void layout()
	{
		if (menu == null)
			return;
		
		float px = getPadX();
		float py = getPadY();
		
		float curX = 0, curY = 0;
		switch (layoutMode)
		{
			case (LAYOUT_BELOW):
				curX = x - py;
				curY = y + height;
				break;
			case (LAYOUT_RIGHT):
			default:
				curX = x + width;
				curY = y - px;
				break;
		}
		subItemRect.x = curX;
		subItemRect.y = curY;
		curX += px;
		curY += py;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			item.calcPreferredSize();
		}
		float maxWidth = getMaxWidth();
		float maxHeight = getMaxHeight();
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			item.setPosition(curX, curY);
//			if (layoutMode == LAYOUT_BELOW)
//				item.setWidth(maxWidth);
//			else
//				item.setHeight(maxHeight);
			item.setSize(maxWidth, maxHeight);
			curY += item.getHeight();
		}
		curY += py;

		subItemRect.width = maxWidth + px * 2;
		subItemRect.height = curY - subItemRect.y;
		
		/*
		 * Trigger the recursive layout.
		 */
		super.layout();
	}

	protected void setLayoutMode(int layoutMode)
	{
		this.layoutMode = layoutMode;
	}
}
