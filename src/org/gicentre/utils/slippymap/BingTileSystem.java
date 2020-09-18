package org.gicentre.utils.slippymap;

import java.awt.Point;

import processing.core.PVector;

//****************************************************************************************
/** Provides methods to obtain tile information about Bing map tiles
 * 
 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
*    
* Only intended to be used by SlippyMap - hence Class and all methods have only
* package-wide visibility
*  
* @author Aidan Slingsby, giCentre, City University London
* based on Microsoft code (c) 2006-2009 Microsoft Corporation.  All rights reserved.
// </copyright>
* @version 1.0, August 2011 
*/ 
//*****************************************************************************************

class BingTileSystem {

	static private final double earthRadius = 6378137;
	static private final double minLatitude = -85.05112878;
	static private final double maxLatitude = 85.05112878;
	static private final double minLongitude = -180;
	static private final double maxLongitude = 180;

	/** Clips a number to the specified minimum and maximum values.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param n  The number to clip
	 * @param minValue Minimum allowable value
	 * @param maxValue Maximum allowable value
	 * @return The clipped value
	 */
	static double clip(double n, double minValue, double maxValue){
		return Math.min(Math.max(n, minValue), maxValue);
	}



	/** Determines the map width and height (in pixels) at a specified level
	 *  of detail
	 *  
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param levelOfDetail  Level of detail, from 1 (lowest detail)
	 * @return return (int) 256 << levelOfDetail
	 */
	static int mapSize(int levelOfDetail){
		return (int) 256 << levelOfDetail;
	}



	/** Determines the ground resolution (in meters per pixel) at a specified
	 * latitude and level of detail.
	 * 
 	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param latitude  Latitude (in degrees) at which to measure the ground resolution.
	 * @param levelOfDetail Level of detail, from 1 (lowest detail) to 23 (highest detail).
	 * @return The ground resolution, in meters per pixel
	 */
	static double groundResolution(double latitude, int levelOfDetail){
		latitude = clip(latitude, minLatitude, maxLatitude);
		return Math.cos(latitude * Math.PI / 180) * 2 * Math.PI * earthRadius / mapSize(levelOfDetail);
	}


	/** Determines the map scale at a specified latitude, level of detail,
	/* and screen resolution
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param latitude Latitude (in degrees) at which to measure the map scale.
	 * @param levelOfDetail  Level of detail, from 1 (lowest detail) to 23 (highest detail).
	 * @param screenDpi  Resolution of the screen, in dots per inch.
	 * @return The map scale, expressed as the denominator N of the ratio 1 : N
	 */
	static double mapScale(double latitude, int levelOfDetail, int screenDpi){
		return groundResolution(latitude, levelOfDetail) * screenDpi / 0.0254;
	}


	/**Converts a point from latitude/longitude WGS-84 coordinates (in degrees)
	 * into pixel XY coordinates at a specified level of detail.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param longitude  Latitude of the point, in degrees
	 * @param latitude  Longitude of the point, in degrees
	 * @param levelOfDetail  Level of detail, from 1 (lowest detail) to 23 (highest detail).
	 * @return pixel XY value
	 */
	static Point lonLatToPixelXY(double longitude,double latitude, int levelOfDetail){
		latitude = clip(latitude, minLatitude, maxLatitude);
		longitude = clip(longitude, minLongitude, maxLongitude);

		double x = (longitude + 180) / 360; 
		double sinLatitude = Math.sin(latitude * Math.PI / 180);
		double y = 0.5 - Math.log((1 + sinLatitude) / (1 - sinLatitude)) / (4 * Math.PI);

		int mapSize = mapSize(levelOfDetail);
		int pixelX = (int) clip(x * mapSize + 0.5, 0, mapSize - 1);
		int pixelY = (int) clip(y * mapSize + 0.5, 0, mapSize - 1);
		return new Point(pixelX,pixelY);
	}


	/**Converts a pixel from pixel XY coordinates at a specified level of detail
	/* into latitude/longitude WGS-84 coordinates (in degrees).
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param pixelX  X coordinate of the point, in pixels.
	 * @param pixelY  Y coordinates of the point, in pixels.
	 * @param levelOfDetail  Level of detail, from 1 (lowest detail) to 23 (highest detail).
	 * @return lon/lat
	 */
	static public PVector pixelXYToLatLong(int pixelX, int pixelY, int levelOfDetail)
	{
		double mapSize = mapSize(levelOfDetail);
		double x = (clip(pixelX, 0, mapSize - 1) / mapSize) - 0.5;
		double y = 0.5 - (clip(pixelY, 0, mapSize - 1) / mapSize);

		float latitude = (float)(90 - 360 * Math.atan(Math.exp(-y * 2 * Math.PI)) / Math.PI);
		float longitude = (float)(360 * x);
		return new PVector(longitude,latitude);
	}


	/**Converts pixel XY coordinates into tile XY coordinates of the tile containing
	/* the specified pixel.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param pixelX  Pixel X coordinate
	 * @param pixelY  Pixel Y coordinate
	 * @return Tile x/y
	 */
	static Point pixelXYToTileXY(int pixelX, int pixelY){
		int x= pixelX / 256;
		int y = pixelY / 256;
		return new Point(x,y);
	}



	/**Converts tile XY coordinates into pixel XY coordinates of the upper-left pixel
	/* of the specified tile.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param tileX  Tile X coordinate
	 * @param tileY  Tile Y coordinate
	 * @return xy
	 */
	static Point tileXYToPixelXY(int tileX, int tileY){
		int pixelX = tileX * 256;
		int pixelY = tileY * 256;
		return new Point(pixelX,pixelY);
	}



	/** Converts tile XY coordinates into a QuadKey at a specified level of detail.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param tileX  Tile X coordinate
	 * @param tileY  Tile Y coordinate
	 * @param levelOfDetail  Level of detail, from 1 (lowest detail) to 23 (highest detail).
	 * @return quadkey
	 */
	static String tileXYToQuadKey(int tileX, int tileY, int levelOfDetail){
		StringBuilder quadKey = new StringBuilder();
		for (int i = levelOfDetail; i > 0; i--)
		{
			char digit = '0';
			int mask = 1 << (i - 1);
			if ((tileX & mask) != 0)
			{
				digit++;
			}
			if ((tileY & mask) != 0)
			{
				digit++;
				digit++;
			}
			quadKey.append(digit);
		}
		return quadKey.toString();
	}



	/** Converts a QuadKey into tile XY coordinates.
	 * 
	 * Adapted from: http://msdn.microsoft.com/en-us/library/bb259689.aspx
	 * 
	 * @param quadKey QuadKey of the tile
	 * @return tileX/tileY
	 */
	static Point quadKeyToTileXY(String quadKey){
		int tileX = 0;
		int tileY = 0;
		int levelOfDetail = quadKey.length();
		for (int i = levelOfDetail; i > 0; i--)
		{
			int mask = 1 << (i - 1);
			switch (quadKey.charAt(levelOfDetail - i))
			{
			case '0':
				break;

			case '1':
				tileX |= mask;
				break;

			case '2':
				tileY |= mask;
				break;

			case '3':
				tileX |= mask;
				tileY |= mask;
				break;

			default:
				System.out.println("Invalid quadkey");
			}
		}
		return new Point(tileX,tileY);
	}
}

