package org.gicentre.tests;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.LonLatBounds;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapType;

import processing.core.PApplet;
import processing.core.PVector;

//****************************************************************************************
/** Demonstrates the use "slippy maps" in Processing 
*  @author Aidan Slingsby, giCentre, City University London.
*  @version 1.0, August 2010 
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

@SuppressWarnings("serial")
public class SlippyMapWithPointTest extends PApplet{

	SlippyMap slippyMap;
	ZoomPan zoomPan;
	LonLatBounds initialLonLatArea;
	PVector cityUniversityLocation;
	Rectangle bounds;
	boolean useZoomPanTransform=false;
	
	
    /** Creates a simple application to test the SlippyMap classe
     *  Left drag to pan and right drag up/down to zoom or use the mouse wheel
     *  Press 't' to change the map type; press 'r' to reset zoom to GB
     *  @param args Command line arguments (ignored). 
     */
    public static void main(String[] args){   
        PApplet.main(new String[] {"org.gicentre.tests.SlippyMapWithPointTest"});
    }
	
    /** Sets up the sketch.
     */
	public void setup(){
		size(800,500);
		smooth();
		
		//Create a ZoomPan instance to handle the zooming and panning
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT); //use the right mouse button to zoom (left to pan)
		zoomPan.setMinZoomScale(0.5); //set the minimum zoom scale
		zoomPan.setMaxZoomScale(150000); //set the maximum zoom scale

		//Create an instance of slippymap
		bounds=new Rectangle(10,10,width-20,height-20);
		slippyMap=new SlippyMap(this,zoomPan,bounds);
		
		//Set the initial view to an initial area
		initialLonLatArea=new LonLatBounds(-0.13183631,51.53467,-0.08562325,51.51638);
		slippyMap.zoomTo(initialLonLatArea);
		
		//Set a position to plot later (see org.gicentre.utils.spatial for some
		//coordinate transformations
		cityUniversityLocation=new PVector(-0.102644086f,51.527701f);
		
		slippyMap.setUseFileCache(false); //turn off the file cache for map tiles
	}
	
	/** The draw loop
	 * 
	 */
	public void draw(){
		//white background
		background(255);
		
		slippyMap.startClipping();		//start clipping to map bounds

		//Draw map
		ZoomPanState zoomPanState=zoomPan.getZoomPanState(); //get the current ZoomPanState (which is passed onto SlippyMap)
		slippyMap.draw(zoomPanState);

		String label="City University London";
		
		//THERE ARE TWO WAYS OF DRAWING ONTO THE MAP

		if (!useZoomPanTransform){

			//Method1 - get the screen location directly from the SlippyMap
			//So that the ZoomPan state used to draw this does not change DURING the drawing
			//it is recommended that to retrieve the ZoomPanState at the start and then use
			//this throughout. This is optional.

			
			//Find screen coordinate
			PVector screenLocation=slippyMap.getScreenFromLonLat(cityUniversityLocation,zoomPanState);
			
			//draw translucent rectangle
			noStroke();
			fill(255,180);
			textSize(20);
			textAlign(LEFT,CENTER);
			float textWidth=textWidth(label); //work out width
			rect(screenLocation.x-10,screenLocation.y-10,textWidth+14,22);//draw box

			//draw circle
			fill(0,0,200,150);
			ellipseMode(CENTER);
			ellipse(screenLocation.x,screenLocation.y,10,10);

			 //draw text
			fill(0,0,200,150);
			text(label,screenLocation.x+6,screenLocation.y);
			
			//draw mouse location
			PVector mouseLonLat=slippyMap.getLonLatFromScreenCoord(mouseX, mouseY); //get lon/lat of mouse cursor
			String mouseLocationText="Mouse cursor:\nLat="+mouseLonLat.y+"\nlon="+mouseLonLat.x;
			textSize(12);
			textLeading(12);
			fill(255,200);
			rect((float)bounds.getMinX(),(float)bounds.getMaxY()-42,textWidth(mouseLocationText),42);
			fill(0);
			textAlign(LEFT,BOTTOM);
			text(mouseLocationText,(float)bounds.getMinX(),(float)bounds.getMaxY());

		}
		
		else{
			//METHOD 2 - USE ZOOMPAN.TRANSFORM()
			
			//Find (unzoomed/unpanned) map coordinate
			PVector mapCoord=slippyMap.getMapCoordFromLonLat(cityUniversityLocation);
			
			//push matrix, then tranform
			pushMatrix();
			zoomPan.transform();
			
			//draw circle
			noStroke();
			fill(0,0,200,150);
			ellipseMode(CENTER);
			ellipse(mapCoord.x,mapCoord.y,(float)(10/zoomPanState.getZoomScale()),(float)(10/zoomPanState.getZoomScale()));
			
			//restore matrix stack
			popMatrix();
		}

		//stop clipping
		slippyMap.stopClipping(); 


	}
	
	public void keyPressed(){
		if (key=='t'){
			if (slippyMap.getMapType()==SlippyMapType.BING_ROAD)
				slippyMap.setMapType(SlippyMapType.BING_AERIAL);
			else if (slippyMap.getMapType()==SlippyMapType.BING_AERIAL)
				slippyMap.setMapType(SlippyMapType.BING_AERIAL_WITH_LABELS);
			else if (slippyMap.getMapType()==SlippyMapType.BING_AERIAL_WITH_LABELS)
				slippyMap.setMapType(SlippyMapType.OSM_MAPNIK);
			else if (slippyMap.getMapType()==SlippyMapType.OSM_MAPNIK)
				slippyMap.setMapType(SlippyMapType.OSM_MAPQUEST);
			else if (slippyMap.getMapType()==SlippyMapType.OSM_MAPQUEST)
				slippyMap.setMapType(SlippyMapType.NONE);
			else if (slippyMap.getMapType()==SlippyMapType.NONE)
				slippyMap.setMapType(SlippyMapType.BING_ROAD);
		}
		else if (key=='r')
			slippyMap.zoomTo(initialLonLatArea);
	}
	
	public void mouseMoved(){
		//disable mouse's controlling of zoom/pan when bound cursor
		//is outside the map bounds
		if (bounds.contains(mouseX,mouseY))
			zoomPan.setMouseMask(0); //use mouse for zooming/panning
		else
			zoomPan.setMouseMask(-1);//disable mouse for zooming/panning
	}
	
}
