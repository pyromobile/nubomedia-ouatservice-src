package com.zed.nubomedia.ouatservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class QRObject 
{
	public int getId()
	{
		return this.alvarMarkerId;
	}

	public String getType()
	{
		return this.type;
	}

	public JsonObject toJson()
	{
		JsonObject json = new JsonObject();

		json.addProperty( "id", this.alvarMarkerId );
		json.addProperty( "type", this.type );

		//string-models.
		JsonObject modelJson = new JsonObject();
		modelJson.addProperty( "model", this.model );
		modelJson.addProperty( "label", this.label );
		JsonArray strings = new JsonArray();
		strings.add( modelJson );
		json.add( "strings", strings );

		//ints
		json.add( "ints", new JsonArray() );

		//floats
		JsonObject scale = new JsonObject();
		scale.addProperty( "scale", this.scale );
		JsonArray floats = new JsonArray();
		floats.add( scale );
		json.add( "floats", floats );

		return json;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "ID:" ).append( this.alvarMarkerId );
		sb.append( " - TYPE:" ).append( this.type );
		sb.append( " - PATH:" ).append( this.model );
		sb.append( " - LABEL:" ).append( this.label );
		sb.append( " - SCALE:" ).append( this.scale );

		return sb.toString();
	}


	private int alvarMarkerId;
	private String type;
	private String model;
	private String label;
	private float scale;
}
