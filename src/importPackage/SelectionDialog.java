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
import ij.Macro;
import ij.gui.GenericDialog;
import ij.gui.Roi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeListener;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import omero.RLong;
import omero.ServerError;
import omero.client;
import omero.api.IContainerPrx;
import omero.api.ServiceFactoryPrx;
import omero.gateway.Gateway;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import omero.model.Dataset;
import omero.model.DatasetI;
import omero.model.IObject;
import omero.model.Pixels;
import omero.model.Project;
import omero.model.RoiI;
import omero.sys.ParametersI;



/**
 * 
 *
 * @author Balaji Ramalingam &nbsp;&nbsp;&nbsp;&nbsp;
 * <a href="mailto:b.ramalingam@dundee.ac.uk">b.ramalingam@dundee.ac.uk</a>
 * @since 5.1
 */
public class SelectionDialog
extends JDialog
implements ItemListener, ActionListener
{

    /** Component hosting projects.*/
    private JComboBox parents;

    /** Component hosting datasets.*/
    private JComboBox children;

    /** The button to close the dialog.*/
    private JButton cancelButton;

    /** Control to start the import.*/
    private JButton saveButton;

    /** Component indicating to import the roi.*/
    private JCheckBox roiExport;

    private Collection<ProjectData> projects;

    private ServiceFactoryPrx session;

    private ServiceFactoryPrx entryUnencrypted;

    private String importButton = "omeroImport";

    private String cancelButton1 = "cancel";

    private Intelligent_Acquisition ia = new Intelligent_Acquisition(null);

    private Gateway gateway;

    private SecurityContext ctx;
    
    private String hostName;

    private void populateChildren()
    {
        DataNode n = (DataNode) parents.getSelectedItem();
        Object data = n.getData();
        Set<DatasetData> d;
        DatasetData obj;
        DataNode node;
        if (data instanceof ProjectData) {
            ProjectData p = (ProjectData) data;
            d = p.getDatasets();
            Iterator<DatasetData> j = d.iterator();
            DefaultComboBoxModel<DataNode> model = new DefaultComboBoxModel<DataNode>();
            while (j.hasNext()) {
                obj = j.next();
                if (obj instanceof DatasetData) {
                    node = new DataNode(obj);
                    model.addElement(node);
                }
            }

            children.removeAllItems();
            children.setModel(model);
            children.setVisible(true);
        }

    }

    private void initComponents(Collection<ProjectData> projects2)
    {
        //Convert the nodes
        Iterator<ProjectData> i = projects2.iterator();
        ProjectData obj;
        DataNode node = null;
        Vector<DataNode> v = new Vector<DataNode>();
        while (i.hasNext()) {
            obj = i.next();
            if (obj instanceof ProjectData) {
                node = new DataNode(obj);
                v.add(node);
            }
        }
        //
        roiExport = new JCheckBox("Import ROI");
        parents = new JComboBox(v);
        parents.addItemListener(this);

        children = new JComboBox();
        children.addItemListener(this);
        populateChildren();

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);
        cancelButton.setActionCommand(cancelButton1);

        saveButton = new JButton("Import");
        saveButton.addActionListener(this);

        saveButton.setActionCommand(importButton);
    }

    private JPanel buildRow(String text, JComboBox box)
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));

        p.add(new JLabel(text));
        p.add(box);
        return p;
    }
    private JPanel buildSelectionPane() 
    {
        JPanel p = new JPanel();

        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));

        p.add(buildRow("Project", parents));
        p.add(buildRow("Datasets", children));
        return p;
    }
    /** Builds and lays out the UI.*/
    private void buildGUI()
    {
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        JPanel p = new JPanel();
        p.setLayout(new FlowLayout(FlowLayout.RIGHT));
        p.add(cancelButton);
        p.add(saveButton);
        p.add(roiExport);
        contentPane.add(buildSelectionPane(), BorderLayout.CENTER);
        contentPane.add(p, BorderLayout.SOUTH);
        setSize(300, 150);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }
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
        projects = getOmeroProjects();
        setTitle("Select Target Project and Dataset");
        this.projects = projects;
        initComponents(projects);
        buildGUI();
        setVisible(true);
    }

    private Collection<ProjectData> getOmeroProjects() throws ServerError, ExecutionException, DSOutOfServiceException, DSAccessException {
        // TODO Auto-generated method stub
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        projects = browse.getProjects(ctx);
        return projects;
    }
    private void ConnectToOmero() throws DSOutOfServiceException {
        // TODO Auto-generated method stub
        
        GenericDialog gd1 = new GenericDialog("Enter OMERO Credentials : ");
        gd1.addStringField("Server :", "eel.openmicroscopy.org");
        gd1.addStringField("Username :", "user-1");
        gd1.addStringField("Password :", "ome");
        gd1.addStringField("port :","4064");
        gd1.setOKLabel("login");

        gd1.setSize(800, 400);
        gd1.setResizable(true);
        gd1.showDialog();

        if (gd1.wasCanceled()) return;

        // TODO Auto-generated method stub
        hostName = gd1.getNextString();
        System.out.println(hostName);
        String userName = gd1.getNextString();
        String password = gd1.getNextString();
        int port = Integer.valueOf(gd1.getNextString());

        gateway = ia.connect(hostName, port, userName, password);
        ExperimenterData user = gateway.getLoggedInUser();
        ctx = new SecurityContext(user.getGroupId());
    }
    /**
     * Returns <code>true</code> to import the ROI into OMERO,
     * <code>false</code> otherwise.
     *
     * @return See above
     */
    boolean exportROI() { return roiExport.isSelected(); }

    @Override
    public void itemStateChanged(ItemEvent e) {

        Object src = e.getSource();
        if (e.getStateChange() == ItemEvent.SELECTED){
            if (src == parents) {
                populateChildren();
            }
        }
    }
    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand() == importButton){
            System.out.println("You clicked the button");
            try {
                ImportImage();
            } catch (Throwable e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
        if (e.getActionCommand() == cancelButton1){
            System.out.println("You clicked the cancel button");
            Macro.abort();
            return;
        }
    }

    private void ImportImage() throws Throwable {
        // TODO Auto-generated method stub
        DataNode n = (DataNode) children.getSelectedItem();
        Object data = n.getData();
        if (data instanceof DatasetData) {
            DatasetData d = (DatasetData) data;
            Long datasetId = d.getId();
            String importArgs = IJ.getImage().getOriginalFileInfo().directory.toString() + IJ.getImage().getOriginalFileInfo().fileName.toString();
            String[] imagePath = new String[] { importArgs };
            ia.uploadImage(imagePath,hostName, datasetId, null);
        }

    }

}

