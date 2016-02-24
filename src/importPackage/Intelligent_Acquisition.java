package importPackage;
import ij.CompositeImage;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.measure.Calibration;
import ij.measure.ResultsTable;
import ij.plugin.HyperStackConverter;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.LUT;
import ij.process.ShortProcessor;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import loci.common.DataTools;
import loci.common.services.DependencyException;
import loci.common.services.ServiceException;
import loci.common.services.ServiceFactory;
import loci.formats.FormatException;
import loci.formats.FormatTools;
import loci.formats.ImageWriter;
import loci.formats.MetadataTools;
import loci.formats.codec.CompressionType;
import loci.formats.gui.Index16ColorModel;
import loci.formats.in.DefaultMetadataOptions;
import loci.formats.in.MetadataLevel;
import loci.formats.meta.IMetadata;
import loci.formats.services.OMEXMLService;
import mmcorej.CMMCore;
import mmcorej.Configuration;
import mmcorej.PropertySetting;
import mmcorej.StrVector;
import mmcorej.TaggedImage;

import org.json.JSONObject;
import org.micromanager.MMStudio;
import org.micromanager.api.ImageCache;
import org.micromanager.api.MMWindow;
import org.micromanager.utils.ChannelSpec;
import org.micromanager.utils.ImageUtils;

import ome.formats.OMEROMetadataStoreClient;
import ome.formats.importer.ImportCandidates;
import ome.formats.importer.ImportConfig;
import ome.formats.importer.ImportContainer;
import ome.formats.importer.ImportLibrary;
import ome.formats.importer.OMEROWrapper;
import ome.formats.importer.cli.ErrorHandler;
import ome.formats.importer.cli.LoggingImportMonitor;
import ome.formats.importer.transfers.SymlinkFileTransfer;
import ome.formats.importer.transfers.UploadRmFileTransfer;
import ome.units.UNITS;
import ome.units.quantity.Time;
import ome.xml.model.MapPair;
import ome.xml.model.primitives.PositiveInteger;
import omero.gateway.Gateway;
import omero.gateway.LoginCredentials;
import omero.gateway.SecurityContext;
import omero.gateway.exception.DSAccessException;
import omero.gateway.exception.DSOutOfServiceException;
import omero.gateway.facility.BrowseFacility;
import omero.gateway.facility.DataManagerFacility;
import omero.gateway.model.DatasetData;
import omero.gateway.model.ExperimenterData;
import omero.gateway.model.ProjectData;
import omero.log.Logger;
import omero.log.SimpleLogger;
import omero.model.Pixels;

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
public class Intelligent_Acquisition {

    private static final String acqFileName = "/Users/bramalingam/Documents/Micromanger_Omero_Java_Dependencies/AcqSettings.xml";

    private Gateway gateway;

    private static String hostName = "localhost";
    private static String userName = "root";
    private static String passWord = "omero";
    private static int port = 4064;
    private static int datasetId = 4;
    private static String mmConfigFilePath = "/Applications/Micro-Manager1.4_latest/MMConfig_demo.cfg";
    private static String acqRoot = "/Applications/Micro-Manager1.4_latest/testImages/";
    private static String acqFormat = ".ome.tiff";
    private File mmConfigFile;
    private static final String ORDER = "XYCZT";

    public Intelligent_Acquisition(File f)
    {
        this(f, null);
    }

    public Intelligent_Acquisition(File f, Logger logger)
    {
        if (f == null) {
            IJ.error("Please provide a valid MicroManager Config File");
        }
        if (logger == null) {
            logger = new SimpleLogger();
        }
        gateway = new Gateway(logger);
        mmConfigFile = new File("/Applications/Micro-Manager1.4_latest/MMConfig_demo.cfg");
    }

    /**
     * 
     * @param hostName
     * @param port
     * @param userName
     * @param password
     * @return 
     * @throws DSOutOfServiceException
     */
    public Gateway connect(String hostName, int port, String userName, String password) throws DSOutOfServiceException{
        this.hostName = hostName;
        this.port = port;
        this.passWord = password;
        this.userName = userName;
        LoginCredentials cred = new LoginCredentials();
        cred.getServer().setHostname(hostName);
        cred.getServer().setPort(port);
        cred.getUser().setUsername(userName);
        cred.getUser().setPassword(password);

        gateway.connect(cred);
        return gateway;
    }
    /**
     * 
     */
    public void disconnect()
    {
        if (gateway != null) {
            gateway.disconnect();
        }
    }

    /**
     * 
     * @param imagePath
     * @param datasetID
     * @param uploadType
     * @return
     * @throws Throwable
     */
    public Collection<Long> uploadImage(String[] imagePath,String hostName, long datasetID, String uploadType) throws Throwable{

        //Extract OMERO Session Information
        ExperimenterData user = gateway.getLoggedInUser();
        SecurityContext ctx = new SecurityContext(user.getGroupId());
        String sessionKey = gateway.getSessionId(user);

        //Configuration Object
        ImportConfig config = new ImportConfig();

        config.email.set("");
        config.sendFiles.set(true);
        config.sendReport.set(false);
        config.contOnError.set(false);
        config.debug.set(false);
        config.hostname.set(hostName);
        config.sessionKey.set(sessionKey);
        config.targetClass.set("omero.model.Dataset");
        config.targetId.set(datasetID);

        //Logging (switch on)
        loci.common.DebugTools.enableLogging("DEBUG");

        //BF MetadataStoreClient
        OMEROMetadataStoreClient store = config.createStore();
        store.logVersionInfo(config.getIniVersionNumber());
        OMEROWrapper reader = new OMEROWrapper(config);

        //ImportLibrary
        ImportLibrary library = null;
        if (uploadType == null){
            library = new ImportLibrary(store, reader);
        }else if (uploadType.equalsIgnoreCase("UploadRmFileTransfer")) {
            library = new ImportLibrary(store, reader, new UploadRmFileTransfer());
        }else if (uploadType.equalsIgnoreCase("SymlinkFileTransfer")){
            library = new ImportLibrary(store, reader, new SymlinkFileTransfer());
        }else{
            IJ.error("Unknown Import Option");
        }

        //ErrorHandling and logging
        ErrorHandler errorHandler = new ErrorHandler(config);
        library.addObserver(new LoggingImportMonitor());

        //ImportCandidates
        ImportCandidates candidates = new ImportCandidates(reader, imagePath, errorHandler);
        reader.setMetadataOptions(new DefaultMetadataOptions(MetadataLevel.ALL));

        //ImportContainer
        List<ImportContainer> containers = candidates.getContainers();
        Iterator<ImportContainer> i = containers.iterator();
        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);

        //GetDataset from list
        Collection<Long> dataId = new HashSet<Long>();
        dataId.add(datasetID);
        Collection<DatasetData> datasets = browse.getDatasets(ctx, dataId);
        DatasetData dataset = datasets.iterator().next();

        //ImportImage in a loop
        int index = 0;
        Collection<Long> imageIds = new HashSet<Long>();
        while(i.hasNext()){
            ImportContainer ic = i.next();
            ic.setTarget(dataset.asDataset());
            List<Pixels> pixels = library.importImage(ic, index, 0, containers.size());
            imageIds.add(pixels.get(0).getImage().getId().getValue());
            index++;
        }
        return imageIds;
    }

    /**
     * 
     * @param projectName
     * @param gateway
     * @return
     * @throws ExecutionException
     * @throws DSOutOfServiceException
     * @throws DSAccessException
     */
    public static ProjectData getFirstProjectWithName(String projectName, Gateway gateway) throws ExecutionException, DSOutOfServiceException, DSAccessException{

        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ExperimenterData user = gateway.getLoggedInUser();
        SecurityContext ctx = new SecurityContext(user.getGroupId());

        Collection<ProjectData> projects = browse.getProjects(ctx);
        Iterator<ProjectData> i = projects.iterator();
        ProjectData project;

        while (i.hasNext()) {
            project = i.next();
            String pname = project.getName();
            if (pname.equalsIgnoreCase(projectName)){
                return project;
            }
        }
        ProjectData projectData = new ProjectData();
        projectData.setName(projectName);
        DataManagerFacility dm = gateway.getFacility(DataManagerFacility.class);
        project = (ProjectData) dm.saveAndReturnObject(ctx, projectData);
        return project;
    }

    /**
     * 
     * @param datasetName
     * @param gateway
     * @return
     * @throws ExecutionException
     * @throws DSOutOfServiceException
     * @throws DSAccessException
     */
    public static DatasetData getFirstDatasetWithName(String datasetName, Gateway gateway) throws ExecutionException, DSOutOfServiceException, DSAccessException{

        BrowseFacility browse = gateway.getFacility(BrowseFacility.class);
        ExperimenterData user = gateway.getLoggedInUser();
        SecurityContext ctx = new SecurityContext(user.getGroupId());

        Collection<DatasetData> datasets = browse.getDatasets(ctx);
        Iterator<DatasetData> i = datasets.iterator();
        DatasetData dataset;

        while (i.hasNext()) {
            dataset = i.next();
            String pname = dataset.getName();
            if (pname.equalsIgnoreCase(datasetName)){
                return dataset;
            }
        }
        DatasetData datasetData = new DatasetData();
        datasetData.setName(datasetName);
        DataManagerFacility dm = gateway.getFacility(DataManagerFacility.class);
        dataset = (DatasetData) dm.saveAndReturnObject(ctx, datasetData);
        return dataset;
    }

    /**
     * 
     * @param x
     * @param y
     * @param fs
     * @param nf
     * @param nwx
     * @param nwy
     * @param xwd
     * @param ywd
     */
    public static void grid_calculator(int x,int y,int fs,int nf,int nwx,int nwy,int xwd,int ywd){
        // TODO Auto-generated method stub
        // Create method to calculate an Imaging grid based on parameters
        int xgrid = (x-fs*nf);
    }

    /**
     * 
     * @param args
     * @throws Throwable
     */
    public static void main(String [ ] args) throws Throwable{

        //omero login
        Intelligent_Acquisition ia = new Intelligent_Acquisition(null);
        ia.connect(hostName, port, userName, passWord);

        CMMCore mmc = new CMMCore();
        MMStudio gui = new MMStudio(false);
        gui.closeAllAcquisitions();
        gui.clearMessageWindow();
        mmc.loadSystemConfiguration(ia.mmConfigFile.getAbsolutePath());
        gui.loadAcquisition(acqFileName);

        //get config of device properties
        String stage = mmc.getXYStageDevice();

        double x = mmc.getXPosition();
        double y = mmc.getYPosition();

        //Matrix for imaging
        //TODO use the grid_calculator here
        int noOfRows = 1;
        int noOfColumns = 1;
        int fieldSpacing = 100;

        //Create MapPair Object and add to List
        ArrayList<MapPair> mapList = new ArrayList<MapPair>();
        MapPair mapPair;
        mapPair = new MapPair("Bio-Formats Version", FormatTools.VERSION);
        mapList.add(mapPair);

        //device properties as mapAnnotations
        StrVector devices = mmc.getLoadedDevices();
        StrVector properties;
        for (int i=0; i<devices.size(); i++){
            properties = mmc.getDevicePropertyNames(devices.get(i));
            for(int j=0; j<properties.size(); j++){
                System.out.println(properties.get(j));
                System.out.println(mmc.getProperty(devices.get(i),properties.get(j)));
                mapPair = new MapPair(properties.get(j),mmc.getProperty(devices.get(i),properties.get(j)));
                mapList.add(mapPair);
            }
        }

        //Available Configs and their values
        StrVector configs = mmc.getAvailableConfigGroups();
        for (int i=0; i<configs.size(); i++){
            StrVector presets = mmc.getAvailableConfigs(configs.get(i));
            for(int j=0; j<presets.size(); j++){
                Configuration cdata = mmc.getConfigData(configs.get(i), presets.get(j));
                System.out.println("Configuration " + configs.get(i) + "," + presets.get(j));
                mapPair = new MapPair("Configuration " + configs.get(i),presets.get(j));
                mapList.add(mapPair);
                for (int k=0; k<cdata.size(); k++) {
                    PropertySetting s = cdata.getSetting(k);
                    System.out.println(" " + s.getDeviceLabel() + ", " + s.getPropertyName() + ", " + s.getPropertyValue());
                    mapPair = new MapPair(s.getDeviceLabel() + " " + s.getPropertyName(), s.getPropertyValue());
                    mapList.add(mapPair);
                }
            }
        }

        for(int i=0; i<noOfRows ; i++){
            if(i>0){
                mmc.setXYPosition(stage, x+fieldSpacing, y);
                mmc.waitForDevice(stage);
            }

            for(int j=0; j<noOfColumns; j++){
                if(j>0){
                    mmc.setXYPosition(stage, x, y+fieldSpacing);
                    mmc.waitForDevice(stage);
                }

                //Snap Image
                gui.loadAcquisition(acqFileName);
                gui.openAcquisition("Well" + i + "Field" + j, acqRoot, gui.getAcquisitionSettings().numFrames, gui.getAcquisitionSettings().channels.size(), gui.getAcquisitionSettings().slices.size());
                gui.runAcquisition();
                gui.closeAcquisition("Well" + i + "Field" + j);

                //Per-Image Properties as Map-Annotations
                ImagePlus plus = WindowManager.getCurrentWindow().getImagePlus();
                MMWindow mmwindow = new MMWindow(plus) ;
                int nFrames = gui.getAcquisitionSettings().numFrames;
                int nChannels = gui.getAcquisitionSettings().channels.size();
                int nSlices = gui.getAcquisitionSettings().slices.size();
                for (int channel = 0; channel<nChannels; channel++){
                    for(int slice = 0; slice<nSlices; slice++){
                        for(int frame =0; frame<nFrames; frame++){
                            JSONObject meta1 = mmwindow.getImageMetadata(channel, slice, frame, 0);
                            Iterator<String> keys1 = meta1.keys();
                            String[] perImageProperties = {"ChannelIndex","Slice", "SliceIndex", "SlicePosition","Frame","FrameIndex" , "Channel", "Dichroic-Label", "ElapsedTime-ms","Emission-Label",
                                    "Excitation-Label","Exposure-ms", "PixelSizeUm","Time", "UUID", "XPositionUm", "YPositionUm", "ZPositionUm","Summary"
                            };
                            for (int iter = 0; iter<perImageProperties.length; iter++){
                                Object value = meta1.get(perImageProperties[iter]);
                                mapPair = new MapPair(perImageProperties[iter] , value.toString());
                                mapList.add(mapPair);
                            }
                        }
                    }
                }

                //Now run the macro
                //IJ.runMacro(macroName);
                //IJ.run("Threshold...");
                //IJ.run("Close");
                //IJ.run(plus, "Analyze Particles...", "display clear add stack");
                
                //Example illustration
                IJ.run(plus, "Measure", "");
                ResultsTable res = ij.measure.ResultsTable.getResultsTable();
                
                if(res.getCounter() > 0) {
                    //Extract basic metadata from the ImagePlus object
                    IMetadata metadata = ia.getMinimalMetadata(plus);

                    //Set Map-Annotations
                    metadata = ia.setMapAnnotation(metadata, mapList);

                    //Save Image with Metadata and MapAnnotations
                    String path = acqRoot + "Well" + i + "Field" + j + acqFormat;
                    ia.bfSave(path, plus, metadata);

                    String[] paths = {path};
                    // TODO getDatasetId for a user defined dataset Name
                    ia.uploadImage(paths,hostName, datasetId, null);
                }

            }
        }
        ia.disconnect();
    }
    
    public void bfSaveSPW(String fileOut,int nRows,int nCols, int nFOV, ImagePlus plus,IMetadata metadata, String plateDescription){
        
        FileWriteSPW SPWWriter = new FileWriteSPW(fileOut, plateDescription);
        int width = plus.getWidth();
        int height = plus.getHeight();
        
        int sizet = plus.getNFrames();
        
        int[][] nFovInWell = new int[nRows][nCols];
        for (int row = 0; row < nRows; row++) {
          for (int col = 0; col < nCols; col++) {
            nFovInWell[row][col] = nFOV;
          }
        }

        byte[] plane;
        ArrayList<String> delayList = new ArrayList<>();
        delayList.add("1000");
        delayList.add("2000");
        delayList.add("3000");
        
        double[] exposureTimes = new double[sizet];
        for (int t = 0; t < sizet; t++)  {
          exposureTimes[t] = 1000.0;
        }
        boolean ok = SPWWriter.init(nFovInWell, width, height, delayList, exposureTimes);
    }

    /**
     * Saves the current active Image on ImageJ, as an ome-tiff.
     * @param path
     * @param mapList 
     * @throws IOException 
     * @throws FormatException 
     * @throws DependencyException 
     * @throws ServiceException 
     */
    public void bfSave(String path, ImagePlus plus, IMetadata metadata) throws FormatException, IOException, DependencyException, ServiceException {

        File imageFile = new File(path);
        if (imageFile.isFile() && imageFile != null) imageFile.delete();

        //Initialize writer and save file
        ImageWriter writer = new ImageWriter();
        writer.setMetadataRetrieve(metadata);
        writer.setId(path);

        int size = plus.getImageStackSize();
        ImageStack is = plus.getImageStack();
        boolean doStack = writer.canDoStacks() && size > 1;
        int start = doStack ? 0 : plus.getCurrentSlice() - 1;
        int end = doStack ? size : start + 1;

        int width = plus.getWidth();
        int height = plus.getHeight();

        byte[] plane = null;
        boolean littleEndian =
                !writer.getMetadataRetrieve().getPixelsBinDataBigEndian(0, 0).booleanValue();
        writer.setInterleaved(false);
        for (int i=start; i<end; i++) {

            //Get Pixels as Bytes from ImagePlus
            ImageProcessor proc = is.getProcessor(i + 1);;
            plane = null;
            if (proc instanceof ByteProcessor) {
                plane = (byte[]) proc.getPixels();
            }
            else if (proc instanceof ShortProcessor) {
                plane = DataTools.shortsToBytes(
                        (short[]) proc.getPixels(), littleEndian);
            }
            else if (proc instanceof FloatProcessor) {
                plane = DataTools.floatsToBytes(
                        (float[]) proc.getPixels(), littleEndian);
            }
            else if (proc instanceof ColorProcessor) {
                byte[][] pix = new byte[3][width*height];
                ((ColorProcessor) proc).getRGB(pix[0], pix[1], pix[2]);
                plane = new byte[3 * width * height];
                System.arraycopy(pix[0], 0, plane, 0, width * height);
                System.arraycopy(pix[1], 0, plane, width * height, width * height);
                System.arraycopy(pix[2], 0, plane, 2 * width * height, width * height);
            }
            // TODO setColorModel
            writer.saveBytes(i, plane);
        }
        writer.close();

    }

    /**
     * 
     * @param plus
     * @return
     * @throws DependencyException
     * @throws ServiceException
     */
    public IMetadata getMinimalMetadata(ImagePlus plus) throws DependencyException, ServiceException{

        int pixelType = plus.getBitDepth();
        int width = plus.getWidth();
        int height = plus.getHeight();
        int sizeC = plus.getNChannels();
        int sizeT = plus.getNFrames();
        int sizeZ = plus.getNSlices();
        //Change this value if its not a gray-scale image
        int samplesPerPixel = 1;

        // create metadata object with minimum required metadata fields
        System.out.println("Populating metadata...");
        System.out.println("ImagePlus Type : " + plus.getType());
        ServiceFactory factory = new ServiceFactory();
        OMEXMLService service = factory.getInstance(OMEXMLService.class);
        IMetadata metadata = service.createOMEXMLMetadata();
        metadata.createRoot();

        //Extract and convert pixel Type
        switch (plus.getType()) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
            case ImagePlus.COLOR_RGB:
                pixelType = FormatTools.UINT8;
                break;
            case ImagePlus.GRAY16:
                pixelType = FormatTools.UINT16;
                break;
            case ImagePlus.GRAY32:
                pixelType = FormatTools.FLOAT;
                break;
        }
        //populate minimal metadata
        MetadataTools.populateMetadata(metadata, 0, null, false, "XYZCT",
                FormatTools.getPixelTypeString(pixelType), width, height, sizeZ, sizeC, sizeT, samplesPerPixel);

        return metadata;

    }

    /**
     * 
     * @param metadata
     * @param mapList
     * @return
     */
    public IMetadata setMapAnnotation(IMetadata metadata, ArrayList<MapPair> mapList){
        int mapAnnotationIndex = 0;
        int annotationRefIndex = 0;
        String mapAnnotationID = MetadataTools.createLSID("MicroManagerMapAnnotation", 0, mapAnnotationIndex);

        metadata.setMapAnnotationID(mapAnnotationID, mapAnnotationIndex);
        metadata.setMapAnnotationValue(mapList, mapAnnotationIndex);
        metadata.setMapAnnotationAnnotator("MicroManager Map Annotation", mapAnnotationIndex);
        metadata.setMapAnnotationDescription("MicroManager Description", mapAnnotationIndex);
        metadata.setMapAnnotationNamespace("MicroManager NameSpace", mapAnnotationIndex);
        metadata.setImageAnnotationRef(mapAnnotationID,0, annotationRefIndex);
        return metadata;
    }

    /**
     * 
     * @param path
     */
    public void bfSaveMinimal(String path){

        StringBuffer buffer = new StringBuffer();

        buffer.append("outfile=" + path);
        buffer.append(" splitz=false");
        buffer.append(" splitc=false");
        buffer.append(" splitt=false");
        buffer.append(" compression=" + CompressionType.UNCOMPRESSED.getCompression());
        buffer.append(" saveroi=true");
        try{
            IJ.runPlugIn("loci.plugins.LociExporter", buffer.toString());
        }catch(Exception e){
            IJ.log(e.getLocalizedMessage());
        }
    }
}

