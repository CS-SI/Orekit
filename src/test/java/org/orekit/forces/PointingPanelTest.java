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
package org.orekit.forces;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.Binary64;
import org.hipparchus.util.Binary64Field;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.analytical.EcksteinHechlerPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class PointingPanelTest {

    @Test
    public void testBestPointing() {

        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        PointingPanel solarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 0.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {

            SpacecraftState state = propagator.propagate(initialDate.shiftedBy(dt));

            Vector3D sunInert = sun.getPosition(initialDate, state.getFrame());
            Vector3D momentum = state.getPVCoordinates().getMomentum();
            double sunElevation = FastMath.PI / 2 - Vector3D.angle(sunInert, momentum);
            Assertions.assertEquals(15.1, FastMath.toDegrees(sunElevation), 0.1);

            Vector3D n = solarArray.getNormal(state);
            Assertions.assertEquals(0.0, n.getY(), 1.0e-10);

            // normal misalignment should be entirely due to sun being out of orbital plane
            Vector3D sunSat = state.getAttitude().getRotation().applyTo(sunInert);
            double misAlignment = Vector3D.angle(sunSat, n);
            Assertions.assertEquals(sunElevation, misAlignment, 1.0e-3);

        }
    }

    @Test
    public void testNormalOptimalRotationDouble() {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel absorbingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 1.0, 0.0);
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            SpacecraftState state = propagator.propagate(date);
            Vector3D normal = absorbingSolarArray.getNormal(state);
            Assertions.assertEquals(0, Vector3D.dotProduct(normal, Vector3D.PLUS_J), 1.0e-16);
        }
    }

    @Test
    public void testNormalOptimalRotationField() {
        AbsoluteDate initialDate = propagator.getInitialState().getDate();
        CelestialBody sun = CelestialBodyFactory.getSun();
        final Panel absorbingSolarArray = new PointingPanel(Vector3D.PLUS_J, sun, 20.0, 0.0, 0.0, 1.0, 0.0);
        Field<Binary64> field = Binary64Field.getInstance();
        for (double dt = 0; dt < 4000; dt += 60) {
            AbsoluteDate date = initialDate.shiftedBy(dt);
            FieldSpacecraftState<Binary64> fState = new FieldSpacecraftState<>(field, propagator.propagate(date));
            FieldVector3D<Binary64> normal = absorbingSolarArray.getNormal(fState);
            Assertions.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
        }
    }

    public void testNormalSunAlignedDouble() {
        ExtendedPVCoordinatesProvider ep = new ExtendedPVCoordinatesProvider() {
            
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
                return new TimeStampedPVCoordinates(date, new Vector3D(0, 1e6, 0), Vector3D.ZERO);
            }
            
            @Override
            public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T>
                getPVCoordinates(FieldAbsoluteDate<T> date, Frame frame) {
                // not used in this test
                return null;
            }
        };
        final Panel panel = new PointingPanel(Vector3D.PLUS_J, ep, 20.0, 0.0, 0.0, 1.0, 0.0);
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                            Vector3D.ZERO,
                                                                            Vector3D.ZERO,
                                                                            Vector3D.ZERO),
                                               FramesFactory.getEME2000(), Constants.EIGEN5C_EARTH_MU);
        final Attitude attitude = new Attitude(FramesFactory.getEME2000(),
                                               new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                 Rotation.IDENTITY,
                                                                                 Vector3D.ZERO,
                                                                                 Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(orbit, attitude, 1000.0);
        Vector3D normal = panel.getNormal(state);
        Assertions.assertEquals(0, Vector3D.dotProduct(normal, Vector3D.PLUS_J), 1.0e-16);
    }

    @Test
    public void testNormalSunAlignedField() {
        ExtendedPVCoordinatesProvider ep = new ExtendedPVCoordinatesProvider() {
            
            @Override
            public TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame) {
                // not used in this test
                return null;
            }
            
            @Override
            public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T>
                getPVCoordinates(FieldAbsoluteDate<T> date, Frame frame) {
                return new TimeStampedFieldPVCoordinates<>(date,
                                                           new FieldVector3D<>(date.getField(), new Vector3D(0, 1e6, 0)),
                                                           FieldVector3D.getZero(date.getField()),
                                                           FieldVector3D.getZero(date.getField()));
            }
        };
        final Panel panel = new PointingPanel(Vector3D.PLUS_J, ep, 20.0, 0.0, 0.0, 1.0, 0.0);
        final Orbit orbit = new CartesianOrbit(new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                            Vector3D.ZERO,
                                                                            Vector3D.ZERO,
                                                                            Vector3D.ZERO),
                                               FramesFactory.getEME2000(), Constants.EIGEN5C_EARTH_MU);
        final Attitude attitude = new Attitude(FramesFactory.getEME2000(),
                                               new TimeStampedAngularCoordinates(AbsoluteDate.J2000_EPOCH,
                                                                                 Rotation.IDENTITY,
                                                                                 Vector3D.ZERO,
                                                                                 Vector3D.ZERO));
        final SpacecraftState state = new SpacecraftState(orbit, attitude, 1000.0);
        Field<Binary64> field = Binary64Field.getInstance();
        FieldVector3D<Binary64> normal = panel.getNormal(new FieldSpacecraftState<>(field, state));
        Assertions.assertEquals(0, FieldVector3D.dotProduct(normal, Vector3D.PLUS_J).getReal(), 1.0e-16);
    }

    @BeforeEach
    public void setUp() {
        try {
        Utils.setDataRoot("regular-data");
        mu  = 3.9860047e14;
        double ae  = 6.378137e6;
        double c20 = -1.08263e-3;
        double c30 = 2.54e-6;
        double c40 = 1.62e-6;
        double c50 = 2.3e-7;
        double c60 = -5.5e-7;

        AbsoluteDate date = new AbsoluteDate(new DateComponents(1970, 7, 1),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());

        // Satellite position as circular parameters, raan chosen to have sun elevation with
        // respect to orbit plane roughly evolving roughly from 15 to 15.2 degrees in the test range
        Orbit circ =
            new CircularOrbit(7178000.0, 0.5e-4, -0.5e-4, FastMath.toRadians(50.), FastMath.toRadians(280),
                                   FastMath.toRadians(10.0), PositionAngleType.MEAN,
                                   FramesFactory.getEME2000(), date, mu);
        propagator =
            new EcksteinHechlerPropagator(circ,
                                          new LofOffset(circ.getFrame(), LOFType.LVLH_CCSDS),
                                          ae, mu, c20, c30, c40, c50, c60);
        } catch (OrekitException oe) {
            Assertions.fail(oe.getLocalizedMessage());
        }
    }

    private double mu;
    private Propagator propagator;

}
