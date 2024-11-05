/* Copyright 2022-2024 Romain Serra
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

package org.orekit.bodies;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.analysis.differentiation.UnivariateDerivative1Field;
import org.hipparchus.complex.Complex;
import org.hipparchus.complex.ComplexField;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.orekit.Utils;
import org.orekit.data.DataContext;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.*;
import org.orekit.utils.Constants;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;

class AnalyticalSolarPositionProviderTest {

    @BeforeEach
    void setUp() {
        Utils.setDataRoot("regular-data");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5})
    void testGetPosition(final int month) {
        // GIVEN
        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2000, month, 1, 0, 0, 0),
                TimeScalesFactory.getUTC());
        final AnalyticalSolarPositionProvider solarPositionProvider = new AnalyticalSolarPositionProvider();
        final Frame frame = FramesFactory.getGCRF();
        // WHEN
        final Vector3D actualPosition = solarPositionProvider.getPosition(date, frame);
        // THEN
        final CelestialBody celestialBody = CelestialBodyFactory.getSun();
        final Vector3D expectedPosition = celestialBody.getPosition(date, frame);
        Assertions.assertEquals(0., Vector3D.angle(expectedPosition, actualPosition), 5e-5);
        Assertions.assertEquals(0., expectedPosition.subtract(actualPosition).getNorm(), 1e7);
    }

    @Test
    void testGetPositionFieldVersusNonField() {
        // GIVEN
        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2025, 1, 1, 0, 0, 0),
                TimeScalesFactory.getUTC());
        final DataContext dataContext = DataContext.getDefault();
        final Frames frames = dataContext.getFrames();
        final Frame frame = frames.getEME2000();
        final AnalyticalSolarPositionProvider solarPositionProvider = new AnalyticalSolarPositionProvider(dataContext);
        final FieldAbsoluteDate<Complex> fieldDate = new FieldAbsoluteDate<>(ComplexField.getInstance(), date);
        // WHEN
        final FieldVector3D<Complex> fieldPosition = solarPositionProvider.getPosition(fieldDate, frame);
        // THEN
        final Vector3D expectedPosition = solarPositionProvider.getPosition(date, frame);
        Assertions.assertEquals(expectedPosition, fieldPosition.toVector3D());
    }

    @Test
    void testGetPositionFieldDate() {
        // GIVEN
        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2025, 1, 1, 0, 0, 0),
                TimeScalesFactory.getUTC());
        final DataContext dataContext = DataContext.getDefault();
        final Frames frames = dataContext.getFrames();
        final Frame frame = frames.getGCRF();
        final AnalyticalSolarPositionProvider solarPositionProvider = new AnalyticalSolarPositionProvider(dataContext);
        final FieldAbsoluteDate<UnivariateDerivative1> fieldDate = new FieldAbsoluteDate<>(UnivariateDerivative1Field.getInstance(),
                date).shiftedBy(new UnivariateDerivative1(0., 1.));
        // WHEN
        final FieldVector3D<UnivariateDerivative1> fieldPosition = solarPositionProvider.getPosition(fieldDate, frame);
        // THEN
        Assertions.assertNotEquals(0., fieldPosition.getX().getFirstDerivative());
    }

    @Test
    void testPropagation() {
        // GIVEN
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final ExtendedPositionProvider positionProvider = new AnalyticalSolarPositionProvider();
        final CelestialBody body = createCelestialBody(sun.getGM(), positionProvider);
        // WHEN
        final NumericalPropagator propagator = createPropagator(body);
        final AbsoluteDate terminalDate = propagator.getInitialState().getDate().shiftedBy(1e6);
        final SpacecraftState actualState = propagator.propagate(terminalDate);
        // THEN
        final NumericalPropagator propagator2 = createPropagator(sun);
        final SpacecraftState expectedState = propagator2.propagate(terminalDate);
        Assertions.assertEquals(0., expectedState.getPosition().subtract(actualState.getPosition()).getNorm(), 1e-1);
    }

    private static CelestialBody createCelestialBody(final double mu, final ExtendedPositionProvider positionProvider) {
        return new CelestialBody() {
            @Override
            public Frame getInertiallyOrientedFrame() {
                return null;
            }

            @Override
            public Frame getBodyOrientedFrame() {
                return null;
            }

            @Override
            public String getName() {
                return "";
            }

            @Override
            public double getGM() {
                return mu;
            }

            @Override
            public Vector3D getPosition(AbsoluteDate date, Frame frame) {
                return positionProvider.getPosition(date, frame);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame) {
                return null;
            }
        };
    }

    private static NumericalPropagator createPropagator(final CelestialBody celestialBody) {
        final NumericalPropagator propagator = new NumericalPropagator(new ClassicalRungeKuttaIntegrator(100));
        final AbsoluteDate date = new AbsoluteDate(new DateTimeComponents(2000, 1, 1, 0, 0, 0),
                TimeScalesFactory.getUTC());
        final EquinoctialOrbit orbit = new EquinoctialOrbit(Constants.EGM96_EARTH_EQUATORIAL_RADIUS + 1000.e3,
                0.1, 0.2, 0.3, 0.4, 5., PositionAngleType.ECCENTRIC, FramesFactory.getGCRF(), date, Constants.EGM96_EARTH_MU);
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.addForceModel(new ThirdBodyAttraction(celestialBody));
        return propagator;
    }
 }
