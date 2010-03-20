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

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

import org.andrewberman.ui.EventManager;
import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.Style;
import org.andrewberman.ui.UIEvent;
import org.andrewberman.ui.UIUtils;
import org.andrewberman.ui.ifaces.Positionable;
import org.andrewberman.ui.ifaces.UIListener;
import org.andrewberman.ui.ifaces.UIObject;
import org.andrewberman.ui.tween.PropertyTween;
import org.andrewberman.ui.tween.Tween;
import org.andrewberman.ui.tween.TweenFriction;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;
import processing.core.PGraphicsJava2D;

/**
 * The <code>Menu</code> class represents a displayable, interactive menu. It
 * is abstract, so it can never be instantiated on its own. Instead, users
 * should call the constructor of one of its subclasses, such as
 * <code>Toolbar</code> or <code>Dock</code>.
 * <p>
 * The main purpose for the <code>Menu</code> class is to hold a large amount
 * of generic structure and logic for managing and displaying a menu-type object
 * within Processing. This includes: dealing with mouse events, keeping track of
 * which menu item is currently hovered, selected, and open, and creating and
 * drawing to an off-screen buffer when necessary (to allow us to use Java2D
 * functions within a P3D or OpenGL PApplet).
 * <p>
 * <p>
 * The <code>Menu</code> class is the base class for all other menu objects
 * within this package. It inherits from the <code>MenuItem</code> class,
 * because a Menu should act like a well-behaved MenuItem as well (this way, we
 * can cascade menus within other menus). It also helped keep down the amount of
 * code repetition.
 * <p>
 * The side effect of this completely inherited organization is, of course, some
 * complexity in thinking about the recursive method calls. You need to keep in
 * mind that when the <code>Menu</code> class calls
 * <code>super.someMethod()</code>, it's calling the method defined in the
 * <code>MenuItem</code> class.
 * <p>
 * There is also a slight issue with the fact that a <code>Menu</code> should
 * not be strictly considered a <code>MenuItem</code>, because more often
 * than not, the <code>Menu</code> object itself does not "act" like a menu.
 * Rather, its sub-items are what is shown to the screen and what interacts with
 * the user. See the classes that extend the <code>Menu</code> class for
 * examples of how to deal with this code organization.
 * <p>
 * <b>Important information for developers:</b> if you plan to create a new
 * type of menu using the <code>Menu</code> class as a base class, you should
 * keep in mind the following:
 * <ul>
 * <li>Check out the built-in Menu subclasses that already exist:
 * <code>Dock</code>, <code>Toolbar</code>, and <code>VerticalMenu</code>.
 * Examining how these classes interact with the <code>Menu</code> structure
 * will be very helpful when designing a novel Menu type.</li>
 * <li>Please, make use of the boolean "options" provided, and override the
 * <code>setOptions()</code> within your new class as a place to define your
 * new menu's behavioral and display options. Particularly important are
 * <code>useCameraCoordinates</code> and <code>usesJava2D</code>. See each
 * option's javadoc for more information.
 * 
 * @author Greg
 * @see org.andrewberman.ui.menu.MenuItem
 * @see org.andrewberman.ui.menu.Dock
 * @see org.andrewberman.ui.menu.Toolbar
 */
public abstract class Menu extends MenuItem implements UIObject
{
	public static final int START_SIZE = 50;

	private int offsetX = 0;
	private int offsetY = 0;

	public MenuStyle style;

	
	/**
	 * Our UIListeners.
	 */
	protected ArrayList listeners = new ArrayList(1);
	/**
	 * The current "canvas" PApplet object.
	 */
	protected PApplet canvas;
	/**
	 * The graphics2D object to which we may be drawing.
	 */
	protected PGraphicsJava2D buff;
	/**
	 * A RenderingHints object, used during the <code>draw()</code> cycle to
	 * store and reload the rendering hints on the graphics2D object being used.
	 */
	RenderingHints origRH;
	/**
	 * A Composite object, used to store and reload the Graphics2D's original
	 * composite state during the draw cycle.
	 */
	Composite origComp;
	/**
	 * Two Rectangle objects which are passed to the sub-items during the
	 * getRect() phase of the draw cycle.
	 */
	Rectangle2D.Float rect = new Rectangle2D.Float(0, 0, 0, 0);
	Rectangle2D.Float buffRect = new Rectangle2D.Float(0, 0, 0, 0);
	/**
	 * One Point object which will be passed to the sub-items during the
	 * mouseEvent() cycle.
	 */
	Point mousePt = new Point(0, 0);
	/**
	 * A Tween for the alpha value. Only created if <code>autoDim</code> is
	 * set to true.
	 */
	protected PropertyTween aTween;
	/**
	 * References to a few relevant MenuItems.
	 */
	protected MenuItem hovered, lastPressed, kbFocus;
	/**
	 * The current alpha value for this menu.
	 */
	public float alpha = 1.0f;

	/*
	 * =============================== GENERAL OPTIONS FOR SUBCLASSES
	 */
	/**
	 * A very important option. If set to FALSE, then this <code>Menu</code>
	 * instance will draw itself to SCREEN coordinates. If set to TRUE, however,
	 * then this <code>Menu</code> will draw itself to the MODEL coordinates.
	 * In general, this is best left to FALSE for UI objects, as they are
	 * usually drawn relative to the screen, despite what the "model" camera may
	 * be doing.
	 * <p>
	 * This option is made <code>public</code>, as opposed to most of the
	 * other <code>protected</code> options, because it is easily forseeable
	 * that the user might want to change the screen-vs-camera behavior of a
	 * menu without going through the effort of making a new subclass and
	 * overriding the <code>setOptions</code> method.
	 */
	public boolean useCameraCoordinates;
	/**
	 * This parameter signals that the sub-classing menu is going to draw itself
	 * using Java2D. As such, if the base canvas isn't Java2D, then we will draw
	 * to the off-screen buffer and then blend the image back onto the canvas.
	 * If this value is set to FALSE, then the <code>Menu</code> will always
	 * draw directly to the on-screen PGraphics canvas. Schweet!
	 */
	protected boolean usesJava2D;
	/**
	 * If true, a mouse hover will expand a menu item. If false, a menu item
	 * requires a click to expand.
	 */
	protected boolean hoverNavigable;
	/**
	 * If true, a click will hide a submenu as well as show it. This option
	 * works best when set to the OPPOSITE of the above hoverNavigable option.
	 */
	protected boolean clickToggles;
	/**
	 * If true, only one of this MenuItem's submenus will be allowed to be shown
	 * at once. Generally best left set to true.
	 */
	protected boolean singletNavigation;
	/**
	 * If true, this menu's items will act like a "menu" and open up on the
	 * mouse down event, as opposed to the more "button"-like behavior of
	 * opening on the mouse up event.
	 */
	protected boolean actionOnMouseDown;
	/**
	 * Defines the behavior of a click that occurs outside the bounds of the
	 * menu and all of its sub-menus).
	 */
	protected int clickAwayBehavior;
	public static final int CLICKAWAY_HIDES = 0;
	public static final int CLICKAWAY_COLLAPSES = 1;
	public static final int CLICKAWAY_IGNORED = 2;
	/**
	 * If true, this Menu will change to the hand cursor when one of its
	 * constituent sub-MenuItems is selected. I am the walrus.
	 */
	protected boolean useHandCursor;

	// protected boolean hideOnAction = true;
	/**
	 * If true, this menu will dim to loAlpha if the mouse is not inside the
	 * menu or any of its sub-items.
	 */
	protected boolean autoDim;
	/**
	 * The "dim" alpha value to drop to. Only effective when
	 * <code>autodim</code> is true.
	 */
	public float dimAlpha = .3f;
	/**
	 * The full alpha value to jump to when the mouse is over this menu. Only
	 * effective when <code>autoDim</code> is true.
	 */
	public float fullAlpha = 1f;
	/**
	 * If set to true, then this menu will grab focus when the show() function
	 * is called. It will then also release focus when the hide() function is
	 * called.
	 */
	public boolean focusOnShow;
	/**
	 * If set to true, then this menu grabs modal focus when opened.
	 */
	public boolean modalFocus;
	public boolean consumeEvents;

	public Menu(PApplet app)
	{
		super();
		UIUtils.loadUISinglets(app);
		EventManager.instance.add(this); // Add ourselves to EventManager.
		canvas = app;
		setMenu(this);
		style = new MenuStyle();
		/*
		 * Give our subclasses a chance to set their options before we start
		 * initing stuff.
		 */
		setOptions();
		init();
	}

	protected void init()
	{
		if (UIUtils.isJava2D(canvas))
			buff = (PGraphicsJava2D) canvas.g;
		else if (usesJava2D)
			createBuffer(START_SIZE, START_SIZE);
		if (autoDim)
			aTween = new PropertyTween(this, "alpha",
					TweenFriction.tween(.25f), Tween.OUT, fullAlpha, fullAlpha,
					15);
	}

	@Override
	protected void setParent(MenuItem item)
	{
		super.setParent(item);
		
	}
	
	public void setOptions()
	{
		useCameraCoordinates = true;
		usesJava2D = true;
		hoverNavigable = false;
		clickToggles = false;
		singletNavigation = true;
		actionOnMouseDown = false;
		clickAwayBehavior = CLICKAWAY_HIDES;
		useHandCursor = true;
		autoDim = false;
		focusOnShow = false;
		modalFocus = false;
		consumeEvents = true;

		// Subclassers should put changes in the boolean options here.
	}

	public void setFontSize(float newSize)
	{
		style.set("f.fontSize", newSize);
//		PFont curFont = (PFont) style.get("font");
//		curFont.font = curFont.font.deriveFont(newSize);
		layout();
	}

	protected void createBuffer(int w, int h)
	{
		buff = (PGraphicsJava2D) canvas.createGraphics(w, h, PApplet.JAVA2D);
	}

	public float getX()
	{
		return x;
	}

	public float getY()
	{
		return y;
	}

	public abstract MenuItem create(String label);

	public void open()
	{
		open(this);
		if (modalFocus)
			FocusManager.instance.setModalFocus(this);
		else if (focusOnShow)
			FocusManager.instance.setFocus(this);
		fireEvent(UIEvent.MENU_OPENED);
	}

	public void close()
	{
		close(this);
		/*
		 * Cause the cursor to be reverted back to normal in the case that this
		 * Menu had changed it to a hand icon or something else.
		 */
		UIUtils.releaseCursor(this, canvas);
		/*
		 * Cause this menu to release its focus, if it had grabbed it.
		 */
		if (modalFocus)
			FocusManager.instance.removeFromFocus(this);
		else if (focusOnShow)
			FocusManager.instance.removeFromFocus(this);
		/*
		 * Finally, fire the MENU_HIDDEN event to our listeners.
		 */
		fireEvent(UIEvent.MENU_CLOSED);
	}

	public void open(MenuItem i)
	{
		if (i.isOpen())
			close();
		i.isOpen = true;
	}

	public void close(MenuItem item)
	{
		ArrayList items = item.items;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem child = (MenuItem) items.get(i);
			close(child);
			child.setState(MenuItem.UP);
		}
		item.isOpen = false;
	}

	public boolean isRootMenu()
	{
		return (menu == this);
	}

	public void setMenu(Menu m)
	{
		super.setMenu(m);
		if (!isRootMenu())
		{
			/*
			 * If we're no longer the root menu, then remove ourselves from the
			 * EventManager's control.
			 */
			EventManager.instance.remove(this);
		}
	}

	public void layout()
	{
		hint();
		super.layout();
		unhint();
	}

	public void draw()
	{
		// System.out.println(hovered);
		if (!isRootMenu())
		{
			super.draw();
			return;
		}
		if (autoDim)
			aTween.update();
		if (UIUtils.isJava2D(canvas))
		{
			/*
			 * If this is a root menu and our canvas PGraphics is a
			 * JavaGraphics2D instance, then we can draw with Java2D directly to
			 * the canvas.
			 */
			canvas.pushMatrix();
			resetMatrix(canvas.g);
			// canvas.translate(x, y);
			hint();
			super.draw();
			unhint();
			canvas.popMatrix();
		} else if (!usesJava2D)
		{
			/*
			 * If this menu has indicated that it won't draw Java2D unless it
			 * checks itself for a J2D canvas, then we can also draw directly to
			 * the canvas.
			 */
			canvas.pushMatrix();
			resetMatrix(canvas.g);
			// canvas.translate(x, y);
			super.draw();
			canvas.popMatrix();
		} else
		{
			/*
			 * If our root canvas is either OpenGL or P3D, we need to draw to
			 * the offscreen Java2D buffer and then blit it onto the canvas
			 * PGraphics.
			 */
			resizeBuffer();
			hint();
			buff.beginDraw();
			buff.background(255, 0);
			buff.translate(-x, -y);
			buff.translate(-offsetX, -offsetY);
			// buff.translate(offsetX, offsetX);
			super.draw(); // Draws all of the sub segments.
			buff.modified = true;
			buff.endDraw();
			drawToCanvas();
			unhint();
		}
	}

	protected void hint()
	{
		origRH = buff.g2.getRenderingHints();
		origComp = buff.g2.getComposite();
		buff.g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
				RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		buff.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		buff.g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
		// buff.g2.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
		// buff.g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
		// RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		// buff.g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
		// RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		buff.g2.setComposite(AlphaComposite.getInstance(
				AlphaComposite.SRC_OVER, alpha));
	}

	protected void unhint()
	{
		buff.g2.setRenderingHints(origRH);
		buff.g2.setComposite(origComp);
	}

	protected void resetMatrix(PGraphics graphics)
	{
		if (useCameraCoordinates)
			return;
		UIUtils.resetMatrix(graphics);
	}

	protected void drawToCanvas()
	{
		int w = (int) (rect.width + PAD * 2);
		int h = (int) (rect.height + PAD * 2);
		canvas.pushMatrix();
		resetMatrix(canvas.g);
		canvas.image(buff, x + offsetX, y + offsetY, w, h, 0, 0, w, h);
		canvas.popMatrix();

		/*
		 * Draw some debug rectangles: - Red: bounding rectangle for the menu. -
		 * Green: size of the buffer PGraphics - Blue: the area actually copied
		 * from the buffer to the drawing canvas.
		 */
		// canvas.noFill();
		// canvas.stroke(255,0,0);
		// canvas.rect(rect.x, rect.y, rect.width, rect.height);
		// canvas.stroke(0,255,0);
		// canvas.rect(x+offsetX,y+offsetY,buff.width,buff.height);
		// canvas.stroke(0,0,255);
		// canvas.rect(x+offsetX,y+offsetY,w,h);
	}

	static final int PAD = 10;

	protected void resizeBuffer()
	{
		rect.setFrame(x, y, 0, 0);
		buffRect.setFrame(x, y, 0, 0);
		getRect(rect, buffRect);

		float dX = 0;
		float dY = 0;
		// if (rect.x - (x+offsetX) < PAD)
		dX = rect.x - (x + offsetX + PAD);
		// if (rect.y - (y+offsetY) < PAD)
		dY = rect.y - (y + offsetY + PAD);

		offsetX += dX;
		offsetY += dY;

		int newWidth = buff.width;
		int newHeight = buff.height;
		boolean resizeMe = false;
		if (rect.width > buff.width - PAD * 2)
		{
			newWidth = (int) (rect.width + PAD * 2);
			resizeMe = true;
		}
		if (rect.height > buff.height - PAD * 2)
		{
			newHeight = (int) (rect.height + PAD * 2);
			resizeMe = true;
		}

		if (resizeMe)
		{
			createBuffer(newWidth, newHeight);
		}
	}

	protected boolean containsPoint(Point pt)
	{
		return false;
	}

	public void setState(MenuItem item, int newState)
	{
		/*
		 * If we're not the item's menu, throw an exception.
		 */
		if (item.menu != this)
			throw new IllegalArgumentException();
		if (item == this)
			return;
		/*
		 * If the state hasn't changed, just return.
		 */
		if (item.getState() == newState)
			return;
		/*
		 * Actually set the state variable.
		 */
		item.setState(newState);

		if (hoverNavigable)
		{
			if (newState == MenuItem.OVER || newState == MenuItem.DOWN)
				timer.setMenuItem(item);
			else if (newState == MenuItem.UP)
				timer.unsetMenuItem(item);
		}

		if (newState == MenuItem.DOWN)
		{
			lastPressed = item;
			hovered = item;
			kbFocus = item;
		} else if (newState == MenuItem.OVER)
		{
			hovered = item;
			kbFocus = item;
		} else if (newState == MenuItem.UP)
		{
			if (item == hovered)
			{
				hovered = null;
			}
		}
	}

	public void keyEvent(KeyEvent e)
	{
		switch (e.getKeyCode())
		{
			case (KeyEvent.VK_ESCAPE):
				close();
				break;
		}
		if (kbFocus != null)
			kbFocus.keyEvent(e);
		super.keyEvent(e);
	}

	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		Point useMe = model;
		if (isRootMenu() && !useCameraCoordinates)
			useMe = screen;
		/*
		 * create a copy of the point we decided to use, and translate it
		 * accordingly.
		 */
		mousePt.setLocation(useMe);
		// mousePt.translate(-x, -y);
		/*
		 * Send the mouse events through the tree of sub-items.
		 */
		setCursor(-1);
		itemMouseEvent(e, mousePt);
		if (!mouseInside && isOpen() && e.getID() == MouseEvent.MOUSE_PRESSED)
		{
			clickaway();
		}

		if (useHandCursor && isOpen() && cursor == -1)
		{
			if (mouseInside)
			{
				UIUtils.setCursor(this, canvas, Cursor.HAND_CURSOR);
			} else
			{
				UIUtils.releaseCursor(this, canvas);
			}
		}
	}

	protected int cursor;

	protected void setCursor(int c)
	{
		cursor = c;
		if (c != -1)
		{
			UIUtils.setCursor(this, canvas, c);
		}
	}

	protected void clickaway()
	{
		switch (clickAwayBehavior)
		{
			case (CLICKAWAY_HIDES):
				close();
				break;
			case (CLICKAWAY_COLLAPSES):
				closeMyChildren();
				break;
			case (CLICKAWAY_IGNORED):
			default:
				break;
		}
	}

	protected void itemMouseEvent(MouseEvent e, Point pt)
	{
		super.itemMouseEvent(e, pt);
		if (mouseInside && consumeEvents)
		{
			e.consume();
		}
		if (autoDim)
		{
			if (mouseInside)
			{
				aTween.continueTo(fullAlpha);
			} else if (menu.hovered == null)
			{
				aTween.continueTo(dimAlpha);
			}
		}
	}

	protected void setState(int state)
	{
		// Do nothing.
	}

	public void focusEvent(FocusEvent e)
	{
		if (e.getID() == FocusEvent.FOCUS_LOST)
		{
			close();
		}
	}

	/*
	 * No multiple inheritance allowed, so I had to copy this boilerplate code
	 * from AbstractUIObject instead of inheriting it. Crap!
	 */

	public void addListener(UIListener o)
	{
		listeners.add(o);
	}

	public void removeListener(UIListener o)
	{
		listeners.remove(o);
	}

	public void fireEvent(int id)
	{
		UIEvent e = new UIEvent(this, id);
		for (int i = 0; i < listeners.size(); i++)
		{
			((UIListener) listeners.get(i)).uiEvent(e);
		}
	}

}
