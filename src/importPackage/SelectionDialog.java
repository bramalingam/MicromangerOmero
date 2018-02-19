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
package importPackage;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GenericDialog;
import ij.io.OpenDialog;
import ij.measure.ResultsTable;
import ij.plugin.Macro_Runner;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;
import loci.common.DataTools;
import loci.formats.FormatTools;
import loci.formats.ImageTools;

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.openmicroscopy.shoola.util.roi.io.ROIReader;

import omero.ServerError;
import omero.api.RawPixelsStorePrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.Facility;
import omero.gateway.facility.ROIFacility;
import omero.gateway.facility.TablesFacility;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ImageData;
import omero.gateway.model.PixelsData;
import omero.gateway.model.ProjectData;
import omero.gateway.model.ROIData;
import omero.gateway.model.TableData;
import omero.gateway.model.TableDataColumn;
import omero.log.SimpleLogger;
;



/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class SelectionDialog implements ActionListener, ItemListener , MouseListener, MouseWheelListener, ListSelectionListener
{

    private Gateway gateway;
    private String hostName;
	private ImageProcessor ip;
	private ImagePlus imp;
	private String macro_path = "";

   
    /**
     * Creates a new instance.
     *
     * @param objects The objects to display for selection.
     * @throws ServerError 
     * @throws ExecutionException 
     * @throws DSAccessException 
     * @throws DSOutOfServiceException 
     */
    public SelectionDialog() throws ServerError, ExecutionException, DSOutOfServiceException, DSAccessException
    {   
        ConnectToOmero();
    }

    private void ConnectToOmero() throws DSOutOfServiceException {
        // TODO Auto-generated method stub
        
        GenericDialog gd1 = new GenericDialog("Enter OMERO Credentials : ");
        gd1.addStringField("Server :", "localhost");
        gd1.addStringField("Username :", "root");
        gd1.addStringField("Password :", "omero");
        gd1.addStringField("port :","4064");
		String[] choices = new String[] {"Image", "Dataset", "Project"};
		gd1.addChoice("Select Object Type", choices, choices[0]);
		gd1.addStringField("Object ID :", "51");
//		gd1.addCheckbox("Switch Context", false);
        gd1.addStringField("TargetUser: ", "root", 0);
        String[] labels = new String[] {"Select Macro", "Save ROIs", "Save Results"};
		boolean[] defaultValues = {true, false, true};
		gd1.addCheckboxGroup(3, 1, labels, defaultValues);
        gd1.setOKLabel("Run");
        gd1.setSize(2000, 800);
        gd1.setResizable(true);
        gd1.showDialog();

        if (gd1.wasCanceled()) return;

        // TODO Auto-generated method stub
        hostName = gd1.getNextString();
        System.out.println(hostName);
        String userName = gd1.getNextString();
        String password = gd1.getNextString();
        String port = gd1.getNextString();
        String choice = gd1.getNextChoice();
        String imageId = gd1.getNextString();
        Boolean macro_select = gd1.getNextBoolean();
        Boolean save_rois = gd1.getNextBoolean();
        Boolean save_results = gd1.getNextBoolean();

        gateway = connect(hostName, port, userName, password);
        ExperimenterData user = gateway.getLoggedInUser();
        SecurityContext ctx = new SecurityContext(user.getGroupId());
        if (macro_select) {
        		OpenDialog macrofile = new ij.io.OpenDialog("Select A Macro File:");
            System.out.println(macrofile.getPath());
            macro_path = macrofile.getPath();
        }
        
        try {
			Map<String, Object> imageDict = openOmeroImage(ctx, Long.valueOf(imageId));
			ImageData omeroImage = (ImageData) imageDict.get("omeroImage");
			imp = (ImagePlus) imageDict.get("imagePlus");
			if (macro_select) {
				Macro_Runner macroRunner = new ij.plugin.Macro_Runner();
				macroRunner.runMacroFile(macro_path, "");
				if (save_rois) {
					saveROIs(ctx, gateway, Long.parseLong(imageId), imp);
				}
				if (save_results) {
					ResultsTable rt = ResultsTable.getResultsTable();
					saveResults(rt, ctx, omeroImage);
				}
			}
			gd1.showDialog();
			
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }

//    public void addPasswordField(GenericDialog gd, String label, String default1) { 
//        gd.addStringField(label,default1); 
//        TextField tf = (TextField) stringField.elementAt(stringField.size()-1); 
//        tf.setEchoChar('*'); 
//    } 
		
    private void saveResults(ResultsTable rt, SecurityContext ctx, ImageData image) {
		
		int nColumns = rt.getLastColumn();
	    double[] first_column = rt.getColumnAsDoubles(0);
        TableDataColumn[] columns = new TableDataColumn[nColumns];
        
        int nRows = first_column.length;
        Object[][] data = new Object[nColumns][nRows];

        for (int c = 0; c < nColumns; c++) {
        		String colname = rt.getColumnHeading(c);
            columns[c] = new TableDataColumn(colname, c, Double.class);
            double[] cols = rt.getColumnAsDoubles(c);
            System.out.println(colname);
            for (int r = 0; r < nRows; r++) {
            		double value = 0;
            		if (cols != null) {
            			value = cols[r];
            		}
            		if (cols == null) {
            			System.out.println(value);
            		}
                double cellData = value; 
                data[c][r] = cellData;
            }    
        }
        
        TableData td = new TableData(columns, data);
        
        TablesFacility tf;
		try {
			tf = gateway.getFacility(TablesFacility.class);
			tf.addTable(ctx, image, "Test_Table", td);
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DSOutOfServiceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DSAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private Gateway connect(String HOST, String PORT, String USERNAME, String PASSWORD) throws DSOutOfServiceException {
		// TODO Auto-generated method stub
    			LoginCredentials credentials = new LoginCredentials();
    		    credentials.getServer().setHostname(HOST);
    		    credentials.getServer().setPort(Integer.parseInt(PORT));
    		    credentials.getUser().setUsername(USERNAME);
    		    credentials.getUser().setPassword(PASSWORD);
    		    SimpleLogger simpleLogger = new SimpleLogger();
    		    gateway = new Gateway(simpleLogger);
    		    gateway.connect(credentials);
    		    ExperimenterData user = gateway.getLoggedInUser();
    		    System.out.println(user.getId());
    		    return gateway;
	}
    
    private Map<String, Object> openOmeroImage(SecurityContext ctx, long image_id) throws ExecutionException {
    			BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
    			Map<String, Object> image_dict = null;
    			try {
					ImageData image = browse.getImage(ctx, image_id);
					PixelsData pixels = image.getDefaultPixels();
					int sizeZ = pixels.getSizeZ();
					int sizeT = pixels.getSizeT();
					int sizeC = pixels.getSizeC();
					int sizeX = pixels.getSizeX();
					int sizeY = pixels.getSizeY();
					
					String pixtype = pixels.getPixelType();
					int pixType = FormatTools.pixelTypeFromString(pixtype);
					int bpp = FormatTools.getBytesPerPixel(pixType);
					boolean isSigned = FormatTools.isSigned(pixType);
					boolean isFloat = FormatTools.isFloatingPoint(pixType);
					boolean isLittle = false;
					boolean interleave = false;
					
				    RawPixelsStorePrx store = gateway.getPixelsStore(ctx);
				    	long pixelsId = pixels.getId();
				    	store.setPixelsId(pixelsId, false);
				    ImageStack stack = new ImageStack(sizeX, sizeY);
				    for (int t=0; t<sizeT; t++) {
				    		for (int z=0; z<sizeZ; z++) {
				    			for (int c=0; c<sizeC; c++) {
				    				byte[] plane = store.getPlane(z, c, t);
				    		        byte[] channel = ImageTools.splitChannels(plane, 0, 1, bpp, false, interleave);
				    		        Object pixels_array = DataTools.makeDataArray(channel, bpp, isFloat, isLittle);

				    		        if (pixels_array instanceof byte[]) {
				    		            byte[] q = (byte[]) pixels_array;
				    		            if (q.length != sizeX * sizeY) {
				    		              byte[] tmp = q;
				    		              q = new byte[sizeX * sizeY];
				    		              System.arraycopy(tmp, 0, q, 0, Math.min(q.length, tmp.length));
				    		            }
				    		            if (isSigned) q = DataTools.makeSigned(q);

				    		            ip = new ByteProcessor(sizeX, sizeY, q, null);
				    		          }
				    		        else if (pixels_array instanceof short[]) {
				    		            short[] q = (short[]) pixels_array;
				    		            if (q.length != sizeX * sizeY) {
				    		              short[] tmp = q;
				    		              q = new short[sizeX * sizeY];
				    		              System.arraycopy(tmp, 0, q, 0, Math.min(q.length, tmp.length));
				    		            }
				    		            if (isSigned) q = DataTools.makeSigned(q);

				    		            ip = new ShortProcessor(sizeX, sizeY, q, null);
				    		          }
				    		        stack.addSlice("", ip);
				    			}
				    		}
				    }
				    String image_name = image.getName() + "--OMERO ID:" + String.valueOf(image.getId());
				    imp = new ImagePlus(image_name, stack);
				    imp.setDimensions(sizeC, sizeZ, sizeT);
				    imp.setOpenAsHyperStack(true);
				    imp.show();
				    image_dict = new HashMap<String, Object>();
				    image_dict.put("omeroImage", image);
				    image_dict.put("imagePlus", imp);
				} catch (DSOutOfServiceException | DSAccessException | ServerError e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    		    
		return image_dict;
    	
    }
    
    public void saveROIs(SecurityContext ctx, Gateway gateway, long image_id, ImagePlus imp) {

    	    ROIReader reader = new ROIReader();
    	    List<ROIData> roi_list = reader.readImageJROIFromSources(image_id, imp);
    	    ROIFacility roi_facility = null;
			try {
				roi_facility = gateway.getFacility(ROIFacility.class);
				long exp_id = gateway.getLoggedInUser().getId();
				Collection<ROIData> result = roi_facility.saveROIs(ctx, image_id, exp_id , roi_list);
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DSOutOfServiceException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DSAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	    

    }

	@Override
	public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		
	}

}

