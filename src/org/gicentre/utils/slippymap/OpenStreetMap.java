package org.gicentre.utils.slippymap;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

//****************************************************************************************
/** Indentifies the required OpenStreetMap tiles
*    
* Only intended to be used by SlippyMap - hence Class and all methods have only
* package-wide visibility
*  
* @author Aidan Slingsby, giCentre, City University London.
* @version 1.0, August 2011 
*/ 
//*****************************************************************************************

class OpenStreetMap extends BaseMapProvider{
	
	/** OpenStreetMap map tile types
	 * 
	 */
	static public enum OsmTileType{
		MAPNIK,
		MAPQUEST,
		CLOUDMADE,
		WATERCOLOUR,
		TONER,
		TERRAIN
	}
	
	int cloudMadeStyleId=1; //Cloudmade style ID
	String ApiKey=null;  //Cloudemade API key
	
	PApplet applet;

	/**Constructor
	 * 
	 * @param applet  The sketch
	 * @param tilePixelWidth The tile width in pixels (affects resolution)
	 * @param tileRetriever Instance of the threaded tile retriever
	 * @param tileCache The tile cache
	 */
	OpenStreetMap(PApplet applet,int tilePixelWidth,TileRetriever tileRetriever){
		super(applet,tilePixelWidth,tileRetriever);
	}

	/** Draws the map
	 * 
	 * @param latLonBounds  Viewport
	 * @param screenBounds Screen area
	 * @param zoomPan ZoomPan (for zoom/pan information)
	 * @param g  The canvas to draw on (use PApplet.g for on-screen drawing)
	 * @param osmTileType  The tile type
	 */
	void drawMap(Rectangle2D latLonBounds,Rectangle screenBounds,ZoomPanState zoomPanState,PGraphics g,OsmTileType osmTileType){ 

		//find appropriate zoom
		int correctNumTilesAcross=screenBounds.width/tilePixelWidth;
		//start at max resolution
		int firstTileX=lon2TileX(latLonBounds.getMinX(),20);
		int firstTileY=lat2TileY(latLonBounds.getMinY(),20);
		int lastTileX=lon2TileX(latLonBounds.getMaxX(),20);
		int lastTileY=lat2TileY(latLonBounds.getMaxY(),20);
		int numTilesAcross=lastTileX-firstTileX;
		int zoom=20;
		//try decreasing zoom level until an appropriate one is found
		while (numTilesAcross>correctNumTilesAcross && zoom>1){
			zoom--;
			numTilesAcross/=2;
		}

		//display tiles in viewable area
		HashSet<String> keys=new HashSet<String>();
		List<TileInfo> tilesToDraw=new ArrayList<OpenStreetMap.TileInfo>();
		
		firstTileX = lon2TileX((float)Math.max(latLonBounds.getMinX(),-180),zoom);
		lastTileX = lon2TileX((float)Math.min(latLonBounds.getMaxX(),180),zoom);
		firstTileY = lat2TileY((float)Math.min(latLonBounds.getMaxY(),85),zoom);
		lastTileY = lat2TileY((float)Math.max(latLonBounds.getMinY(),-85),zoom);
		for (int tileX=firstTileX;tileX<=lastTileX;tileX++){
			for (int tileY=firstTileY;tileY<=lastTileY;tileY++){
				if (!keys.contains(tileX+":"+tileY+":"+zoom)){
					PImage im = getTileImage(tileX,tileY,zoom,false,osmTileType);
					if (im!=null){
						//If a tile is successfully retrieved...
						//Save as an instance of TileInfo
						TileInfo tileInfo = new TileInfo();
						tileInfo.tileX=tileX;
						tileInfo.tileY=tileY;
						tileInfo.zoom=zoom;
						tileInfo.image=im;
						//Add to list of tiles to draw
						tilesToDraw.add(tileInfo);
						keys.add(tileX+":"+tileY+":"+zoom);
					}
					else{
						//then look for a lower-res one from the cache
						int newZoom=zoom;
						double lon = tileX2Lon(tileX, zoom);
						double lat = tileY2Lat(tileY, zoom);
						int newTileX=0;
						int newTileY=0;
						while (newZoom>0 && im==null){
							newZoom--;
							newTileX=lon2TileX(lon, newZoom);
							newTileY=lat2TileY(lat, newZoom);
							im = getTileImage(newTileX,newTileY,newZoom,true,osmTileType);
						}
//						System.out.println(newZoom);
						if (im!=null && !keys.contains(newTileX+":"+newTileY+":"+newZoom)){
							TileInfo tileInfo = new TileInfo();
							tileInfo.tileX=newTileX;
							tileInfo.tileY=newTileY;
							tileInfo.zoom=newZoom;
							tileInfo.image=im;
							tilesToDraw.add(tileInfo);
							keys.add(newTileX+":"+newTileY+":"+newZoom);
						}
					}
				}
			}
		}
		
		//Sort the tiles by their map resolution (so lower res ones are drawn first
		Collections.sort(tilesToDraw);
		//Draw the tiles
		for (TileInfo tileInfo:tilesToDraw){
			PVector p1=new PVector(
					(float)(SlippyMap.map((float)Mercator.lonToMercX(tileX2Lon(tileInfo.tileX, tileInfo.zoom)),Mercator.getMinMercX(),Mercator.getMaxMercX(),screenBounds.x,screenBounds.x+screenBounds.width)),
					//use width below because need assume tiles are square - we can always use width
					(float)(SlippyMap.map((float)Mercator.latToMercY(tileY2Lat(tileInfo.tileY, tileInfo.zoom)),Mercator.getMinMercY(),Mercator.getMaxMercY(),screenBounds.y+screenBounds.width,screenBounds.y))
			);
			PVector p2=new PVector(
					(float)(SlippyMap.map((float)Mercator.lonToMercX(tileX2Lon(tileInfo.tileX+1, tileInfo.zoom)),Mercator.getMinMercX(),Mercator.getMaxMercX(),screenBounds.x,screenBounds.x+screenBounds.width)),
					//use width below because need assume tiles are square - we can always use width
					(float)(SlippyMap.map((float)Mercator.latToMercY(tileY2Lat(tileInfo.tileY+1, tileInfo.zoom)),Mercator.getMinMercY(),Mercator.getMaxMercY(),screenBounds.y+screenBounds.width,screenBounds.y))
			);
			if (zoomPanState==null)
				g.image(tileInfo.image,
						p1.x,
						p1.y,
						p2.x-p1.x,
						p2.y-p1.y
						);
			else
				g.image(tileInfo.image,
						zoomPanState.getCoordToDisp(p1).x,
						zoomPanState.getCoordToDisp(p1).y,
						zoomPanState.getCoordToDisp(p2).x-zoomPanState.getCoordToDisp(p1).x,
						zoomPanState.getCoordToDisp(p2).y-zoomPanState.getCoordToDisp(p1).y
						);
//			g.fill(0);
//			g.textAlign(PApplet.LEFT,PApplet.TOP);
//			g.text(tileInfo.tileX+","+tileInfo.tileY+","+tileInfo.zoom,zoomPanState.getCoordToDisp(p1).x,zoomPanState.getCoordToDisp(p1).y);
		}
		
		//Draw copyright statements
		g.pushStyle();
		g.fill(100);
		g.textSize(10);
		g.textAlign(PConstants.LEFT,PConstants.TOP);
		if (osmTileType==OsmTileType.MAPNIK)
			g.text("Data and map information provided by Open Street Map and contributors, CC-BY-SA",(int)screenBounds.x,(int)screenBounds.y);
		else if (osmTileType==OsmTileType.MAPQUEST)
			g.text("Data, imagery and map information provided by MapQuest, Open Street Map and contributors, CC-BY-SA",(int)screenBounds.x,(int)screenBounds.y);
		else if (osmTileType==OsmTileType.CLOUDMADE)
			g.text("Data and map information provided by Open Street Map, Cloudmade and contributors, CC-BY-SA",(int)screenBounds.x,(int)screenBounds.y);
		else if (osmTileType==OsmTileType.WATERCOLOUR)
			g.text("Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA",(int)screenBounds.x,(int)screenBounds.y);
		else if (osmTileType==OsmTileType.TONER)
			g.text("Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA",(int)screenBounds.x,(int)screenBounds.y);
		else if (osmTileType==OsmTileType.TERRAIN)
			g.text("Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under CC BY SA",(int)screenBounds.x,(int)screenBounds.y);
		g.popStyle();
		
	}
	
	/** Sets the API key to use with CloudMade (only required if cloudmade tiles being used) 
	 *  
	 * @param apiKey
	 */
	void setCloudMadeApiKey(String apiKey){
		this.ApiKey=apiKey;
	}
	
	/** Sets the Cloudmade style ID (only needed if cloudmade tiles being used
	 * 
	 * @param cloudMadeStyleId
	 */
	void setCloudMadeStyle(int cloudMadeStyleId){
		this.cloudMadeStyleId=cloudMadeStyleId;
	}


	/** Get a map tile
	 * @param lat
	 * @param lon
	 * @param zoom 
	 * @returns PImage
	 */
	private  PImage getTileImage(int tileX, int tileY, int zoom,boolean onlyGetFromCache,OsmTileType osmTileType) {
		String tileUrl=getTileUrl(osmTileType, tileX, tileY, zoom);
		String tileCacheFilename=getTileCacheFileName(osmTileType, tileX, tileY, zoom);
		if (tileUrl!=null && tileCacheFilename!=null)
			return tileRetriever.getTileImage(tileUrl,tileCacheFilename,onlyGetFromCache);
		else
			return null;
	}
	/** Get x tile number from longitude
	 * 
	 * Modified from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * 
	 * @param lon
	 * @param zoom 
	 * @returns int
	 */
	private static int lon2TileX(double lon, int zoom){
		return (int)(Math.floor((lon+180.0)/360.0*Math.pow(2.0,zoom)));
	}
	/** Get y tile number from longitude
	 * 
	 * Modified from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * 
	 * @param lat
	 * @param zoom 
	 * @returns int
	 */
	private static int lat2TileY(double lat, int zoom){
		return (int)(Math.floor((1.0-Math.log(Math.tan(lat*Math.PI/180.0) + 1.0/Math.cos(lat*Math.PI/180.0))/Math.PI)/2.0 *Math.pow(2.0,zoom)));
	}

	/** Get lon tile x's left side
	 * 
	 * Modified from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * 
	 * @param lat
	 * @param zoom 
	 * @returns float
	 */
	private static double tileX2Lon(int tileX, int zoom){
		return ((tileX / Math.pow(2.0, zoom) * 360.0) - 180.0);
	}

	/** Get lat tile y's top
	 * 
	 * Modified from http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * 
	 * @param lat
	 * @param zoom 
	 * @returns float
	 */
	private static double tileY2Lat(int tileY, int zoom){
		double n = Math.PI - ((2.0 * Math.PI * tileY) / Math.pow(2.0, zoom));
		return (180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
	}


	private String getTileUrl(OsmTileType osmTileType,int tileX, int tileY, int zoom){
		if (tileX<0 || tileX>Math.pow(2,zoom)-1 || tileY<0 || tileY>Math.pow(2,zoom)-1)
			return null;
		else{
			String tileName=zoom+"/"+tileX+"/"+tileY;
			String url=null;
			if (osmTileType==OsmTileType.MAPNIK){
				int r=((int)(Math.random()*4));
				String subdomain="";
				if (r==1)
					subdomain="a.";
				if (r==2)
					subdomain="b.";
				if (r==3)
					subdomain="c.";
				url = "http://"+subdomain+"tile.openstreetmap.org/"+tileName+".png";
			}
			else if (osmTileType==OsmTileType.MAPQUEST){
				int r=((int)(Math.random()*3))+1;
				url = "http://otile"+r+".mqcdn.com/tiles/1.0.0/osm/"+tileName+".jpg";
			}
			else if (osmTileType==OsmTileType.CLOUDMADE){
				int r=((int)(Math.random()*3));
				String subdomain=null;
				if (r==0)
					subdomain="a";
				if (r==1)
					subdomain="b.";
				if (r==2)
					subdomain="c.";
				url = "http://"+subdomain+".tile.cloudmade.com/"+ApiKey+"/"+cloudMadeStyleId+"/256/"+tileName+".png";
			}
			else if (osmTileType==OsmTileType.WATERCOLOUR){
				url = "http://tile.stamen.com/watercolor/"+tileName+".png";
			}
			else if (osmTileType==OsmTileType.TONER){
				url = "http://tile.stamen.com/toner/"+tileName+".png";
			}
			else if (osmTileType==OsmTileType.TERRAIN){
				url = "http://tile.stamen.com/terrain/"+tileName+".png";
			}
			return tileRetriever.applyPassThroughURL(url);
		}
	}
	
	private String getTileCacheFileName(OsmTileType osmTileType,int tileX, int tileY, int zoom){
		return "osm-"+osmTileType.toString().toLowerCase()+"-"+tileX+"-"+tileY+"-"+zoom+".png";
	}
	
	/** private class that stores details of tiles that need to be
	 * drawn. Instances sort themselves by zoom level, so that tiles
	 * of a less coarse resolution are drawn over the top of tiles
	 * with a more coarse resolution
	 * 
	 * @author Aidan Slingsby
	 *
	 */
	private class TileInfo implements Comparable<TileInfo>{
		int tileX;
		int tileY;
		int zoom;
		PImage image;
		
		//Sorts by zoom level
		public int compareTo(TileInfo o) {
			if (o.zoom<this.zoom)
				return 1;
			else
				return -1;
		}
	}
}
