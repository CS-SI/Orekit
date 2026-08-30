/* Copyright 2002-2026 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.OrekitConfiguration;
import org.orekit.utils.PVCoordinates;

import java.io.FileNotFoundException;


public class TODProviderTest {

    @ParameterizedTest
    @EnumSource(IERSConventions.class)
    public void testRawTransformKinematics(final IERSConventions conventions) {
        checkRawTransformKinematics(conventions, null);
        checkRawTransformKinematics(conventions, FramesFactory.getEOPHistory(conventions, true));
    }

    @Test
    public void testRotationRate() {
        TransformProvider provider =
                new InterpolatingTransformProvider(new TODProvider(IERSConventions.IERS_1996, null, DataContext.getDefault().getTimeScales()),
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
                                                   3, 1.0, 5, Constants.JULIAN_DAY, 100.0);
        AbsoluteDate tMin = new AbsoluteDate(2035, 3, 2, 15, 58, 59, TimeScalesFactory.getUTC());
        double minRate = provider.getTransform(tMin).getRotationRate().getNorm();
        Assertions.assertEquals(6.4e-14, minRate, 1.0e-15);
        AbsoluteDate tMax = new AbsoluteDate(2043, 12, 16, 14, 18, 9, TimeScalesFactory.getUTC());
        double maxRate = provider.getTransform(tMax).getRotationRate().getNorm();
        Assertions.assertEquals(1.4e-11, maxRate, 1.0e-12);
    }

    @Test
    public void testAASReferenceLEO() {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, ITRFVersion.ITRF_2008, new double[][] {
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
    public void testAASReferenceGEO() {

        // this reference test has been extracted from the following paper:
        // Implementation Issues Surrounding the New IAU Reference Systems for Astrodynamics
        // David A. Vallado, John H. Seago, P. Kenneth Seidelmann
        // http://www.centerforspace.com/downloads/files/pubs/AAS-06-134.pdf
        Utils.setLoaders(IERSConventions.IERS_1996,
                         Utils.buildEOPList(IERSConventions.IERS_1996, ITRFVersion.ITRF_2008, new double[][] {
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
    public void testInterpolationAccuracyWithEOP() throws FileNotFoundException {

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
        TransformProvider nonInterpolating = new TODProvider(IERSConventions.IERS_1996, eopHistory, DataContext.getDefault().getTimeScales());
        final TransformProvider interpolating =
                new InterpolatingTransformProvider(nonInterpolating,
                                                   CartesianDerivativesFilter.USE_PVA,
                                                   AngularDerivativesFilter.USE_R,
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

        Assertions.assertTrue(maxError < 7e-12);

    }

    @Test
    public void testInterpolationAccuracyWithoutEOP() throws FileNotFoundException {

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
        TransformProvider nonInterpolating = new TODProvider(IERSConventions.IERS_1996, null, DataContext.getDefault().getTimeScales());
                final TransformProvider interpolating =
                        new InterpolatingTransformProvider(nonInterpolating,
                                                           CartesianDerivativesFilter.USE_PVA,
                                                           AngularDerivativesFilter.USE_R,
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

                Assertions.assertTrue(maxError < 4.0e-15);

    }

    @Test
    public void testSofaPnm80() {

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
    public void testTOD1976vs2006() {

        final Frame tod1976 = FramesFactory.getTOD(IERSConventions.IERS_1996, true);
        final Frame tod2006 = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
        for (double dt = 0; dt < 2 * Constants.JULIAN_YEAR; dt += 100 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = tod1976.getTransformTo(tod2006, date).getRotation().getAngle();
            // TOD2006 and TOD2000 are similar to about 65 milli-arcseconds
            // between 2000 and 2002, with EOP corrections taken into account in both cases
            Assertions.assertEquals(0.0, delta, 3.2e-7);
        }

    }

    @Test
    public void testTOD2000vs2006() {

        final Frame tod2000 = FramesFactory.getTOD(IERSConventions.IERS_2003, true);
        final Frame tod2006 = FramesFactory.getTOD(IERSConventions.IERS_2010, true);
        for (double dt = 0; dt < 2 * Constants.JULIAN_YEAR; dt += 100 * Constants.JULIAN_DAY) {
            AbsoluteDate date = new AbsoluteDate(AbsoluteDate.J2000_EPOCH, dt);
            double delta = tod2000.getTransformTo(tod2006, date).getRotation().getAngle();
            // TOD2006 and TOD2000 are similar to about 30 micro-arcseconds
            // between 2000 and 2002, with EOP corrections taken into account in both cases
            Assertions.assertEquals(0.0, delta, 1.5e-10);
        }

    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("compressed-data");
    }

    private void checkPV(PVCoordinates reference, PVCoordinates result,
                         double expectedPositionError, double expectedVelocityError) {

        Vector3D dP = result.getPosition().subtract(reference.getPosition());
        Vector3D dV = result.getVelocity().subtract(reference.getVelocity());
        Assertions.assertEquals(expectedPositionError, dP.getNorm(), 0.01 * expectedPositionError);
        Assertions.assertEquals(expectedVelocityError, dV.getNorm(), 0.01 * expectedVelocityError);
    }

    private void checkRawTransformKinematics(final IERSConventions conventions,
                                             final EOPHistory eopHistory) {
        final TODProvider provider = new TODProvider(conventions, eopHistory,
                                                     DataContext.getDefault().getTimeScales());
        final AbsoluteDate date = new AbsoluteDate(2004, 4, 6, 7, 51, 28.386009,
                                                   TimeScalesFactory.getUTC());
        final Transform scalarTransform = provider.getTransform(date);
        final KinematicTransform kinematicTransform = provider.getKinematicTransform(date);

        final double[] angles = conventions.getNutationFunction(DataContext.getDefault().getTimeScales()).value(date);
        final double moe = conventions.getMeanObliquityFunction(DataContext.getDefault().getTimeScales()).value(date);
        double dpsi = angles[0];
        double deps = angles[1];
        if (eopHistory != null) {
            final double[] correction = eopHistory.getEquinoxNutationCorrection(date);
            dpsi += correction[0];
            deps += correction[1];
        }
        final Rotation referenceRotation =
                        new Rotation(RotationOrder.XZX, RotationConvention.FRAME_TRANSFORM,
                                     moe, -dpsi, -moe - deps);

        final UnivariateDerivative2Field ud2Field = UnivariateDerivative2Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative2> ud2Date =
                        new FieldAbsoluteDate<>(ud2Field, date).shiftedBy(new UnivariateDerivative2(0, 1, 0));
        final FieldTransform<UnivariateDerivative2> derivativeTransform = provider.getTransform(ud2Date);
        final AngularCoordinates embeddedDerivatives = new AngularCoordinates(derivativeTransform.getRotation());

        final UnivariateDerivative1Field ud1Field = UnivariateDerivative1Field.getInstance();
        final FieldAbsoluteDate<UnivariateDerivative1> ud1Date =
                        new FieldAbsoluteDate<>(ud1Field, date).shiftedBy(new UnivariateDerivative1(0, 1));
        final FieldKinematicTransform<UnivariateDerivative1> derivativeKinematicTransform =
                        provider.getKinematicTransform(ud1Date);
        final AngularCoordinates embeddedKinematicDerivatives =
                        new AngularCoordinates(derivativeKinematicTransform.getRotation());

        final FieldAbsoluteDate<Binary64> binaryDate =
                        new FieldAbsoluteDate<>(Binary64Field.getInstance(), date);
        final FieldTransform<Binary64> binaryTransform = provider.getTransform(binaryDate);
        final FieldKinematicTransform<Binary64> binaryKinematicTransform =
                        provider.getKinematicTransform(binaryDate);

        Assertions.assertEquals(0.0, Rotation.distance(referenceRotation, scalarTransform.getRotation()), 2.0e-15);
        Assertions.assertEquals(0.0,
                                Rotation.distance(binaryTransform.getRotation().toRotation(),
                                                  scalarTransform.getRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryTransform.getRotationRate().toVector3D(),
                                                  scalarTransform.getRotationRate()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryTransform.getRotationAcceleration().toVector3D(),
                                                  scalarTransform.getRotationAcceleration()),
                                2.0e-28);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedDerivatives.getRotationRate(),
                                                  derivativeTransform.getRotationRate().toVector3D()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedDerivatives.getRotationAcceleration(),
                                                  derivativeTransform.getRotationAcceleration().toVector3D()),
                                2.0e-28);
        Assertions.assertEquals(0.0,
                                Rotation.distance(kinematicTransform.getRotation(), scalarTransform.getRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(kinematicTransform.getRotationRate(), scalarTransform.getRotationRate()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Rotation.distance(binaryKinematicTransform.getRotation().toRotation(),
                                                  binaryTransform.getRotation().toRotation()),
                                2.0e-15);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(binaryKinematicTransform.getRotationRate().toVector3D(),
                                                  binaryTransform.getRotationRate().toVector3D()),
                                2.0e-22);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(embeddedKinematicDerivatives.getRotationRate(),
                                                  derivativeKinematicTransform.getRotationRate().toVector3D()),
                                2.0e-22);

        final Vector3D position = new Vector3D(42000000.0, -13000000.0, 5000000.0);
        final double h = 1.0;
        final Vector3D pM = provider.getTransform(date.shiftedBy(-h)).transformPosition(position);
        final Vector3D pP = provider.getTransform(date.shiftedBy(+h)).transformPosition(position);
        final Vector3D finiteDifference = new Vector3D(-1.0 / (2.0 * h), pM, 1.0 / (2.0 * h), pP);
        final Vector3D transformedVelocity =
                        scalarTransform.transformPVCoordinates(new PVCoordinates(position)).getVelocity();
        Assertions.assertEquals(0.0, Vector3D.distance(finiteDifference, transformedVelocity), 5.0e-8);

        final PVCoordinates pv = new PVCoordinates(position, new Vector3D(1200.0, 2900.0, -400.0));
        final PVCoordinates transformedPV = scalarTransform.transformPVCoordinates(pv);
        final PVCoordinates transformedOnlyPV = kinematicTransform.transformOnlyPV(pv);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(transformedPV.getPosition(), transformedOnlyPV.getPosition()),
                                2.0e-9);
        Assertions.assertEquals(0.0,
                                Vector3D.distance(transformedPV.getVelocity(), transformedOnlyPV.getVelocity()),
                                2.0e-12);
    }

    private void checkRotation(double[][] reference, Transform t, double epsilon) {
        double[][] mat = t.getRotation().getMatrix();
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                Assertions.assertEquals(reference[i][j], mat[i][j], epsilon);
            }
        }
    }

}
