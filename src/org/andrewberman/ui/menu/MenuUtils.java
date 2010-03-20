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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

import org.andrewberman.ui.Color;
import org.andrewberman.ui.UIUtils;

import processing.core.PFont;
import processing.core.PGraphicsJava2D;

public class MenuUtils
{

	static RoundRectangle2D.Float roundRect = new RoundRectangle2D.Float();

	public static synchronized void drawWhiteTextRect(MenuItem item, float x,
			float y, float w, float h)
	{
		Graphics2D g2 = item.menu.buff.g2;

		roundRect.setRoundRect(x, y, w, h, h / 3, h / 3);
		if (!item.isEnabled())
			g2.setPaint(item.menu.style.getC("c.disabled"));
		else
			g2.setPaint(Color.white);
		g2.fill(roundRect);
		g2.setPaint(Color.black);
		g2.setStroke(new BasicStroke(item.menu.style.getF("f.strokeWeight")));
		g2.draw(roundRect);
	}

	public static synchronized void drawBackgroundRoundRect(MenuItem item,
			float x, float y, float width, float height)
	{
		PGraphicsJava2D buff = item.menu.buff;
		MenuStyle style = item.menu.style;
		Menu menu = item.menu;

		float ro = style.getF("f.roundOff");

		roundRect.setRoundRect(x, y, width, height, ro, ro);

		buff.g2.setPaint(style.getC("c.background"));
		buff.g2.fill(roundRect);

		buff.g2.setStroke(new BasicStroke(style.getF("f.strokeWeight")));
		buff.g2.setPaint(style.getC("c.foreground"));
		buff.g2.draw(roundRect);
	}

	public static synchronized void drawVerticalGradientRect(MenuItem item,
			float x, float y, float width, float height)
	{
		PGraphicsJava2D buff = item.menu.buff;
		MenuStyle style = item.menu.style;
		Menu menu = item.menu;

		float ro = style.getF("f.roundOff");

		roundRect.setRoundRect(x, y, width, height, ro, ro);
		/*
		 * Draw the first gradient: a full-width gradient.
		 */
		buff.g2.setPaint(style.getGradient(MenuItem.UP, x, y, x + width, y));
		buff.g2.fill(roundRect);
		/*
		 * Draw a translucent gradient on top of the first, starting halfway and
		 * going to the bottom.
		 */
		Composite oldC = buff.g2.getComposite();
		AlphaComposite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.5f * menu.alpha);
		buff.g2.setComposite(c);
		buff.g2.setPaint(style.getGradient(MenuItem.UP, x + item.width, y, x
				+ item.width / 3, y));
		buff.g2.fillRect((int) (x + width / 2), (int) (y), (int) (width / 2),
				(int) height);
		buff.g2.setComposite(oldC);
		/*
		 * Finally, draw the stroke on top of everything.
		 */
		RenderingHints rh = menu.buff.g2.getRenderingHints();
		buff.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		buff.g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
				RenderingHints.VALUE_STROKE_PURE);
		buff.g2.setRenderingHint(RenderingHints.KEY_RENDERING,
				RenderingHints.VALUE_RENDER_QUALITY);
		buff.g2.setPaint(style.getC("c.foreground"));
		buff.g2.setStroke(new BasicStroke(style.getF("f.strokeWeight")));
		buff.g2.draw(roundRect);
		buff.g2.setRenderingHints(rh);
	}

	public static void drawRoundOutline(MenuItem item, RoundRectangle2D r2)
	{
		MenuStyle style = item.menu.style;
		float sw = style.getF("f.strokeWeight");

		//		float newX = (float) (Math.floor(r2.getX())-sw);
		//		float newY = (float) (Math.ceil(r2.getY())-sw);
		//		
		//		float newWidth = (float) (Math.ceil(r2.getWidth()+r2.getX()-newX));
		//		float newHeight = (float) (Math.ceil(r2.getHeight()+r2.getY()-newY));

		//		RoundRectangle2D clone = (RoundRectangle2D) r2.clone();
		RoundRectangle2D clone = r2;

		//		clone.setRoundRect(newX, newY, newWidth, newHeight, r2.getArcWidth(),
		//				r2.getArcHeight());

		preDraw(item);
		item.menu.buff.g2.draw(clone);
		postDraw(item);
	}

	static RenderingHints rh;

	static void preDraw(MenuItem item)
	{
		Menu menu = item.menu;
		MenuStyle style = menu.style;
		PGraphicsJava2D buff = item.menu.buff;
		rh = menu.buff.g2.getRenderingHints();
		buff.g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		buff.g2.setPaint(style.getC("c.foreground"));
		buff.g2.setStroke(new BasicStroke(style.getF("f.strokeWeight")));
	}

	static void postDraw(MenuItem item)
	{
		item.menu.buff.g2.setRenderingHints(rh);
	}

	public static synchronized void drawDoubleGradientRect(MenuItem item,
			float x, float y, float width, float height)
	{
		PGraphicsJava2D buff = item.menu.buff;
		MenuStyle style = item.menu.style;
		Menu menu = item.menu;

		float ro = style.getF("f.roundOff");

		roundRect.setRoundRect(x, y, width, height, ro, ro);

		/*
		 * Draw the first gradient: a full-height gradient.
		 */
		buff.g2.setPaint(style.getGradient(MenuItem.UP, x, y, x, y + height));
		buff.g2.fill(roundRect);
		/*
		 * Draw a translucent gradient on top of the first, starting halfway and
		 * going to the bottom.
		 */
		Composite oldC = buff.g2.getComposite();
		AlphaComposite c = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				0.5f * menu.alpha);
		buff.g2.setComposite(c);
		buff.g2.setPaint(style.getGradient(MenuItem.UP, x, y + height, x, y
				+ height / 3));
		buff.g2.fillRect((int) x, (int) (y + height / 2), (int) width,
				(int) height / 2);
		buff.g2.setComposite(oldC);
		/*
		 * Finally, draw the stroke on top of everything.
		 */
		drawRoundOutline(item, roundRect);

	}

	public static synchronized void drawBlankRect(MenuItem item, float x,
			float y, float w, float h)
	{
		Menu menu = item.menu;
		MenuStyle style = menu.style;

		roundRect.setRoundRect(x, y, w, h, 0, 0);
		drawRoundOutline(item, roundRect);
	}

	public static synchronized void drawSingleGradientRect(MenuItem item,
			float x, float y, float width, float height)
	{
		float ro = item.menu.style.getF("f.roundOff");
		drawSingleGradientRect(item, x, y, width, height, ro);
	}

	public static synchronized void drawSingleGradientRect(MenuItem item,
			float x, float y, float width, float height, float roundOff)
	{
		Menu menu = item.menu;
		MenuStyle style = menu.style;

		roundRect.setRoundRect(x, y, width, height, roundOff, roundOff);
		Graphics2D g2 = menu.buff.g2;
		/*
		 * Set the correct fill gradient
		 */
		if (item.isOpen())
		{
			g2.setPaint(menu.style.getGradient(MenuItem.DOWN, y, y + height));
		} else
		{
			g2.setPaint(menu.style.getGradient(MenuItem.OVER, y, y + height));
		}
		/*
		 * Only perform the fill if the mood is right.
		 */
		if (item.getState() != MenuItem.UP || item.isOpen())
		{
			g2.fill(roundRect);
		}
		/*
		 * Draw the rounded rectangle outline.
		 */
		if (item.getState() != MenuItem.UP || item.isOpen())
		{
			drawRoundOutline(item, roundRect);
		}
	}

	public static void drawCenteredText(MenuItem item)
	{
		drawText(item, item.getName(), true, true, item.x, item.y, item.width,
				item.height);
	}

	public static void drawLeftText(MenuItem item, String s, float x)
	{
		drawText(item, s, false, true, x, item.y, item.width, item.height);
	}

	public static void drawText(MenuItem item, String s, boolean centerH,
			boolean centerV, float x, float y, float width, float height)
	{

		Graphics2D g2 = item.menu.buff.g2;

		float descent = getTextDescent(item);

		float xOffset = 0, yOffset = 0;
		if (centerH)
		{
			float tWidth = getTextWidth(item, s);
			xOffset = (width - tWidth) / 2f;
		}
		if (centerV)
		{
			float tHeight = getTextHeight(item, s);
			yOffset = (height - tHeight) / 2f + descent;
		} else
			yOffset = height - descent;

		MenuStyle style = item.menu.style;
		PFont font = style.getFont("font");
		float fs = style.getF("f.fontSize");

		g2.setFont(font.font.deriveFont(fs));
		g2.setPaint(style.getC("c.foreground"));
		g2.setStroke(new BasicStroke(style.getF("f.strokeWeight")));
		g2.drawString(s, x + xOffset, y + yOffset);
	}

	public static float getTextWidth(MenuItem item, String s)
	{
		MenuStyle style = item.menu.style;
		return UIUtils.getTextWidth(item.menu.buff, style.getFont("font"),
				style.getF("f.fontSize"), s, true);
	}

	public static float getTextHeight(MenuItem item, String s)
	{
		MenuStyle style = item.menu.style;
		return UIUtils.getTextHeight(item.menu.buff, style.getFont("font"),
				style.getF("f.fontSize"), s, true);
	}

	public static float getTextAscent(MenuItem item)
	{
		MenuStyle style = item.menu.style;
		return UIUtils.getTextAscent(item.menu.buff, style.getFont("font"),
				style.getF("f.fontSize"), true);
	}

	public static float getTextDescent(MenuItem item)
	{
		MenuStyle style = item.menu.style;
		return UIUtils.getTextAscent(item.menu.buff, style.getFont("font"),
				style.getF("f.fontSize"), true);
	}
}
