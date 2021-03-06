package org.gicentre.utils.slippymap;

//****************************************************************************************
/** Interface that allows other classes to be notified when a new tiles are available to draw
 * 
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

public interface SlippyMapListener {
	public void newTileAvailable();
}
