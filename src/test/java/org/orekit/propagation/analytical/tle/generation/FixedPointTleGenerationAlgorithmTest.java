/* Copyright 2002-2023 CS GROUP
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
package org.orekit.propagation.analytical.tle.generation;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldCartesianOrbit;
import org.orekit.propagation.FieldPropagator;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.tle.FieldTLE;
import org.orekit.propagation.analytical.tle.FieldTLEPropagator;
import org.orekit.propagation.analytical.tle.TLE;
import org.orekit.propagation.analytical.tle.TLEPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


public class FixedPointTleGenerationAlgorithmTest {

    private TLE geoTLE;
    private TLE leoTLE;

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        geoTLE = new TLE("1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        leoTLE = new TLE("1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                         "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
    }

    @Test
    public void testOneMoreRevolution() {
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(leoTLE);
        final int initRevolutionNumber = leoTLE.getRevolutionNumberAtEpoch();
        final double dt =  2 * FastMath.PI / leoTLE.getMeanMotion();
        final AbsoluteDate target = leoTLE.getDate().shiftedBy(dt);
        final SpacecraftState endState = propagator.propagate(target);
        final TLE endLEOTLE = new FixedPointTleGenerationAlgorithm().generate(endState, leoTLE);
        final int endRevolutionNumber = endLEOTLE.getRevolutionNumberAtEpoch();
        Assertions.assertEquals(initRevolutionNumber + 1 , endRevolutionNumber);
    }

    @Test
    public void testOneLessRevolution() {
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(leoTLE);
        final int initRevolutionNumber = leoTLE.getRevolutionNumberAtEpoch();
        final double dt =  - 2 * FastMath.PI / leoTLE.getMeanMotion();
        final AbsoluteDate target = leoTLE.getDate().shiftedBy(dt);
        final SpacecraftState endState = propagator.propagate(target);
        final TLE endLEOTLE = new FixedPointTleGenerationAlgorithm().generate(endState, leoTLE);
        final int endRevolutionNumber = endLEOTLE.getRevolutionNumberAtEpoch();
        Assertions.assertEquals(initRevolutionNumber - 1 , endRevolutionNumber);
    }

    @Test
    public void testIssue781() {

        final DSFactory factory = new DSFactory(6, 3);
        final String line1 = "1 05709U 71116A   21105.62692147  .00000088  00000-0  00000-0 0  9999";
        final String line2 = "2 05709  10.8207 310.3659 0014139  71.9531 277.0561  0.99618926100056";
        Assertions.assertTrue(TLE.isFormatOK(line1, line2));

        final FieldTLE<DerivativeStructure> fieldTLE = new FieldTLE<>(factory.getDerivativeField(), line1, line2);
        final FieldTLEPropagator<DerivativeStructure> tlePropagator = FieldTLEPropagator.selectExtrapolator(fieldTLE, fieldTLE.getParameters(factory.getDerivativeField()));
        final FieldTLE<DerivativeStructure> fieldTLE1 = new FixedPointTleGenerationAlgorithm().generate(tlePropagator.getInitialState(), fieldTLE);
        Assertions.assertEquals(line2, fieldTLE1.getLine2());

    }

    @Test
    public void testIssue802() {

        // Initialize TLE
        final TLE tleISS = new TLE("1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                   "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleISS);

        // State at TLE epoch
        final SpacecraftState state = propagator.propagate(tleISS.getDate());

        // Changes frame
        final Frame eme2000 = FramesFactory.getEME2000();
        final TimeStampedPVCoordinates pv = state.getPVCoordinates(eme2000);
        final CartesianOrbit orbit = new CartesianOrbit(pv, eme2000, state.getMu());

        // Convert to TLE
        final TLE rebuilt = new FixedPointTleGenerationAlgorithm().generate(new SpacecraftState(orbit), tleISS);

        // Verify
        Assertions.assertEquals(tleISS.getLine1(), rebuilt.getLine1());
        Assertions.assertEquals(tleISS.getLine2(), rebuilt.getLine2());
    }

    @Test
    public void testIssue802Field() {
        doTestIssue802Field(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestIssue802Field(final Field<T> field) {

        // Initialize TLE
        final FieldTLE<T> tleISS = new FieldTLE<>(field, "1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                                         "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final FieldTLEPropagator<T> propagator = FieldTLEPropagator.selectExtrapolator(tleISS, tleISS.getParameters(field));

        // State at TLE epoch
        final FieldSpacecraftState<T> state = propagator.propagate(tleISS.getDate());

        // Changes frame
        final Frame eme2000 = FramesFactory.getEME2000();
        final TimeStampedFieldPVCoordinates<T> pv = state.getPVCoordinates(eme2000);
        final FieldCartesianOrbit<T> orbit = new FieldCartesianOrbit<T>(pv, eme2000, state.getMu());

        // Convert to TLE
        final FieldTLE<T> rebuilt = new FixedPointTleGenerationAlgorithm().generate(new FieldSpacecraftState<T>(orbit), tleISS);

        // Verify
        Assertions.assertEquals(tleISS.getLine1(), rebuilt.getLine1());
        Assertions.assertEquals(tleISS.getLine2(), rebuilt.getLine2());
    }

    @Test
    public void testIssue864() {

        // Initialize TLE
        final TLE tleISS = new TLE("1 25544U 98067A   21035.14486477  .00001026  00000-0  26816-4 0  9998",
                                   "2 25544  51.6455 280.7636 0002243 335.6496 186.1723 15.48938788267977");

        // TLE propagator
        final TLEPropagator propagator = TLEPropagator.selectExtrapolator(tleISS);

        // State at TLE epoch
        final SpacecraftState state = propagator.propagate(tleISS.getDate());

        // Set the BStar driver to selected
        tleISS.getParametersDrivers().forEach(driver -> driver.setSelected(true));

        // Convert to TLE
        final TLE rebuilt = new FixedPointTleGenerationAlgorithm().generate(state, tleISS);

        // Verify if driver is still selected
        rebuilt.getParametersDrivers().forEach(driver -> Assertions.assertTrue(driver.isSelected()));

    }

    @Test
    public void testIssue859() {

        // INTELSAT 25 TLE taken from Celestrak the 2021-11-24T07:45:00.000
        // Because the satellite eccentricity and inclination are closed to zero, this satellite
        // reach convergence issues when converting the spacecraft's state to TLE using default
        // parameters (i.e., epsilon = 1.0e-10, maxIterations = 100, and scale = 1.0).
        final TLE tle = new TLE("1 33153U 08034A   21327.46310733 -.00000207  00000+0  00000+0 0  9990",
                                "2 33153   0.0042  20.7353 0003042 213.9370 323.2156  1.00270917 48929");

        // The purpose here is to verify that reducing the scale value (from 1.0 to 0.5) while keeping
        // 1.0e-10 for epsilon value  solve the issue. In other words, keeping epsilon value to its
        // default value show that the accuracy of the generated TLE is acceptable
        Propagator p = TLEPropagator.selectExtrapolator(tle);
        FixedPointTleGenerationAlgorithm algorithm =
                        new FixedPointTleGenerationAlgorithm(FixedPointTleGenerationAlgorithm.EPSILON_DEFAULT,
                                                             400, 0.5);
        final TLE converted = algorithm.generate(p.getInitialState(), tle);

        // Verify
        Assertions.assertEquals(tle.getLine2(),                   converted.getLine2());
        Assertions.assertEquals(tle.getBStar(),                   converted.getBStar());
        Assertions.assertEquals(0.,                               converted.getDate().durationFrom(tle.getDate()));
        Assertions.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assertions.assertEquals(tle.getClassification(),          converted.getClassification());
        Assertions.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assertions.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assertions.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assertions.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());

    }

    @Test
    public void testIssue859Field() {
        dotestIssue859Field(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void dotestIssue859Field(Field<T> field) {

        // INTELSAT 25 TLE taken from Celestrak the 2021-11-24T07:45:00.000
        // Because the satellite eccentricity and inclination are closed to zero, this satellite
        // reach convergence issues when converting the spacecraft's state to TLE using default
        // parameters (i.e., epsilon = 1.0e-10, maxIterations = 100, and scale = 1.0).
        final FieldTLE<T> tle = new FieldTLE<>(field, "1 33153U 08034A   21327.46310733 -.00000207  00000+0  00000+0 0  9990",
                                               "2 33153   0.0042  20.7353 0003042 213.9370 323.2156  1.00270917 48929");

        // The purpose here is to verify that reducing the scale value (from 1.0 to 0.5) while keeping
        // 1.0e-10 for epsilon value  solve the issue. In other words, keeping epsilon value to its
        // default value show that the accuracy of the generated TLE is acceptable
        FieldPropagator<T> p = FieldTLEPropagator.selectExtrapolator(tle, tle.getParameters(field, tle.getDate()));
        FixedPointTleGenerationAlgorithm algorithm =
                        new FixedPointTleGenerationAlgorithm(FixedPointTleGenerationAlgorithm.EPSILON_DEFAULT,
                                                             400, 0.5);
        final FieldTLE<T> converted = algorithm.generate(p.getInitialState(), tle);

        // Verify
        Assertions.assertEquals(tle.getLine2(),                   converted.getLine2());
        Assertions.assertEquals(tle.getBStar(),                   converted.getBStar());
        Assertions.assertEquals(0.,                               converted.getDate().durationFrom(tle.getDate()).getReal());
        Assertions.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assertions.assertEquals(tle.getClassification(),          converted.getClassification());
        Assertions.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assertions.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assertions.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assertions.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());

    }

    @Test
    public void testConversionLeo() {
        checkConversion(leoTLE, 5.2e-9);
    }

    @Test
    public void testConversionGeo() {
        checkConversion(geoTLE, 9.2e-8);
    }

    /** Check the State to TLE conversion. */
    private void checkConversion(final TLE tle, final double threshold) {

        Propagator p = TLEPropagator.selectExtrapolator(tle);
        final TLE converted = new FixedPointTleGenerationAlgorithm().generate(p.getInitialState(), tle);

        Assertions.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assertions.assertEquals(tle.getClassification(),          converted.getClassification());
        Assertions.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assertions.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assertions.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assertions.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());

        Assertions.assertEquals(tle.getMeanMotion(), converted.getMeanMotion(), threshold * tle.getMeanMotion());
        Assertions.assertEquals(tle.getE(), converted.getE(), threshold * tle.getE());
        Assertions.assertEquals(tle.getI(), converted.getI(), threshold * tle.getI());
        Assertions.assertEquals(tle.getPerigeeArgument(), converted.getPerigeeArgument(), threshold * tle.getPerigeeArgument());
        Assertions.assertEquals(tle.getRaan(), converted.getRaan(), threshold * tle.getRaan());
        Assertions.assertEquals(tle.getMeanAnomaly(), converted.getMeanAnomaly(), threshold * tle.getMeanAnomaly());
        Assertions.assertEquals(tle.getBStar(), converted.getBStar(), threshold * tle.getBStar());

    }

    @Test
    public void testConversionLeoField() {
        doTestConversionLeoField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doTestConversionLeoField(final Field<T> field) {
        final FieldTLE<T> leoTLE = new FieldTLE<>(field, "1 31135U 07013A   11003.00000000  .00000816  00000+0  47577-4 0    11",
                                                  "2 31135   2.4656 183.9084 0021119 236.4164  60.4567 15.10546832    15");
        checkConversion(leoTLE, field, 5.2e-9);
    }

    @Test
    public void testConversionGeoField() {
        doConversionGeoField(Binary64Field.getInstance());
    }

    private <T extends CalculusFieldElement<T>> void doConversionGeoField(final Field<T> field) {
        final FieldTLE<T> geoTLE = new FieldTLE<>(field, "1 27508U 02040A   12021.25695307 -.00000113  00000-0  10000-3 0  7326",
                                                         "2 27508   0.0571 356.7800 0005033 344.4621 218.7816  1.00271798 34501");
        checkConversion(geoTLE, field, 9.2e-8);
    }

    private <T extends CalculusFieldElement<T>> void checkConversion(final FieldTLE<T> tle, final Field<T> field,
                                                                     final double threshold) {

        FieldPropagator<T> p = FieldTLEPropagator.selectExtrapolator(tle, tle.getParameters(field, tle.getDate()));
        final FieldTLE<T> converted = new FixedPointTleGenerationAlgorithm().generate(p.getInitialState(), tle);
        
        Assertions.assertEquals(tle.getSatelliteNumber(),         converted.getSatelliteNumber());
        Assertions.assertEquals(tle.getClassification(),          converted.getClassification());
        Assertions.assertEquals(tle.getLaunchYear(),              converted.getLaunchYear());
        Assertions.assertEquals(tle.getLaunchNumber(),            converted.getLaunchNumber());
        Assertions.assertEquals(tle.getLaunchPiece(),             converted.getLaunchPiece());
        Assertions.assertEquals(tle.getElementNumber(),           converted.getElementNumber());
        Assertions.assertEquals(tle.getRevolutionNumberAtEpoch(), converted.getRevolutionNumberAtEpoch());
        
        Assertions.assertEquals(tle.getMeanMotion().getReal(), converted.getMeanMotion().getReal(),threshold * tle.getMeanMotion().getReal());
        Assertions.assertEquals(tle.getE().getReal(), converted.getE().getReal(), threshold * tle.getE().getReal());
        Assertions.assertEquals(tle.getI().getReal(), converted.getI().getReal(), threshold * tle.getI().getReal());
        Assertions.assertEquals(tle.getPerigeeArgument().getReal(), converted.getPerigeeArgument().getReal(), threshold * tle.getPerigeeArgument().getReal());
        Assertions.assertEquals(tle.getRaan().getReal(), converted.getRaan().getReal(), threshold * tle.getRaan().getReal());
        Assertions.assertEquals(tle.getMeanAnomaly().getReal(), converted.getMeanAnomaly().getReal(), threshold * tle.getMeanAnomaly().getReal());
        Assertions.assertEquals(tle.getBStar(), converted.getBStar(), threshold * tle.getBStar());
    }
}
