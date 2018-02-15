package com.bc.calvalus.processing.fire.format.grid.s2;

import com.bc.calvalus.processing.beam.CalvalusProductIO;
import com.bc.calvalus.processing.fire.format.grid.AbstractGridMapper;
import com.bc.calvalus.processing.fire.format.grid.GridCell;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Product;

import java.io.File;
import java.io.IOException;
import java.time.Year;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipFile;

import static com.bc.calvalus.processing.fire.format.grid.s2.S2FireGridDataSource.STEP;

/**
 * Runs the fire S2 formatting grid mapper.
 *
 * @author thomas
 */
public class S2GridMapper extends AbstractGridMapper {

    private static final float S2_PIXEL_AREA = 400.0f;

    protected S2GridMapper() {
        super(STEP * 4, STEP * 4);
    }

    @Override
    public void run(Context context) throws IOException, InterruptedException {

        int year = Integer.parseInt(context.getConfiguration().get("calvalus.year"));
        int month = Integer.parseInt(context.getConfiguration().get("calvalus.month"));

        CombineFileSplit inputSplit = (CombineFileSplit) context.getInputSplit();
        Path[] paths = inputSplit.getPaths();
        if (paths.length == 1) {
            return;
        }
        LOG.info("paths=" + Arrays.toString(paths));

        List<ZipFile> geoLookupTables = new ArrayList<>();
        String twoDegTile = paths[paths.length - 1].getName();

        List<Product> sourceProducts = new ArrayList<>();
        List<Product> lcProducts = new ArrayList<>();
        for (int i = 0; i < paths.length - 1; i++) {
            String utmTile = paths[i].getName().substring(4, 9);
            String localGeoLookupFileName = twoDegTile + "-" + utmTile + ".zip";
            Path geoLookup = new Path("hdfs://calvalus/calvalus/projects/fire/aux/s2-geolookup/" + localGeoLookupFileName);
            if (!new File(localGeoLookupFileName).exists()) {
                File localGeoLookup = CalvalusProductIO.copyFileToLocal(geoLookup, context.getConfiguration());
                geoLookupTables.add(new ZipFile(localGeoLookup));
            }
            File sourceProductFile = CalvalusProductIO.copyFileToLocal(paths[i], context.getConfiguration());
            sourceProducts.add(ProductIO.readProduct(sourceProductFile));
            Path lcPath = new Path("hdfs://calvalus/calvalus/projects/fire/aux/lc4s2/lc-2010-T" + utmTile + ".nc");
            File lcFile = new File(".", lcPath.getName());
            if (!lcFile.exists()) {
                CalvalusProductIO.copyFileToLocal(lcPath, lcFile, context.getConfiguration());
            }
            Product lcProduct = ProductIO.readProduct(lcFile);
            lcProducts.add(lcProduct);
            LOG.info(String.format("Loaded %02.2f%% of input products", (i + 1) * 100 / (float) (paths.length - 1)));
        }


        int doyFirstOfMonth = Year.of(year).atMonth(month).atDay(1).getDayOfYear();
        int doyLastOfMonth = Year.of(year).atMonth(month).atDay(Year.of(year).atMonth(month).lengthOfMonth()).getDayOfYear();
        int doyFirstHalf = Year.of(year).atMonth(month).atDay(7).getDayOfYear();
        int doySecondHalf = Year.of(year).atMonth(month).atDay(22).getDayOfYear();

        S2FireGridDataSource dataSource = new S2FireGridDataSource(twoDegTile, sourceProducts.toArray(new Product[0]), lcProducts.toArray(new Product[0]), geoLookupTables);
        dataSource.setDoyFirstOfMonth(doyFirstOfMonth);
        dataSource.setDoyLastOfMonth(doyLastOfMonth);
        dataSource.setDoyFirstHalf(doyFirstHalf);
        dataSource.setDoySecondHalf(doySecondHalf);

        setDataSource(dataSource);
        GridCell gridCell = computeGridCell(year, month);

        context.write(new Text(String.format("%d-%02d-%s", year, month, twoDegTile)), gridCell);
    }

    @Override
    protected boolean maskUnmappablePixels() {
        return false;
    }

    @Override
    protected void validate(float burnableFraction, List<float[]> baInLcFirst, List<float[]> baInLcSecond, int targetPixelIndex, double area) {
        double lcAreaSum = 0.0F;
        for (int i = 0; i < baInLcFirst.size(); i++) {
            float[] firstBaValues = baInLcFirst.get(i);
            float[] secondBaValues = baInLcSecond.get(i);
            lcAreaSum += firstBaValues[targetPixelIndex];
            lcAreaSum += secondBaValues[targetPixelIndex];
        }
        float lcAreaSumFraction = getFraction(lcAreaSum, area);
        if (Math.abs(lcAreaSumFraction - burnableFraction) > lcAreaSumFraction * 0.05) {
            throw new IllegalStateException("fraction of burned pixels in LC classes (" + lcAreaSumFraction + ") > burnable fraction (" + burnableFraction + ") at target pixel " + targetPixelIndex + "!");
        }
    }

    @Override
    protected float getErrorPerPixel(double[] probabilityOfBurn, int num) {
        /*
            p is array of burned_probability in cell c
            var(C) = (p (1-p)).sum()
            standard_error(c) = sqrt(var(c) *(n/(n-1))
            sum(C) = p.sum()
        */

        double var_c = 0.0;
        double sum_c = 0.0;
        int count = 0;
        for (double p : probabilityOfBurn) {
            if (Double.isNaN(p)) {
                continue;
            }
            if (p > 1) {
                // no-data/cloud/water
                continue;
            }
            if (p < 0) {
                throw new IllegalStateException("p < 0");
            }
            var_c += p * (1.0 - p);
            sum_c += p;
            count++;
        }
        if (count == 0) {
            return 0;
        }
        if (count == 1) {
            return 1;
        }

        return (float) Math.sqrt(var_c * (count / (count - 1.0))) * S2_PIXEL_AREA;
    }

    @Override
    protected void predict(float[] ba, double[] areas, float[] originalErrors) {
    }
}