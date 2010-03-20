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
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;

import org.andrewberman.ui.camera.Camera;
import org.andrewberman.ui.ifaces.UIObject;
import org.andrewberman.ui.tools.ToolManager;

import processing.core.PApplet;

/**
 * The <code>EventManager</code> class, along with <code>FocusManager</code>,
 * is a fundamental part of this package. <code>EventManager's</code> job is
 * to keep track of all the UI objects currently on the screen and dispatch
 * <code>Event</code> objects accordingly.
 * <p>
 * The <code>UIObject</code> interface is used in order to ensure that all
 * objects wishing to register with the <code>EventManager</code> are capable
 * of receiving mouse, keyboard, focus, and draw events.
 * <p>
 * Every UI object wishing to be controlled by the EventManager object should do
 * the following in its constructor:
 * <ul>
 * <li>Call the static method <code>UIUtils.lazyLoad</code>. This causes all
 * "singlet" objects to load up their instances if they haven't done so already.</li>
 * <li>Call <code>EventManager.instance.add(this)</code>. This causes the
 * EventManager to begin managing this UIObject. This consists of: calling the
 * draw() method every frame, and if the FocusManager allows it, calling the
 * mouseEvent, keyEvent, and focusEvent methods accordingly.</li>
 * </ul>
 * 
 * @author Greg
 * @see org.andrewberman.ui.ifaces.UIObject
 * @see org.andrewberman.ui.FocusManager
 * @see org.andrewberman.ui.ShortcutManager
 * @see org.andrewberman.ui.UIUtils
 */
public final class EventManager implements MouseListener, MouseMotionListener,
		MouseWheelListener, KeyListener
{
	private PApplet p;
	private ArrayList delegates = new ArrayList(5);
	private ToolManager toolManager;
	public Camera toolCamera;

	public static EventManager instance;

	Point screen = new Point(0, 0);
	Point model = new Point(0, 0);

	public static void lazyLoad(PApplet p)
	{
		// if (instance == null)
		instance = new EventManager(p);
	}

	public EventManager(PApplet p)
	{
		this.p = p;
		setup();
	}

	public void setup()
	{
		if (p.g.getClass().getName().equals(PApplet.OPENGL))
		{
//			PGraphicsOpenGL gl = (PGraphicsOpenGL) p.g;
//			gl.canvas.addMouseListener(this);
//			gl.canvas.addMouseMotionListener(this);
//			gl.canvas.addKeyListener(this);
//			gl.canvas.addMouseWheelListener(this);
		} else
		{
			p.addMouseListener(this);
			p.addMouseMotionListener(this);
			p.addMouseWheelListener(this);
			p.addKeyListener(this);
		}
		/*
		 * Register ourselves with the PApplet to be drawn every frame.
		 */
		p.registerDraw(this);
	}

	public void add(UIObject o)
	{
		delegates.add(o);
	}

	public void setToolManager(ToolManager t)
	{
		toolManager = t;
	}

	public ToolManager getToolManager()
	{
		return toolManager;
	}
	
	public void setCamera(Camera c)
	{
		toolCamera = c;
	}
	
	public void remove(UIObject o)
	{
		delegates.remove(o);
	}

	public void draw()
	{
		UIUtils.setMatrix(p);

		for (int i = 0; i < delegates.size(); i++)
		{
			((UIObject) delegates.get(i)).draw();
		}

		/*
		 * Do any drawing of the current tool.
		 */
		if (toolManager != null)
			toolManager.draw();
	}

	public void mouseEvent(MouseEvent e)
	{
		screen.setLocation(e.getX(), e.getY());
		model.setLocation(screen.x, screen.y);
		UIUtils.screenToModel(model);

		/*
		 * First, send the event directly to the focused object.
		 */
		if (FocusManager.instance.getFocusedObject() instanceof UIObject)
		{
			// System.out.println(FocusManager.instance.getFocusedObject());
			((UIObject) FocusManager.instance.getFocusedObject()).mouseEvent(e,
					screen, model);
		}
		/*
		 * If the FocusManager is in a modal state, return without further
		 * dispatching.
		 */
		if (FocusManager.instance.isModal())
			return;
		/*
		 * Now, send it to the ToolManager.
		 */
		if (!e.isConsumed())
		{
			if (toolManager != null)
				toolManager.mouseEvent(e, screen, model);
		}
		/*
		 * Then, if the focus isn't modal and the object wasn't consumed,
		 * continue sending the mouse event to the other uiobjects.
		 */
		for (int i = delegates.size() - 1; i >= 0; i--)
		{
			if (e.isConsumed())
				break;
			UIObject ui = (UIObject) delegates.get(i);
			if (ui == FocusManager.instance.getFocusedObject())
				continue;
			ui.mouseEvent(e, screen, model);
		}
	}

	public void keyEvent(KeyEvent e)
	{
		boolean modal = FocusManager.instance.isModal();
		/*
		 * We only send keyboard events to the focused object.
		 */
		if (FocusManager.instance.getFocusedObject() instanceof UIObject)
			((UIObject) FocusManager.instance.getFocusedObject()).keyEvent(e);
		/*
		 * If modal, return early.
		 */
		if (modal)
			return;
		/*
		 * Then, if the focus isn't modal and the object wasn't consumed,
		 * continue sending the mouse event to the other uiobjects.
		 */
		for (int i = delegates.size() - 1; i >= 0; i--)
		{
			if (e.isConsumed())
				break;
			UIObject ui = (UIObject) delegates.get(i);
			if (ui == FocusManager.instance.getFocusedObject())
				continue;
			ui.keyEvent(e);
		}
		/*
		 * Lastly, dispatch to the tool manager if not consumed.
		 */
		if (!e.isConsumed())
		{
			if (toolManager != null)
				toolManager.keyEvent(e);
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseEntered(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseExited(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mousePressed(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseReleased(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseDragged(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseMoved(MouseEvent e)
	{
		mouseEvent(e);
	}

	public void mouseWheelMoved(MouseWheelEvent e)
	{
		mouseEvent(e);
	}

	public void keyPressed(KeyEvent e)
	{
		keyEvent(e);
	}

	public void keyReleased(KeyEvent e)
	{
		keyEvent(e);
	}

	public void keyTyped(KeyEvent e)
	{
		keyEvent(e);
	}

}
