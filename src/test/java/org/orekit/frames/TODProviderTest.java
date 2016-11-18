/* Copyright 2002-2016 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.frames;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.PVCoordinates;


public class TODProviderTest {

    @Test
    public void testRotationRate() throws OrekitException {
        TransformProvider provider =
                new InterpolatingTransformProvider(new TODProvider(IERSConventions.IERS_1996, null),
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        AbsoluteDate tMin = new AbsoluteDate(2035, 3, 2, 15, 58, 59, TimeScalesFactory.getUTC());
        double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assert.assertEquals(6.4e-14, minRate, 1.0e-15);
        AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 14, 18, 9, TimeScalesFactory.getUTC());
        double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assert.assertEquals(1.4e-11, maxRate, 1.0e-12);
    }

    @Test
    public void testAASReferenceLEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53098, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53099, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53100, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53101, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53102, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53103, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53104, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN },
                             { 53105, -0.4399619, 0.0015563, -0.140682, 0.333309, -0.052195, -0.003875, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 04, 06),
                                           new TimeComponents(07, 51, 28.386009),
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getMOD(IERSConventions.IERS_1996).
                getTransformTo(FramesFactory.getTOD(IERSConventions.IERS_1996, true), t0);
        Transform ff = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        //TOD iau76
        PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(5094514.7804, 6127366.4612, 6380344.5328),
                              new Vector3D(-4746.088567, 786.077222, 5531.931288));
        //MOD iau76
        PVCoordinates pvMODiau76WithoutNutCorr =
            new PVCoordinates(new Vector3D(5094029.0167, 6127870.9363, 6380247.8885),
                              new Vector3D(-4746.262495, 786.014149, 5531.791025));
        //MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(5094028.3745, 6127870.8164, 6380248.5164),
                              new Vector3D(-4746.263052, 786.014045, 5531.790562));

        // it seems the induced effect of pole nutation correction δΔψ on the equation of the equinoxes
        // was not taken into account in the reference paper, so we fix it here for the test
        final double dDeltaPsi =
                FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true).getEquinoxNutationCorrection(t0)[0];
        final double epsilonA = IERSConventions.IERS_1996.getMeanObliquityFunction().value(t0);
        final Transform fix =
                new Transform(t0, new Rotation(Vector3D.PLUS_K,
                                               dDeltaPsi * FastMath.cos(epsilonA),
                                               RotationConvention.FRAME_TRANSFORM));

        checkPV(pvTODiau76, fix.transformPVCoordinates(tt.transformPVCoordinates(pvMODiau76)), 1.13e-3, 5.3e-5);
        checkPV(pvTODiau76, ff.transformPVCoordinates(pvMODiau76WithoutNutCorr), 1.07e-3, 5.3e-5);

    }

    @Test
    public void testAASReferenceGEO() throws OrekitException {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, new double[][] {
                             { 53153, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53154, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53155, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53156, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53157, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53158, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53159, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN },
                             { 53160, -0.4709050,  0.0000000, -0.083853,  0.467217, -0.053614, -0.004494, Double.NaN, Double.NaN }
                         }));
        AbsoluteDate t0 = new AbsoluteDate(new DateComponents(2004, 06, 01),
                                           TimeComponents.H00,
                                           TimeScalesFactory.getUTC());

        Transform tt = FramesFactory.getMOD(IERSConventions.IERS_1996).
                getTransformTo(FramesFactory.getTOD(IERSConventions.IERS_1996, true), t0);
        Transform ff = FramesFactory.getMOD(false).getTransformTo(FramesFactory.getTOD(false), t0);

        // TOD iau76
        PVCoordinates pvTODiau76 =
            new PVCoordinates(new Vector3D(-40577427.7501, -11500096.1306, 10293.2583),
                              new Vector3D(837.552338, -2957.524176, -0.928772));
        // MOD iau76
        PVCoordinates pvMODiau76WithoutNutCorr =
            new PVCoordinates(new Vector3D(-40576822.6385, -11502231.5013, 9738.2304),
                              new Vector3D(837.708020, -2957.480118, -0.814275));

        // MOD iau76
        PVCoordinates pvMODiau76 =
            new PVCoordinates(new Vector3D(-40576822.6395, -11502231.5015, 9733.7842),
                              new Vector3D(837.708020, -2957.480117, -0.814253));


        // it seems the induced effect of pole nutation correction δΔψ on the equation of the equinoxes
        // was not taken into account in the reference paper, so we fix it here for the test
        final double dDeltaPsi =
                FramesFactory.getEOPHistory(IERSConventions.IERS_1996, true).getEquinoxNutationCorrection(t0)[0];
        final double epsilonA = IERSConventions.IERS_1996.getMeanObliquityFunction().value(t0);
        final Transform fix =
                new Transform(t0, new Rotation(Vector3D.PLUS_K,
                                               dDeltaPsi * FastMath.cos(epsilonA),
                                               RotationConvention.FRAME_TRANSFORM));

        checkPV(pvTODiau76, fix.transformPVCoordinates(tt.transformPVCoordinates(pvMODiau76)), 4.86e-4, 6.2e-5);
        checkPV(pvTODiau76, ff.transformPVCoordinates(pvMODiau76WithoutNutCorr), 4.87e-4, 6.31e-5);

    }

    @Test
    public void testInterpolationAccuracyWithEOP() throws OrekitException, FileNotFoundException {

        // max interpolation error observed on a one month period with 60 seconds step
        //
        // number of sample points    time between sample points    max error
        //        6                          86400s /  8 =  3h       19.56e-12 rad
        //        6                          86400s / 12 =  2h       13.02e-12 rad
        //        6                          86400s / 16 =  1h30      9.75e-12 rad
        //        6                          86400s / 20 =  1h12      7.79e-12 rad
        //        6                          86400s / 24 =  1h        6.48e-12 rad
        //        8                          86400s /  8 =  3h       20.91e-12 rad
        //        8                          86400s / 12 =  2h       13.91e-12 rad
        //        8                          86400s / 16 =  1h30     10.42e-12 rad
        //        8                          86400s / 20 =  1h12      8.32e-12 rad
        //        8                          86400s / 24 =  1h        6.92e-12 rad
        //       10                          86400s /  8 =  3h       21.65e-12 rad
        //       10                          86400s / 12 =  2h       14.41e-12 rad
        //       10                          86400s / 16 =  1h30     10.78e-12 rad
        //       10                          86400s / 20 =  1h12      8.61e-12 rad
        //       10                          86400s / 24 =  1h        7.16e-12 rad
        //       12                          86400s /  8 =  3h       22.12e-12 rad
        //       12                          86400s / 12 =  2h       14.72e-12 rad
        //       12                          86400s / 16 =  1h30     11.02e-12 rad
        //       12                          86400s / 20 =  1h12      8.80e-12 rad
        //       12                          86400s / 24 =  1h        7.32e-12 rad
        //
        // looking at error behavior during along the sample show the max error is
        // a peak at 00h00 each day for all curves, which matches the EOP samples
        // points used for correction (eopHistoru is set to non null at construction here).
        // So looking only at max error does not allow to select an interpolation
        // setting as they all fall in a similar 6e-12 to 8e-12 range. Looking at
        // the error behavior between these peaks however shows that there is still
        // some signal if the time interval is between sample points is too large,
        // in order to get only numerical noise, we have to go as far as 1h between
        // the points.
        // We finally select 6 interpolation points separated by 1 hour each
        EOPHistory eopHistory = FramesFactory.getEOPHistory(IERSConventions.IERS_1996, false);
        TransformProvider nonInterpolating = new TODProvider(IERSConventions.IERS_1996, eopHistory);
        final TransformProvider interpolating =
                new InterpolatingTransformProvider(nonInterpolating,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                   6, Constants.JULIAN_DAY / 24,
                                                   OrekitConfiguration.getCacheSlotsNumber(),
                                                   Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

        // the following time range is located around the maximal observed error
        AbsoluteDate start = new AbsoluteDate(2002, 11, 11, 0, 0, 0.0, TimeScalesFactory.getTAI());
        AbsoluteDate end   = new AbsoluteDate(2002, 11, 15, 6, 0, 0.0, TimeScalesFactory.getTAI());
        double maxError = 0.0;
        for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
            final Transform transform =
                    new Transform(date,
                                  interpolating.getTransform(date),
                                  nonInterpolating.getTransform(date).getInverse());
            final double error = transform.getRotation().getAngle();
            maxError = FastMath.max(maxError, error);
        }

        Assert.assertTrue(maxError < 7e-12);

    }

    @Test
    public void testInterpolationAccuracyWithoutEOP() throws OrekitException, FileNotFoundException {

        // max interpolation error observed on a one month period with 60 seconds step
        //
        // number of sample points    time between sample points    max error
        //        5                          86400s /  3 =  8h     3286.90e-15 rad
        //        5                          86400s /  6 =  4h      103.90e-15 rad
        //        5                          86400s /  8 =  3h       24.74e-15 rad
        //        5                          86400s / 12 =  2h        4.00e-15 rad
        //        6                          86400s /  3 =  8h      328.91e-15 rad
        //        6                          86400s /  6 =  4h        5.92e-15 rad
        //        6                          86400s /  8 =  3h        3.95e-15 rad
        //        6                          86400s / 12 =  2h        3.94e-15 rad
        //        8                          86400s /  3 =  8h        5.87e-15 rad
        //        8                          86400s /  6 =  4h        4.73e-15 rad
        //        8                          86400s /  8 =  3h        4.45e-15 rad
        //        8                          86400s / 12 =  2h        3.87e-15 rad
        //       10                          86400s /  3 =  8h        5.29e-15 rad
        //       10                          86400s /  6 =  4h        5.36e-15 rad
        //       10                          86400s /  8 =  3h        5.86e-15 rad
        //       10                          86400s / 12 =  2h        5.76e-15 rad
        //
        //
        // We don't see anymore the peak at 00h00 so this confirms it is related to EOP
        // sampling. All values between 3e-15 and 6e-15 are really equivalent: it is
        // mostly numerical noise. The best settings are 6 or 8 points every 2 or 3 hours.
        // We finally select 6 interpolation points separated by 3 hours each
        TransformProvider nonInterpolating = new TODProvider(IERSConventions.IERS_1996, null);
                final TransformProvider interpolating =
                        new InterpolatingTransformProvider(nonInterpolating,
                                                           CartesianDerivativesFilter.USE_PVA,
                                                           AngularDerivativesFilter.USE_R,
                                                           AbsoluteDate.PAST_INFINITY, AbsoluteDate.FUTURE_INFINITY,
                                                           6, Constants.JULIAN_DAY / 8,
                                                           OrekitConfiguration.getCacheSlotsNumber(),
                                                           Constants.JULIAN_YEAR, 30 * Constants.JULIAN_DAY);

                // the following time range is located around the maximal observed error
                AbsoluteDate start = new AbsoluteDate(2002, 11, 11, 0, 0, 0.0, TimeScalesFactory.getTAI());
                AbsoluteDate end   = new AbsoluteDate(2002, 11, 15, 6, 0, 0.0, TimeScalesFactory.getTAI());
                double maxError = 0.0;
                for (AbsoluteDate date = start; date.compareTo(end) < 0; date = date.shiftedBy(60)) {
                    final Transform transform =
                            new Transform(date,
                                          interpolating.getTransform(date),
                                          nonInterpolating.getTransform(date).getInverse());
                    final double error = transform.getRotation().getAngle();
                    maxError = FastMath.max(maxError, error);
                }

                Assert.assertTrue(maxError < 4.0e-15);

    }

    @Test
    public void testSofaPnm80() throws OrekitException {

        // the reference value has been computed using the March 2012 version of the SOFA library
        // http://www.iausofa.org/2012_0301_C.html, with the following code
        //
        //        double utc1, utc2, tai1, tai2, tt1, tt2, rmatpn[3][3];
        //
        //        // 2004-02-14:00:00:00Z, MJD = 53049, UT1-UTC = -0.4093509
        //        utc1  = DJM0 + 53049.0;
        //        utc2  = 0.0;
        //        iauUtctai(utc1, utc2, &tai1, &tai2);
        //        iauTaitt(tai1, tai2, &tt1, &tt2);
        //
        //        iauPnm80(tt1, tt2, rmatpn);
        //
        //        printf("iauPnm80(%.20g, %.20g, rmatpn)\n"
        //               "  --> %.20g %.20g %.20g\n"
        //               "      %.20g %.20g %.20g\n"
        //               "      %.20g %.20g %.20g\n",
        //               tt1, tt2,
        //               rmatpn[0][0], rmatpn[0][1], rmatpn[0][2],
        //               rmatpn[1][0], rmatpn[1][1], rmatpn[1][2],
        //               rmatpn[2][0], rmatpn[2][1], rmatpn[2][2]);
        //
        // the output of this test reads:
        //        iauNutm80(2453049.5, 0.00074287037037037029902, nut)
        //         --> 0.99999999859236310407 4.8681019508684473249e-05 2.1105264333587349032e-05
        //            -4.8680343021901595118e-05 0.99999999830143670998 -3.205231683600651138e-05
        //            -2.1106824637199909505e-05 3.2051289379386727063e-05 0.99999999926360838565
        //        iauPnm80(2453049.5, 0.00074287037037037029902, rmatpn)
        //         --> 0.99999954755358466674 -0.00087243169070689370777 -0.00037915111913272635073
        //            0.0008724195377896877112 0.99999961892302935418 -3.2217171614061089913e-05
        //            0.00037917908192846747854 3.1886378193416632805e-05 0.99999992760323874741

        // As the iauNutm80 and iauPnm80 do not allow user to specify EOP corrections,
        // the test is done with Predefined.TOD_WITHOUT_EOP_CORRECTIONS.

        AbsoluteDate date = new AbsoluteDate(2004, 2, 14, TimeScalesFactory.getUTC());
        Frame tod  = FramesFactory.getFrame(Predefined.TOD_WITHOUT_EOP_CORRECTIONS);
        checkRotation(new double[][] {
            { 0.99999999859236310407, 4.8681019508684473249e-05, 2.1105264333587349032e-05 },
            { -4.8680343021901595118e-05, 0.99999999830143670998, -3.205231683600651138e-05 },
            { -2.1106824637199909505e-05, 3.2051289379386727063e-05, 0.99999999926360838565    }

        }, tod.getParent().getTransformTo(tod, date), 5.0e-11);
        checkRotation(new double[][] {
            { 0.99999954755358466674,   -0.00087243169070689370777, -0.00037915111913272635073 },
            { 0.0008724195377896877112,  0.99999961892302935418,    -3.2217171614061089913e-05 },
            { 0.00037917908192846747854, 3.1886378193416632805e-05,  0.99999992760323874741    }

        }, tod.getParent().getParent().getTransformTo(tod, date), 5.0e-11);

    }

    @Test
    public void testTOD1976vs2006() throws OrekitException {

        final Frame tod1976 = FramesFactory.getTOD(IERSConventions.IERS_1996, true);
        final Frame tod2006 = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
        for (double dt = 0; dt < 2 * Constants.JULIAN_YEAR; dt += 100 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = tod1976.getTransformTo(tod2006, date).getRotation().getAngle();
            // TOD2006 and TOD2000 are similar to about 65 milli-arcseconds
            // between 2000 and 2002, with EOP corrections taken into account in both cases
            Assert.assertEquals(0.0, delta, 3.2e-7);
        }

    }

    @Test
    public void testTOD2000vs2006() throws OrekitException {

        final Frame tod2000 = FramesFactory.getTOD(IERSConventions.IERS_2003, true);
        final Frame tod2006 = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
        for (double dt = 0; dt < 2 * Constants.JULIAN_YEAR; dt += 100 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = tod2000.getTransformTo(tod2006, date).getRotation().getAngle();
            // TOD2006 and TOD2000 are similar to about 30 micro-arcseconds
            // between 2000 and 2002, with EOP corrections taken into account in both cases
            Assert.assertEquals(0.0, delta, 1.5e-10);
        }

    }

    @Test
    public void testSerialization() throws OrekitException, IOException, ClassNotFoundException {
        TODProvider provider = new TODProvider(IERSConventions.IERS_2010,
                                               FramesFactory.getEOPHistory(IERSConventions.IERS_2010, true));

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream    oos = new ObjectOutputStream(bos);
        oos.writeObject(provider);

        Assert.assertTrue(bos.size() > 280000);
        Assert.assertTrue(bos.size() < 285000);

        ByteArrayInputStream  bis = new ByteArrayInputStream(bos.toByteArray());
        ObjectInputStream     ois = new ObjectInputStream(bis);
        TODProvider deserialized  = (TODProvider) ois.readObject();
        for (int i = 0; i < FastMath.min(100, provider.getEOPHistory().getEntries().size()); ++i) {
            AbsoluteDate date = provider.getEOPHistory().getEntries().get(i).getDate();
            Transform expectedIdentity = new Transform(date,
                                                       provider.getTransform(date).getInverse(),
                                                       deserialized.getTransform(date));
            Assert.assertEquals(0.0, expectedIdentity.getTranslation().getNorm(), 1.0e-15);
            Assert.assertEquals(0.0, expectedIdentity.getRotation().getAngle(),   1.0e-15);
        }

    }

    @Before
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assert.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assert.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

    private void checkRotation(double[][] reference, Transform t, double epsilon) {
        double[][] mat = t.getRotation().getMatrix();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                Assert.assertEquals(reference[i][j], mat[i][j], epsilon);
            }
        }
    }

}
