package org.gicentre.utils.slippymap;

import processing.core.PApplet;
import processing.core.PImage;

//****************************************************************************************
/** Base class for base map providers.
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

abstract class BaseMapProvider {
	
	PApplet applet;
	TileRetriever tileRetriever;
	int tilePixelWidth;  //The pixel (in pixels) of tiles (affects the map resolution)

	
	/** Constructor
	 * 
	 * @param applet The sketch
	 * @param tilePixelWidth The pixel (in pixels) of tiles (affects the map resolution)
	 * @param tileRetriever Instance of a class which retrieves the urls in a different thread
	 * @param tileCache  In-memory cache
	 */
	BaseMapProvider(PApplet applet,int tilePixelWidth,TileRetriever tileRetriever){
		this.applet=applet;
		this.tilePixelWidth=tilePixelWidth;
		this.tileRetriever=tileRetriever;
	}

}
