

import java.awt.Rectangle;

import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapType;

import processing.core.PApplet;

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
public class SlippyMapTest extends PApplet{

	SlippyMap slippyMap;
	ZoomPan zoomPan;
	
	
    /** Creates a simple application to test the SlippyMap classe
     *  Left drag to pan and right drag up/down to zoom or use the mouse wheel
     *  Press 't' to change the map type; press 'r' to reset zoom to GB
     *  @param args Command line arguments (ignored). 
     */
    public static void main(String[] args){   
        PApplet.main(new String[] {"org.gicentre.tests.SlippyMapTest"});
    }
	
    /** Sets up the sketch.
     */
	public void setup(){
		size(1400,900);
		smooth();
		
		//Create a ZoomPan instance to handle the zooming and panning
		zoomPan=new ZoomPan(this);
		zoomPan.setZoomMouseButton(RIGHT); //use the right mouse button to zoom (left to pan)

		//Create an instance of slippymap
		slippyMap=new SlippyMap(this,zoomPan,new Rectangle(0,0,width,height),300,1000);
		slippyMap.setBingApiKey("AoVLv4X3PU31TbprsfobnWkIS5xiyg-_txBKYQ9EpWdRubeu_iSjBzU3PMa-q0o2");
		
	}
	
	/** The draw loop
	 * 
	 */
	public void draw(){
		//white background
		background(255);
		slippyMap.draw();
//		pushStyle();
//		tint(255,100);
//		rotate(r);
//		image(image,0,0,image.width*1.5f,image.height*r);
//		popStyle();
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
				slippyMap.setMapType(SlippyMapType.BING_ROAD);
			println(slippyMap.getMapType());
		}
		
	}	
	
}