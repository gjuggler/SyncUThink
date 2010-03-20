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
package org.andrewberman.sync;

import java.util.regex.Pattern;

public class RegexUtils
{

	public static String d = ".*?";
	public static String nq = "[^\"]*?";
	public static String nb = "[^<>]*?";

	public static Pattern grabUntilClosingElement(String tag, Pattern inside)
	{
		String ins = inside.pattern();
	
		String start = "<" + nb + tag + nb + ">";
		String end = "</" + nb + tag + nb + ">";
	
		return createPattern("(" + inside + d + end + ")");
	}

	public static Pattern createPattern(String s)
	{
		return Pattern.compile(s, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	}

	public static Pattern hrefRegex(String search)
	{
		return generalRegex("href", search);
	}

	public static Pattern generalRegex(String key, String search)
	{
		return createPattern(genRegString(key, search));
	}

	public static String genRegString(String key, String search)
	{
		return key + "=\"(" + nq + search + nq + ")\"";
	}

	public static Pattern elementIdRegex(String el, String id, String search)
	{
		// return
		// Pattern.compile("<"+el+".*?id=\""+id+"\".*?href=\"("+nq+search+nq+")\".*?</"+el);
		String a = ".*?";
		String b = "[^<>]*?";
	
		String idS = "id=\"(" + id + ")\"";
	
		String leftEl = "<" + b + el + b + idS + b + ">";
		String rightEl = "</" + b + el + b + ">";
		String middle = genRegString("href", search);
		return createPattern(leftEl + a + middle + a + rightEl);
	
	}

}
