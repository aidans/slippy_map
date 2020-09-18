import java.awt.Rectangle;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.gicentre.utils.gui.BusyIcon;
import org.gicentre.utils.gui.GraphicBuffer;
import org.gicentre.utils.gui.ThreadedDraw;
import org.gicentre.utils.gui.ThreadedGraphicBuffer;
import org.gicentre.utils.gui.ThreadedGraphicBufferListener;
import org.gicentre.utils.move.ZoomPan;
import org.gicentre.utils.move.ZoomPanListener;
import org.gicentre.utils.move.ZoomPanState;
import org.gicentre.utils.slippymap.LonLatBounds;
import org.gicentre.utils.slippymap.SlippyMap;
import org.gicentre.utils.slippymap.SlippyMapListener;
import org.gicentre.utils.slippymap.SlippyMapType;
import org.gicentre.utils.spatial.OSGB;

import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PVector;


public class SlippyMapPostcodes extends PApplet implements SlippyMapListener,ZoomPanListener,MouseWheelListener,ThreadedDraw,ThreadedGraphicBufferListener{

	org.gicentre.utils.slippymap.SlippyMap slippyMap;
	ZoomPan zoomPan;
	Map<String, PVector> postcodes;
	ThreadedGraphicBuffer threadedGraphicBuffer;
	BusyIcon busyIcon;
	
	public void setup(){
		size(1200,500);
		smooth();
		zoomPan=new ZoomPan(this);
		zoomPan.setMinZoomScale(0.5);
		zoomPan.setZoomMouseButton(RIGHT);
		zoomPan.addZoomPanListener(this);
		slippyMap=new SlippyMap(this,zoomPan,new Rectangle(0,0,width,height));
		slippyMap.zoomTo(new LonLatBounds(-1.55f,52.41f,-1.05f,52.61f));
		slippyMap.setUseFileCache(false);
		slippyMap.setCloudMadeStyleId(15505);
		
		postcodes=new HashMap<String, PVector>();
		OSGB osgb=new OSGB();
		String[] lines = loadStrings("data/uk_postcodes.csv");
		for (String line:lines){
			String[] toks=split(line,",");
			PVector xy=new PVector(Float.parseFloat(toks[1]),Float.parseFloat(toks[2]));
			PVector lonLat=osgb.invTransformCoords(xy);
			postcodes.put(toks[0],lonLat);
		}
		
		threadedGraphicBuffer=new ThreadedGraphicBuffer(this,zoomPan,this,new Rectangle(0,0,width,height));
		threadedGraphicBuffer.addListener(this);
//		threadedGraphicBuffer.setUpdateDuringZoomPan(true);
		noLoop();
		slippyMap.addSlippyMapListener(this);
		zoomPan.addZoomPanListener(this);
		this.addMouseWheelListener(this);
		busyIcon=new BusyIcon();
	}
	
	public void draw(){
		background(255);
		//copy zoompan state for this draw loop
		ZoomPanState zoomPanState=zoomPan.getZoomPanState();
		slippyMap.draw(zoomPanState);
		threadedGraphicBuffer.draw(zoomPanState);
		//draw busy icon if threaded drawing is in progress

		if (threadedGraphicBuffer.isDrawingInThread())
			busyIcon.draw(this,width/2,height-30,30);
	}
	
	public void mouseMoved(){
		redraw();
	}

	public void mouseDragged(){
		redraw();
	}

	
	public void keyPressed(){
		if (key=='t')
			if (slippyMap.getMapType()==SlippyMapType.BING_ROAD)
				slippyMap.setMapType(SlippyMapType.BING_AERIAL);
			else if (slippyMap.getMapType()==SlippyMapType.BING_AERIAL)
				slippyMap.setMapType(SlippyMapType.BING_AERIAL_WITH_LABELS);
			else if (slippyMap.getMapType()==SlippyMapType.BING_AERIAL_WITH_LABELS)
				slippyMap.setMapType(SlippyMapType.OSM_MAPNIK);
			else if (slippyMap.getMapType()==SlippyMapType.OSM_MAPNIK)
				slippyMap.setMapType(SlippyMapType.OSM_MAPQUEST);
			else if (slippyMap.getMapType()==SlippyMapType.OSM_MAPQUEST)
				slippyMap.setMapType(SlippyMapType.OSM_CLOUDMADE);
			else if (slippyMap.getMapType()==SlippyMapType.OSM_CLOUDMADE)
				slippyMap.setMapType(SlippyMapType.BING_ROAD);
		else if (key=='r')
			slippyMap.zoomTo(new LonLatBounds(-1.55f,52.41f,-1.05f,52.61f));
		redraw();
	}

	public void newTileAvailable() {
		redraw();
	}

	public void zoomEnded() {
		threadedGraphicBuffer.setUpdateFlag();
		redraw();
	}

	public void panEnded() {
		threadedGraphicBuffer.setUpdateFlag();
		redraw();
	}

	public void mouseWheelMoved(MouseWheelEvent e) {
		threadedGraphicBuffer.setUpdateFlag();
		redraw();
	}

	public void threadedDraw(PGraphics canvas,ZoomPanState zoomPanState,Object drawData) {
		int[][] pixelArray;
		pixelArray=new int[width+1][height+1];
		Rectangle2D viewport=slippyMap.getLonLatViewPort();
		canvas.fill(100,100,0,100);
		canvas.noStroke();
		for(Entry<String, PVector> entry: postcodes.entrySet()){
			if (viewport.contains(entry.getValue().x,entry.getValue().y)){
				PVector screenXY=slippyMap.getScreenFromLonLat(entry.getValue(),zoomPanState);
				try{
					if (pixelArray[(int)screenXY.x][(int)screenXY.y]<1){
						pixelArray[(int)screenXY.x][(int)screenXY.y]++;
						canvas.ellipse(screenXY.x,screenXY.y,5,5);
					}
				}
				catch (ArrayIndexOutOfBoundsException e){

				}
			}
		}
	}

	public void newBufferedImageAvailable() {
		this.redraw();
	}
}
