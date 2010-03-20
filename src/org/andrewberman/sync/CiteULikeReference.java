package org.andrewberman.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CiteULikeReference
{
	String article_id;
	String citation;
	String href;
	ArrayList<String> tags;
	ArrayList<String> linkouts;
	Map<String, String> userfiles;
	Map<String, String> userfileIds;

	public CiteULikeReference(JSONObject jso)
	{
		try
		{
			this.article_id = "";
			if (jso.has("article_id"))
			{
				this.article_id = jso.getString("article_id");
			}
			this.citation = "";
			if (jso.has(""))
			{
				this.article_id = jso.getString("article_id");
			}
			this.href = "";
			if (jso.has("href"))
			{
				this.href = jso.getString("href");
			}
			this.tags = new ArrayList<String>();
			if (jso.has("tags")) {
				JSONArray jsonTags = jso.getJSONArray("tags");
				for (int i = 0; i < jsonTags.length(); i++)
				{
					this.tags.add(jsonTags.getString(i));
				}
			}
			this.linkouts = new ArrayList<String>();
			if (jso.has("linkouts"))
			{
				JSONArray jsonLinks = jso.getJSONArray("linkouts");
				for (int i = 0; i < jsonLinks.length(); i++)
				{
					JSONObject linkout = jsonLinks.getJSONObject(i);
					if (linkout.has("url"))
					{
						this.linkouts.add(linkout.getString("url"));
					}

				}
			}
			this.userfiles = new HashMap<String,String>();
			this.userfileIds = new HashMap<String,String>();
			if (jso.has("userfiles"))
			{
				JSONArray userFileArray = jso.getJSONArray("userfiles");
				for (int i = 0; i < userFileArray.length(); i++)
				{
					JSONObject userFile = userFileArray.getJSONObject(i);
					if (userFile.has("fileId") && userFile.has("name"))
					{
						this.userfiles.put(userFile.getString("name"), userFile.getString("path"));
						this.userfileIds.put(userFile.getString("name"), userFile.getString("fileId"));
					}
				}
			}
		} catch (JSONException jse)
		{
			jse.printStackTrace();
		}
	}

	public String getArticle_id()
	{
		return article_id;
	}

	public String getHref()
	{
		return href;
	}

	public ArrayList<String> getTags()
	{
		return tags;
	}

	public ArrayList<String> getLinkouts()
	{
		return linkouts;
	}

	public Map<String, String> getUserfiles()
	{
		return userfiles;
	}

}
