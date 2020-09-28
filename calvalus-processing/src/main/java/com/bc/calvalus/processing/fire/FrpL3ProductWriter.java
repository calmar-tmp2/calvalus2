package com.bc.calvalus.processing.fire;

import com.bc.calvalus.commons.DateUtils;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.write.Nc4Chunking;
import ucar.nc2.write.Nc4ChunkingDefault;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class FrpL3ProductWriter extends AbstractProductWriter {

    private NetcdfFileWriter fileWriter;

    public FrpL3ProductWriter(ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);

        fileWriter = null;
    }

    static String getOutputPath(Object output) {
        final String filePath;
        if (output instanceof File) {
            filePath = ((File) output).getAbsolutePath();
        } else if (output instanceof String) {
            filePath = (String) output;
        } else {
            throw new IllegalArgumentException("invalid input type");
        }
        return filePath;
    }

    static void addDimensions(NetcdfFileWriter fileWriter, Product product) {
        fileWriter.addDimension("time", 1);
        fileWriter.addDimension("lon", product.getSceneRasterWidth());
        fileWriter.addDimension("lat", product.getSceneRasterHeight());
        fileWriter.addDimension("bounds", 2);
    }

    static void addGlobalMetadata(NetcdfFileWriter fileWriter, Product product) {
        final SimpleDateFormat COMPACT_ISO_FORMAT = DateUtils.createDateFormat("yyyyMMdd'T'HHmmss'Z'");
        final String dateStringNow = COMPACT_ISO_FORMAT.format(new Date());

        fileWriter.addGlobalAttribute("title", "ECMWF C3S Gridded OLCI Fire Radiative Power product");
        fileWriter.addGlobalAttribute("institution", "King's College London, Brockmann Consult GmbH");
        fileWriter.addGlobalAttribute("source", "ESA Sentinel-3 A+B SLSTR FRP");
        fileWriter.addGlobalAttribute("history", "Created on " + dateStringNow);
        fileWriter.addGlobalAttribute("references", "See https://climate.copernicus.eu/");
        fileWriter.addGlobalAttribute("tracking_id", UUID.randomUUID().toString());
        fileWriter.addGlobalAttribute("Conventions", "CF-1.7");
        fileWriter.addGlobalAttribute("summary", "TODO!");
        fileWriter.addGlobalAttribute("keywords", "Fire Radiative Power, Climate Change, ESA, C3S, GCOS");
        fileWriter.addGlobalAttribute("id", product.getName());
        fileWriter.addGlobalAttribute("naming_authority", "org.esa-cci");
        fileWriter.addGlobalAttribute("keywords_vocabulary", "NASA Global Change Master Directory (GCMD) Science keywords");
        fileWriter.addGlobalAttribute("cdm_data_type", "Grid");
        fileWriter.addGlobalAttribute("comment", "These data were produced as part of the Copernicus Climate Change Service programme.");
        fileWriter.addGlobalAttribute("date_created", dateStringNow);
        fileWriter.addGlobalAttribute("creator_name", "Brockmann Consult GmbH");
        fileWriter.addGlobalAttribute("creator_url", "https://www.brockmann-consult.de");
        fileWriter.addGlobalAttribute("creator_email", "martin.boettcher@brockmann-consult.de");
        fileWriter.addGlobalAttribute("contact", "http://copernicus-support.ecmwf.int");
        fileWriter.addGlobalAttribute("project", "EC C3S Fire Radiative Power");
        fileWriter.addGlobalAttribute("geospatial_lat_min", "-90");
        fileWriter.addGlobalAttribute("geospatial_lat_max", "90");
        fileWriter.addGlobalAttribute("geospatial_lon_min", "-180");
        fileWriter.addGlobalAttribute("geospatial_lon_max", "180");
        fileWriter.addGlobalAttribute("geospatial_vertical_min", "0");
        fileWriter.addGlobalAttribute("geospatial_vertical_max", "0");
        fileWriter.addGlobalAttribute("standard_name_vocabulary", "NetCDF Climate and Forecast (CF) Metadata Convention");
        fileWriter.addGlobalAttribute("platform", "Sentinel-3");
        fileWriter.addGlobalAttribute("sensor", "SLSTR");
        fileWriter.addGlobalAttribute("geospatial_lon_units", "degrees_east");
        fileWriter.addGlobalAttribute("geospatial_lat_units", "degrees_north");
    }

    static void addAxesAndBoundsVariables(NetcdfFileWriter fileWriter) {
        Variable variable = fileWriter.addVariable("lon", DataType.FLOAT, "lon");
        variable.addAttribute(new Attribute("units", "degrees_east"));
        variable.addAttribute(new Attribute("standard_name", "longitude"));
        variable.addAttribute(new Attribute("long_name", "longitude"));
        variable.addAttribute(new Attribute("bounds", "lon_bounds"));

        variable = fileWriter.addVariable("lat", DataType.FLOAT, "lat");
        variable.addAttribute(new Attribute("units", "degrees_north"));
        variable.addAttribute(new Attribute("standard_name", "latitude"));
        variable.addAttribute(new Attribute("long_name", "latitude"));
        variable.addAttribute(new Attribute("bounds", "lat_bounds"));

        // @todo 2 tb/** is this standard? Double seems to be a too large datatype 2020-09-28
        variable = fileWriter.addVariable("time", DataType.DOUBLE, "time");
        variable.addAttribute(new Attribute("units", "days since 1970-01-01 00:00:00"));
        variable.addAttribute(new Attribute("standard_name", "time"));
        variable.addAttribute(new Attribute("long_name", "time"));
        variable.addAttribute(new Attribute("bounds", "time_bounds"));
        variable.addAttribute(new Attribute("calendar", "standard"));

        fileWriter.addVariable("lon_bounds", DataType.FLOAT, "lon bounds");
        fileWriter.addVariable("lat_bounds", DataType.FLOAT, "lat bounds");
        fileWriter.addVariable("time_bounds", DataType.FLOAT, "time bounds");
    }

    @Override
    public void writeBandRasterData(Band sourceBand, int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight, ProductData sourceBuffer, ProgressMonitor pm) throws IOException {

    }

    @Override
    protected void writeProductNodesImpl() throws IOException {
        final String filePath = getOutputPath(getOutput());

        final Nc4Chunking chunking = Nc4ChunkingDefault.factory(Nc4Chunking.Strategy.standard, 5, true);
        fileWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf4, filePath, chunking);

        final Product sourceProduct = getSourceProduct();
        addDimensions(fileWriter, sourceProduct);
        addGlobalMetadata(fileWriter, sourceProduct);
        addAxesAndBoundsVariables(fileWriter);
        // add variables from product
        // add weighted FRP variables

        fileWriter.create();

        // write time, lon, lat bounds
        // write time lon, lat axis variables
    }

    @Override
    public void flush() throws IOException {
        if (fileWriter != null) {
            fileWriter.flush();
        }
    }

    @Override
    public void close() throws IOException {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        return false;
    }

    @Override
    public boolean isIncrementalMode() {
        return false;
    }

    @Override
    public void setIncrementalMode(boolean enabled) {

    }

    @Override
    public void deleteOutput() throws IOException {

    }

    @Override
    public void removeBand(Band band) {

    }

    @Override
    public void setFormatName(String formatName) {

    }
}
