/* Copyright 2023 Mark Rutten
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Mark Rutten licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.estimation.measurements.modifiers;

import org.hipparchus.Field;
import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.differentiation.*;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.EstimatedMeasurement;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.frames.FieldTransform;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.earth.ReferenceEllipsoid;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AberrationModifierTest {

    static GroundStation groundStation;

    @BeforeAll
    static void setup() {
        Utils.setDataRoot("cr3bp:regular-data");

        // Site frame
        Frame fixedFrame = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // Observer
        GeodeticPoint stationLocation = new GeodeticPoint(FastMath.toRadians(-31.511673),
                FastMath.toRadians(139.343695),
                51.8);
        TopocentricFrame stationFrame = new TopocentricFrame(ReferenceEllipsoid.getWgs84(fixedFrame),
                stationLocation, "station");
        groundStation = new GroundStation(stationFrame);

        // Select parameters and set reference date
        List<ParameterDriver> parameterDrivers = new ArrayList<>();
        parameterDrivers.add(groundStation.getPrimeMeridianOffsetDriver());
        parameterDrivers.add(groundStation.getPolarOffsetXDriver());
        parameterDrivers.add(groundStation.getPolarOffsetYDriver());
        for (ParameterDriver driver : parameterDrivers) {
            driver.setReferenceDate(AbsoluteDate.ARBITRARY_EPOCH);
            driver.setSelected(true);
        }

    }

    @Test
    void testAberration() {

        // Frames
        Frame measurementFrame = FramesFactory.getGCRF();

        // Natural direction
        double[] raDec = new double[]{-0.094838051874766, 0.157294151619018};

        // Time
        AbsoluteDate epoch = new AbsoluteDate(2022, 11, 8, 5, 59, 42.0, TimeScalesFactory.getUTC());

        // Correct for aberration
        double[] proper = AberrationModifier.naturalToProper(raDec, groundStation, epoch, measurementFrame);

        // Test (output from SOFA)
        double[] expected = new double[]{-0.0947807299849455, 0.157333364635137};
        Assertions.assertArrayEquals(expected, proper, 1e-10);

        // Undo aberration
        double[] natural = AberrationModifier.properToNatural(proper, groundStation, epoch, measurementFrame);
        Assertions.assertArrayEquals(raDec, natural, 1e-12);

    }

    @FunctionalInterface
    public interface AberrationTransform {
        Gradient[] apply(Gradient[] raDecDS, FieldTransform<Gradient> stationToInertial, Frame obsFrame);
    }

    private void checkDerivative(AberrationTransform aberrationTransform,
                                 int raDecIndex,
                                 double gradient,
                                 Gradient[] raDecDS,
                                 GroundStation groundStation,
                                 ParameterDriver driver,
                                 Frame obsFrame,
                                 AbsoluteDate epoch,
                                 int nbParams,
                                 Map<String, Integer> indices) {

        FiniteDifferencesDifferentiator differentiator =
                new FiniteDifferencesDifferentiator(3, 50.0 * driver.getScale());
        UnivariateFunction uf = new UnivariateFunction() {
            /**
             * {@inheritDoc}
             */
            @Override
            public double value(final double value) {
                double current = driver.getValue();

                driver.setValue(value);
                FieldTransform<Gradient> stationToInertial =
                        groundStation.getOffsetToInertial(obsFrame, epoch, nbParams, indices);
                Gradient[] naturalDS = aberrationTransform.apply(raDecDS, stationToInertial, obsFrame);

                driver.setValue(current);
                return naturalDS[raDecIndex].getValue();
            }
        };

        DSFactory factory = new DSFactory(1, 1);
        final DerivativeStructure dsParam = factory.variable(0, driver.getValue());
        final DerivativeStructure dsValue = differentiator.differentiate(uf).value(dsParam);

        double deriv = dsValue.getPartialDerivative(1);
        //System.out.println(deriv);
        Assertions.assertEquals(gradient, deriv, 1e-3 * FastMath.abs(gradient));
    }

    private void checkNaturalToProperDerivative(int raDecIndex,
                                                double gradient,
                                                Gradient[] raDecDS,
                                                GroundStation groundStation,
                                                ParameterDriver driver,
                                                Frame obsFrame,
                                                AbsoluteDate epoch,
                                                int nbParams,
                                                Map<String, Integer> indices) {

        checkDerivative(AberrationModifier::fieldNaturalToProper, raDecIndex,
                gradient, raDecDS, groundStation, driver,
                obsFrame, epoch, nbParams, indices);
    }

    private void checkProperToNaturalDerivative(int raDecIndex,
                                                double gradient,
                                                Gradient[] raDecDS,
                                                GroundStation groundStation,
                                                ParameterDriver driver,
                                                Frame obsFrame,
                                                AbsoluteDate epoch,
                                                int nbParams,
                                                Map<String, Integer> indices) {

        checkDerivative(AberrationModifier::fieldProperToNatural, raDecIndex,
                gradient, raDecDS, groundStation, driver,
                obsFrame, epoch, nbParams, indices);
    }

    @Test
    void testFieldAberration() {

        // Frame
        Frame measurementFrame = FramesFactory.getGCRF();

        // Natural direction
        double[] raDec = new double[]{-0.094838051874766, 0.157294151619018};

        // Time
        AbsoluteDate epoch = new AbsoluteDate(2022, 11, 8, 5, 59, 42.0, TimeScalesFactory.getUTC());

        // Form parameter list
        List<ParameterDriver> parameterDrivers = new ArrayList<>();
        parameterDrivers.add(groundStation.getPrimeMeridianOffsetDriver());
        parameterDrivers.add(groundStation.getPolarOffsetXDriver());
        parameterDrivers.add(groundStation.getPolarOffsetYDriver());

        // Setup measurement to generate estimates
        int nbParams = 6;
        final Map<String, Integer> indices = new HashMap<>();

        for (ParameterDriver driver : parameterDrivers) {
            driver.setReferenceDate(epoch);
            driver.setSelected(true);
            indices.put(driver.getNameSpan(epoch), nbParams++);
        }

        // Station to inertial transform
        final FieldTransform<Gradient> stationToInertial =
                groundStation.getOffsetToInertial(measurementFrame, epoch, nbParams, indices);

        // Correct for aberration
        final Field<Gradient> field = GradientField.getField(nbParams);
        Gradient[] raDecDS = new Gradient[]{
                field.getZero().add(raDec[0]),
                field.getZero().add(raDec[1])
        };
        Gradient[] properDS = AberrationModifier.fieldNaturalToProper(raDecDS, stationToInertial, measurementFrame);
        double[] proper = new double[]{properDS[0].getValue(), properDS[1].getValue()};

        // Test value
        double[] expected = new double[]{-0.0947807299849455, 0.157333364635137};
        Assertions.assertArrayEquals(expected, proper, 1e-10);

        // Test derivatives against finite differences
        for (ParameterDriver driver : parameterDrivers) {
            double raGradient = properDS[0].getGradient()[indices.get(driver.getNameSpan(epoch))];
            checkNaturalToProperDerivative(0, raGradient, raDecDS, groundStation, driver,
                    measurementFrame, epoch, nbParams, indices);

            double decGradient = properDS[1].getGradient()[indices.get(driver.getNameSpan(epoch))];
            checkNaturalToProperDerivative(1, decGradient, raDecDS, groundStation, driver,
                    measurementFrame, epoch, nbParams, indices);
        }

        // Undo aberration
        Gradient[] expectedDS = new Gradient[]{
                field.getZero().add(expected[0]),
                field.getZero().add(expected[1])
        };
        Gradient[] naturalDS = AberrationModifier.fieldProperToNatural(expectedDS, stationToInertial, measurementFrame);
        double[] natural = new double[]{naturalDS[0].getValue(), naturalDS[1].getValue()};

        // Test that we get what we started with
        Assertions.assertArrayEquals(raDec, natural, 1e-10);

        // Test derivatives against finite differences
        for (ParameterDriver driver : parameterDrivers) {
            double raGradient = naturalDS[0].getGradient()[indices.get(driver.getNameSpan(epoch))];
            checkProperToNaturalDerivative(0, raGradient, expectedDS, groundStation, driver,
                    measurementFrame, epoch, nbParams, indices);

            double decGradient = naturalDS[1].getGradient()[indices.get(driver.getNameSpan(epoch))];
            checkProperToNaturalDerivative(1, decGradient, expectedDS, groundStation, driver,
                    measurementFrame, epoch, nbParams, indices);
        }

    }


    @Test
    void testAberrationModifier() {

        // Date
        AbsoluteDate epoch = new AbsoluteDate(2022, 11, 9, 3, 15, 18.454, TimeScalesFactory.getUTC());

        // Calculate spacecraft state
        Vector3D position = new Vector3D(-4350341.308092136, 2.5233509978715107E7, -8187957.452234574);
        Vector3D velocity = new Vector3D(-2097.9025703889993, -1293.1199759574952, -2928.2553383447744);
        PVCoordinates coordinates = new PVCoordinates(position, velocity);
        CartesianOrbit orbit = new CartesianOrbit(coordinates, FramesFactory.getGCRF(), epoch, Constants.IERS2010_EARTH_MU);
        SpacecraftState state = new SpacecraftState(orbit);

        // RA/Dec no modifier
        AngularRaDec raDec = defaultRaDec(FramesFactory.getGCRF(), epoch);

        // Estimated (no modifier)
        double[] estimatedRaDec = raDec
                .estimate(0, 0, new SpacecraftState[]{state})
                .getEstimatedValue();

        // RA/Dec with modifier
        AngularRaDec modifiedRaDec = defaultRaDec(FramesFactory.getGCRF(), epoch);
        modifiedRaDec.addModifier(new AberrationModifier());

        // Estimated value
        EstimatedMeasurement<AngularRaDec> modEstimated = modifiedRaDec
                .estimate(0, 0, new SpacecraftState[]{state});
        double[] estModRaDec = modEstimated.getEstimatedValue();

        // Apply aberration to result should get us back to unmodified values
        double[] unmodRaDec = AberrationModifier.naturalToProper(estModRaDec, groundStation, epoch, FramesFactory.getGCRF());
        Assertions.assertArrayEquals(estimatedRaDec, unmodRaDec, 1e-12);
    }

    @Test
    public void testIssue1230() {

        // Calculate spacecraft state
        final AbsoluteDate    epoch       = new AbsoluteDate(2022, 11, 9, 3, 15, 18.454, TimeScalesFactory.getUTC());
        final Frame           frame       = FramesFactory.getGCRF();
        final Vector3D        position    = new Vector3D(-4350341.308092136, 2.5233509978715107E7, -8187957.452234574);
        final Vector3D        velocity    = new Vector3D(-2097.9025703889993, -1293.1199759574952, -2928.2553383447744);
        final PVCoordinates   coordinates = new PVCoordinates(position, velocity);
        final CartesianOrbit  orbit       = new CartesianOrbit(coordinates, frame, epoch, Constants.IERS2010_EARTH_MU);
        final SpacecraftState state       = new SpacecraftState(orbit);

        // RA/Dec with modifier
        final AngularRaDec raDec1 = defaultRaDec(frame, epoch);
        final AngularRaDec raDec2 = defaultRaDec(frame, epoch);

        // Estimated values
        final EstimatedMeasurement<AngularRaDec> estimated1 = raDec1.estimate(0, 0, new SpacecraftState[] { state });
        final EstimatedMeasurement<AngularRaDec> estimated2 = raDec2.estimate(0, 0, new SpacecraftState[] { state });

        // Default data context
        final DataContext dataContext = DataContext.getDefault();

        // Test after modification
        new AberrationModifier().modify(estimated1);
        new AberrationModifier(dataContext).modify(estimated2);
        Assertions.assertEquals(estimated1.getEstimatedValue()[0], estimated2.getEstimatedValue()[0]);
        Assertions.assertEquals(estimated1.getEstimatedValue()[1], estimated2.getEstimatedValue()[1]);

        // Apply a second modification to verify the "modifyWithoutDerivatives" signature
        new AberrationModifier().modifyWithoutDerivatives(estimated1);
        new AberrationModifier(dataContext).modifyWithoutDerivatives(estimated2);
        Assertions.assertEquals(estimated1.getEstimatedValue()[0], estimated2.getEstimatedValue()[0]);
        Assertions.assertEquals(estimated1.getEstimatedValue()[1], estimated2.getEstimatedValue()[1]);

        // Verify "naturalToProper" and "properToNatural" methods
        final double[] proper1  = AberrationModifier.naturalToProper(raDec1.getObservedValue(), groundStation, epoch, frame);
        final double[] natural1 = AberrationModifier.properToNatural(proper1, groundStation, epoch, frame);
        final double[] proper2  = AberrationModifier.naturalToProper(raDec2.getObservedValue(), groundStation, epoch, frame, dataContext);
        final double[] natural2 = AberrationModifier.properToNatural(proper2, groundStation, epoch, frame, dataContext);
        Assertions.assertEquals(natural1[0], natural2[0]);
        Assertions.assertEquals(natural1[1], natural2[1]);

        // Verify Field versions of "naturalToProper" and "properToNatural" methods
        final Field<Gradient> field = GradientField.getField(6);
        final Gradient[] raDecG = new Gradient[] { field.getZero().add(raDec1.getObservedValue()[0]),
                                                   field.getZero().add(raDec1.getObservedValue()[1]) };
        final FieldTransform<Gradient> stationToInertial =
                groundStation.getBaseFrame().getTransformTo(frame, new FieldAbsoluteDate<>(field, epoch));

        final Gradient[] proper1G  = AberrationModifier.fieldNaturalToProper(raDecG, stationToInertial, frame);
        final Gradient[] natural1G = AberrationModifier.fieldProperToNatural(proper1G, stationToInertial, frame);
        final Gradient[] proper2G  = AberrationModifier.fieldNaturalToProper(raDecG, stationToInertial, frame, dataContext);
        final Gradient[] natural2G = AberrationModifier.fieldProperToNatural(proper2G, stationToInertial, frame, dataContext);

        Assertions.assertEquals(natural1G[0].getValue(), natural2G[0].getValue());
        Assertions.assertEquals(natural1G[1].getValue(), natural2G[1].getValue());

    }

    @Test
    public void testExceptionIfNonInertialFrame() {
        // GIVEN
        final AbsoluteDate epoch  = new AbsoluteDate(2022, 11, 9, 3, 15, 18.454, TimeScalesFactory.getUTC());
        final double[]     raDec1 = new double[] { 1.0, 1.0 };
        final Frame        itrf   = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

        // WHEN & THEN
        // Assert that an error is thrown
        final OrekitException exceptionNToP = Assertions.assertThrows(OrekitException.class,
                                                                      () -> AberrationModifier.naturalToProper(raDec1,
                                                                                                               groundStation,
                                                                                                               epoch, itrf));
        final OrekitException exceptionPToN = Assertions.assertThrows(OrekitException.class,
                                                                      () -> AberrationModifier.properToNatural(raDec1,
                                                                                                               groundStation,
                                                                                                               epoch, itrf));
        // Assert that the expected kind of error is thrown
        Assertions.assertEquals(exceptionNToP.getSpecifier(), OrekitMessages.NON_PSEUDO_INERTIAL_FRAME);
        Assertions.assertEquals(exceptionPToN.getSpecifier(), OrekitMessages.NON_PSEUDO_INERTIAL_FRAME);
    }

    private static AngularRaDec defaultRaDec(Frame frame, AbsoluteDate date) {
        return  new AngularRaDec(groundStation, frame, date, new double[]{0.0, 0.0},
            new double[]{1.0, 1.0}, new double[]{1.0, 1.0}, new ObservableSatellite(0));
    }
}
