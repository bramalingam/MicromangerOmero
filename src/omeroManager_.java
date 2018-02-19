
import java.util.concurrent.ExecutionException;

import ij.IJ;
import ij.gui.*;
import ij.plugin.PlugIn;
import importPackage.SelectionDialog;
import omero.ServerError;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;



/*
 *------------------------------------------------------------------------------
 *  Copyright (C) 2014 University of Dundee. All rights reserved.
 *
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along
 *  with this program; if not, write to the Free Software Foundation, Inc.,
 *  51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *------------------------------------------------------------------------------
 */

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class omeroManager_ implements PlugIn {

	private static final double port = 0;
	private static String host = "localhost";
	private static String user = "root";
	private static String pass = "omero";
	private static String targetUser = "root";

	public void run(String arg0) {
		try {
			new SelectionDialog();
		} catch (ServerError | ExecutionException | DSOutOfServiceException | DSAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


}
