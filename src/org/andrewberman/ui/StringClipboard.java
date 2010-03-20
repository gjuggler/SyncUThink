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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * The <code>StringClipboard</code> class is a simple implementation of a
 * <code>String</code>-backed clipboard object. It attempts to connect itself
 * with the system clipboard, but if it doesn't have the security privileges (if you're
 * running from an unsigned applet, you probably won't have the privileges), then it
 * creates its own internal Clipboard object.
 * <p>
 * TODO: Create a <code>TreeClipboard</code> object, to allow for easy cutting and pasting
 * of trees and subtrees (for PhyloWidget).
 * 
 * @author Greg
 * @see		java.awt.datatransfer.Clipboard
 * @see		java.awt.datatransfer.ClipboardOwner
 * @see		java.lang.SecurityManager
 */
public class StringClipboard implements ClipboardOwner
{
	public static StringClipboard instance;
	private Clipboard clip;
	private DataFlavor flavor = DataFlavor.stringFlavor;

	public static void lazyLoad()
	{
		if (instance == null)
			instance = new StringClipboard();
	}

	private StringClipboard()
	{
		/*
		 * Check security to see if we can latch onto the system clipboard.
		 */
		SecurityManager security = System.getSecurityManager();
		boolean useSystem = false;
		if (security != null)
		{
			try
			{
				security.checkSystemClipboardAccess();
				useSystem = true;
			} catch (SecurityException e)
			{
				// Do nothing.
			}
		}
		if (useSystem)
			clip = Toolkit.getDefaultToolkit().getSystemClipboard();
		else
			clip = new Clipboard("PhyloWidget Clipboard");
	}

	public void lostOwnership(Clipboard clipboard, Transferable contents)
	{
		// Do nothing.
	}

	public void toClipboard(String s)
	{
		StringSelection sel = new StringSelection(s);
		clip.setContents(sel, this);
	}

	public String fromClipboard()
	{
		Transferable clipboardContent = clip.getContents(this);
		if ((clipboardContent != null)
				&& clipboardContent.isDataFlavorSupported(flavor))
		{
			try
			{
				return (String) clipboardContent.getTransferData(flavor);
			} catch (UnsupportedFlavorException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
				return "";
			}
		}
		return new String("");
	}
	
	public Clipboard getClipboard()
	{
		return clip;
	}
}