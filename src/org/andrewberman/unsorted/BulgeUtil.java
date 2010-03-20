/**************************************************************************
 * Copyright (c) 2007, 2008 Gregory Jordan
 * 
 * This file is part of SyncUThink.
 * 
 * SyncUThink is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * SyncUThink is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with SyncUThink.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.andrewberman.unsorted;

public class BulgeUtil
{

	public static float bulge(float dist, float bulgeAmount,float bulgeWidth)
	{
		return (float)(1.0 + bulgeAmount
		*Math.exp(-dist * dist / (bulgeWidth * bulgeWidth)));
	}
	
}
