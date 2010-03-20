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
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

import org.andrewberman.ui.Color;
import org.andrewberman.ui.FocusManager;
import org.andrewberman.ui.FontLoader;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;

import processing.core.PApplet;
import processing.core.PFont;
import processing.core.PGraphics;

/**
 * The <code>Dock</code> class is a close approximation of Apple's infamous
 * Dock menubar.
 * <p>
 * It is built on top of the <code>Menu</code> superclass, except unlike most
 * other <code>Menu</code> derivatives, the <code>Dock</code> does not rely
 * on Java2D rendering (although it makes use of it if available). As such, it
 * can be drawn directly to the canvas, without requiring the processor and
 * memory overhead of creating and drawing to an off-screen buffer. Thus, it
 * should be snappy under P3D and OpenGL renderers.
 * <p>
 * By default, the <code>Dock</code> "docks" and centers itself along one side
 * of the screen. You can alter this behavior by
 * 
 * @author Greg
 */
public class Dock extends Menu
{
	public static final int LEFT = 0;
	public static final int RIGHT = 1;
	public static final int TOP = 2;
	public static final int BOTTOM = 3;
	DockRotationHandler rotation;

	RoundRectangle2D.Float mouseRect, drawRect;
	Point mousePt;
	float origWidth, inset, offset, maxPossibleWidth;
	float curWidth, curHeight, curLow;
	boolean isActivated;

	/**
	 * The amount by which the icons "bulge" when approached.
	 */
	public float bulgeAmount = .7f;
	/**
	 * The "rolloff" factor for the icons' bulge. Play around with it to find a
	 * value that you like.
	 */
	public float bulgeWidth = 20.0f;

	/**
	 * If set to true, then this Dock will automatically center itself to the
	 * side against which it's docked. Offset will offset it in the positive x
	 * or y direction away from the center. If false, then the offset will cause
	 * the dock to be offset from the corner by that amount.
	 * <p>
	 * Note that in the <code>false</code> case, the offset calculated will be
	 * based on the <em>resting</em> size of the <code>Dock</code> When
	 * items become bulged, the dock will likely extend a bit out in both
	 * directions, so you'd best give it a little extra <code>offset</code>
	 * just to be safe!
	 */
	public boolean autoCenter;

	/**
	 * If set to true, then a triangle will be drawn on the last clicked item.
	 * If false, no triangles. Simple!
	 */
	public boolean triangleOnSelected;

	public boolean consumeWhenActive;

	public Dock(PApplet app)
	{
		super(app);
		inset = style.getF("f.padX");

		drawRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
		mouseRect = new RoundRectangle2D.Float(0, 0, 0, 0, 0, 0);
		mousePt = new Point(0, 0);
		rotation = new DockRotationHandler();
		rotation.setRotation(BOTTOM);

		setWidth(40);
		open();
	}

	public void setOptions()
	{
		super.setOptions();

		useCameraCoordinates = false;
		clickAwayBehavior = CLICKAWAY_COLLAPSES;
		hoverNavigable = false;
		clickToggles = true;
		useHandCursor = true;
		actionOnMouseDown = true;
		usesJava2D = true;
		autoDim = true;

		autoCenter = true;
		triangleOnSelected = true;
		consumeWhenActive = true;
	}

	public void layout()
	{
		mousePt.setLocation(canvas.mouseX, canvas.mouseY);
		if (useCameraCoordinates)
			UIUtils.screenToModel(mousePt);
		float mousePos = mousePos();
		float origHeight = origWidth * items.size();
		float origCenter = rotation.getCenter();
		float origLow = origCenter - origHeight / 2;
		float mouseOffset = mousePos - origLow;

		float newOffset = 0;
		float pos = origLow;
		for (MenuItem item : items)
		{
			float mid = pos + origWidth / 2;
			float scale = bulge(mid - mousePos);
			item.setSize(origWidth * scale, origWidth * scale);
			newOffset += item.getHeight();
			pos += origWidth;
		}
		curLow = origLow - (newOffset - origHeight)
				* (mouseOffset / origHeight);
		pos = curLow;
		float maxWidth = 0;
		for (int i = 0; i < items.size(); i++)
		{
			MenuItem item = (MenuItem) items.get(i);
			rotation.positionItem(item, pos);
			pos += item.getHeight();
			if (item.getWidth() > maxWidth)
				maxWidth = item.getWidth();
		}
		curHeight = pos - curLow;
		curWidth = maxWidth;
	}

	float mousePos()
	{
		return rotation.getMousePos(mousePt);
	}

	float bulge(float dist)
	{
		if (isActivated)
		{
			return 1.0f + bulgeAmount
					* PApplet.exp(-dist * dist / (bulgeWidth * bulgeWidth));
		} else
			return 1.0f;
	}

	public void setOffsetFromCenter(float offset)
	{
		this.offset = offset;
		layout();
	}

	public void setWidth(float newWidth)
	{
		origWidth = newWidth;
		maxPossibleWidth = origWidth * (1 + bulgeAmount);
		layout();
	}

	public void setInset(float inset)
	{
		this.inset = inset;
		layout();
	}

	public void setRotation(String rot)
	{
		if (rot.equalsIgnoreCase("left"))
			rotation.setRotation(LEFT);
		else if (rot.equalsIgnoreCase("right"))
			rotation.setRotation(RIGHT);
		if (rot.equalsIgnoreCase("top"))
			rotation.setRotation(TOP);
		if (rot.equalsIgnoreCase("bottom"))
			rotation.setRotation(BOTTOM);
	}

	public DockItem getSelectedItem()
	{
		if (lastPressed == null)
			return null;
		else
			return (DockItem) lastPressed;
	}

	void resetPosition()
	{
		for (MenuItem item : items)
		{
			DockItem d = (DockItem) item;
			d.forceSize(origWidth, origWidth);
		}
		layout();
	}

	public void drawBefore()
	{
		layout();
		rotation.setRect(drawRect, origWidth);
		
		Color strokeC = menu.style.getC("c.foreground");
		float sw = menu.style.getF("f.strokeWeight");
		Stroke stroke = new BasicStroke(sw);
		float px = menu.style.getF("f.padX");
		float py = menu.style.getF("f.padY");
		PFont font = menu.style.getFont("font");
		
		/*
		 * Draw a nice-looking background gradient.
		 */
		if (usesJava2D)
		{
			Graphics2D g2 = menu.buff.g2;
			g2.setPaint(menu.style.getGradient(MenuItem.UP, 0, (float) drawRect.getMinY(),
					0, (float) drawRect.getMaxY()));
			g2.fill(drawRect);
			g2.setStroke(stroke);
			g2.setPaint(strokeC);
			RenderingHints rh = g2.getRenderingHints();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_ON);
			g2.draw(drawRect);
			g2.setRenderingHints(rh);
		} else
		{
			int alpha = (int) (menu.alpha * 255);
			int color;
			Color c;
			canvas.beginShape(PApplet.QUADS);
			canvas.stroke(canvas.color(strokeC.getRGB(), alpha));
			c = style.getC("c.gradientLo");
			color = canvas.color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
			canvas.fill(color);
			canvas.vertex((float) drawRect.getMinX(), (float) drawRect
					.getMinY());
			canvas.vertex((float) drawRect.getMaxX(), (float) drawRect
					.getMinY());
			c = style.getC("c.gradientHi");
			color = canvas.color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
			canvas.fill(color);
			canvas.vertex((float) drawRect.getMaxX(), (float) drawRect
					.getMaxY());
			canvas.vertex((float) drawRect.getMinX(), (float) drawRect
					.getMaxY());
			canvas.endShape();
		}

		if (hovered != null)
		{
			DockItem i = (DockItem) hovered;
			float fontSize = origWidth / 2f;
			float ascent = UIUtils.getTextAscent(menu.canvas.g,
					font, fontSize, false);
			float descent = UIUtils.getTextDescent(menu.canvas.g,
					font, fontSize, false);
			float tHeight = (ascent + descent);
			float tWidth = UIUtils.getTextWidth(menu.canvas.g, font,
					fontSize, i.getLabel(), false);

			float tX = 0;
			float tY = 0;
			switch (rotation.rot)
			{
				case (LEFT):
					menu.canvas.textAlign(PApplet.LEFT);
					tX = inset + maxPossibleWidth + px;
					tY = i.getY() + i.getHeight() / 2 + tHeight / 2 - descent
							/ 2;
					break;
				case (RIGHT):
					menu.canvas.textAlign(PApplet.LEFT);
					tX = menu.canvas.width - inset - maxPossibleWidth
							- px - tWidth;
					tY = i.getY() + i.getHeight() / 2 + tHeight / 2 - descent
							/ 2;
					break;
				case (TOP):
					menu.canvas.textAlign(PApplet.CENTER);
					tX = i.getX() + i.getWidth() / 2;
					tY = inset + maxPossibleWidth + px + tHeight
							- descent;
					break;
				case (BOTTOM):
					menu.canvas.textAlign(PApplet.CENTER);
					tX = i.getX() + i.getWidth() / 2;
					tY = menu.canvas.width - inset - maxPossibleWidth
							- px;
					break;
			}

			if (usesJava2D)
			{
				MenuUtils.drawWhiteTextRect(this, tX - px, tY
						- ascent - px, tWidth + px
						* 2, tHeight + px * 2);
			}

			Color c = strokeC;
			int alpha = (int) (menu.alpha * 255);
			menu.canvas.fill(menu.canvas.color(c.getRed(), c.getGreen(), c
					.getBlue(), alpha));
			// menu.canvas.fill(0,alpha);
			menu.canvas.textFont(FontLoader.instance.vera);
			menu.canvas.textSize(fontSize);
			menu.canvas.text(i.getLabel(), tX, tY);
			menu.canvas.textAlign(PApplet.LEFT);
		}

		if (lastPressed != null && triangleOnSelected)
		{
			MenuItem i = lastPressed;
			PGraphics pg = canvas.g;
			int alpha = (int) (menu.alpha * 255);
			Color c = strokeC;
			pg.fill(menu.canvas.color(c.getRed(), c.getGreen(), c.getBlue(),
					alpha));

			float height = i.getWidth() / 8;
			switch (rotation.rot)
			{
				case (LEFT):
					float cy = i.getY() + i.getHeight() / 2;
					float cx = inset + px;
					pg.triangle(cx, cy + height, cx, cy - height, cx + height,
							cy);
					i.setPosition(i.getX() + height, i.getY());
					break;
				case (RIGHT):
					cy = i.getY() + i.getHeight() / 2;
					cx = canvas.width - inset - px;
					pg.triangle(cx, cy + height, cx, cy - height, cx - height,
							cy);
					i.setPosition(i.getX() - height, i.getY());
					break;
				case (TOP):
					cy = inset + py;
					cx = i.getX() + i.getWidth() / 2;
					pg.triangle(cx + height, cy, cx - height, cy, cx, cy
							+ height);
					i.setPosition(i.getX(), i.getY() + height);
					break;
				case (BOTTOM):
					cy = canvas.width - inset - py;
					cx = i.getX() + i.getWidth() / 2;
					pg.triangle(cx + height, cy, cx - height, cy, cx, cy
							- height);
					i.setPosition(i.getX(), i.getY() - height);
					break;
			}
		}

	}

	public void focusEvent(FocusEvent e)
	{
		if (e.getID() == FocusEvent.FOCUS_LOST)
		{
			isActivated = false;
			layout();
		}
	}

	public void keyEvent(KeyEvent e)
	{
		super.keyEvent(e);
	}

	@Override
	public void setState(MenuItem i, int s)
	{
		super.setState(i, s);
	}

	public void setHidden(String s)
	{
		if (s.startsWith("t"))
		{
			close();
		} else
			open();
	}
	
	public void mouseEvent(MouseEvent e, Point screen, Point model)
	{
		if (useCameraCoordinates)
			mousePt.setLocation(model);
		else
			mousePt.setLocation(screen);
		if (e.getID() == MouseEvent.MOUSE_MOVED
				|| e.getID() == MouseEvent.MOUSE_DRAGGED)
		{
			if (containsPoint(mousePt))
			{
				isActivated = true;
				FocusManager.instance.setModalFocus(this);
			} else
			{
				isActivated = false;
				FocusManager.instance.removeFromFocus(this);
			}
		}
		super.mouseEvent(e, screen, model);

		if (isActivated && consumeWhenActive)
		{
			e.consume();
		}
	}

	public boolean containsPoint(Point pt)
	{
		rotation.setRect(mouseRect, curWidth);
		return mouseRect.contains(pt);
	}

	public float getX()
	{
		return 0;
	}

	public float getY()
	{
		return 0;
	}

	public void setPosition(float inset, float offset)
	{
		this.inset = inset;
		this.offset = offset;
		layout();
	}

	public MenuItem create(String label)
	{
		DockItem di = new DockItem();
		di.setName(label);
		return di;
	}

	public DockItem add(String label, String iconFile)
	{
		DockItem addMe = new DockItem();
		addMe.setName(label);
		addMe.setIcon(iconFile);
		add(addMe);
		return addMe;
	}
	
	@Override
	public MenuItem add(MenuItem item)
	{
		super.add(item);
		resetPosition();
		return item;
	}

	class DockRotationHandler
	{
		int rot = LEFT;

		void setRotation(int i)
		{
			rot = i;
			layout();
		}

		boolean isHorizontal()
		{
			return (rot == TOP || rot == BOTTOM);
		}

		boolean isVertical()
		{
			return (rot == LEFT || rot == RIGHT);
		}

		float getMousePos(Point pt)
		{
			switch (rot)
			{
				case (TOP):
				case (BOTTOM):
					return pt.x;
				case (LEFT):
				case (RIGHT):
				default:
					return pt.y;
			}
		}

		float getCenter()
		{
			if (!autoCenter)
				return offset + origWidth * items.size() / 2;
			else
			{
				switch (rot)
				{
					case (TOP):
					case (BOTTOM):
						return canvas.width / 2 + offset;
					case (LEFT):
					case (RIGHT):
					default:
						return canvas.height / 2 + offset;
				}
			}
		}

		void setRect(RoundRectangle2D.Float rect, float width)
		{
			float r = origWidth / 4;
			switch (rot)
			{
				case (RIGHT):
					rect.setRoundRect(canvas.width - inset - width, curLow,
							width, curHeight, r, r);
					break;
				case (TOP):
					rect.setRoundRect(curLow, inset, curHeight, width, r, r);
					break;
				case (BOTTOM):
					rect.setRoundRect(curLow, canvas.height - inset - width,
							curHeight, width, r, r);
					break;
				case (LEFT):
				default:
					rect.setRoundRect(inset, curLow, width, curHeight, r, r);
			}
		}

		void positionItem(MenuItem item, float pos)
		{
			DockItem d = (DockItem) item;
			PGraphics pg = canvas.g;
			switch (rot)
			{
				case (LEFT):
					d.setPosition(inset, pos);
					break;
				case (RIGHT):
					d.setPosition(pg.width - item.getWidth() - inset, pos);
					break;
				case (TOP):
					d.setPosition(pos, inset);
					break;
				case (BOTTOM):
					d.setPosition(pos, pg.height - item.getHeight() - inset);
					break;
			}
		}
	}

	public float getBulgeAmount()
	{
		return bulgeAmount;
	}

	public void setBulgeAmount(float bulgeAmount)
	{
		this.bulgeAmount = bulgeAmount;
	}

	public float getBulgeWidth()
	{
		return bulgeWidth;
	}

	public void setBulgeWidth(float bulgeWidth)
	{
		this.bulgeWidth = bulgeWidth;
	}
}
