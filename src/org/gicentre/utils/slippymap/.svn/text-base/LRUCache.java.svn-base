package org.gicentre.utils.slippymap;

import java.util.LinkedHashMap;
import java.util.Map;

//****************************************************************************************
/** An ordered set that removes the oldest entries when it reaches its size limit
* 
* Code taken from http://blog.meschberger.ch/2008/10/linkedhashmaps-hidden-features.html
* 
*  @author Aidan Slingsby, giCentre, City University London, based on http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames
*  @version 1.0, August 2010 
*/ 
//*****************************************************************************************

class LRUCache<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = 1L;

	private final int limit;
	public LRUCache(int limit) {
		super(16, 0.75f, true);
		this.limit = limit;
	}
	
	@Override
	protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
		return size() > limit;
	}
}