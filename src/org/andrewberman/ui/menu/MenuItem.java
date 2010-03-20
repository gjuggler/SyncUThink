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

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.andrewberman.ui.Action;
import org.andrewberman.ui.Color;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.Shortcut;
import org.andrewberman.ui.ShortcutManager;
import org.andrewberman.ui.UIEvent;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.Malleable;
import org.andrewberman.ui.ifaces.Positionable;
import org.andrewberman.ui.ifaces.Sizable;
import org.andrewberman.ui.ifaces.UIObject;

import processing.core.PFont;

/**
 * The <code>MenuItem</code> class is the base class for ALL objects in the
 * <code>Menu</code> package. Its main purpose is to provide the recursive
 * functions necessary for managing a tree-like menu structure.
 * <p>
 * If you are interested in designing a new type of menu based on this
 * structure, then you need to (a) create a <code>Menu</code> subclass and (b)
 * create a <code>MenuItem</code> subclass. Your new <code>Menu</code>
 * subclass will handle the root-level layout and logic handling, while your
 * <code>MenuItem</code> subclass should draw itself and layout any sub-items
 * it may have. See the examples within this package, all of which were designed
 * in this way.
 * <p>
 * 
 * @author Greg
 */
public abstract class MenuItem implements Positionable, Sizable, Malleable
{
	/**
	 * Constants that define the values for the "state" field.
	 */
	public static final int UP = 0;
	public static final int OVER = 1;
	public static final int DOWN = 2;
	public static final int DISABLED = 3;

	protected static ZDepthComparator zComp;
	protected static MenuTimer timer = MenuTimer.instance();

	protected Menu menu;
	protected Menu nearestMenu;
	protected MenuItem parent;

	protected Action action;
	protected Shortcut shortcut;
	protected String name;
	protected ArrayList<MenuItem> items;
	/**
	 * The same items as above, but z-sorted for hit detection and drawing
	 * purposes.
	 */
	protected ArrayList<MenuItem> zSortedItems;
	protected boolean needsZSort;

	protected float x, y;
	protected float width, height;

	protected int z;

	protected int state;
	protected boolean isOpen;
	protected boolean mouseInside;
	private boolean enabled = true;

	public MenuItem()
	{
		this.name = new String();
		items = new ArrayList<MenuItem>(1);
		zSortedItems = new ArrayList<MenuItem>(1);
	}

	public MenuItem setAction(Object object, String method)
	{
//		System.out.println(this.name);
		action = new Action(object, method);
		if (shortcut != null)
		{
			shortcut.action = action;
		}
		menu.layout();
		return this;
	}

	public Action getAction()
	{
		return action;
	}

	/**
	 * If true, this menu item will hide itself when its action is performed. If
	 * false, it will remain open.
	 */
	public boolean getCloseOnAction()
	{
		return true;
	}

	public MenuItem setShortcut(String s)
	{
		shortcut = ShortcutManager.instance.createShortcut(s);
		if (action != null)
		{
			shortcut.action = action;
		} else
		{
			shortcut.action = new Action(this,"performAction");
		}
		menu.layout();
		return this;
	}

	public Shortcut getShortcut()
	{
		return shortcut;
	}

	public MenuItem add(MenuItem item)
	{
		items.add(item);
		/*
		 * Add this item to the zSortedList and re-sort.
		 */
		zSortedItems.add(item);
		zSort();
		/*
		 * Set the sub-item's parent to this item, and its menu to our menu.
		 */
		item.setParent(this);
		//		item.setMenu(menu);
		/*
		 * Layout the entire menu so things look nice.
		 */
		if (menu != null)
			menu.layout();
		return item;
	}

	public void remove(MenuItem item)
	{
		items.remove(item);
		zSortedItems.remove(item);
		zSort();
		if (menu != null)
			menu.layout();
	}

	protected void zSort()
	{
		if (zComp == null)
			zComp = new ZDepthComparator();
		Collections.sort(zSortedItems, zComp);
	}

	public MenuItem add(String newLabel)
	{
		return add(create(newLabel));
	}

	/**
	 * Creates a MenuItem that this Menu can have added to it. Subclassers
	 * should implement this method to create a new top-level Menuitem, i.e.
	 * your DinnerMenu object should create and return a DinnerMenuItem that
	 * could then be inserted into your DinnerMenu using add(MenuItem).
	 * 
	 * @param label
	 *            the label of the MenuItem to be created
	 * @return a MenuItem that is compatible with the current Menu instance.
	 */
	public MenuItem create(String label)
	{
		if (nearestMenu != null)
			return nearestMenu.create(label);
		else if (menu != null)
			return menu.create(label);
		else
			throw new RuntimeException("Error in MenuItem.create(String label)");
	}

	/**
	 * Searches through the sub-item tree for the MenuItem with a given name.
	 * 
	 * @param search
	 *            the name to search with.
	 * @return
	 */
	public MenuItem get(String search)
	{
		if (getName().equals(search))
			return this;
		else
		{
			for (int i = 0; i < items.size(); i++)
			{
				MenuItem mightBeNull = ((MenuItem) items.get(i)).get(search);
				if (mightBeNull != null)
					return mightBeNull;
			}
		}
		return null;
	}

	/**
	 * Returns whether this MenuItem is enabled or not. Could be used by
	 * subclasses to sometimes *not* be enabled.
	 * 
	 * If isEnabled() returns false, the MenuItem will (a) return DISABLED in
	 * its getState() method, and (b) will not perform its action when pressed
	 * or otherwise activated.
	 * 
	 * @return
	 */
	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enabled)
	{
		this.enabled = enabled;
	}

	public boolean isOpen()
	{
		if (items.size() == 0)
			return false;
		return isOpen;
	}

	public boolean hasChildren()
	{
		return items.size() > 0;
	}

	public boolean hasOpenChildren()
	{
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			if (item.isOpen())
				return true;
		}
		return false;
	}

	/**
	 * Draws this MenuItem to the current root menu's PGraphics object.
	 */
	public void draw()
	{
		drawMyself();
		if (!isOpen())
			return;
		if (needsZSort)
		{
			zSort();
			needsZSort = false;
		}
		/*
		 * Here's where the zSorted items come in handy. Draw items in order of
		 * the zSortedItems list, so that items on "top" (i.e. with the lowest z
		 * value) draw last.
		 */
		drawBefore();
		for (int i = 0; i < zSortedItems.size(); i++)
		{
			MenuItem seg = (MenuItem) zSortedItems.get(i);
			seg.draw();
		}
		drawAfter();
	}

	protected void drawMyself()
	{

	}

	protected void drawBefore()
	{
		// Do nothing.
	}

	protected void drawAfter()
	{
		// Do nothing.
	}

	/**
	 * Lays out this MenuItem and all of its sub-items.
	 */
	public void layout()
	{
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem seg = (MenuItem) items.get(i);
			seg.layout();
		}
	}

	protected boolean isAncestorOfHovered()
	{
		if (menu == null)
			return false;
		if (this == menu.hovered)
			return true;
		else if (isAncestorOf(menu.hovered))
			return true;
		return false;
	}

	protected boolean isAncestorOf(MenuItem child)
	{
		if (child == null)
			return false;
		else if (child.parent == this)
			return true;
		else
		{
			boolean found = false;
			for (int i = 0; i < items.size(); i++)
			{
				MenuItem item = (MenuItem) items.get(i);
				if (item.isAncestorOf(child))
					found = true;
			}
			return found;
		}
	}

	/**
	 * Shows this MenuItem and its direct sub-MenuItems.
	 */
	//	private void open()
	//	{
	////		if (isOpen)
	////			close();
	////		isOpen = true;
	//		menu.open(this);
	//	}
	//	private void close()
	//	{
	////		for (int i = 0; i < items.size(); i++)
	////		{
	////			MenuItem item = (MenuItem) items.get(i);
	////			item.close();
	////		}
	////		isOpen = false;
	//		menu.close(this);
	//	}
	public void closeMyChildren()
	{
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			menu.close(item);
		}
	}

	protected void toggleChildren()
	{
		if (isOpen)
			menu.close(this);
		else
			menu.open(this);
	}

	protected void setMenu(Menu menu)
	{
		this.menu = menu;
		getNearestMenu();
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			item.setMenu(menu);
		}
	}

	protected Menu getNearestMenu()
	{
		// if (nearestMenu != null) return nearestMenu;
		MenuItem item = this;
		while (item != null)
		{
			if (item instanceof Menu)
			{
				nearestMenu = (Menu) item;
				return nearestMenu;
			} else
				item = item.parent;
		}
		return null;
	}

	protected void setParent(MenuItem item)
	{
		parent = item;
		setMenu(item.menu);
	}

	public void performAction()
	{
		if (!isEnabled())
			return; // Do nothing if disabled.
		if (items.size() > 0)
		{
			/*
			 * If we have sub-items, trigger an open or close event.
			 */
			menuTriggerLogic();
		} else
		{
			menu.fireEvent(UIEvent.MENU_ACTIONPERFORMED);
			if (getCloseOnAction())
				menu.clickaway();
			if (action != null)
				action.performAction();
		}
	}

	protected void menuTriggerLogic()
	{
		if (timer.item == this || !nearestMenu.clickToggles)
		{
			if (nearestMenu.singletNavigation && parent != null)
			{
				parent.closeMyChildren();
				menu.open(this);
			} else
				menu.open(this);
		} else if (nearestMenu.clickToggles)
		{
			if (nearestMenu.singletNavigation)
			{
				if (parent != null)
				{
					if (isOpen())
						parent.closeMyChildren();
					else
					{
						parent.closeMyChildren();
						menu.open(this);
					}
				}
			} else
				toggleChildren();
		}
	}

	// protected void setOpenItem(MenuItem item)
	// {
	// for (int i = 0; i < items.size(); i++)
	// {
	// MenuItem cur = (MenuItem) items.get(i);
	// if (cur == item)
	// cur.open();
	// else
	// cur.close();
	// }
	// }

	/**
	 * Subclasses should return true if the point is contained within their
	 * mouse-responsive area.
	 * 
	 * @param p
	 *            a Point (in model coordinates) representing the mouse.
	 * @return true if this MenuItem contains the point, false if not.
	 */
	abstract protected boolean containsPoint(Point p);

	/**
	 * Subclasses should union their bounding rectangle with the Rectangle
	 * passed in as the rect parameter.
	 * 
	 * @param rect
	 *            The rectangle with which to union this MenuItem's rectangle.
	 * @param buff
	 *            A buffer Rectangle2D object, to be used for anything.
	 */
	protected void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		if (!isOpen())
			return;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			item.getRect(rect, buff);
		}
	}

	/**
	 * Calculates the maximum width among this MenuItem's sub-items.
	 * 
	 * @return the maximum width of the MenuItems in the "items" arraylist.
	 */
	protected float getMaxWidth()
	{
		float max = 0;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			float curWidth = item.width;
			if (curWidth > max)
				max = curWidth;
		}
		return max;
	}

	protected float getMaxHeight()
	{
		float max = 0;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			float curHeight = item.getHeight();
			if (curHeight > max)
				max = curHeight;
		}
		return max;
	}

	protected void calcPreferredSize()
	{
	}

	protected float getTextHeight()
	{
		int padY = menu.style.getI("f.padY");
		PFont font = (PFont) menu.style.getO("font");
		float fontSize = menu.style.getF("f.fontSize");
		return UIUtils.getTextHeight(menu.buff, font, fontSize, name, true)
				+ padY * 2;
	}

	protected void itemMouseEvent(MouseEvent e, Point tempPt)
	{
		mouseInside = false;
		visibleMouseEvent(e, tempPt);
		if (isOpen())
		{
			for (int i = zSortedItems.size() - 1; i >= 0; i--)
			{
				MenuItem item = (MenuItem) zSortedItems.get(i);
				if (e.isConsumed())
					continue;
				item.itemMouseEvent(e, tempPt);
				if (item.mouseInside)
					mouseInside = true;
			}
		}
		if (mouseInside && getZ() == 0)
		{
			setZ(1);
			if (parent != null)
				parent.needsZSort = true;
		} else if (!mouseInside && getZ() == 1)
		{
			setZ(0);
			if (parent != null)
				parent.needsZSort = true;
		}
	}

	//	private void setState(int state)
	//	{
	//		menu.setState(this, state);

	//		if (this.state == state)
	//			return;
	//		this.state = state;
	//		if (nearestMenu.hoverNavigable)
	//		{
	//			if (state == MenuItem.OVER || state == MenuItem.DOWN)
	//				timer.setMenuItem(this);
	//			else if (state == MenuItem.UP)
	//				timer.unsetMenuItem(this);
	//		}
	//		if (state == MenuItem.DOWN)
	//		{
	//			menu.lastPressed = this;
	//			menu.hovered = this;
	////			menu.currentlyFocused = this;
	////			menu.lastHovered = this;
	//		} else if (state == MenuItem.OVER)
	//		{
	////			menu.lastHovered = this;
	//			menu.hovered = this;
	//		} else if (state == MenuItem.UP && menu.hovered == this)
	//		{
	////			menu.hovered = null;
	//		}
	//	}

	protected int getState()
	{
		if (!isEnabled())
			return DISABLED;
		else
			return state;
	}

	protected void setState(int s)
	{
		state = s;
	}

	protected void visibleMouseEvent(MouseEvent e, Point tempPt)
	{
		if (!isEnabled())
			return;
		boolean containsPoint = containsPoint(tempPt);
		if (containsPoint)
			mouseInside = true;
		switch (e.getID())
		{
			case MouseEvent.MOUSE_MOVED:
				if (containsPoint)
				{
					menu.setState(this, MenuItem.OVER);
				} else
				{
					menu.setState(this, MenuItem.UP);
				}
				break;
			case MouseEvent.MOUSE_PRESSED:
				if (containsPoint)
				{
					if (nearestMenu.actionOnMouseDown)
						performAction();
				}
				// The switch statement continues on through the next case...
			case MouseEvent.MOUSE_DRAGGED:
				if (containsPoint)
				{
					menu.setState(this, MenuItem.DOWN);
				} else
					menu.setState(this, MenuItem.UP);
				break;
			case MouseEvent.MOUSE_RELEASED:
				if (containsPoint)
				{
					if (!nearestMenu.actionOnMouseDown)
						performAction();
					// setState(MenuItem.OVER);
					if (getState() == MenuItem.DOWN)
						menu.setState(this, MenuItem.OVER);
				} else
					menu.setState(this, MenuItem.UP);
			default:
				break;
		}
	}

	public void keyEvent(KeyEvent e)
	{
		if (!isOpen())
			return;
		//		for (int i = 0; i < items.size(); i++)
		//		{
		//			MenuItem seg = (MenuItem) items.get(i);
		//			seg.keyEvent(e);
		//		}
	}

	public int getZ()
	{
		return z;
	}

	public void setZ(int z)
	{
		this.z = z;
	}

	public String toString()
	{
		return name;
	}

	class ZDepthComparator implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			MenuItem i1 = (MenuItem) o1;
			MenuItem i2 = (MenuItem) o2;

			int z1 = i1.getZ();
			int z2 = i2.getZ();

			if (z1 > z2)
				return 1;
			if (z1 < z2)
				return -1;
			return 0;
		}
	}

	static class VisibleDepthComparator implements Comparator
	{

		public int compare(Object o1, Object o2)
		{
			MenuItem i1 = (MenuItem) o1;
			MenuItem i2 = (MenuItem) o2;

			int d1 = maxDepth(i1);
			int d2 = maxDepth(i2);

			if (d1 > d2)
				return -1;
			if (d1 < d2)
				return 1;
			return 0;
		}

		int maxDepth(MenuItem item)
		{
			int max = 0;
			if (!item.isOpen())
			{
				max = 0;
			} else
			{
				for (int i = 0; i < item.items.size(); i++)
				{
					MenuItem child = (MenuItem) item.items.get(i);
					int childDepth = maxDepth(child);
					if (childDepth + 1 > max)
						max = childDepth + 1;
				}
			}
			return max;
		}

	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Menu getMenu()
	{
		return menu;
	}

	public String getName()
	{
		return name;
	}

	public void setPosition(float x, float y)
	{
		setX(x);
		setY(y);
		layout();
	}

	public void setX(float x)
	{
		this.x = x;
	}

	public void setY(float y)
	{
		this.y = y;
	}

	public float getX()
	{
		return x;
	}

	public float getY()
	{
		return y;
	}

	public void setSize(float w, float h)
	{
		setWidth(w);
		setHeight(h);
	}

	public void setWidth(float width)
	{
		this.width = width;
	}

	public void setHeight(float height)
	{
		this.height = height;
	}

	public float getWidth()
	{
		return width;
	}

	public float getHeight()
	{
		return height;
	}

	public PFont getFont()
	{
		return getStyle().getFont("font");
	}

	public float getPadX()
	{
		return getStyle().getF("f.padX");
	}

	public float getPadY()
	{
		return getStyle().getF("f.padY");
	}

	public float getFontSize()
	{
		return getStyle().getF("f.fontSize");
	}

	public Color getStrokeColor()
	{
		return getStyle().getC("c.foreground");
	}

	public MenuStyle getStyle()
	{
		return menu.style;
	}

	public MenuStyle getRootStyle()
	{
		return menu.style;
	}
	
	public Stroke getStroke()
	{
		return new BasicStroke(getStyle().getF("f.strokeWeight"));
	}

}