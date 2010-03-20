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

import java.awt.event.KeyEvent;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * The <code>Shortcut</code> class holds all the information necessary to
 * load, represent, and activate a keyboard shortcut.
 * <p>
 * The most important and useful method here is the <code>parseString</code>
 * method, which is a "smarter" version of AWT's built-in
 * <code>KeyStroke.getKeyStroke(String s)</code> method. While they make you
 * conform to their annoying formatting rules, this Shortcut class uses a few
 * simplistic regular expressions to try and parse a string into a keyboard
 * shortcut. Hopefully it works well for you!
 * <p>
 * Similar to the <code>Action</code> class, you probably won't want to use
 * the <code>Shortcut</code> class on your own unless you're doing something
 * advanced. Any <code>Menu</code> object that allows keyboard shortcuts
 * should provide a reasonable <code>add()</code> or <code>create()</code>
 * method.
 * <p>
 * TODO: Add an <code>isEnabled</code> field to the <code>Shortcut</code>
 * class, so that the user can disable and re-enable shortcuts as desired. The
 * <code>performAction</code> method should check this field to see whether it
 * should perform the associated Action.
 * 
 * @author Greg
 * @see org.andrewberman.ui.Action
 * @see org.andrewberman.ui.ShortcutManager
 */
public class Shortcut
{
	public Action action;
	public int keyMask;
	public int keyCode;
	public String label;

	static String control = "(control|ctrl|meta|cmd|command|apple)";
	static String alt = "(alt)";
	static String shift = "(shift|shft)";

	static int shortcutMask = UIUtils.getMetaMask();

	public Shortcut(String s)
	{
//		ShortcutManager.instance.add(this);
		parseString(s);
	}

	public void parseString(String s)
	{
		s = s.toLowerCase();
		StringTokenizer st = new StringTokenizer(s, "+-. ");
		int modifiers = 0;
		int code = 0;
		while (st.hasMoreTokens())
		{
			String token = st.nextToken();
			if (Pattern.matches(alt, token))
			{
				modifiers = modifiers | KeyEvent.ALT_DOWN_MASK;
			} else if (Pattern.matches(control, token))
			{
				modifiers = modifiers | shortcutMask;
			} else if (Pattern.matches(shift, token))
			{
				modifiers = modifiers | KeyEvent.SHIFT_DOWN_MASK;
			} else
			{
				// code = token.charAt(0);
				String keyCodeName = "VK_" + token.toUpperCase();
				try
				{
					code = KeyEvent.class.getField(keyCodeName).getInt(
							KeyEvent.class);
				} catch (Exception e)
				{
					throw new RuntimeException(
							"Error parsing shortcut text. The offending token: "
									+ token);
				}
			}
		}
		keyMask = modifiers;
		keyCode = code;
		label = new String();
		if (keyMask != 0)
		{
			String modS = KeyEvent.getModifiersExText(keyMask);
			if (modS.equalsIgnoreCase("command"))
			{
				modS = "Cmd";
			}
			label +=  modS + "+";
		}
		if (keyCode != 0)
		{
			label += KeyEvent.getKeyText(keyCode);
		}
//		System.out.println(label);
//		 System.out.println(KeyEvent.getModifiersExText(keyMask));
		// System.out.println(KeyEvent.getKeyText(keyCode));
	}

	public boolean matchesKeyEvent(KeyEvent e)
	{
		boolean modMatch = (e.getModifiersEx() == keyMask);
		if (modMatch &&
				e.getKeyCode() == keyCode)
			return true;
		else
			return false;
	}
	
	public void performAction()
	{
		if (action != null)
		{
			action.performAction();
		}
	}

}
