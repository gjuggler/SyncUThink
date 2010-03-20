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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;

import org.andrewberman.ui.Color;
import org.andrewberman.ui.Point;
import org.andrewberman.ui.UIUtils;

import processing.core.PApplet;
import processing.core.PFont;

public class RadialMenuItem extends MenuItem
{
	public static final float SIZE_DECAY = .9f;

	String displayLabel;

	float rLo, rHi, tLo, tHi;
	float radius;

	float outerX, outerY, innerX, innerY;
	float rectX, rectY, rectW, rectH;
	float textX, textY;
	float textWidth, textHeight, pad;
	float hintX, hintY;
	float fontSize, hintSize;
	Area wedge;

	char hint;

	static Ellipse2D.Float tempCircle = new Ellipse2D.Float(0, 0, 0, 0);
	static Arc2D.Float tempArc = new Arc2D.Float(Arc2D.PIE);
	static RoundRectangle2D.Float roundedRect = new RoundRectangle2D.Float(0,
			0, 0, 0, 0, 0);

	public RadialMenuItem()
	{
		super();
	}

	public void setHint(String hint)
	{
		this.hint = hint.charAt(0);
	}

	public void drawUnder()
	{
		Graphics2D g2 = menu.buff.g2;
		MenuUtils.drawWhiteTextRect(this, rectX, rectY, rectW, rectH);
		super.draw();
	}

	public void draw()
	{
		super.draw();
		if (isShowingLabel())
		{
			drawUnder();
			drawText();
		}
		drawShape();
		drawHint();
	}

	boolean isShowingLabel()
	{
		if (!isOpen())
		{
			MenuItem par = parent;
			int distToMenu = 1;
			while (par != menu)
			{
				distToMenu++;
				par = par.parent;
			}
			RadialMenu rm = (RadialMenu) menu;
			if (distToMenu == rm.maxLevelOpen)
			{
				return true;
			}
		}
		return false;
	}

	void drawShape()
	{
		/*
		 * Draw the main wedge shape.
		 */
		Graphics2D g2 = menu.buff.g2;
		// this.isAncestorOf(menu.currentlyHovered);
		// if (this.isAncestorOf(menu.currentlyHovered))
		if (isOpen())
			g2.setPaint(menu.style.getGradient(Menu.OVER, x - rHi, y - rHi, x
					+ rHi, y + rHi));
		else
			g2.setPaint(menu.style.getGradient(getState(), x - rHi, y - rHi, x
					+ rHi, y + rHi));
		g2.fill(wedge);
		
		
		g2.setStroke(getStroke());
		g2.setPaint(getStrokeColor());

		g2.draw(wedge);
		/*
		 * Draw the sub-items triangle, if necessary
		 */
		if (items.size() > 0 && !isOpen())
		{
			float theta = (tLo + tHi) / 2;
			float scale = (rHi - rLo) / 2;
			float dx = (float) (Math.cos(theta) * scale / 4f);
			float dy = (float) (Math.sin(theta) * scale / 4f);
			AffineTransform at = AffineTransform.getTranslateInstance(outerX
					+ dx, outerY + dy);
			at.scale(scale, scale);
			at.rotate(theta);
			Area tri = (Area) menu.style.get("subTriangle");
			Area newTri = tri.createTransformedArea(at);
			g2.setPaint(getStrokeColor());
			g2.fill(newTri);
		}
	}

	void drawText()
	{
		Graphics2D g2 = menu.buff.g2;
		PFont pf = menu.style.getFont("font");
		Font f = pf.font.deriveFont(fontSize);
		g2.setFont(f);
		g2.setPaint(menu.style.getC("c.foreground"));
		g2.drawString(displayLabel, textX, textY);
	}

	void drawHint()
	{
		Graphics2D g2 = menu.buff.g2;
		PFont pf = menu.style.getFont("font");
		Font f = pf.font.deriveFont(fontSize);
		f = f.deriveFont(hintSize);
		g2.setFont(f);
		g2.setPaint(menu.style.getC("c.foreground"));
		g2.drawString(String.valueOf(hint), hintX, hintY);
	}

	public void layout()
	{

	}

	void layout(float radLo, float radHi, float thLo, float thHi)
	{
		this.rLo = radLo;
		this.rHi = radHi;
		this.tLo = thLo;
		this.tHi = thHi;
		this.radius = radHi;

		this.layoutText();
		this.createShapes();
		/*
		 * Give each item in the next level up half as much theta room as I have
		 * This works out alright, because the radius is growing bigger as theta
		 * grows smaller.
		 */
		float tMid = (tHi + tLo) / 2;
		float dTheta = tHi - tLo;
		/*
		 * Sub-item sizing: Ensure that sub-items' thetas are constrained within
		 * a certain range.
		 */
		float minTheta = PApplet.QUARTER_PI / 2f * items.size();
		float maxTheta = Math.min(PApplet.HALF_PI * 1.5f, dTheta);

		dTheta = PApplet.constrain(dTheta, minTheta, maxTheta);
		layoutSubItems(rLo, rHi, tMid - dTheta / 2, tMid + dTheta / 2);
	}

	void layoutSubItems(float radLo, float radHi, float thLo, float thHi)
	{
		float dTheta = thHi - thLo;
		float thetaStep = dTheta / items.size();
		for (int i = 0; i < items.size(); i++)
		{
			RadialMenuItem seg = (RadialMenuItem) items.get(i);
			seg.setPosition(x, y);
			float theta = thLo + i * thetaStep;
			seg.layout(radHi, radHi + (radHi - radLo) * SIZE_DECAY, theta,
					theta + thetaStep);
		}
	}

	float radToDeg(float rad)
	{
		return PApplet.degrees(rad);
	}

	void createShapes()
	{
		tempCircle.setFrameFromCenter(x, y, x + rLo, y + rLo);
		tempArc.setFrameFromCenter(x, y, x + rHi, y + rHi);

		float degLo = radToDeg(-tLo);
		float degHi = radToDeg(-tHi);
		tempArc.setAngleStart(degLo);
		tempArc.setAngleExtent(degHi - degLo);
		wedge = new Area(tempArc);
		Area delete = new Area(tempCircle);
		wedge.subtract(delete);
	}

	void layoutText()
	{
		/*
		 * Calculate the sine and cosine, which we'll need to use often.
		 */
		float theta = (tLo + tHi) / 2;
		float cos = (float) Math.cos(theta);
		float sin = (float) Math.sin(theta);
		outerX = x + cos * rHi;
		outerY = y + sin * rHi;
		innerX = x + cos * rLo;
		innerY = y + sin * rLo;
		PFont font = menu.style.getFont("font");
		FontMetrics fm = UIUtils.getMetrics(menu.buff, font.font, 1);
		float unitTextHeight = (float) fm.getMaxCharBounds(menu.buff.g2)
				.getHeight();
		fontSize = (rHi - rLo) / unitTextHeight * .9f;
		fm = UIUtils.getMetrics(menu.buff, font.font, fontSize);
		// float descent = fm.getDescent();
		float ascent = fm.getAscent();

		// Rectangle2D bounds = fm.getStringBounds(label, menu.buff.g2);

		if (items.size() > 0)
			displayLabel = getName().concat("...");
		else
			displayLabel = getName();

		textHeight = UIUtils.getTextHeight(menu.buff, font, fontSize,
				displayLabel, true);
		// textHeight = (float) bounds.getHeight();
		// textWidth = (float) bounds.getWidth();
		textWidth = UIUtils.getTextWidth(menu.buff, font, fontSize,
				displayLabel, true);
		// Calculate the necessary x and y offsets for the text.
		float outX = x + cos * (rHi + textHeight);
		float outY = y + sin * (rHi + textHeight);
		float pad = menu.style.getF("f.padX");
		rectW = textWidth + 2 * pad;
		rectH = textHeight + 2 * pad;
		rectX = outX + cos * rectW / 2 - rectW / 2;
		rectY = outY + sin * rectH / 2 - rectH / 2;
		textX = rectX + pad;
		textY = rectY + pad + ascent;
		// textX = cos * textWidth/2;
		// textX += -textWidth / 2;
		// textX += outerX;
		// textY = sin * (textHeight)/2;
		// textY += -descent + (textHeight)/2;
		// textY += outerY;
		/*
		 * Set the background rectangle.
		 */

		// rectX = textX-pad;
		// rectY = textY + descent - textHeight - pad;
		/*
		 * Now, let's handle the hint characters.
		 */
		float rMid = (rLo + rHi) / 2;
		float centerX = x + cos * rMid;
		float centerY = y + sin * rMid;
		/*
		 * Measure the character at 1px, then scale up accordingly.
		 */
		fm = UIUtils.getMetrics(menu.buff, font.font, 1);
		String s = String.valueOf(hint);
		Rectangle2D charBounds = fm.getStringBounds(s, menu.buff.g2);
		float charHeight = (float) charBounds.getHeight();
		float charWidth = (float) charBounds.getWidth();
		float charDiagonal = PApplet.sqrt(charHeight * charHeight + charWidth
				* charWidth);
		hintSize = (rHi - rLo) / charDiagonal;
		hintSize = Math.min(hintSize, (float) Math.sin(tHi - tLo) * rMid);
		fm = UIUtils.getMetrics(menu.buff, font.font, hintSize);
		charBounds = fm.getStringBounds(s, menu.buff.g2);
		charHeight = (float) charBounds.getHeight();
		charWidth = (float) charBounds.getWidth();
		charDiagonal = PApplet.sqrt(charHeight * charHeight + charWidth
				* charWidth);
		float charDesc = fm.getDescent();

		hintX = centerX - charWidth / 2.0f;
		hintY = centerY - charDesc + charHeight / 2.0f;
	}

	protected boolean alreadyContainsChar(char c)
	{
		if (hint == c)
			return true;
		for (int i = 0; i < items.size(); i++)
		{
			RadialMenuItem rmi = (RadialMenuItem) items.get(i);
			if (rmi.alreadyContainsChar(c))
				return true;
		}
		return false;
	}

	float getMaxRadius()
	{
		if (!isOpen())
			return 0;
		float max = this.rHi;
		for (int i = 0; i < items.size(); i++)
		{
			RadialMenuItem rmi = (RadialMenuItem) items.get(i);
			float cur = rmi.getMaxRadius();
			if (cur > max)
				max = cur;
		}
		return max;
	}

	public boolean containsPoint(Point pt)
	{
//		if (!isOpen())
//			return false;
		boolean contained = false;
		if (wedge.contains(pt.x, pt.y))
			contained = true;
		if (isShowingLabel())
		{
			Rectangle2D.Float temp = new Rectangle2D.Float(rectX, rectY, rectW,
					rectH);
			if (temp.contains(pt.x, pt.y))
				contained = true;
		}
		return contained;
	}

	protected void keyHintEvent(KeyEvent e)
	{
		if (isOpen())
		{
			for (int i = 0; i < items.size(); i++)
			{
				RadialMenuItem rmi = (RadialMenuItem) items.get(i);
				rmi.keyHintEvent(e);
			}
		}
		if (e.isConsumed())
			return;
		char c = (char) e.getKeyChar();
		if (Character.toLowerCase(c) == Character.toLowerCase(hint))
		{
			this.performAction();
			e.consume();
			return;
		}
	}

	protected void visibleMouseEvent(MouseEvent e, Point tempPt)
	{
		super.visibleMouseEvent(e, tempPt);
		if (getState() == MenuItem.OVER)
			e.consume();
	}

	public void getRect(Rectangle2D.Float rect, Rectangle2D.Float buff)
	{
		// if (isOpen())
		// {
		super.getRect(rect, buff);
		buff.setRect(wedge.getBounds2D());
		Rectangle2D.union(rect, buff, rect);
		buff.setRect(rectX, rectY, rectW, rectH);
		Rectangle2D.union(rect, buff, rect);
		// }
	}
}
