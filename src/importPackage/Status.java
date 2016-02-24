package importPackage;
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
import ome.formats.importer.IObservable;
import ome.formats.importer.IObserver;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportEvent;
import ome.formats.importer.ImportEvent.FILESET_UPLOAD_END;
import ome.formats.importer.util.ErrorHandler;

/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class Status 
implements IObserver{

    /**
     * Displays the status of an on-going import.
     * @see IObserver#update(IObservable, ImportEvent)
     */
    public void update(IObservable observable, ImportEvent event)
    {
        if (event == null) return;
        System.err.println(event);
        if (event instanceof ImportEvent.FILESET_UPLOAD_PREPARATION) {
            ImportEvent.FILESET_UPLOAD_PREPARATION e = (ImportEvent.FILESET_UPLOAD_PREPARATION) event;
            //e.exception.printStackTrace();
            System.err.println("name:"+e.filename);
        }
    }
}
