package org.gicentre.tests;

import java.awt.Rectangle;

import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.slippymap.LonLatBounds;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapType;

import processing.core.PApplet;
import processing.core.PVector;

public class MultipleSlippyMaps extends PApplet{

	// Simple sketch to demonstrate the use of the slippy map.
	// Plots a blue dot on the map
	// Version 1.0; 06 July, 2012.
	// Author Aidan Slingsby, giCentre.org

	ZoomPan zoomPan1, zoomPan2;                //To handle zooming and panning, one for each independently zoomed window
	SlippyMap slippyMap1, slippyMap2;
	Rectangle mapBounds1,mapBounds2;           //screen bounds of each hmap
	LonLatBounds initialLonLatArea;
	PVector cityLoc;

	public void setup() {
	  size(1000, 500);
	  smooth();

	  zoomPan1=new ZoomPan(this);               //To handle zooming and panning
	  zoomPan1.setZoomMouseButton(RIGHT);       //Sets right mouse button up/down drag to zoom (left mouse button to pan)
	  mapBounds1=new Rectangle(0,0,width/2,height);//Place to draw map1
	  slippyMap1=new SlippyMap(this, zoomPan1,mapBounds1);
	  slippyMap1.setMapType(SlippyMapType.BING_ROAD);
	  slippyMap1.setBingApiKey("AoVLv4X3PU31TbprsfobnWkIS5xiyg-_txBKYQ9EpWdRubeu_iSjBzU3PMa-q0o2");

	  zoomPan2=new ZoomPan(this);               //To handle zooming and panning
	  zoomPan2.setZoomMouseButton(RIGHT);       //Sets right mouse button up/down drag to zoom (left mouse button to pan)
	  mapBounds2=new Rectangle(width/2,0,width/2,height);//Place to draw map2
	  slippyMap2=new SlippyMap(this, zoomPan2,mapBounds2);
	  slippyMap2.setMapType(SlippyMapType.BING_AERIAL);
	  slippyMap2.setBingApiKey("AoVLv4X3PU31TbprsfobnWkIS5xiyg-_txBKYQ9EpWdRubeu_iSjBzU3PMa-q0o2");


	  //Set the initial view
	  initialLonLatArea=new LonLatBounds(-0.13183631, 51.53467, -0.08562325, 51.51638);
	  slippyMap1.zoomTo(initialLonLatArea);
	  slippyMap2.zoomTo(initialLonLatArea);

	  cityLoc=new PVector(-0.102644086f, 51.527701f);
	}

	public void draw() {
	  background(255);

	  //draw map1
	  slippyMap1.draw();
	  slippyMap1.startClipping(); //clip all drawing to area
	  PVector pt=slippyMap1.getScreenFromLonLat(cityLoc); //get lon/lat of mouse cursor
	  fill(0, 0, 200);
	  ellipse(pt.x, pt.y, 15, 15);
	  slippyMap1.stopClipping();//stop clipping

	  slippyMap2.draw();
	  slippyMap2.startClipping();
	  pt=slippyMap2.getScreenFromLonLat(cityLoc); //get lon/lat of mouse cursor
	  fill(0, 0, 200);
	  ellipse(pt.x, pt.y, 15, 15);
	  slippyMap2.stopClipping();


	  PVector mouseLonLat;
	  if (mapBounds1.contains(mouseX,mouseY))
	    mouseLonLat=slippyMap1.getLonLatFromScreenCoord(mouseX, mouseY); //get lon/lat of mouse cursor
	  else
	    mouseLonLat=slippyMap2.getLonLatFromScreenCoord(mouseX, mouseY); //get lon/lat of mouse cursor
	  
	  textSize(16);
	  textAlign(CENTER, TOP);
	  fill(255,100);
	  noStroke();
	  rect(0, 0, width, 20);
	  fill(0);
	  text("lon="+mouseLonLat.x+", lat="+mouseLonLat.y, width/2, 0);
	}

	public void keyPressed() {
	  if (key=='r')
	    slippyMap1.zoomTo(initialLonLatArea);
	    slippyMap2.zoomTo(initialLonLatArea);
	}

	
}
