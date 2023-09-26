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
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedAngularCoordinates;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class TabulatedLofOffsetTest {

    // Computation date
    private AbsoluteDate date;

    // Body mu
    private double mu;

    // Reference frame = ITRF
    private Frame itrf;

    // Earth shape
    OneAxisEllipsoid earth;

    //  Satellite position
    CircularOrbit orbit;
    PVCoordinates pvSatEME2000;

    @Test
    public void testConstantOffset() {

        RandomGenerator random = new Well19937a(0x1199d4bb8f53d2b6l);
        for (LOFType type : LOFType.values()) {
            for (int i = 0; i < 100; ++i) {
                double a1 = random.nextDouble() * MathUtils.TWO_PI;
                double a2 = random.nextDouble() * MathUtils.TWO_PI;
                double a3 = random.nextDouble() * MathUtils.TWO_PI;
                LofOffset law          = new LofOffset(orbit.getFrame(), type, RotationOrder.XYZ, a1, a2, a3);
                Rotation  offsetAtt    = law.getAttitude(orbit, orbit.getDate(), orbit.getFrame()).getRotation();
                LofOffset aligned      = new LofOffset(orbit.getFrame(), type);
                Rotation  alignedAtt   = aligned.getAttitude(orbit, orbit.getDate(), orbit.getFrame()).getRotation();
                Rotation  offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
                TabulatedLofOffset tabulated =
                        new TabulatedLofOffset(orbit.getFrame(), type,
                                               Arrays.asList(new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(-10),
                                                                                               offsetProper,
                                                                                               Vector3D.ZERO,
                                                                                               Vector3D.ZERO),
                                                             new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(0),
                                                                                               offsetProper,
                                                                                               Vector3D.ZERO,
                                                                                               Vector3D.ZERO),
                                                             new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(+10),
                                                                                               offsetProper,
                                                                                               Vector3D.ZERO,
                                                                                               Vector3D.ZERO)),
                                               2, AngularDerivativesFilter.USE_R);
                Rotation rebuilt = tabulated.getAttitude(orbit, orbit.getDate(), orbit.getFrame()).getRotation();
                Assertions.assertEquals(0.0, Rotation.distance(offsetAtt, rebuilt), 1.48e-15);
                Assertions.assertEquals(3, tabulated.getTable().size());
            }
        }
    }

    @Test
    public void testYawCompensation() {

        // create a sample from Yaw compensation law
        final LOFType type = LOFType.VNC;
        final List<TimeStampedAngularCoordinates> sample = new ArrayList<TimeStampedAngularCoordinates>();
        final AttitudeProvider yawCompensLaw =
                new YawCompensation(orbit.getFrame(), new NadirPointing(orbit.getFrame(), earth));
        final Propagator originalPropagator = new KeplerianPropagator(orbit);
        originalPropagator.setAttitudeProvider(yawCompensLaw);
        originalPropagator.setStepHandler(1.0, currentState -> {
                Rotation  offsetAtt    = currentState.getAttitude().getRotation();
                LofOffset aligned      = new LofOffset(currentState.getFrame(), type);
                Rotation  alignedAtt   = aligned.getAttitude(currentState.getOrbit(), currentState.getDate(),
                                                             currentState.getFrame()).getRotation();
                Rotation  offsetProper = offsetAtt.compose(alignedAtt.revert(), RotationConvention.VECTOR_OPERATOR);
                sample.add(new TimeStampedAngularCoordinates(currentState.getDate(),
                                                             offsetProper, Vector3D.ZERO, Vector3D.ZERO));
            });
        final AbsoluteDate endDate = orbit.getDate().shiftedBy(2000);
        originalPropagator.propagate(endDate);
        originalPropagator.clearStepHandlers();

        // use the sample and compare it to original
        final BoundedAttitudeProvider tabulated = new TabulatedLofOffset(orbit.getFrame(), type, sample,
                                                                         6, AngularDerivativesFilter.USE_RR);
        Assertions.assertEquals(0., orbit.getDate().durationFrom(tabulated.getMinDate()), Double.MIN_VALUE);
        Assertions.assertEquals(0., endDate.durationFrom(tabulated.getMaxDate()), Double.MIN_VALUE);
        final Propagator rebuildingPropagator = new KeplerianPropagator(orbit);
        rebuildingPropagator.setAttitudeProvider(tabulated);
        rebuildingPropagator.setStepHandler(0.3, currentState -> {
                final SpacecraftState rebuilt = originalPropagator.propagate(currentState.getDate());
                final Rotation r1 = currentState.getAttitude().getRotation();
                final Rotation r2 = rebuilt.getAttitude().getRotation();
                Assertions.assertEquals(0.0, Rotation.distance(r1, r2), 7.0e-6);
                checkField(Binary64Field.getInstance(), tabulated,
                           currentState.getOrbit(), currentState.getDate(), currentState.getFrame());
            });
        rebuildingPropagator.propagate(orbit.getDate().shiftedBy(50), orbit.getDate().shiftedBy(1950));

    }

    private <T extends CalculusFieldElement<T>> void checkField(final Field<T> field, final AttitudeProvider provider,
                                                                final Orbit orbit, final AbsoluteDate date,
                                                                final Frame frame)
        {
        Attitude attitudeD = provider.getAttitude(orbit, date, frame);
        final FieldOrbit<T> orbitF = new FieldSpacecraftState<>(field, new SpacecraftState(orbit)).getOrbit();
        final FieldAbsoluteDate<T> dateF = new FieldAbsoluteDate<>(field, date);
        FieldAttitude<T> attitudeF = provider.getAttitude(orbitF, dateF, frame);
        Assertions.assertEquals(0.0, Rotation.distance(attitudeD.getRotation(), attitudeF.getRotation().toRotation()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getSpin(), attitudeF.getSpin().toVector3D()), 1.0e-15);
        Assertions.assertEquals(0.0, Vector3D.distance(attitudeD.getRotationAcceleration(), attitudeF.getRotationAcceleration().toVector3D()), 1.0e-15);
    }

    @Test
    public void testNonPseudoInertialFrame() {
        final Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        try {
            new TabulatedLofOffset(itrf, LOFType.QSW,
                                   Arrays.asList(new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(-10),
                                                                                   Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO),
                                                 new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(0),
                                                                                   Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO),
                                                 new TimeStampedAngularCoordinates(orbit.getDate().shiftedBy(+10),
                                                                                   Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO)),
                                   2, AngularDerivativesFilter.USE_R);
        } catch (OrekitException oe) {
            Assertions.assertEquals(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME, oe.getSpecifier());
            Assertions.assertEquals(itrf.getName(), oe.getParts()[0]);
        }
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
            mu = 3.9860047e14;

            // Reference frame = ITRF
            itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);

            // Elliptic earth shape
            earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                         Constants.WGS84_EARTH_FLATTENING,
                                         itrf);

            //  Satellite position
            orbit =
                new CircularOrbit(7178000.0, 0.5e-8, -0.5e-8, FastMath.toRadians(50.), FastMath.toRadians(150.),
                                       FastMath.toRadians(5.300), PositionAngleType.MEAN,
                                       FramesFactory.getEME2000(), date, mu);
            pvSatEME2000 = orbit.getPVCoordinates();


        } catch (OrekitException oe) {
            Assertions.fail(oe.getMessage());
        }

    }

    @AfterEach
    public void tearDown() {
        date = null;
        itrf = null;
        earth = null;
    }

}

