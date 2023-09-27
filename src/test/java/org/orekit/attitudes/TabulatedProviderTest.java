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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.ArrayList;
import java.util.List;

public class TabulatedProviderTest {

    // Computation date
    private AbsoluteDate date;

    // Reference frame = ITRF
    private Frame itrf;

    // Satellite position
    CircularOrbit circOrbit;

    // Earth shape
    OneAxisEllipsoid earthShape;

    @Test
    public void testDifferentFrames() {
        double             samplingRate      = 10.0;
        int                n                 = 8;
        AttitudeProvider   referenceProvider = new NadirPointing(circOrbit.getFrame(), earthShape);
        List<TimeStampedAngularCoordinates> sample = createSample(samplingRate, referenceProvider);
        TabulatedProvider  provider          = new TabulatedProvider(circOrbit.getFrame(), sample, n,
                                                                     AngularDerivativesFilter.USE_R);
        Attitude attE = provider.getAttitude((date, frame) -> new TimeStampedPVCoordinates(date, PVCoordinates.ZERO),
                                             date,
                                             circOrbit.getFrame());
        Assertions.assertEquals(circOrbit.getFrame().getName(), attE.getReferenceFrame().getName());
        Frame gcrf = FramesFactory.getGCRF();
        Attitude attG = provider.getAttitude((date, frame) -> new TimeStampedPVCoordinates(date, PVCoordinates.ZERO),
                                             date,
                                             gcrf);
        Assertions.assertEquals(gcrf.getName(), attG.getReferenceFrame().getName());

        Assertions.assertEquals(1.12e-7,
                            Rotation.distance(attE.getRotation(), attG.getRotation()), 1.0e-9);
        Assertions.assertEquals(circOrbit.getFrame().getTransformTo(gcrf, date).getRotation().getAngle(),
                            Rotation.distance(attE.getRotation(), attG.getRotation()),
                            1.0e-14);

        FieldAttitude<Binary64> attG64 =
                        provider.getAttitude((date, frame) -> new TimeStampedFieldPVCoordinates<>(date,
                                                                        FieldPVCoordinates.getZero(Binary64Field.getInstance())),
                                             new FieldAbsoluteDate<>(Binary64Field.getInstance(), date),
                                             gcrf);
        Assertions.assertEquals(gcrf.getName(), attG64.getReferenceFrame().getName());

    }

    @Test
    public void testWithoutRate() {
        double             samplingRate      = 10.0;
        double             checkingRate      = 1.0;
        int                n                 = 8;
        AttitudeProvider   referenceProvider = new NadirPointing(circOrbit.getFrame(), earthShape);
        List<TimeStampedAngularCoordinates> sample = createSample(samplingRate, referenceProvider);
        final double       margin            = samplingRate * n / 2;
        final AbsoluteDate start             = sample.get(0).getDate().shiftedBy(margin);
        final AbsoluteDate end               = sample.get(sample.size() - 1).getDate().shiftedBy(-margin);
        TabulatedProvider  provider          = new TabulatedProvider(circOrbit.getFrame(), sample, n,
                                                                     AngularDerivativesFilter.USE_R);
        Assertions.assertEquals(0.0, checkError(start, end, checkingRate, referenceProvider, provider), 2.2e-14);
    }

    @Test
    public void testWithRate() {
        double             samplingRate      = 10.0;
        double             checkingRate      = 1.0;
        int                n                 = 8;
        AttitudeProvider   referenceProvider = new NadirPointing(circOrbit.getFrame(), earthShape);
        List<TimeStampedAngularCoordinates> sample = createSample(samplingRate, referenceProvider);
        final double       margin            = samplingRate * n / 2;
        final AbsoluteDate start             = sample.get(0).getDate().shiftedBy(margin);
        final AbsoluteDate end               = sample.get(sample.size() - 1).getDate().shiftedBy(-margin);
        TabulatedProvider  provider          = new TabulatedProvider(circOrbit.getFrame(), sample, n,
                                                                     AngularDerivativesFilter.USE_RR);
        Assertions.assertEquals(0.0, checkError(start, end, checkingRate, referenceProvider, provider), 1.3e-11);
    }

    @Test
    public void testWithAcceleration() {
        double             samplingRate      = 10.0;
        double             checkingRate      = 1.0;
        int                n                 = 8;
        AttitudeProvider   referenceProvider = new NadirPointing(circOrbit.getFrame(), earthShape);
        List<TimeStampedAngularCoordinates> sample = createSample(samplingRate, referenceProvider);
        final double       margin            = samplingRate * n / 2;
        final AbsoluteDate start             = sample.get(0).getDate().shiftedBy(margin);
        final AbsoluteDate end               = sample.get(sample.size() - 1).getDate().shiftedBy(-margin);
        TabulatedProvider  provider          = new TabulatedProvider(circOrbit.getFrame(), sample, n,
                                                                     AngularDerivativesFilter.USE_RRA);
        Assertions.assertEquals(0.0, checkError(start, end, checkingRate, referenceProvider, provider), 4.3e-9);
        checkField(Binary64Field.getInstance(), provider, circOrbit, circOrbit.getDate(), circOrbit.getFrame());
    }

    private List<TimeStampedAngularCoordinates> createSample(double samplingRate, AttitudeProvider referenceProvider) {

        // reference propagator, using a yaw compensation law
        final KeplerianPropagator referencePropagator = new KeplerianPropagator(circOrbit);
        referencePropagator.setAttitudeProvider(referenceProvider);

        // create sample
        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        referencePropagator.setStepHandler(samplingRate, currentState -> sample.add(currentState.getAttitude().getOrientation()));
        referencePropagator.propagate(circOrbit.getDate().shiftedBy(2 * circOrbit.getKeplerianPeriod()));

        return sample;

    }

    private double checkError(final AbsoluteDate start, AbsoluteDate end, double checkingRate,
                              final AttitudeProvider referenceProvider, TabulatedProvider provider) {

        // prepare an interpolating provider, using only internal steps
        // (i.e. ignoring interpolation near boundaries)
        Propagator interpolatingPropagator = new KeplerianPropagator(circOrbit.shiftedBy(start.durationFrom(circOrbit.getDate())));
        interpolatingPropagator.setAttitudeProvider(provider);

        // compute interpolation error on the internal steps .
        final double[] error = new double[1];
        interpolatingPropagator.setStepHandler(checkingRate, new OrekitFixedStepHandler() {

            public void init(SpacecraftState s0, AbsoluteDate t, double step) {
                error[0] = 0.0;
            }

            public void handleStep(SpacecraftState currentState) {
                Attitude interpolated = currentState.getAttitude();
                Attitude reference    = referenceProvider.getAttitude(currentState.getOrbit(),
                                                                      currentState.getDate(),
                                                                      currentState.getFrame());
                double localError = Rotation.distance(interpolated.getRotation(), reference.getRotation());
                error[0] = FastMath.max(error[0], localError);
            }

        });

        interpolatingPropagator.propagate(end);

        return error[0];

    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final AttitudeProvider provider,
                                                            final Orbit orbit, final AbsoluteDate date,
                                                            final Frame frame) {
        Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);
    }

    @BeforeEach
    public void setUp() {
        try {
            Utils.setDataRoot("regular-data");

            // Computation date
            date = new AbsoluteDate(new DateComponents(2008, 04, 07),
                                    TimeComponents.H00,
                                    TimeScalesFactory.getUTC());

            // Body mu
            final double mu = 3.9860047e14;

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            //  Satellite position
            circOrbit =
                new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(270.),
                                       FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                       FramesFactory.getEME2000(), date, mu);

            // Elliptic earth shape
            earthShape =
                new OneAxisEllipsoid(6378136.460, 1 / 298.257222101, itrf);

        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    @AfterEach
    public void tearDown() {
        date = null;
        itrf = null;
        circOrbit = null;
        earthShape = null;
    }

}

