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
package org.andrewberman.ui.ifaces;

import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import org.andrewberman.ui.Point;

/**
 * The <code>UIObject</code> interface guarantees that an object has the
 * minimum methods necessary to be managed by the <code>EventManager</code>
 * and <code>FocusManager</code> managers. In other words, it can respond to
 * mouse, keyboard, and focus events, and can be drawn to the canvas.
 * 
 * @author Greg
 */
public interface UIObject
{
	public void mouseEvent(MouseEvent e, Point screen, Point model);

	public void keyEvent(KeyEvent e);

	public void focusEvent(FocusEvent e);

	public void draw();
}
