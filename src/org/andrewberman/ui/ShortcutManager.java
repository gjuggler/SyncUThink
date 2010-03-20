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

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import processing.core.PApplet;

/**
 * The <code>ShortcutManager</code> class is used to detect keyboard shortcut
 * key events. Although its functionality is similar to the
 * <code>EventManager</code> class, in order to allow keyboard shortcuts to be
 * activated on a global level, we need to "shortcut" the
 * EventManager/FocusManager system... get it?
 * <p>
 * 
 * @author Greg
 * @see org.andrewberman.ui.Shortcut
 * @see org.andrewberman.ui.EventManager
 */
public class ShortcutManager implements KeyListener
{
	PApplet p;
	public static ShortcutManager instance;

	public ArrayList keys;

	int meta = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private ShortcutManager(PApplet app)
	{
		p = app;
		keys = new ArrayList();
		setup();
	}

	public static void lazyLoad(PApplet app)
	{
		// if (instance == null)
		instance = new ShortcutManager(app);
	}

	public void setup()
	{
		if (p.g.getClass().getName().equals(PApplet.OPENGL))
		{
//			PGraphicsOpenGL gl = (PGraphicsOpenGL) p.g;
//			gl.canvas.addKeyListener(this);
		} else
		{
			p.addKeyListener(this);
		}
	}

	public Shortcut createShortcut(String s)
	{
		Shortcut sh = new Shortcut(s);
		add(sh);
		return sh;
	}

	public void add(Shortcut key)
	{
		keys.add(key);
	}

	public void remove(Shortcut key)
	{
		keys.remove(key);
	}

	public void keyEvent(KeyEvent e)
	{
		if (e.getID() != KeyEvent.KEY_PRESSED)
			return;
		for (int i = 0; i < keys.size(); i++)
		{
			Shortcut key = (Shortcut) keys.get(i);
			if (key.matchesKeyEvent(e))
				key.performAction();
		}
	}

	public void keyTyped(KeyEvent e)
	{
		keyEvent(e);
	}

	public void keyPressed(KeyEvent e)
	{
		keyEvent(e);
	}

	public void keyReleased(KeyEvent e)
	{
		keyEvent(e);
	}
}
