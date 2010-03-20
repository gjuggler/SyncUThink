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

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import org.andrewberman.ui.ifaces.UIObject;

import processing.core.PApplet;

/**
 * The <code>FocusManager</code> class is a simple "singlet" class for managing
 * the focus of user interface objects.
 * <p>
 * <code>FocusManager</code> takes a fairly <em>laissez-faire</em> approach to focus managing,
 * allowing <em>any object</em> to grab focus at any point in time. Most importantly, however,
 * it allows for two levels of focus: standard focus, and modal focus. If an object has grabbed modal focus,
 * then no other objects are allowed to grab focus until the modal focus is released.
 * <p>
 * It is left up to the UI objects to respond accordingly to the FocusManager's state. The most useful
 * method call is usually <code>FocusManager.instance.isFocused(this)</code>, to test if the current object
 * has focus. For examples of how to successfully use the FocusManager, search through the <code>Menu</code> and 
 * <code>MenuItem</code> sources for "FocusManager."
 * 
 * @author Greg
 * @see		org.andrewberman.ui.ifaces.UIObject
 * @see		org.andrewberman.ui.EventManager
 * @see		org.andrewberman.ui.menu.MenuItem
 * @see		org.andrewberman.ui.menu.Menu
 */
public class FocusManager implements FocusListener
{	
	private PApplet p;
	private Object focusedObject = null;
	private Object lostFocusHolder = null;
	private boolean isModal = false;
	
	public static FocusManager instance;
	
	private FocusManager(PApplet app)
	{
		p = app;
		p.addFocusListener(this);
	}
	
	public static void lazyLoad(PApplet p)
	{
//		if (instance == null)
//		{
			instance = new FocusManager(p);
//		}
	}
	
	public boolean setFocus(Object o)
	{
		if (isModal && !isFocused(o))
		{
			return false;
		} else
		{
			focusedObject = o;
//			isModal = false;
			return true;
		}
		
	}
	
	public boolean setModalFocus(Object o)
	{
		focusedObject = o;
		isModal = true;
		return true;
	}
	
	/*
	 * Removes the object from focus. Returns true if the object WAS in focus and was removed,
	 * false if the object WAS NOT in focus to begin with.
	 */
	public boolean removeFromFocus(Object o)
	{
		if (focusedObject == o)
		{
			focusedObject = null;
			isModal = false;
			return true;
		} else
		{
			return false;
		}
	}
	
	public boolean isFocused(Object o)
	{
		if (o == null) return false;
		return (o == focusedObject);
	}
	
	public Object getFocusedObject()
	{
		return focusedObject;
	}
	
	public boolean isModal()
	{
		return isModal;
	}

	public void focusEvent(FocusEvent e)
	{
		if (focusedObject instanceof UIObject)
			((UIObject)focusedObject).focusEvent(e);
		switch(e.getID())
		{
			case (FocusEvent.FOCUS_LOST):
				lostFocusHolder = focusedObject;
				focusedObject = null;
				break;
			case (FocusEvent.FOCUS_GAINED):
				if (lostFocusHolder != null && focusedObject == null)
					focusedObject = lostFocusHolder;
				break;
		}
	}

	public void focusGained(FocusEvent e)
	{
		focusEvent(e);
	}

	public void focusLost(FocusEvent e)
	{
		focusEvent(e);
	}
	
}
