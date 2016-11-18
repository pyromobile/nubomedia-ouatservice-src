package com.zed.nubomedia.ouatservice;

/**
 * Mapping class to Overprints Kusasars Entity
 *  
 * @author jemalpartida
 *
 */
public class ARObject 
{
	private String name;
	private String pack;
	private String url;
	private Coords coords;

	public String getName()
	{
		return this.name;
	}

	public String getPack()
	{
		return this.pack;
	}

	public String getUrl()
	{
		return this.url;
	}

	public Coords getCoords()
	{
		return this.coords;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();

		sb.append( "NAME:" ).append( this.name );
		sb.append( " - URL:" ).append( this.url );
		sb.append( " - PACK:" ).append( this.pack );
		sb.append( " - COORDS:{" ).append( this.coords ).append( "}" );

		return sb.toString();
	}
}


class Coords
{
	private float offsetXPercent;
	private float offsetYPercent;
	private float widthPercent;
	private float heightPercent;

	public float getOffsetX()
	{
		return this.offsetXPercent;
	}

	public float getOffsetY()
	{
		return this.offsetYPercent;
	}

	public float getWidth()
	{
		return this.widthPercent;
	}

	public float getHeight()
	{
		return this.heightPercent;
	}

	@Override
	public String toString()
	{
		StringBuffer sb = new StringBuffer();

		sb.append( "offsetX:" ).append( this.offsetXPercent );
		sb.append( " - offsetY:" ).append( this.offsetYPercent );
		sb.append( " - width:" ).append( this.widthPercent );
		sb.append( " - height:" ).append( this.heightPercent );

		return sb.toString();
	}
}
