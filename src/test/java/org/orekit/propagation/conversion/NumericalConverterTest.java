/* Copyright 2002-2015 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.forces.ForceModel;
import org.orekit.forces.SphericalSpacecraft;
import org.orekit.forces.drag.Atmosphere;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.SimpleExponentialAtmosphere;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.NewtonianAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;

public class NumericalConverterTest {

    private final static double mu = 3.9860047e14;
    private final static double minStep = 0.001;
    private final static double maxStep = 200.0;
    private final static double dP = 0.01;

    private Orbit orbit;
    private NumericalPropagator propagator;
    private ForceModel gravity;
    private ForceModel drag;

    @Test
    public void testConversionWithoutParameters() throws OrekitException, IOException, ParseException {
        checkFit(orbit, 9000, 300, 1.0e-3, 0.);
    }

    @Test
    public void testConversionWithFreeParameter() throws OrekitException, IOException, ParseException {
        checkFit(orbit, 9000, 300, 1.0e-3, 0.,
                 DragSensitive.DRAG_COEFFICIENT, NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
    }

    @Test
    public void testIntegrators() throws OrekitException {

        final double stepSize = 100.;

        FirstOrderIntegratorBuilder abBuilder = new AdamsBashforthIntegratorBuilder(2, minStep, maxStep, dP);
        checkFit(abBuilder);

        FirstOrderIntegratorBuilder amBuilder = new AdamsMoultonIntegratorBuilder(2, minStep, maxStep, dP);
        checkFit(amBuilder);

        FirstOrderIntegratorBuilder crkBuilder = new ClassicalRungeKuttaIntegratorBuilder(stepSize);
        checkFit(crkBuilder);

        FirstOrderIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        checkFit(dp54Builder);

        FirstOrderIntegratorBuilder eBuilder = new EulerIntegratorBuilder(stepSize);
        checkFit(eBuilder);

        FirstOrderIntegratorBuilder gBuilder = new GillIntegratorBuilder(stepSize);
        checkFit(gBuilder);

        FirstOrderIntegratorBuilder gbsBuilder = new GraggBulirschStoerIntegratorBuilder(minStep, maxStep, dP);
        checkFit(gbsBuilder);

        FirstOrderIntegratorBuilder hh54Builder = new HighamHall54IntegratorBuilder(minStep, maxStep, dP);
        checkFit(hh54Builder);

        FirstOrderIntegratorBuilder mBuilder = new MidpointIntegratorBuilder(stepSize);
        checkFit(mBuilder);

        FirstOrderIntegratorBuilder teBuilder = new ThreeEighthesIntegratorBuilder(stepSize);
        checkFit(teBuilder);
    }

    protected void checkFit(final Orbit orbit, final double duration,
                            final double stepSize, final double threshold,
                            final double expectedRMS,
                            final String... freeParameters)
        throws OrekitException, IOException, ParseException {

        NumericalPropagatorBuilder builder =
                        new NumericalPropagatorBuilder(mu,
                                                       propagator.getFrame(),
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       OrbitType.CARTESIAN, PositionAngle.TRUE);

        builder.addForceModel(drag);
        builder.addForceModel(gravity);

        JacobianPropagatorConverter fitter = new JacobianPropagatorConverter(builder,
                                                                             threshold,
                                                                             5000);

        fitter.convert(propagator, duration, 1 + (int) (duration / stepSize), freeParameters);

        NumericalPropagator prop = (NumericalPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        for (String param: freeParameters) {
            for (ForceModel force: propagator.getForceModels()) {
                if (force.isSupported(param)) {
                    for (ForceModel model: prop.getForceModels()) {
                        if (model.isSupported(param)) {
                            Assert.assertEquals(force.getParameter(param), model.getParameter(param), 0.);
                        }
                    }
                }
            }
        }

        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        final double eps = 1.e-12;
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getX(),
                            fitted.getPVCoordinates().getPosition().getX(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getPosition().getX()));
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getY(),
                            fitted.getPVCoordinates().getPosition().getY(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getPosition().getY()));
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getZ(),
                            fitted.getPVCoordinates().getPosition().getZ(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getPosition().getZ()));

        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getX()));
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getY()));
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            eps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getZ()));
    }

    protected void checkFit(final FirstOrderIntegratorBuilder foiBuilder) throws OrekitException {

        NumericalPropagatorBuilder builder = new NumericalPropagatorBuilder(mu,
                                                                            propagator.getFrame(),
                                                                            foiBuilder,
                                                                            OrbitType.CARTESIAN,
                                                                            PositionAngle.TRUE);

        builder.addForceModel(drag);
        builder.addForceModel(gravity);

        JacobianPropagatorConverter fitter = new JacobianPropagatorConverter(builder, 1.0, 500);

        fitter.convert(propagator, 1000., 11);

        NumericalPropagator prop = (NumericalPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        final double peps = 1.e-1;
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getX(),
                            fitted.getPVCoordinates().getPosition().getX(),
                            peps * FastMath.abs(orbit.getPVCoordinates().getPosition().getX()));
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getY(),
                            fitted.getPVCoordinates().getPosition().getY(),
                            peps * FastMath.abs(orbit.getPVCoordinates().getPosition().getY()));
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getZ(),
                            fitted.getPVCoordinates().getPosition().getZ(),
                            peps * FastMath.abs(orbit.getPVCoordinates().getPosition().getZ()));

        final double veps = 5.e-1;
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            veps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getX()));
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            veps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getY()));
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            veps * FastMath.abs(orbit.getPVCoordinates().getVelocity().getZ()));
    }

    @Before
    public void setUp() throws OrekitException, IOException, ParseException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        //use a orbit that comes close to Earth so the drag coefficient has an effect
        final Vector3D position     = new Vector3D(7.0e6, 1.0e6, 4.0e6).normalize()
                .scalarMultiply(Constants.WGS84_EARTH_EQUATORIAL_RADIUS + 300e3);
        final Vector3D velocity     = new Vector3D(-500.0, 8000.0, 1000.0);
        final AbsoluteDate initDate = new AbsoluteDate(2010, 10, 10, 10, 10, 10.0, TimeScalesFactory.getUTC());
        orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                     FramesFactory.getEME2000(), initDate, mu);

        final double[][] tol = NumericalPropagator.tolerances(dP, orbit, OrbitType.CARTESIAN);
        propagator = new NumericalPropagator(new DormandPrince853Integrator(minStep, maxStep, tol[0], tol[1]));
        propagator.setInitialState(new SpacecraftState(orbit));
        propagator.setOrbitType(OrbitType.CARTESIAN);

        gravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                        GravityFieldFactory.getNormalizedProvider(2, 0));

        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        earth.setAngularThreshold(1.e-7);
        final Atmosphere atmosphere = new SimpleExponentialAtmosphere(earth, 0.0004, 42000.0, 7500.0);
        final double dragCoef = 2.0;
        final SphericalSpacecraft ss = new SphericalSpacecraft(10., dragCoef, 0., 0.);
        drag = new DragForce(atmosphere, ss);

        propagator.addForceModel(gravity);
        propagator.addForceModel(drag);
    }

}

