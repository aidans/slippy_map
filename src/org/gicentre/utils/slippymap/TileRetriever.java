package org.gicentre.utils.slippymap;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;
import javax.xml.ws.http.HTTPException;

import processing.core.PApplet;
import processing.core.PImage;

//****************************************************************************************
/** Class which retrieves and caches maptiles in its own thread.
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

class TileRetriever extends Thread{

	List<TileInfo> urls=Collections.synchronizedList(new LinkedList<TileInfo>()); //list of URLs to retrieve
//	Map<String, PImage> cachedTiles; //in-memory cache for tiles
	PApplet applet;
	Set<SlippyMapListener> slippyMapListeners; //listeners to inform about whether there are tiles available
	PImage noInternetImage; //blank image for where tile has not been successfully retrieved
	LRUCache<String, PImage> tileCache;
	boolean useFileCache=true; //whether or not to use a file-based cache for tiles
	String passthroughURL=null; //URL to retrieve external URLs for the tiles. Needs to be used by unsigned applets. See SlippyMsp documentation
	
    MediaTracker tracker;
	
	private class TileInfo{
		String url;
		String cacheFilename;
		public TileInfo(String url, String cacheFilename) {
			this.url=url;
			this.cacheFilename=cacheFilename;
		}
		public boolean equals(Object o) {
			return this.url.equals(((TileInfo)o).url) && this.cacheFilename.equals(((TileInfo)o).cacheFilename);
		}

	}
	
	/** Constructor
	 * 
	 * @param applet The sketch
	 * @param cachedTiles The tile cache to use
	 * @param maxItemsInCache Maximum number of cache items
	 */
	TileRetriever(PApplet applet, int maxItemsInCache){
		this.applet=applet;
		File tempDir=new File(getFileCachePath());
		if (!tempDir.exists())
			tempDir.mkdir();
		tracker = new MediaTracker(applet);
		tileCache=new LRUCache<String, PImage>(maxItemsInCache);
		this.start(); //start the thread
	}
	
	/** Sets whether or not to use the file-based cache
	 * 
	 * @param useFileCache
	 */
	void setUseFileCache(boolean useFileCache){
		this.useFileCache=useFileCache;
	}
	
	
	/** Thread which retrieves tiles from the URL list
	 * 
	 */
	public void run(){
		boolean x=true;
		while (x){ //run forever
			while (!urls.isEmpty()){
				TileInfo tileInfo=urls.remove(0);//take the first URL
				PImage image=null;
				
				String tileFileName=tileInfo.cacheFilename;
				String pathFileName=getFileCachePath()+File.separatorChar+tileFileName;
				
//				//if it's on disk, get it
				if (useFileCache){
					try{
						if (new File(pathFileName).exists()) //try to load from disk
							image=loadImage(new FileInputStream(pathFileName));
					}
					catch (Exception e) {
						// silently handle
					}
					if (image!=null){
						tileCache.put(tileFileName, image); //add to in-memory cache
					}
				}
				
				
				//Try and load from web
				try{
					if (image==null){
						URL actualUrl=new URL(tileInfo.url);
						InputStream inputStream=actualUrl.openStream();
						image=loadImage(inputStream); //otherwise, try to load from URL

					}
					if (image.width<=0) //if invalid image, set to null
						image=null;
					if (image!=null && useFileCache){
						String ext=tileFileName.substring(tileFileName.lastIndexOf("."));
						File tempFile=new File(getFileCachePath()+File.separatorChar+"temp"+ext);
						BufferedImage bimage = new BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB);
						Graphics2D bGr = bimage.createGraphics();
						bGr.drawImage(image.getImage(), 0, 0, null);
						bGr.dispose();
						ImageIO.write(bimage, "PNG",tempFile);
						tempFile.renameTo(new File(pathFileName));
					}
				}
				catch (Exception e) {
					System.out.println(e);
					// silently handle
				}
				if (image==null){
					//then internet is down
					if (noInternetImage==null){
						noInternetImage=new PImage(250, 250);
					}
					image=noInternetImage;
				}

				if (image!=noInternetImage){
					tileCache.put(tileFileName, image); //add to in-memory cache
					if (slippyMapListeners!=null) //notify all the listeners that a new tile is available
						for (SlippyMapListener slippyMapListener:slippyMapListeners){
							slippyMapListener.newTileAvailable();
						}
				}
				//pause the thread if there are no more tiles to get
				if (urls.isEmpty()){
					try {
						synchronized (this) {
							this.wait();
						}
					} catch (InterruptedException e) {
						//silently handle
					}
				}
			}
		}
	}
	
	public String getFileCachePath(){
		return applet.sketchPath+File.separatorChar+"tilecache"+File.separatorChar;
	}

	/** Adds a slippy map listener
	 * 
	 * @param slippyMapListener
	 */
	void addSlippyMapListener(SlippyMapListener slippyMapListener){
		if (this.slippyMapListeners==null)
			this.slippyMapListeners=new HashSet<SlippyMapListener>();
		this.slippyMapListeners.add(slippyMapListener);
	}

	/**Removes a slippy map listener
	 * 
	 * @param slippyMapListener
	 */
	void removeSlippyMapListener(SlippyMapListener slippyMapListener){
		if (this.slippyMapListeners!=null)
			this.slippyMapListeners.remove(slippyMapListener);
	}

	/** queue URL for retrieval
	 * 
	 * @param url
	 */
	private void retrieve(String url, String cacheFilename){
		TileInfo tileInfo=new TileInfo(url, cacheFilename);
		synchronized (urls) {
			urls.remove(url);//remove if it is already there.
			urls.add(0,tileInfo);//put at top
			synchronized (this) {
				this.notify();
			}
			//don't let this list become too big
			if (urls.size()>30)
				urls.remove(urls.size()-1);
		}
	}
	
	/** Get a tile image from the memory cache or the disk cache
	 * If it gets it from the disk, it stores in memory
	 * 
	 * @param tileUrlString
	 * @param passthroughURL
	 * @return
	 */
	PImage getTileImage(String tileUrlString, String cacheFilename,boolean onlyGetFromCache){

		PImage image=null;

		//if it's in memory, get it
		image=tileCache.get(cacheFilename);
		if (image!=null)
			return image;

//		//if it's on disk, get it
		if (useFileCache){
			String pathFileName=getFileCachePath()+File.separatorChar+cacheFilename;
			try{
				if (new File(pathFileName).exists()){ //try to load from disk
					image=loadImage(new FileInputStream(pathFileName));
				}
			}
			catch (Exception e) {
				// silently handle
				e.printStackTrace();
			}
			if (image!=null){
				tileCache.put(cacheFilename, image); //add to in-memory cache
			}
		}
		
		if (image==null && !onlyGetFromCache){
			//Then try to get from web
			if (passthroughURL!=null){
				try {
					tileUrlString=passthroughURL+"?url="+URLEncoder.encode(tileUrlString,"UTF-8");
				}
				catch (UnsupportedEncodingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			retrieve(tileUrlString,cacheFilename);
		}



		return image;
	}
	
	void setPassThroughURL(String passthroughURL){
		this.passthroughURL=passthroughURL;
	}

	String applyPassThroughURL(String url){
		if (passthroughURL==null)
			return url;
		else{
			try {
				url=passthroughURL+"?url="+URLEncoder.encode(url,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return url;
		}
	}

	
	/**
	   * @nowebref
	   */
	  public PImage loadImage(InputStream inputStream) {
	    // For jpeg, gif, and png, load them using createImage(),
	    // because the javax.imageio code was found to be much slower, see
	    // <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=392">Bug 392</A>.
	    try {
	     {
	        byte bytes[] = loadBytes(inputStream);
	        if (bytes == null) {
	          return null;
	        } else {
	          Image awtImage = Toolkit.getDefaultToolkit().createImage(bytes);
	          PImage image = loadImageMT(awtImage);
	          if (image.width == -1) {
	            return null;
	          }

	          return image;
	        }
	      }
	    } catch (Exception e) {
	    }
	   return null;
	  }

	  static public byte[] loadBytes(InputStream input) {
		    try {
		      BufferedInputStream bis = new BufferedInputStream(input);
		      ByteArrayOutputStream out = new ByteArrayOutputStream();

		      int c = bis.read();
		      while (c != -1) {
		        out.write(c);
		        c = bis.read();
		      }
		      return out.toByteArray();

		    } catch (Exception e) {
		    }
		    return null;
		  }
	  
	  /**
	   * Load an AWT image synchronously by setting up a MediaTracker for
	   * a single image, and blocking until it has loaded.
	   */
	  protected PImage loadImageMT(Image awtImage) {
		  MediaTracker tracker = new MediaTracker(applet);
		  tracker.addImage(awtImage, 0);
		  try {
			  tracker.waitForAll();
		  }
		  catch (Exception e) {
		  }
		  PImage image = new PImage(awtImage);
		  image.parent = applet;
		  return image;
	  }

}
