package org.gicentre.utils.slippymap;

import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.Bing.BingTileType;
import org.gicentre.utils.slippymap.OpenStreetMap.OsmTileType;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PGraphicsJava2D;
import processing.core.PVector;

//****************************************************************************************
/** Displays a zoomable/pannable map using tiles from Bing or OpenStreetMap. It uses the
 * (simple) Spherical Mercator projection and can convert lon/lat (WGS84) to this
 * projection.
 *    
 * To use, create an instance of ZoomPan (to handling the zooming and panning), create
 * an instance of SlippyMap (passing in the ZoomPan instance), and then call its draw() 
 * method in the draw loop. See org.gicentre.tests.SlippyMapTest
 *  
 * @author Aidan Slingsby, giCentre, City University London.
 * @version 1.0, August 2011 
 */ 
//*****************************************************************************************

/* This file is part of giCentre utilities library. gicentre.utils is free software: you can 
* redistribute it and/or modify it under the terms of the GNU Lesser General Public License
* as published by the Free Software Foundation, either version 3 of the License, or (at your
* option) any later version.
* 
* gicentre.utils is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  
* See the GNU Lesser General Public License for more details.
* 
* You should have received a copy of the GNU Lesser General Public License along with this
* source code (see COPYING.LESSER included with this source code). If not, see 
* http://www.gnu.org/licenses/.
*/


public class SlippyMap{

	private PApplet applet;
	private Rectangle screenBounds;
	private ZoomPan zoomPan; //this handles zooming, panning and related transformations
	private Bing bing;
	private OpenStreetMap osm;
	private SlippyMapType slippyMapType=SlippyMapType.OSM_MAPNIK; //default base map style
	private TileRetriever tileRetriever; //Tiles are retrieved in a different thread
	
	private int tilePixelWidth;
	private String bingApiKey;
	

	/** Constructor
	 * 
	 * Displays tiles at 300 pixels in width and uses a in-memory
	 * cache of 200 tiles and uses the whole screen.
	 * 
	 * @param applet  The sketch
	 * @param zoomPan  An instance of ZoomPan, that will handle the zooming and panning
	 */
	public SlippyMap(PApplet applet, ZoomPan zoomPan){
		this(applet,zoomPan,new Rectangle(0,0,applet.width,applet.height),300,50);
	}

	
	/** Constructor
	 * 
	 * By default, displays tiles at 300 pixels in width and uses a in-memory
	 * cache of 200 tiles. Different values can be used by using the other constructor.
	 * 
	 * @param applet  The sketch
	 * @param screenBounds  The screen bounds
	 * @param zoomPan  An instance of ZoomPan, that will handle the zooming 
	 * and panning
	 */
	public SlippyMap(PApplet applet, ZoomPan zoomPan, Rectangle screenBounds){
		this(applet,zoomPan,screenBounds,300,50);
	}

	/** Constructor
	 * 
	 * @param applet  The sketch
	 * @param screenBounds  The screen bounds
	 * @param zoomPan  An instance of ZoomPan, that will handle the zooming 
	 * @param tilePixelWidth  The width at which tiles are displayed. This sets the map scale 
	 * @param maxItemsInCache  The maximum number of tiles that are held in the in-memory cache.
	 */
	public SlippyMap(PApplet applet, ZoomPan zoomPan, Rectangle screenBounds, int tilePixelWidth, int maxItemsInCache){
		this.applet=applet;
		this.screenBounds=screenBounds;
		this.zoomPan=zoomPan;
		this.tilePixelWidth=tilePixelWidth;

		//For tile retrieval in another thread
		tileRetriever=new TileRetriever(applet, maxItemsInCache);
		
	}

	/** If this is to be used as an UNSIGNED APPLET, tile requests need to be routed through
	 * the server which hosts the applet. This is because Java applets are not allowed to
	 * access external resources.
	 * 
	 * Create a php script on your domain that gets takes a URL as a GET argument called "url"
	 * and returns the contents of the external resource. This will be appended with
	 * "?url=http://TILE_URL/tile.png
	 * 
	 * An example of how this file could look is:
	 * 
	 * <?php
	 *     readfile(urldecode($_GET['url']));
	 * ?>
	 * 
	 * NOTE: many PHP servers have the same restriction! If this is the case and it cannot be
	 * changed, you cannot use this in an unsigned applet.
	 * @param passthroughUrl  URL on local domain that will take the tile URL and return the image tile 
	 */
	public void useInUnsignedApplet(String passthroughUrl){
		tileRetriever.setPassThroughURL(passthroughUrl);
		setUseFileCache(false);
	}
	
	/** Returns the lon/lat rectangle of the current viewport, based on zoom/pan.
	 * 
	 * @return  The lon/lat bounds of the current viewport
	 */
	public LonLatBounds getLonLatViewPort(){
		return getLonLatViewPort(zoomPan.getZoomPanState());
	}
	
	/** Sets the Bing API key. You need a Bing API key to use Bing maps.
	 * See:
	 *    http://msdn.microsoft.com/en-us/library/ff428642
	 * @param bingApiKey
	 */
 	public void setBingApiKey(String bingApiKey){
 		this.bingApiKey=bingApiKey;
 	}
	
	
	/** Returns the lon/lat rectangle of the current viewport, based on specified ZoomPanState (usually
	 * that at the start of the sketch's draw loop
	 * 
	 * @return  The lon/lat bounds of the current viewport
	 */
	public LonLatBounds getLonLatViewPort(ZoomPanState zoomPanState){
		//Find the coordinates of the top left and bottom right corners
		PVector topLeft=zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMinX(),(float)screenBounds.getMinY()));
		PVector bottomRight=zoomPanState.getDispToCoord(new PVector((float)screenBounds.getMaxX(),(float)screenBounds.getMaxY()));
		topLeft=getLonLatFromMapCoord(topLeft.x, topLeft.y);
		bottomRight=getLonLatFromMapCoord(bottomRight.x, bottomRight.y);
		LonLatBounds coordBounds=new LonLatBounds(topLeft.x,bottomRight.y,bottomRight.x,topLeft.y);
		return coordBounds;
	}
	
	/** Sets whether the tiles should be cached on disk or not
	 * 
	 * @param useFileCache
	 */
	public void setUseFileCache(boolean useFileCache){
		tileRetriever.setUseFileCache(useFileCache);
	}
	
	/** Start clipping all drawn content to the screen bounds of this map
	 * 
	 */
	public void startClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(screenBounds);
		else
			System.err.println("Cannot clip with this renderer.");
	}

	/** Stop clipping drawn content
	 * 
	 */
	public void stopClipping(){
		if (applet.g instanceof PGraphicsJava2D)
			((PGraphicsJava2D)applet.g).g2.setClip(null);
	}

	/** Draws the map
	 * 
	 */
	public void draw(){
		this.draw(zoomPan.getZoomPanState(),applet.g);
	}

	/** Draws the map using a particular zoompan state, usually that of ZoomPan at the beginning
	 * of the sketch's draw loop.
	 * 
	 */
	public void draw(ZoomPanState zoomPanState){
		this.draw(zoomPanState,applet.g);
	}
	
	/** Draws the map using a particular zoompan state, usually that of ZoomPan at the beginning
	 * of the sketch's draw loop.
	 * 
	 */
	public void draw(ZoomPanState zoomPanState,PGraphics canvas){
		
		if (slippyMapType.toString().startsWith("BING_") && bing==null){
			if (this.bingApiKey==null){
				System.err.println("Must call setBingApiKey() before using Bing maps");
				return;
			}
			
			else {
				bing=new Bing(applet,tilePixelWidth,tileRetriever,bingApiKey);//can replace with osm
			}
		}
		if (slippyMapType.toString().startsWith("OSM_") && osm==null)
			osm=new OpenStreetMap(applet,tilePixelWidth,tileRetriever);//can replace with osm

		
		startClipping(); //start clipping
		//draw the correct style of map
		if (slippyMapType==SlippyMapType.BING_AERIAL)
			bing.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,BingTileType.AERIAL);
		else if (slippyMapType==SlippyMapType.BING_ROAD)
			bing.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,BingTileType.ROAD);
		else if (slippyMapType==SlippyMapType.BING_AERIAL_WITH_LABELS)
			bing.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,BingTileType.AERIAL_WITH_LABELS);
		else if (slippyMapType==SlippyMapType.OSM_MAPNIK)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.MAPNIK);
		else if (slippyMapType==SlippyMapType.OSM_MAPQUEST)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.MAPQUEST);
		else if (slippyMapType==SlippyMapType.OSM_CLOUDMADE)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.CLOUDMADE);
		else if (slippyMapType==SlippyMapType.STAMEN_WATERCOLOUR)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.WATERCOLOUR);
		else if (slippyMapType==SlippyMapType.STAMEN_TONER)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.TONER);
		else if (slippyMapType==SlippyMapType.STAMEN_TERRAIN)
			osm.drawMap(this.getLonLatViewPort(),screenBounds,zoomPanState,canvas,OsmTileType.TERRAIN);
		stopClipping(); //stop clipping
	}

	

	/** Convert longitude and latitude to unzoomed/unpanned map coordinates (Mercator)
	 * 
	 * @param lon  Longitude
	 * @param lat  Latitude
	 * @return  Map coordinate
	 */
	Point2D getMapCoordFromLonLat(float lon,float lat){
		double x=map(Mercator.lonToMercX(lon),Mercator.getMinMercX(),Mercator.getMaxMercX(),screenBounds.x,screenBounds.x+screenBounds.width);
		//use width below because need assume tiles are square - we can always use width
		double y=map(Mercator.latToMercY(lat),Mercator.getMinMercY(),Mercator.getMaxMercY(),screenBounds.y+screenBounds.width,screenBounds.y);
		return new Point2D.Double(x,y);
	}

	/** Convert longitude and latitude to unzoomed/unpanned map coordinates (Mercator)
	 * 
	 * @param lonLat
	 * @return  Map coordinate
	 */
	Point2D getMapCoordFromLonLat(PVector lonLat){
		return getMapCoordFromLonLat(lonLat.x,lonLat.y);
	}

	
	/** Convert longitude and latitude to screen coordinates
	 * 
	 * @param lon  Longitude
	 * @param lat  Latitude
	 * @return Screen coordinate
	 */
	public PVector getScreenFromLonLat(float lon,float lat){
		return getScreenFromLonLat(new PVector(lon,lat),zoomPan.getZoomPanState());
	}

	/** Convert longitude and latitude to screen coordinates
	 * 
	 * @param lonLat Longitude/latitude
	 * @return Screen coordinate
	 */	
	public PVector getScreenFromLonLat(PVector lonLat){
		return getScreenFromLonLat(lonLat.x,lonLat.y);
	}

	/** Convert longitude and latitude to screen coordinates using a particular ZoomPanState - 
	 * usually that of ZoomPan at the start of the sketch's draw loop
	 * 
	 * @param lon
	 * @param lat
	 * @return Screen coordinate
	 */	
	public PVector getScreenFromLonLat(float lon,float lat,ZoomPanState zoomPanState){
		Point2D pCoord = getMapCoordFromLonLat(lon,lat);
		return zoomPanState.getCoordToDisp(new PVector((float)pCoord.getX(),(float)pCoord.getY()));
	}

	
	/** Convert longitude and latitude to screen coordinates using a particular ZoomPanState - 
	 * usually that of ZoomPan at the start of the sketch's draw loop
	 * 
	 * @param lonLat Longitude/latitude
	 * @return Screen coordinate
	 */	
	public PVector getScreenFromLonLat(PVector lonLat,ZoomPanState zoomPanState){
		return getScreenFromLonLat(lonLat.x,lonLat.y,zoomPanState);
	}

	
	/** Convert unzoomed/unpanned map coordinate (Mercator) to longitude
	 * and latitude
	 * 
	 * @param x x map coordinate
	 * @param y y map coordinate
	 * @return lon/lat pair
	 */
	PVector getLonLatFromMapCoord(float x,float y){
		return getMapLonLatFromCoord(new PVector(x,y));
	}

	/** Convert unzoomed/unpanned map coordinate (Mercator) to longitude
	 * and latitude
	 * 
	 * @param mapCoord  Map coordinate as an x,y pair
	 * @return lon/lat pair
	 */
	PVector getMapLonLatFromCoord(PVector mapCoord){
		double lon=Mercator.mercXToLon(map(mapCoord.x,screenBounds.x,screenBounds.x+screenBounds.width,Mercator.getMinMercX(),Mercator.getMaxMercX()));
		//use width below because need assume tiles are square - we can always use width
		double lat=Mercator.mercYToLat(map(mapCoord.y,screenBounds.y+screenBounds.width,screenBounds.y,Mercator.getMinMercY(),Mercator.getMaxMercY()));
		return new PVector((float)lon,(float)lat);
	}


	/** Convert screen coordinate to lon/lat
	 * 
	 * @param x Screen x coordinate
	 * @param y Screen y coordinate
	 * @return lon/lat pair
	 */
	public PVector getLonLatFromScreenCoord(float x,float y){
		return getLonLatFromScreenCoord(new PVector(x,y));
	}

	/** Convert screen coordinate to lon/lat
	 * 
	 * @param screenCoord Screen xy coordinate
	 * @return lon/lat pair
	 */
	public PVector getLonLatFromScreenCoord(PVector screenCoord){
		PVector p1 = zoomPan.getDispToCoord(screenCoord);
		float lon=(float)Mercator.mercXToLon(map(p1.x,screenBounds.x,screenBounds.x+screenBounds.width,Mercator.getMinMercX(),Mercator.getMaxMercX()));
		//use width below because need assume tiles are square - we can always use width
		float lat=(float)Mercator.mercYToLat(map(p1.y,screenBounds.y+screenBounds.width,screenBounds.y,Mercator.getMinMercY(),Mercator.getMaxMercY()));
		return new PVector(lon,lat);
	}
	
	/** Convert screen coordinate to lon/lat
	 * 
	 * @param x Screen x coordinate
	 * @param y Screen y coordinate
	 * @return lon/lat pair
	 */
	public PVector getLonLatFromScreenCoord(float x,float y,ZoomPanState zoomPanState){
		return getLonLatFromScreenCoord(new PVector(x,y),zoomPanState);
	}

	/** Convert screen coordinate to lon/lat
	 * 
	 * @param screenCoord Screen xy coordinate
	 * @return lon/lat pair
	 */
	public PVector getLonLatFromScreenCoord(PVector screenCoord, ZoomPanState zoomPanState){
		PVector p1 = zoomPanState.getDispToCoord(screenCoord);
		float lon=(float)Mercator.mercXToLon(map(p1.x,screenBounds.x,screenBounds.x+screenBounds.width,Mercator.getMinMercX(),Mercator.getMaxMercX()));
		//use width below because need assume tiles are square - we can always use width
		float lat=(float)Mercator.mercYToLat(map(p1.y,screenBounds.y+screenBounds.width,screenBounds.y,Mercator.getMinMercY(),Mercator.getMaxMercY()));
		return new PVector(lon,lat);
	}

	/** Set the map type (set of tiles)
	 * 
	 * @param slippyMapType  Map type
	 */
	public void setMapType(SlippyMapType slippyMapType){
		this.slippyMapType=slippyMapType;
	}

	/** Get the current map type
	 * 
	 * @return Map type
	 */
	public SlippyMapType getMapType(){
		return slippyMapType;
	}

	/** Sets the tile pixel width (resolution of base map)
	 * 
	 * @param tilePixelWidth
	 */
	public void setTilePixelWidth(int tilePixelWidth){
		if (tilePixelWidth>50){
			this.tilePixelWidth=tilePixelWidth;
			if (osm!=null)
				osm.tilePixelWidth=tilePixelWidth;
			if (bing!=null)
				bing.tilePixelWidth=tilePixelWidth;
		}
	}

	/** Gets the tile pixel width (resolution of base map)
	 * 
	 * @return
	 */
	public int getTilePixelWidth(){
		return this.tilePixelWidth;
	}

	
	/** If you use the CloudMade tileset, you need to obtain an API key
	 * from http://developers.cloudmade.com/projects 
	 * 
	 * @param apiKey Your API key
	 */
	public void setCloudMadeApiKey(String apiKey){
		osm.setCloudMadeApiKey(apiKey);
	}
	
	/** If you use the CloudMade tileset, set the style ID here. See gallery
	 * of styles here: http://maps.cloudmade.com/editor
	 * 
	 * @param cloudMadeStyleId
	 */
	public void setCloudMadeStyleId(int cloudMadeStyleId){
		osm.setCloudMadeStyle(cloudMadeStyleId);
	}

	/** Zooms to the a lon/lat bounding box
	 * The zoom level is derived from the bounding boxes extent
	 * @param latlonBounds  Bounding box
	 */
	public void zoomTo(Rectangle2D latlonBounds){
		zoomTo(latlonBounds,Double.NaN);
	}

	private void zoomTo(Rectangle2D latlonBounds,double zoomScale){
		PVector pt1=new PVector(screenBounds.x,screenBounds.y);
		PVector pt2=new PVector(screenBounds.x+screenBounds.width,screenBounds.y+screenBounds.height);

		Point2D pt3=getMapCoordFromLonLat((float)latlonBounds.getMinX(), (float)latlonBounds.getMinY());
		Point2D pt4=getMapCoordFromLonLat((float)latlonBounds.getMaxX(), (float)latlonBounds.getMaxY());

		if (Double.isNaN(zoomScale)){
			double zoomScaleW=(pt2.x-pt1.x)/((double)pt4.getX()-pt3.getX());
			double zoomScaleH=(pt1.y-pt2.y)/((double)pt4.getY()-pt3.getY());
			zoomScale=Math.min(zoomScaleW,zoomScaleH);
		}
		
		zoomPan.setPanOffset(0,0);
		zoomPan.setZoomScale(zoomScale);

		PVector pt5 = zoomPan.getCoordToDisp(pt1);
		PVector pt6 = zoomPan.getCoordToDisp(pt2);

		Point2D pt7=new Point2D.Double(map(pt3.getX(),pt1.x,pt2.x,pt5.x,pt6.x),map(pt4.getY(),pt1.y,pt2.y,pt5.y,pt6.y));
		Point2D pt8=new Point2D.Double(map(pt4.getX(),pt1.x,pt2.x,pt5.x,pt6.x),map(pt3.getY(),pt1.y,pt2.y,pt5.y,pt6.y));
		
		zoomPan.setPanOffset((float)(screenBounds.x-((pt7.getX()+pt8.getX())/2)+screenBounds.width/2),(float)(screenBounds.y-((pt8.getY()+pt7.getY())/2)+screenBounds.height/2));
		
		zoomPan.setZoomScale(zoomPan.getZoomScale());
		zoomPan.setPanOffset(zoomPan.getPanOffset().x,zoomPan.getPanOffset().y);

	}

	/** Centres the lon/lat at the specified zoom scale
	 * 
	 * @param lonLat
	 * @param zoomScale  ZoomPan's zoomScale
	 */
	public void panTo(PVector lonLat,double zoomScale){
		//Make a bounding box with zero height
		panTo(lonLat.x,lonLat.y, zoomScale);
	}

	/** Centres the lon/lat at the specified zoom scale
	 * 
	 * @param lon Longitude
	 * @param lat Latitude
	 * @param zoomScale  ZoomPan's zoomScale
	 */
	public void panTo(double lon,double lat,double zoomScale){
		//Make a bounding box with zero height
		zoomTo(new LonLatBounds(lon,lat), zoomScale);
	}

	
	/** Adds a listener. This will be informed when a new tile is available
	 * 
	 * @param slippyMapListener
	 */
	public void addSlippyMapListener(SlippyMapListener slippyMapListener){
		tileRetriever.addSlippyMapListener(slippyMapListener);
	}

	/** Removes a listener
	 * 
	 * @param slippyMapListener
	 */
	public void removeSlippyMapListener(SlippyMapListener slippyMapListener){
		tileRetriever.removeSlippyMapListener(slippyMapListener);
	}

	/**Returns the approximate distance between two lpn/lats in metres
	 * 
	 * Adapted from http://bluemm.blogspot.com/2007/01/excel-formula-to-calculate-distance.html
	 * 
	 * @param lon1
	 * @param lat1
	 * @param lon2
	 * @param lat2
	 * @return Distance in metres
	 */
	static public float distanceInMetres(float lon1,float lat1,float lon2,float lat2){
 		return (float)Math.acos(Math.cos(Math.toRadians(90-lat1))*Math.cos(Math.toRadians(90-lat2)) +Math.sin(Math.toRadians(90-lat1))*Math.sin(Math.toRadians(90-lat2))*Math.cos(Math.toRadians(lon1-lon2)))*6371*1000;//x1000 to get metres
	}

	static public final double map(double value, double istart, double istop, double ostart, double ostop) {
		return ostart + (ostop - ostart) * ((value - istart) / (istop - istart));
	}
	
}
