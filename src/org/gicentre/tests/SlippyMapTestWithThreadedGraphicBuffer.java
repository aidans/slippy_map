package org.gicentre.tests;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

import org.gicentre.utils.gui.ThreadedDraw;
import org.gicentre.utils.gui.ThreadedGraphicBuffer;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.LonLatBounds;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapListener;
import org.gicentre.utils.slippymap.SlippyMapType;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;

//****************************************************************************************
/** Simplest demonstration of "slippy maps" in Processing 
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
public class SlippyMapTestWithThreadedGraphicBuffer extends PApplet implements ThreadedDraw,SlippyMapListener{

	SlippyMap slippyMap;
	ZoomPan zoomPan;
	ThreadedGraphicBuffer buffer;
	
	
    /** Creates a simple application to test the SlippyMap classe
     *  Left drag to pan and right drag up/down to zoom or use the mouse wheel
     *  Press 't' to change the map type; press 'r' to reset zoom to GB
     *  @param args Command line arguments (ignored). 
     */
    public static void main(String[] args){   
        PApplet.main(new String[] {"org.gicentre.tests.SlippyMapTestWithThreadedGraphicBuffer"});
    }
	
    /** Sets up the sketch.
     */
	public void setup(){
		size(1400,800);
		smooth();
		
		//Create a ZoomPan instance to handle the zooming and panning
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT); //use the right mouse button to zoom (left to pan)

		//Create an instance of slippymap
		slippyMap=new SlippyMap(this,zoomPan);
		slippyMap.addSlippyMapListener(this);
		slippyMap.setUseFileCache(true);
		
		buffer=new ThreadedGraphicBuffer(this, zoomPan, this);
		buffer.setUpdateDuringZoomPan(true);
	}
	
	/** The draw loop
	 * 
	 */
	public void draw(){
		//white background
		background(255,50);
		buffer.draw(zoomPan.getZoomPanState());
	}
	
	public void keyPressed(){
		if (key=='t'){
			//switch through the various map types
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
			buffer.setUpdateFlag();
		}
	}

	public void threadedDraw(PGraphics canvas, ZoomPanState zoomState, Object arg2) {
		// TODO Auto-generated method stub
		slippyMap.draw(zoomState,canvas);
	}

	public void newTileAvailable() {
		// TODO Auto-generated method stub
		buffer.setUpdateFlag();
	}	
	
}
