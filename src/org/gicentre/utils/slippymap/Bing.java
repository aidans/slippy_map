package org.gicentre.utils.slippymap;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.OpenStreetMap.OsmTileType;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

//****************************************************************************************
/** Indentifies the required Bing map tiles
 *    
 * Only intended to be used by SlippyMap - hence Class and all methods have only
 * package-wide visibility
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
class Bing extends BaseMapProvider{

	private String culture="en-GB"; //map culture/language
	private Map<BingTileType, String> metadataURLs; //URLs of the XML files that give the location of the map tiles
	private String bingApiKey;

	//Bing map styles
	static public enum BingTileType{
		AERIAL,
		ROAD,
		AERIAL_WITH_LABELS
	}

	private Comparator<String> quadKeyLengthComparator; //Comparator that ensures less detailed tiles are drawn first
	private PImage providerLogo=null; //bing logo


	private Map<BingTileType, String> baseUrls;	//urls of map tiles
	private Map<BingTileType, List<String>> subdomains;	//list of all the subdomains that can be used

	

	/** Constructor
	 * 
	 * @param applet  The sketch 
	 * @param tilePixelWidth  The the pixel width of tiles
	 * @param tileRetriever  Instance of the class that retrieves the tiles
	 * @param tileCache  In-memory cache to use
	 */
	public Bing(PApplet applet,int tilePixelWidth,TileRetriever tileRetriever, String bingApiKey){
		super(applet,tilePixelWidth,tileRetriever);
		this.bingApiKey=bingApiKey;

		//Sets up metadata information:the XML files that provide details of the Bing tiles
		//Bing API is here: http://msdn.microsoft.com/en-us/library/ff701716.aspx
		metadataURLs=new HashMap<Bing.BingTileType, String>();
		metadataURLs.put(BingTileType.AERIAL,"http://dev.virtualearth.net/REST/v1/Imagery/Metadata/Aerial?mapVersion=v1&o=xml&incl=ImageryProviders&key="+bingApiKey);	
		metadataURLs.put(BingTileType.ROAD,"http://dev.virtualearth.net/REST/v1/Imagery/Metadata/Road?mapVersion=v1&o=xml&incl=ImageryProviders&key="+bingApiKey);	
		metadataURLs.put(BingTileType.AERIAL_WITH_LABELS,"http://dev.virtualearth.net/REST/v1/Imagery/Metadata/AerialWithLabels?mapVersion=v1&o=xml&incl=ImageryProviders&key="+bingApiKey);	

		
		//Create an instance of a Comparator that sorted quadkeys by their length (which relates to map resolution) 
		quadKeyLengthComparator=new Comparator<String>(){
			public int compare(String o1,String o2){
				if (o1.length()<o2.length())
					return -1;
				else if (o1.length()>o2.length())
					return 1;
				else
					return 0;
			}
		};
	}

	/** Sets the culture/language of the map tile
	 * 
	 * See http://msdn.microsoft.com/en-us/library/ff701709.aspx 
	 * 
	 * @param culture
	 */
	void setCulture(String culture){
		this.culture=culture;
	}


	/** Draws the map
	 * 
	 * @param latLonBounds  Viewport
	 * @param screenBounds Screen area
	 * @param zoomPan ZoomPan (for zoom/pan information)
	 * @param g  The canvas to draw on (use PApplet.g for on-screen drawing)
	 * @param bingTileType  The tile type
	 */
	void drawMap(Rectangle2D latLonBounds,Rectangle screenBounds, ZoomPanState zoomPanState, PGraphics g,BingTileType bingTileType){ 

		//find appropriate zoom
		int correctNumTilesAcross=(int)(screenBounds.width/(float)tilePixelWidth);
		Point pixelXY=BingTileSystem.lonLatToPixelXY(latLonBounds.getMinX(),latLonBounds.getMinY(),20);
		Point tileXY=BingTileSystem.pixelXYToTileXY(pixelXY.x, pixelXY.y);
		int firstTileX = tileXY.x; 
		pixelXY=BingTileSystem.lonLatToPixelXY(latLonBounds.getMaxX(),latLonBounds.getMaxY(),20);
		tileXY=BingTileSystem.pixelXYToTileXY(pixelXY.x, pixelXY.y);
		int lastTileX = tileXY.x; 
		int numTilesAcross=lastTileX-firstTileX;
		int zoom=20;
		while (numTilesAcross>correctNumTilesAcross && zoom>1){
			zoom--;
			numTilesAcross/=2;
		}

		//Make list to hold the tiles to draw
		ArrayList<String> tilesToDraw=new ArrayList<String>();

		//Identify which tiles need to be drawn
		pixelXY=BingTileSystem.lonLatToPixelXY(latLonBounds.getMinX(),latLonBounds.getMinY(),zoom);
		tileXY=BingTileSystem.pixelXYToTileXY(pixelXY.x, pixelXY.y);
		firstTileX = tileXY.x; 
		int lastTileY = tileXY.y;
		pixelXY=BingTileSystem.lonLatToPixelXY(latLonBounds.getMaxX(),latLonBounds.getMaxY(),zoom);
		tileXY=BingTileSystem.pixelXYToTileXY(pixelXY.x, pixelXY.y);
		lastTileX = tileXY.x; 
		int firstTileY = tileXY.y;
		for (int tileX=firstTileX;tileX<=lastTileX;tileX++){
			for (int tileY=firstTileY;tileY<=lastTileY;tileY++){
				
				if (Thread.currentThread().isInterrupted())
					return;

				//find quadkey
				String quadKey=BingTileSystem.tileXYToQuadKey(tileX,tileY,zoom);
				PImage im = getTileImage(bingTileType, quadKey,false);
				if (im!=null){
					// the tile exists NOW, add to the list of tiles to draw 
					tilesToDraw.add(quadKey);
				}
				else{
					//Look for lower resolution tiles that exist
					for (int quadKeyLength=quadKey.length()-1;quadKeyLength>0;quadKeyLength--){
						String lowerResQuadKey=quadKey.substring(0,quadKeyLength);
						im = getTileImage(bingTileType,lowerResQuadKey,true);
						if (im != null){
							if (!tilesToDraw.contains(lowerResQuadKey))
								//if we find one that's not already scheduled for drawing, add
								tilesToDraw.add(lowerResQuadKey);
							break;
						}
					}
				}
			}
		}

		//Sort the tiles by length of key (i.e. resolution) so that the lowest res ones
		//are drawn first
		Collections.sort(tilesToDraw,quadKeyLengthComparator);

		//Draw tiles
		for (String quadKey:tilesToDraw){
			PImage im = getTileImage(bingTileType,quadKey,false);
			if (im!=null){
				//identify where to draw the tile
				zoom=quadKey.length();
				Point tile = BingTileSystem.quadKeyToTileXY(quadKey);
				int tileX=tile.x;
				int tileY=tile.y;
				PVector p1=new PVector(
						(float)(SlippyMap.map((float)Mercator.lonToMercX(tileX2Lon(tileX, zoom)),Mercator.getMinMercX(),Mercator.getMaxMercX(),screenBounds.x,screenBounds.x+screenBounds.width)),
						//use width below because need assume tiles are square - we can always use width
						(float)(SlippyMap.map((float)Mercator.latToMercY(tileY2Lat(tileY, zoom)),Mercator.getMinMercY(),Mercator.getMaxMercY(),screenBounds.y+screenBounds.width,screenBounds.y))
						);
				PVector p2=new PVector(
						(float)(SlippyMap.map((float)Mercator.lonToMercX(tileX2Lon(tileX+1, zoom)),Mercator.getMinMercX(),Mercator.getMaxMercX(),screenBounds.x,screenBounds.x+screenBounds.width)),
						//use width below because need assume tiles are square - we can always use width
						(float)(SlippyMap.map((float)Mercator.latToMercY(tileY2Lat(tileY+1, zoom)),Mercator.getMinMercY(),Mercator.getMaxMercY(),screenBounds.y+screenBounds.width,screenBounds.y))
						);
				//draw it
				g.image(im,
						zoomPanState.getCoordToDisp(p1).x,
						zoomPanState.getCoordToDisp(p1).y,
						zoomPanState.getCoordToDisp(p2).x-zoomPanState.getCoordToDisp(p1).x,
						zoomPanState.getCoordToDisp(p2).y-zoomPanState.getCoordToDisp(p1).y
						);
			}
			
			if (Thread.currentThread().isInterrupted())
				return;
		}
		//Draw the Bing image
		if (providerLogo!=null){
			g.image(providerLogo, (int)screenBounds.getMaxX()-providerLogo.width, (int)screenBounds.getMaxY()-providerLogo.height);
			g.fill(0);
			g.textSize(10);
			g.textAlign(PConstants.RIGHT,PConstants.BOTTOM);
			g.text("Maps copyright \u00a9 2011 Microsoft and its suppliers. All rights reserved.",screenBounds.x+screenBounds.width,screenBounds.y+screenBounds.height);
		}
	}


	/** Get a map tile as an image
	 * 
	 * @param lat
	 * @param lon
	 * @param zoom 
	 * @returns PImage
	 */
	PImage getTileImage(BingTileType bingTileType,String quadKey, boolean onlyGetFromCache) {
		String tileUrl=getTileURL(bingTileType, quadKey);
		String cacheFilename=getTileCacheFileName(bingTileType,quadKey);
		if (tileUrl!=null && cacheFilename!=null){
			PImage image=tileRetriever.getTileImage(tileUrl,cacheFilename,onlyGetFromCache);
			return image;
		}
		else{
			return null;
		}
	}

	
	/** Obtain the tile server information from Bing using their API documented
	 * here: http://msdn.microsoft.com/en-us/library/ff701716.aspx
	 */
	void retrieveBaseUrl(){

		//Set up libraries to read and parse XML
		DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder=null;
		Document doc=null;
		baseUrls=new HashMap<Bing.BingTileType, String>();
		subdomains=new HashMap<Bing.BingTileType, List<String>>();
		for (Entry<BingTileType, String> entry:metadataURLs.entrySet()){

			try {
				docBuilder = docBuilderFactory.newDocumentBuilder();
				String urlString=entry.getValue();
				//append to the passthrough URL if required
				urlString=tileRetriever.applyPassThroughURL(urlString);
				URL url=new URL(urlString);
				doc = docBuilder.parse(url.openStream());
			} catch (ParserConfigurationException e) {
				baseUrls=null;
			} catch (SAXException e) {
				baseUrls=null;
			} catch (IOException e) {
				baseUrls=null;
			}
			
			if (doc!=null){

				doc.getDocumentElement().normalize();

				//Get the Bing maps logo
				NodeList list = doc.getElementsByTagName("BrandLogoUri");
				Node node = list.item(0);
				String logoUrl=node.getTextContent();
				tileRetriever.applyPassThroughURL(logoUrl);
				providerLogo=applet.loadImage(logoUrl);

				//get the tile server url
				list = doc.getElementsByTagName("ImageUrl");
				node = list.item(0);
				baseUrls.put(entry.getKey(),node.getTextContent());

				//get the available subdomains
				List<String> theseSubdomains=new ArrayList<String>();
				list = doc.getElementsByTagName("ImageUrlSubdomains");
				node = list.item(0);
				list=node.getChildNodes();
				for (int i=0;i<list.getLength();i++)
					theseSubdomains.add(list.item(i).getTextContent());
				subdomains.put(entry.getKey(), theseSubdomains);
			}
			else{
				//defaults
				baseUrls=new HashMap<Bing.BingTileType, String>();
				baseUrls.put(BingTileType.ROAD,"http://ecn.{subdomain}.tiles.virtualearth.net/tiles/r{quadkey}.jpeg?g=950&mkt={culture}&shading=hill&stl=H");
				baseUrls.put(BingTileType.AERIAL_WITH_LABELS,"http://ecn.{subdomain}.tiles.virtualearth.net/tiles/h{quadkey}.jpeg?g=950&mkt={culture}&stl=H");
				baseUrls.put(BingTileType.AERIAL,"http://ecn.{subdomain}.tiles.virtualearth.net/tiles/a{quadkey}.jpeg?g=950");
				subdomains=new HashMap<Bing.BingTileType, List<String>>();
				ArrayList<String> domains=new ArrayList<String>();
				domains.add("t0");
				subdomains.put(BingTileType.AERIAL, domains);
				subdomains.put(BingTileType.AERIAL_WITH_LABELS, domains);
				subdomains.put(BingTileType.ROAD, domains);
			}
		}
	}

	private String getTileCacheFileName(BingTileType bingTileType,String quadKey){
		return "bing-"+bingTileType.toString().toLowerCase()+"-"+quadKey+".jpeg";
	}
	
	private String getTileURL(BingTileType bingTileType,String quadKey){
		if (baseUrls==null){
			retrieveBaseUrl();
		}

		if (baseUrls!=null){
			//Choose a random subdomain
			String subdomain=subdomains.get(bingTileType).get((int)(Math.random()*subdomains.size())+1);

			//Substitute the required parameters into the base URL
			String url=baseUrls.get(bingTileType).replace("{subdomain}",subdomain).replace("{quadkey}",quadKey).replace("{culture}",culture);

			//Set an an parameter of a passThroughURL if this has been identified
			url+="&ext=.jpeg";//processing needs the image extension to recognise the image type

			return tileRetriever.applyPassThroughURL(url);
		}
		return null;
	}
	
	/** Get the longitude of the tile's left side
	 * 
	 * Adapted from: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * @param lat
	 * @param zoom 
	 * @returns float
	 */
	private static double tileX2Lon(int tileX, int zoom){
		return ((tileX / Math.pow(2.0, zoom) * 360.0) - 180.0);
	}

	/** Get latitude of the tile's top
	 * 
 	 * Adapted from: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
	 * @param lat
	 * @param zoom 
	 * @returns float
	 */
	private static double tileY2Lat(int tileY, int zoom){
		double n = Math.PI - ((2.0 * Math.PI * tileY) / Math.pow(2.0, zoom));
		return (180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n))));
	}

}
