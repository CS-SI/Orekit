/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.models.earth;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.StringTokenizer;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.EGMFormatReader;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.models.earth.GeoMagneticFieldFactory.FieldModel;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;

public class GeoMagneticFieldTest {

    /** maximum degree and order used in testing {@link Geoid}. */
    @SuppressWarnings("javadoc")
    private static final int maxOrder = 360, maxDegree = 360;
    /** The WGS84 reference ellipsoid. */
    private static ReferenceEllipsoid WGS84 = new ReferenceEllipsoid(
            6378137.00, 1 / 298.257223563, FramesFactory.getGCRF(),
            3.986004418e14, 7292115e-11);

    /**
     * The potential to use in {@link #getComponent()}. Set in {@link #setUpBefore()}.
     */
    private static NormalizedSphericalHarmonicsProvider potential;

    // test results for test values provided as part of the WMM2015 Report
    private final double[][] wmmTestValues = {
        // Date  Alt  Lat  Lon        X        Y         Z        H        F       I      D
        //        km  deg  deg       nT       nT        nT       nT       nT     deg    deg
        {2015.0,   0,  80,   0,  6627.1,  -445.9,  54432.3,  6642.1, 54836.0,  83.04, -3.85},
        {2015.0,   0,   0, 120, 39518.2,   392.9, -11252.4, 39520.2, 41090.9, -15.89,  0.57},
        {2015.0,   0, -80, 240,  5797.3, 15761.1, -52919.1, 16793.5, 55519.8, -72.39, 69.81},
        {2015.0, 100,  80,   0,  6314.3,  -471.6,  52269.8,  6331.9, 52652.0,  83.09, -4.27},
        {2015.0, 100,   0, 120, 37535.6,   364.4, -10773.4, 37537.3, 39052.7, -16.01,  0.56},
        {2015.0, 100, -80, 240,  5613.1, 14791.5, -50378.6, 15820.7, 52804.4, -72.57, 69.22},
        {2017.5,   0,  80,   0,  6599.4,  -317.1,  54459.2,  6607.0, 54858.5,  83.08, -2.75},
        {2017.5,   0,   0, 120, 39571.4,   222.5, -11030.1, 39572.0, 41080.5, -15.57,  0.32},
        {2017.5,   0, -80, 240,  5873.8, 15781.4, -52687.9, 16839.1, 55313.4, -72.28, 69.58},
        {2017.5, 100,  80,   0,  6290.5,  -348.5,  52292.7,  6300.1, 52670.9,  83.13, -3.17},
        {2017.5, 100,   0, 120, 37585.5,   209.5, -10564.2, 37586.1, 39042.5, -15.70,  0.32},
        {2017.5, 100, -80, 240,  5683.5, 14808.8, -50163.0, 15862.0, 52611.1, -72.45, 69.00}
    };

    // test results for test values provided as part of the WMM2015 Report
    // the results for the IGRF12 model have been obtained from the NOAA
    // online calculator: http://www.ngdc.noaa.gov/geomag-web/#igrfwmm
    private final double[][] igrfTestValues = {
        // Date  Alt  Lat  Lon        X        Y         Z        H        F       I      D
        //        km  deg  deg       nT       nT        nT       nT       nT     deg    deg
        {2015.0,   0,  80,   0,  6630.9,  -447.2,  54434.5,  6645.9, 54838.7,  83.039, -3.858},
        {2015.0,   0,   0, 120, 39519.3,   388.6, -11251.7, 39521.3, 41091.7, -15.891,  0.563},
        {2015.0,   0, -80, 240,  5808.8, 15754.8, -52945.5, 16791.5, 55544.4, -72.403, 69.761},
        {2015.0, 100,  80,   0,  6317.2,  -472.6,  52272.0,  6334.9, 52654.5,  83.090, -4.278},
        {2015.0, 100,   0, 120, 37536.9,   361.2, -10773.1, 37538.6, 39053.9, -16.012,  0.551},
        {2015.0, 100, -80, 240,  5622.8, 14786.8, -50401.4, 15819.8, 52825.8, -72.574, 69.180},
        {2017.5,   0,  80,   0,  6601.0,  -316.4,  54455.5,  6608.5, 54855.0,  83.080, -2.744},
        {2017.5,   0,   0, 120, 39568.1,   225.0, -11041.4, 39568.7, 41080.3, -15.591,  0.325},
        {2017.5,   0, -80, 240,  5894.7, 15768.1, -52696.8, 16833.9, 55320.2, -72.283, 69.502},
        {2017.5, 100,  80,   0,  6291.6,  -347.2,  52289.9,  6301.2, 52668.2,  83.128, -3.158},
        {2017.5, 100,   0, 120, 37583.0,   212.3, -10575.1, 37583.6, 39043.0, -15.715,  0.323},
        {2017.5, 100, -80, 240,  5702.0, 14797.8, -50170.0, 15858.3, 52616.7, -72.458, 68.927}
    };

    /**
     * load orekit data and gravity field.
     *
     * @throws Exception on error.
     */
    @BeforeClass
    public static void setUpBefore() throws Exception {
        Utils.setDataRoot("earth:geoid:regular-data");
        GravityFieldFactory.clearPotentialCoefficientsReaders();
        GravityFieldFactory.addPotentialCoefficientsReader(new EGMFormatReader("egm96", false));
        potential = GravityFieldFactory.getConstantNormalizedProvider(maxDegree, maxOrder);
    }

    @Test
    public void testInterpolationYYY5() throws OrekitException {
        double decimalYear = GeoMagneticField.getDecimalYear(1, 1, 2005);
        GeoMagneticField field = GeoMagneticFieldFactory.getIGRF(decimalYear);
        GeoMagneticElements e = field.calculateField(1.2, 0.7, -2.5);
        Assert.assertEquals(-6.0032, e.getDeclination(), 1.0e-4);

        decimalYear = GeoMagneticField.getDecimalYear(2, 1, 2005);
        field = GeoMagneticFieldFactory.getIGRF(decimalYear);
        e = field.calculateField(1.2, 0.7, -2.5);
        Assert.assertEquals(-6.0029, e.getDeclination(), 1.0e-4);
    }

    @Test
    public void testInterpolationAtEndOfEpoch() throws OrekitException {
        double decimalYear = GeoMagneticField.getDecimalYear(31, 12, 2009);
        GeoMagneticField field1 = GeoMagneticFieldFactory.getIGRF(decimalYear);
        GeoMagneticField field2 = GeoMagneticFieldFactory.getIGRF(2010.0);

        Assert.assertNotEquals(field1.getEpoch(), field2.getEpoch());

        GeoMagneticElements e1 = field1.calculateField(0, 0, 0);
        Assert.assertEquals(-6.1068, e1.getDeclination(), 1.0e-4);

        GeoMagneticElements e2 = field2.calculateField(0, 0, 0);
        Assert.assertEquals(-6.1064, e2.getDeclination(), 1.0e-4);
    }

    @Test
    public void testInterpolationAtEndOfValidity() throws OrekitException {
        double decimalYear = GeoMagneticField.getDecimalYear(1, 1, 2020);
        GeoMagneticField field = GeoMagneticFieldFactory.getIGRF(decimalYear);

        GeoMagneticElements e = field.calculateField(0, 0, 0);
        Assert.assertEquals(-4.7446, e.getDeclination(), 1.0e-4);
    }

    @Test
    public void testContinuityAtPole() throws OrekitException {
        double decimalYear = GeoMagneticField.getDecimalYear(1, 1, 2020);
        GeoMagneticField field = GeoMagneticFieldFactory.getIGRF(decimalYear);

        GeoMagneticElements eClose = field.calculateField(89.999999, 0, 0);
        GeoMagneticElements ePole  = field.calculateField(90.0,      0, 0);
        Assert.assertEquals("" + (eClose.getDeclination()-         ePole.getDeclination()),         eClose.getDeclination(),         ePole.getDeclination(),         7.0e-7);
        Assert.assertEquals("" + (eClose.getInclination()-         ePole.getInclination()),         eClose.getInclination(),         ePole.getInclination(),         3.0e-7);
        Assert.assertEquals("" + (eClose.getTotalIntensity()-      ePole.getTotalIntensity()),      eClose.getTotalIntensity(),      ePole.getTotalIntensity(),      2.0e-4);
        Assert.assertEquals("" + (eClose.getHorizontalIntensity()- ePole.getHorizontalIntensity()), eClose.getHorizontalIntensity(), ePole.getHorizontalIntensity(), 3.0e-4);
    }

    @Test(expected=OrekitException.class)
    public void testTransformationOutsideValidityPeriod() throws OrekitException {
        double decimalYear = GeoMagneticField.getDecimalYear(10, 1, 2020);
        @SuppressWarnings("unused")
        GeoMagneticField field = GeoMagneticFieldFactory.getIGRF(decimalYear);
    }

    @Test
    public void testWMM() throws Exception {
        // test values from sample coordinate file
        // provided as part of the geomag 7.0 distribution available at
        // http://www.ngdc.noaa.gov/IAGA/vmod/igrf.html
        // modification: the julian day calculation of geomag is slightly different
        // to the one from the WMM code, we use the WMM convention thus the outputs
        // have been adapted.
        runSampleFile(FieldModel.WMM, "sample_coords.txt", "sample_out_WMM2015.txt");

        final double eps = 1e-1;
        final double degreeEps = 1e-2;
        for (int i = 0; i < wmmTestValues.length; i++) {
            final GeoMagneticField model = GeoMagneticFieldFactory.getWMM(wmmTestValues[i][0]);
            final GeoMagneticElements result = model.calculateField(wmmTestValues[i][2],
                                                                    wmmTestValues[i][3],
                                                                    wmmTestValues[i][1]);

            // X
            Assert.assertEquals(wmmTestValues[i][4], result.getFieldVector().getX(), eps);
            // Y
            Assert.assertEquals(wmmTestValues[i][5], result.getFieldVector().getY(), eps);
            // Z
            Assert.assertEquals(wmmTestValues[i][6], result.getFieldVector().getZ(), eps);
            // H
            Assert.assertEquals(wmmTestValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(wmmTestValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(wmmTestValues[i][9], result.getInclination(), degreeEps);
            // declination
            Assert.assertEquals(wmmTestValues[i][10], result.getDeclination(), degreeEps);
        }
    }

    @Test
    public void testWMMWithHeightAboveMSL() throws Exception {
        // test results for test values provided as part of the WMM2015 Report
        // using height above MSL instead of height above ellipsoid
        // the results have been obtained from the NOAA online calculator:
        // http://www.ngdc.noaa.gov/geomag-web/#igrfwmm
        final double[][] testValues = {
            // Date  Alt  Lat  Lon        X        Y         Z        H        F       I      D
            //        km  deg  deg       nT       nT        nT       nT       nT     deg    deg
            {2015.0, 100,  80,   0,  6314.2,  -471.6,  52269.1,  6331.8, 52651.2,  83.093, -4.271},
            {2015.0, 100,   0, 120, 37534.4,   364.3, -10773.1, 37536.2, 39051.6, -16.013,  0.556},
            {2015.0, 100, -80, 240,  5613.2, 14791.9, -50379.6, 15821.1, 52805.4, -72.565, 69.219}
        };

        final Geoid geoid = new Geoid(potential, WGS84);

        final double eps = 1e-1;
        final double degreeEps = 1e-2;
        for (int i = 0; i < testValues.length; i++) {
            final AbsoluteDate date = new AbsoluteDate(2015, 1, 1, TimeScalesFactory.getUTC());
            final GeoMagneticField model = GeoMagneticFieldFactory.getWMM(testValues[i][0]);
            final double undulation = geoid.getUndulation(FastMath.toRadians(testValues[i][2]),
                                                          FastMath.toRadians(testValues[i][3]),
                                                          date);
            final GeoMagneticElements result = model.calculateField(testValues[i][2],
                                                                    testValues[i][3],
                                                                    testValues[i][1] + undulation/1000d);

            // X
            Assert.assertEquals(testValues[i][4], result.getFieldVector().getX(), eps);
            // Y
            Assert.assertEquals(testValues[i][5], result.getFieldVector().getY(), eps);
            // Z
            Assert.assertEquals(testValues[i][6], result.getFieldVector().getZ(), eps);
            // H
            Assert.assertEquals(testValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(testValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(testValues[i][9], result.getInclination(), degreeEps);
            // declination
            Assert.assertEquals(testValues[i][10], result.getDeclination(), degreeEps);
        }
    }

    @Test
    public void testIGRF() throws Exception {
        // test values from sample coordinate file
        // provided as part of the geomag 7.0 distribution available at
        // http://www.ngdc.noaa.gov/IAGA/vmod/igrf.html
        // modification: the julian day calculation of geomag is slightly different
        // to the one from the WMM code, we use the WMM convention thus the outputs
        // have been adapted.
        runSampleFile(FieldModel.IGRF, "sample_coords.txt", "sample_out_IGRF12.txt");

        final double eps = 1e-1;
        final double degreeEps = 1e-2;
        for (int i = 0; i < igrfTestValues.length; i++) {
            final GeoMagneticField model = GeoMagneticFieldFactory.getIGRF(igrfTestValues[i][0]);
            final GeoMagneticElements result = model.calculateField(igrfTestValues[i][2],
                                                                    igrfTestValues[i][3],
                                                                    igrfTestValues[i][1]);

            final Vector3D b = result.getFieldVector();

            // X
            Assert.assertEquals(igrfTestValues[i][4], b.getX(), eps);
            // Y
            Assert.assertEquals(igrfTestValues[i][5], b.getY(), eps);
            // Z
            Assert.assertEquals(igrfTestValues[i][6], b.getZ(), eps);
            // H
            Assert.assertEquals(igrfTestValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(igrfTestValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(igrfTestValues[i][9], result.getInclination(), degreeEps);
            // declination
            Assert.assertEquals(igrfTestValues[i][10], result.getDeclination(), degreeEps);
        }
    }

    @Test(expected=OrekitException.class)
    public void testUnsupportedTransform() throws Exception {
        final GeoMagneticField model = GeoMagneticFieldFactory.getIGRF(1910);

        // the IGRF model of 1910 does not have secular variation, thus time transformation is not supported
        model.transformModel(1950);
    }

    @Test(expected=OrekitException.class)
    public void testOutsideValidityTransform() throws Exception {
        final GeoMagneticField model1 = GeoMagneticFieldFactory.getIGRF(2005);
        final GeoMagneticField model2 = GeoMagneticFieldFactory.getIGRF(2010);

        // the interpolation transformation is only allowed between 2005 and 2010
        model1.transformModel(model2, 2012);
    }

    @Test
    public void testValidTransform() throws Exception {
        final GeoMagneticField model = GeoMagneticFieldFactory.getWMM(2015);

        Assert.assertTrue(model.supportsTimeTransform());

        final GeoMagneticField transformedModel = model.transformModel(2017);

        Assert.assertEquals(2015, transformedModel.validFrom(), 1e0);
        Assert.assertEquals(2020, transformedModel.validTo(), 1e0);
        Assert.assertEquals(2017, transformedModel.getEpoch(), 1e0);
    }

    @Test
    public void testLoadOriginalWMMModel() throws Exception {
        GeoMagneticModelLoader loader = new GeoMagneticModelLoader();

        InputStream input = getResource("WMM2015.COF");
        loader.loadData(input, "WMM2015.COF");

        Collection<GeoMagneticField> models = loader.getModels();
        Assert.assertNotNull(models);
        Assert.assertEquals(1, models.size());

        GeoMagneticField wmmModel = models.iterator().next();
        Assert.assertEquals("WMM-2015", wmmModel.getModelName());
        Assert.assertEquals(2015, wmmModel.getEpoch(), 1e-9);

        final double eps = 1e-1;
        final double degreeEps = 1e-2;
        for (int i = 0; i < wmmTestValues.length; i++) {
            if (wmmTestValues[i][0] != wmmModel.getEpoch()) {
                continue;
            }
            final GeoMagneticElements result = wmmModel.calculateField(wmmTestValues[i][2],
                                                                       wmmTestValues[i][3],
                                                                       wmmTestValues[i][1]);

            // X
            Assert.assertEquals(wmmTestValues[i][4], result.getFieldVector().getX(), eps);
            // Y
            Assert.assertEquals(wmmTestValues[i][5], result.getFieldVector().getY(), eps);
            // Z
            Assert.assertEquals(wmmTestValues[i][6], result.getFieldVector().getZ(), eps);
            // H
            Assert.assertEquals(wmmTestValues[i][7], result.getHorizontalIntensity(), eps);
            // F
            Assert.assertEquals(wmmTestValues[i][8], result.getTotalIntensity(), eps);
            // inclination
            Assert.assertEquals(wmmTestValues[i][9], result.getInclination(), degreeEps);
            // declination
            Assert.assertEquals(wmmTestValues[i][10], result.getDeclination(), degreeEps);
        }
    }

    public void runSampleFile(final FieldModel type, final String inputFile, final String outputFile)
        throws Exception {

        final BufferedReader inReader = new BufferedReader(new InputStreamReader(getResource(inputFile)));
        final BufferedReader outReader = new BufferedReader(new InputStreamReader(getResource(outputFile)));

        // read header line
        outReader.readLine();

        String line = null;
        while ((line = inReader.readLine()) != null) {
            if (line.trim().length() == 0) {
                break;
            }

            final StringTokenizer st = new StringTokenizer(line);

            final double year = getYear(st.nextToken());
            final String heightType = st.nextToken();
            final String heightStr = st.nextToken();
            final double lat = getLatLon(st.nextToken());
            final double lon = getLatLon(st.nextToken());

            final GeoMagneticField field = GeoMagneticFieldFactory.getField(type, year);

            double height = Double.valueOf(heightStr.substring(1));
            if (heightStr.startsWith("M")) {
                // convert from m to km
                height /= 1000d;
            } else if (heightStr.startsWith("F")) {
                // convert from feet to km
                height *= 3.048e-4;
            }

            final GeoMagneticElements ge = field.calculateField(lat, lon, height);
            final String validateLine = outReader.readLine();
            // geocentric altitude is not yet supported, ignore by now
            if (!heightType.startsWith("C")) {
                validate(ge, validateLine);
            }

            String geString = ge.toString();
            Assert.assertNotNull(geString);
            Assert.assertFalse(geString.isEmpty());
        }
    }

    private double getYear(final String yearStr) {

        if (yearStr.contains(",")) {
            final StringTokenizer st = new StringTokenizer(yearStr, ",");
            final String y = st.nextToken();
            final String m = st.nextToken();
            final String d = st.nextToken();
            return GeoMagneticField.getDecimalYear(Integer.valueOf(d), Integer.valueOf(m), Integer.valueOf(y));
        } else {
            return Double.valueOf(yearStr);
        }
    }

    private double getLatLon(final String str) {

        if (str.contains(",")) {
            final StringTokenizer st = new StringTokenizer(str, ",");
            final int d = Integer.valueOf(st.nextToken());
            final int m = Integer.valueOf(st.nextToken());
            int s = 0;
            if (st.hasMoreTokens()) {
                s = Integer.valueOf(st.nextToken());
            }
            double deg = FastMath.abs(d) + m / 60d + s / 3600d;
            if (d < 0) {
                deg = -deg;
            }
            return deg;
        } else {
            return Double.valueOf(str);
        }
    }

    private double getDegree(final String degree, final String minute) {
        double result = Double.valueOf(degree.substring(0, degree.length() - 1));
        final double min = Double.valueOf(minute.substring(0, minute.length() - 1)) / 60d;
        result += (result < 0) ? -min : min;
        return result;
    }

    @SuppressWarnings("unused")
    private void validate(final GeoMagneticElements ge, final String outputLine)
        throws Exception {

        final StringTokenizer st = new StringTokenizer(outputLine);

        final double year = getYear(st.nextToken());

        final String coord = st.nextToken();
        final String heightStr = st.nextToken();
        final String latStr = st.nextToken();
        final String lonStr = st.nextToken();

        final double dec = getDegree(st.nextToken(), st.nextToken());
        final double inc = getDegree(st.nextToken(), st.nextToken());

        final double h = Double.valueOf(st.nextToken());
        final double x = Double.valueOf(st.nextToken());
        final double y = Double.valueOf(st.nextToken());
        final double z = Double.valueOf(st.nextToken());
        final double f = Double.valueOf(st.nextToken());

        final double eps = 1e-1;
        Assert.assertEquals(h, ge.getHorizontalIntensity(), eps);
        Assert.assertEquals(f, ge.getTotalIntensity(), eps);
        Assert.assertEquals(x, ge.getFieldVector().getX(), eps);
        Assert.assertEquals(y, ge.getFieldVector().getY(), eps);
        Assert.assertEquals(z, ge.getFieldVector().getZ(), eps);
        Assert.assertEquals(dec, ge.getDeclination(), eps);
        Assert.assertEquals(inc, ge.getInclination(), eps);
    }

    private InputStream getResource(final String name) throws FileNotFoundException {
        // the data path has multiple components, the resources are in the first one
        final String separator = System.getProperty("path.separator");
        final String dataPath = System.getProperty(DataProvidersManager.OREKIT_DATA_PATH).split(separator)[0];
        return new FileInputStream(new File(dataPath, name));
    }

}
