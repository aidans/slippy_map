package org.gicentre.utils.slippymap;


//****************************************************************************************
/** Set of classes which convert between lon/lat and Mercator
 * 
 * Only intended to be used by SlippyMap - hence Class and all methods have only
 * package-wide visibility
 *
 *  Adapted from: http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
* 
*  @author Aidan Slingsby, giCentre, City University London, based on http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
*  @version 1.0, August 2010 
*/ 
//*****************************************************************************************


class Mercator {

	/** Converts latitude to mercator Y
	 * 
	 * @param lat
	 * @return y
	 */
	static public double latToMercY(double lat) {
		if (lat<-85){
			lat=-85;
		}
		else if (lat>85){
			lat=85;
		}
		return Math.log(Math.tan(Math.toRadians(lat))+(1/Math.cos(Math.toRadians(lat))));
	}
	
	/** Converts longitude to Mercator X
	 * 
	 * @param lon
	 * @return x
	 */
	static double lonToMercX(double lon) {
		if (lon<-180){
			lon=-180;
		}
		else if (lon>180){
			lon=180;
		}
		return Math.toRadians(lon);
	}    

	/** Converts Mercator Y to latitude
	 * 
	 * @param y
	 * @return latitude
	 */
	static double mercYToLat(double y){
		return Math.toDegrees((Math.atan(Math.sinh(y))));
	}

	/** Converts Merctor X to longitude
	 * 
	 * @param x
	 * @return longitude
	 */
	static double mercXToLon(double x){
		return Math.toDegrees(x);
	}    
	
	/* Min Mercator Y coordinate
	 * 
	 */
	static double getMinMercY(){
		return latToMercY(-85);
	}

	/* Max Mercator Y coordinate
	 * 
	 */
	static public double getMaxMercY(){
		return latToMercY(85);
	}

	/* Min Mercator X coordinate
	 * 
	 */
	static public double getMinMercX(){
		return latToMercY(-180);
	}

	/* Max Mercator Y coordinate
	 * 
	 */
	static public double getMaxMercX(){
		return lonToMercX(180);
	}


}
