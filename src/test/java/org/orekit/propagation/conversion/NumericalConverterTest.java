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
package org.orekit.propagation.conversion;

import java.io.IOException;
import java.text.ParseException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.InertialProvider;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.drag.IsotropicDrag;
import org.orekit.forces.drag.atmosphere.Atmosphere;
import org.orekit.forces.drag.atmosphere.SimpleExponentialAtmosphere;
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
import org.orekit.utils.ParameterDriver;

public class NumericalConverterTest {

    private double mu;
    private double minStep;
    private double maxStep;
    private double dP;

    private Orbit orbit;
    private NumericalPropagator propagator;
    private ForceModel gravity;
    private ForceModel drag;
    private Atmosphere atmosphere;
    private double crossSection;

    @Test
    public void testOnlyCartesianAllowed() throws OrekitException {
        NumericalPropagatorBuilder builder =
                        new NumericalPropagatorBuilder(OrbitType.CIRCULAR.convertType(orbit),
                                                       new LutherIntegratorBuilder(100.0),
                                                       PositionAngle.TRUE, 1.0);
        builder.addForceModel(drag);
        builder.addForceModel(gravity);
        try {
            new JacobianPropagatorConverter(builder, 1.0e-3, 5000);
            Assert.fail("an exception should have been thrown");
        } catch (OrekitException oe) {
            Assert.assertEquals(OrekitMessages.ORBIT_TYPE_NOT_ALLOWED, oe.getSpecifier());
            Assert.assertEquals(OrbitType.CIRCULAR, oe.getParts()[0]);
            Assert.assertEquals(OrbitType.CARTESIAN, oe.getParts()[1]);
        }
    }

    @Test
    public void testConversionWithoutParameters() throws OrekitException, IOException, ParseException {
        checkFit(orbit, 6000, 300, 1.0e-3, 0.855);
    }

    @Test
    public void testConversionWithFreeParameter() throws OrekitException, IOException, ParseException {
        checkFit(orbit, 6000, 300, 1.0e-3, 0.826,
                 DragSensitive.DRAG_COEFFICIENT, NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT);
    }

    @Test
    public void testIntegrators() throws OrekitException {

        final double stepSize = 100.;

        ODEIntegratorBuilder abBuilder = new AdamsBashforthIntegratorBuilder(2, minStep, maxStep, dP);
        checkFit(abBuilder);

        ODEIntegratorBuilder amBuilder = new AdamsMoultonIntegratorBuilder(2, minStep, maxStep, dP);
        checkFit(amBuilder);

        ODEIntegratorBuilder crkBuilder = new ClassicalRungeKuttaIntegratorBuilder(stepSize);
        checkFit(crkBuilder);

        ODEIntegratorBuilder lBuilder = new LutherIntegratorBuilder(stepSize);
        checkFit(lBuilder);

        ODEIntegratorBuilder dp54Builder = new DormandPrince54IntegratorBuilder(minStep, maxStep, dP);
        checkFit(dp54Builder);

        ODEIntegratorBuilder eBuilder = new EulerIntegratorBuilder(stepSize);
        checkFit(eBuilder);

        ODEIntegratorBuilder gBuilder = new GillIntegratorBuilder(stepSize);
        checkFit(gBuilder);

        ODEIntegratorBuilder gbsBuilder = new GraggBulirschStoerIntegratorBuilder(minStep, maxStep, dP);
        checkFit(gbsBuilder);

        ODEIntegratorBuilder hh54Builder = new HighamHall54IntegratorBuilder(minStep, maxStep, dP);
        checkFit(hh54Builder);

        ODEIntegratorBuilder mBuilder = new MidpointIntegratorBuilder(stepSize);
        checkFit(mBuilder);

        ODEIntegratorBuilder teBuilder = new ThreeEighthesIntegratorBuilder(stepSize);
        checkFit(teBuilder);
    }

    protected void checkFit(final Orbit orbit, final double duration,
                            final double stepSize, final double threshold,
                            final double expectedRMS,
                            final String... freeParameters)
        throws OrekitException, IOException, ParseException {

        NumericalPropagatorBuilder builder =
                        new NumericalPropagatorBuilder(OrbitType.CARTESIAN.convertType(orbit),
                                                       new DormandPrince853IntegratorBuilder(minStep, maxStep, dP),
                                                       PositionAngle.TRUE, dP);

        ForceModel guessedDrag = drag;
        ForceModel guessedGravity = gravity;
        for (String param: freeParameters) {
            if (DragSensitive.DRAG_COEFFICIENT.equals(param)) {
                // we want to adjust drag coefficient, we need to start from a wrong value
                ParameterDriver driver = drag.getParameterDriver(param);
                double coeff = driver.getReferenceValue() - driver.getScale();
                guessedDrag = new DragForce(atmosphere, new IsotropicDrag(crossSection, coeff));
            } else if (NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT.equals(param)) {
                // we want to adjust mu, we need to start from  a wrong value
                guessedGravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                                       GravityFieldFactory.getNormalizedProvider(2, 0));
                ParameterDriver driver = guessedGravity.getParameterDriver(param);
                driver.setValue(driver.getReferenceValue() + driver.getScale());
            }
        }
        builder.addForceModel(guessedDrag);
        builder.addForceModel(guessedGravity);

        JacobianPropagatorConverter fitter = new JacobianPropagatorConverter(builder,
                                                                             threshold,
                                                                             5000);

        fitter.convert(propagator, duration, 1 + (int) (duration / stepSize), freeParameters);

        NumericalPropagator prop = (NumericalPropagator)fitter.getAdaptedPropagator();
        Orbit fitted = prop.getInitialState().getOrbit();

        for (String param: freeParameters) {
            for (ForceModel force: propagator.getAllForceModels()) {
                if (force.isSupported(param)) {
                    for (ForceModel model: prop.getAllForceModels()) {
                        if (model.isSupported(param)) {
                            Assert.assertEquals(force.getParameterDriver(param).getValue(),
                                                model.getParameterDriver(param).getValue(),
                                                3.0e-4 * FastMath.abs(force.getParameterDriver(param).getValue()));
                        }
                    }
                }
            }
        }

        Assert.assertEquals(expectedRMS, fitter.getRMS(), 0.01 * expectedRMS);

        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getX(),
                            fitted.getPVCoordinates().getPosition().getX(),
                            1.1);
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getY(),
                            fitted.getPVCoordinates().getPosition().getY(),
                            1.1);
        Assert.assertEquals(orbit.getPVCoordinates().getPosition().getZ(),
                            fitted.getPVCoordinates().getPosition().getZ(),
                            1.1);

        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getX(),
                            fitted.getPVCoordinates().getVelocity().getX(),
                            0.0005);
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getY(),
                            fitted.getPVCoordinates().getVelocity().getY(),
                            0.0005);
        Assert.assertEquals(orbit.getPVCoordinates().getVelocity().getZ(),
                            fitted.getPVCoordinates().getVelocity().getZ(),
                            0.0005);
    }

    protected void checkFit(final ODEIntegratorBuilder foiBuilder) throws OrekitException {

        NumericalPropagatorBuilder builder = new NumericalPropagatorBuilder(OrbitType.CARTESIAN.convertType(orbit),
                                                                            foiBuilder,
                                                                            PositionAngle.TRUE,
                                                                            1.0);

        builder.addForceModel(drag);
        builder.addForceModel(gravity);
        builder.setAttitudeProvider(InertialProvider.EME2000_ALIGNED);
        builder.setMass(1000.0);

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
        gravity = new HolmesFeatherstoneAttractionModel(FramesFactory.getITRF(IERSConventions.IERS_2010, true),
                                                        GravityFieldFactory.getNormalizedProvider(2, 0));
        mu = gravity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue();
        minStep = 1.0;
        maxStep = 600.0;
        dP = 10.0;

        // use a orbit that comes close to Earth so the drag coefficient has an effect
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


        final OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                            Constants.WGS84_EARTH_FLATTENING,
                                                            FramesFactory.getITRF(IERSConventions.IERS_2010, true));
        earth.setAngularThreshold(1.e-7);
        atmosphere = new SimpleExponentialAtmosphere(earth, 0.0004, 42000.0, 7500.0);
        final double dragCoef = 2.0;
        crossSection = 10.0;
        drag = new DragForce(atmosphere, new IsotropicDrag(crossSection, dragCoef));

        propagator.addForceModel(gravity);
        propagator.addForceModel(drag);
    }

}

