package com.zed.nubomedia.ouatservice;

import com.google.gson.JsonObject;

/**
 * 
 * @author jemalpartida
 *
 */
public class Book 
{
	public Book(String id, String langId)
	{
		this.id =id;
		this.langId = langId;
		this.currentPage = 0;
	}

	public String getId()
	{
		return this.id;
	}

	public String langId()
	{
		return this.langId;
	}

	public void nextPage()
	{
		this.currentPage += 1;
		this.action = "next";
	}

	public void prevPage()
	{
		this.currentPage -= 1;
		this.action = "prev";
	}

	public JsonObject getJson()
	{
		JsonObject json = new JsonObject();

		json.addProperty( "id", this.id );
		json.addProperty( "langId", this.langId );
		json.addProperty( "action", this.action );
		json.addProperty( "currentPage", this.currentPage );

		return json;
	}


	private String id;
	private String langId;
	private String action;
	private int currentPage;
}
