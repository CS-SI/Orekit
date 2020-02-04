/* Copyright 2002-2020 CS Group
 * Licensed to CS Group (CS) under one or more
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
package org.orekit.forces.drag;


import java.util.Locale;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.BodyShape;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.BoxAndSolarArraySpacecraft;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.models.earth.atmosphere.HarrisPriester;
import org.orekit.models.earth.atmosphere.SimpleExponentialAtmosphere;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;

public class TimeSpanDragForceTest extends AbstractLegacyForceModelTest {

    /** Compute acceleration derivatives around input position at input date.
     *  Using finite differences in position.
     */
    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final AbsoluteDate date, final  Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass)
        {
        try {

            java.lang.reflect.Field atmosphereField = TimeSpanDragForce.class.getDeclaredField("atmosphere");
            atmosphereField.setAccessible(true);
            Atmosphere atmosphere = (Atmosphere) atmosphereField.get(forceModel);
            java.lang.reflect.Field spacecraftField = DragForce.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            // Get the DragSensitive model at date
            DragSensitive spacecraft = ((TimeSpanDragForce) (forceModel)).getDragSensitive(date);

            // retrieve derivation properties
            final DSFactory factory = mass.getFactory();

            // get atmosphere properties in atmosphere own frame
            final Frame      atmFrame  = atmosphere.getFrame();
            final Transform  toBody    = frame.getTransformTo(atmFrame, date);
            final FieldVector3D<DerivativeStructure> posBodyDS = toBody.transformPosition(position);
            final Vector3D   posBody   = posBodyDS.toVector3D();
            final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

            // estimate density model by finite differences and composition
            // the following implementation works only for first order derivatives.
            // this could be improved by adding a new method
            // getDensity(AbsoluteDate, DerivativeStructure, Frame)
            // to the Atmosphere interface
            if (factory.getCompiler().getOrder() > 1) {
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, factory.getCompiler().getOrder());
            }
            final double delta  = 1.0;
            final double x      = posBody.getX();
            final double y      = posBody.getY();
            final double z      = posBody.getZ();
            final double rho0   = atmosphere.getDensity(date, posBody, atmFrame);
            final double dRhodX = (atmosphere.getDensity(date, new Vector3D(x + delta, y,         z),         atmFrame) - rho0) / delta;
            final double dRhodY = (atmosphere.getDensity(date, new Vector3D(x,         y + delta, z),         atmFrame) - rho0) / delta;
            final double dRhodZ = (atmosphere.getDensity(date, new Vector3D(x,         y,         z + delta), atmFrame) - rho0) / delta;
            final double[] dXdQ = posBodyDS.getX().getAllDerivatives();
            final double[] dYdQ = posBodyDS.getY().getAllDerivatives();
            final double[] dZdQ = posBodyDS.getZ().getAllDerivatives();
            final double[] rhoAll = new double[dXdQ.length];
            rhoAll[0] = rho0;
            for (int i = 1; i < rhoAll.length; ++i) {
                rhoAll[i] = dRhodX * dXdQ[i] + dRhodY * dYdQ[i] + dRhodZ * dZdQ[i];
            }
            final DerivativeStructure rho = factory.build(rhoAll);

            // we consider that at first order the atmosphere velocity in atmosphere frame
            // does not depend on local position; however atmosphere velocity in inertial
            // frame DOES depend on position since the transform between the frames depends
            // on it, due to central body rotation rate and velocity composition.
            // So we use the transform to get the correct partial derivatives on vAtm
            final FieldVector3D<DerivativeStructure> vAtmBodyDS =
                            new FieldVector3D<>(factory.constant(vAtmBody.getX()),
                                            factory.constant(vAtmBody.getY()),
                                            factory.constant(vAtmBody.getZ()));
            final FieldPVCoordinates<DerivativeStructure> pvAtmBody = new FieldPVCoordinates<>(posBodyDS, vAtmBodyDS);
            final FieldPVCoordinates<DerivativeStructure> pvAtm     = toBody.getInverse().transformPVCoordinates(pvAtmBody);

            // now we can compute relative velocity, it takes into account partial derivatives with respect to position
            final FieldVector3D<DerivativeStructure> relativeVelocity = pvAtm.getVelocity().subtract(velocity);

            
            // Extract drag parameters of the proper model
            DerivativeStructure[] allParameters = forceModel.getParameters(factory.getDerivativeField());
            DerivativeStructure[] parameters = ((TimeSpanDragForce) (forceModel)).extractParameters(allParameters,
                                                                                                    new FieldAbsoluteDate<>(factory.getDerivativeField(), date));
            
            // compute acceleration with all its partial derivatives
            return spacecraft.dragAcceleration(new FieldAbsoluteDate<>(factory.getDerivativeField(), date),
                                               frame, position, rotation, mass, rho, relativeVelocity,
                                               parameters);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }
    
    /** Test that the getParameterDrivers method is working as expected
     * on an IsotropicDrag-based (ie. spherical) TimeSpanDragForce model.
     */
    @Test
    public void testGetParameterDriversSphere() {

        // Atmosphere
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        // A date
        AbsoluteDate date = new AbsoluteDate("2000-01-01T00:00:00.000", TimeScalesFactory.getUTC());
        
        // One IsotropicDrag added, only one driver should be in the drivers' array
        // its name should be the default name: IsotropicDrag.DRAG_COEFFICIENT
        // -----------------------
        double dragArea = 2.;
        double dragCd0 = 0.;
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, new IsotropicDrag(dragArea, dragCd0));
        Assert.assertFalse(forceModel.dependsOnPositionOnly());
        ParameterDriver[] drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(1,  drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        
        // Extract drag model at an arbitrary epoch and check it is the one added
        IsotropicDrag isoDrag = (IsotropicDrag) forceModel.getDragSensitive(date);
        drivers = isoDrag.getDragParametersDrivers();
        Assert.assertEquals(1, drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        
        // 3 IsotropicDrag models added, with one default
        // ----------------------------------------------
        double dragCd1 = 1.;
        double dragCd2 = 2.;
        double dt = 120.;
        // Build the force model
        isoDrag = new IsotropicDrag(dragArea, dragCd0);
        IsotropicDrag isoDrag1 = new IsotropicDrag(dragArea, dragCd1);
        IsotropicDrag isoDrag2 = new IsotropicDrag(dragArea, dragCd2);
        forceModel = new TimeSpanDragForce(atmosphere, isoDrag);
        forceModel.addDragSensitiveValidAfter(isoDrag1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(isoDrag2, date.shiftedBy(-dt));
        
        // Extract the drivers and check their values and names
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(3,  drivers.length);
        Assert.assertEquals(dragCd2,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(),
                            drivers[0].getName());
        Assert.assertEquals(dragCd0,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[1].getName());
        Assert.assertEquals(dragCd0,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(),
                            drivers[2].getName());
        
        // Check that proper models are returned at significant test dates
        // Cd0 model
        double eps = 1.e-14;
        Assert.assertEquals(isoDrag, forceModel.getDragSensitive(date));
        Assert.assertEquals(isoDrag, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assert.assertEquals(isoDrag, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assert.assertEquals(isoDrag2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assert.assertEquals(isoDrag2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assert.assertEquals(isoDrag1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assert.assertEquals(isoDrag1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));
        
        // Add a custom-named driver
        // ------------
        double dragCd3 = 3.;
        IsotropicDrag isoDrag3 = new IsotropicDrag(dragArea, dragCd3);
        isoDrag3.getDragParametersDrivers()[0].setName("custom-Cd");
        forceModel.addDragSensitiveValidAfter(isoDrag3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(4,  drivers.length);
        Assert.assertEquals(dragCd3,  drivers[3].getValue(), 0.);
        Assert.assertEquals("custom-Cd", drivers[3].getName());
        
    }
    

    /** Test parameter derivatives for an IsotropicDrag TimeSpanDragForce.
     *  This test is more or less a copy of the same one in DragForceTest class
     *  with addition of several IsotropicDrag models valid at different dates
     *  to test that the different parameters' derivatives are computed correctly.
     */
    @Test
    public void testParameterDerivativeSphere() {

        // Low Earth orbit definition (about 360km altitude)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final AbsoluteDate date = new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       date,
                                                       Constants.EIGEN5C_EARTH_MU));

        // Atmosphere
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        
        // Constant area for the different tests
        final double dragArea = 2.5;
        
        // Initialize force model (first coef is valid at all epochs)
        final double dragCd  = 1.2;
        final IsotropicDrag isotropicDrag = new IsotropicDrag(dragArea, dragCd);
        isotropicDrag.getDragParametersDrivers()[0].setName("Cd");
        final TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, isotropicDrag);
        
                
        // After t2 = t + 4h
        final double dragCd2 = 3.;
        final double dt2 = 4 * 3600.;
        final AbsoluteDate date2 = date.shiftedBy(dt2);
        final IsotropicDrag isotropicDrag2 = new IsotropicDrag(dragArea, dragCd2);
        isotropicDrag2.getDragParametersDrivers()[0].setName("Cd2");
        forceModel.addDragSensitiveValidAfter(isotropicDrag2, date2);
        
        // Before t3 = t - 1day
        final double dragCd3 = 3.;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final IsotropicDrag isotropicDrag3 = new IsotropicDrag(dragArea, dragCd3);
        isotropicDrag3.getDragParametersDrivers()[0].setName("Cd3");
        forceModel.addDragSensitiveValidBefore(isotropicDrag3, date3);
        

        Assert.assertFalse(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only "Cd" shouldn't be 0.
        checkParameterDerivative(state, forceModel, "Cd" , 1.0e-4, 2.0e-12);
        checkParameterDerivative(state, forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, "Cd3", 1.0e-4, 0.);
        
        // Check parameter derivatives after date2: only "Cd2" shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cd", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cd2", 1.0e-4, 2.0e-12);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cd3", 1.0e-4, 0.);
        
        // Check parameter derivatives after date3: only "Cd3" shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cd", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cd3", 1.0e-4, 2.0e-12);
    }

    /** Test check state Jacobian computation. */
    @Test
    public void testStateJacobianSphere()
        {

        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);

        
        
        // Atmosphere
        final Atmosphere atmosphere = new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        
        // Time span drag force model init
        double dragArea = 2.;
        double dragCd0 = 1.;
        double dragCd1 = 2.;
        double dragCd2 = 3.;
        double dt = 1. * 3600.;
        // Build the force model
        IsotropicDrag isoDrag0 = new IsotropicDrag(dragArea, dragCd0);
        IsotropicDrag isoDrag1 = new IsotropicDrag(dragArea, dragCd1);
        IsotropicDrag isoDrag2 = new IsotropicDrag(dragArea, dragCd2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, isoDrag0);
        forceModel.addDragSensitiveValidAfter(isoDrag1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(isoDrag2, date.shiftedBy(-dt));
        
        // Check state derivatives inside first IsotropicDrag model
        NumericalPropagator propagator =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                               tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        // Set target date to 0.5*dt to be inside 1st drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(0.5 * dt),
                           1e3, tolerances[0], 9.2e-10);
        
        // Check state derivatives inside 2nd IsotropicDrag model
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to 1.5*dt to be inside 2nd drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(1.5 * dt),
                           1e3, tolerances[0], 6.7e-9);
        
        // Check state derivatives inside 3rd IsotropicDrag model
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to *1.5*dt to be inside 3rd drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(-1.5 * dt),
                           1e3, tolerances[0], 6.0e-9);
    }
    
    /** Test that the getParameterDrivers method is working as expected
     * on an BoxAndSolarArraySpacecraft-based TimeSpanDragForce model.
     * Here only the drag coefficient is modeled.
     */
    @Test
    public void testGetParameterDriversBoxOnlyDrag() {

        // Atmosphere
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final Atmosphere atmosphere = new HarrisPriester(sun,
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        // A date
        AbsoluteDate date = new AbsoluteDate("2000-01-01T00:00:00.000", TimeScalesFactory.getUTC());
        
        // One BoxAndSolarArraySpacecraft added, test with one or two "default" drivers
        // -----------------------

        double dragCd0 = 0.;
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd0,
                                                                         0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        Assert.assertFalse(forceModel.dependsOnPositionOnly());
        ParameterDriver[] drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(1,  drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        
        // Extract drag model at an arbitrary epoch and check it is the one added
        BoxAndSolarArraySpacecraft box = (BoxAndSolarArraySpacecraft) forceModel.getDragSensitive(date);
        drivers = box.getDragParametersDrivers();
        Assert.assertEquals(1, drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        
        // 3 BoxAndSolarArraySpacecraft models added, with one "default" in the middle
        // ----------------------------------------------
        double dragCd1 = 1.;
        double dragCd2 = 2.;
        double dt = 120.;
        // Build the force model
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd1,
                                                                         0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd2,
                                                                         0.7, 0.2);
        forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));
        
        // Extract the drivers and check their values and names
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(3,  drivers.length);
        Assert.assertEquals(dragCd2,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(),
                            drivers[0].getName());
        
        Assert.assertEquals(dragCd0,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[1].getName());
        
        Assert.assertEquals(dragCd1,  drivers[2].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(),
                            drivers[2].getName());
        
        // Check the models at dates
        // Cd0 model
        double eps = 1.e-14;
        Assert.assertEquals(box0, forceModel.getDragSensitive(date));
        Assert.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assert.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assert.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assert.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assert.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assert.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));
        
        // Add a custom-named driver
        // ----------------
        double dragCd3 = 3.;
        BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd3,
                                                                         0.7, 0.2);
        box3.getDragParametersDrivers()[0].setName("custom-Cd");
        forceModel.addDragSensitiveValidAfter(box3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(4,  drivers.length);
        Assert.assertEquals(dragCd3,  drivers[3].getValue(), 0.);
        Assert.assertEquals("custom-Cd", drivers[3].getName());
    }
    
    /** Test that the getParameterDrivers method is working as expected
     * on an BoxAndSolarArraySpacecraft-based TimeSpanDragForce model.
     * Here both drag and lift coefficients are modeled.
     */
    @Test
    public void testGetParameterDriversBox() {

        // Atmosphere
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final Atmosphere atmosphere = new HarrisPriester(sun,
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));

        // A date
        AbsoluteDate date = new AbsoluteDate("2000-01-01T00:00:00.000", TimeScalesFactory.getUTC());
        
        // One BoxAndSolarArraySpacecraft added, test with one or two "default" drivers
        // -----------------------

        double dragCd0 = 0.;
        double dragCl0 = 0.;
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd0, dragCl0,
                                                                         0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        Assert.assertFalse(forceModel.dependsOnPositionOnly());
        ParameterDriver[] drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(2,  drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        Assert.assertEquals(dragCl0,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.LIFT_RATIO,  drivers[1].getName());
        
        // Extract drag model at an arbitrary epoch and check it is the one added
        BoxAndSolarArraySpacecraft box = (BoxAndSolarArraySpacecraft) forceModel.getDragSensitive(date);
        drivers = box.getDragParametersDrivers();
        Assert.assertEquals(2, drivers.length);
        Assert.assertEquals(dragCd0,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[0].getName());
        Assert.assertEquals(dragCl0,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.LIFT_RATIO,  drivers[1].getName());
        
        // 3 BoxAndSolarArraySpacecraft models added, with one "default" in the middle
        // ----------------------------------------------
        double dragCd1 = 1.;
        double dragCl1 = 0.1;
        double dragCd2 = 2.;
        double dragCl2 = 0.2;
        double dt = 120.;
        // Build the force model
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd1, dragCl1,
                                                                         0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd2, dragCl2,
                                                                         0.7, 0.2);
        forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));
        
        // Extract the drivers and check their values and names
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(6,  drivers.length);
        Assert.assertEquals(dragCd2,  drivers[0].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(),
                            drivers[0].getName());
        Assert.assertEquals(dragCl2,  drivers[1].getValue(), 0.);
        Assert.assertEquals(DragSensitive.LIFT_RATIO + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(),
                            drivers[1].getName());
        
        Assert.assertEquals(dragCd0,  drivers[2].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers[2].getName());
        Assert.assertEquals(dragCl0,  drivers[3].getValue(), 0.);
        Assert.assertEquals(DragSensitive.LIFT_RATIO,  drivers[3].getName());
        
        Assert.assertEquals(dragCd1,  drivers[4].getValue(), 0.);
        Assert.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(),
                            drivers[4].getName());
        Assert.assertEquals(dragCl1,  drivers[5].getValue(), 0.);
        Assert.assertEquals(DragSensitive.LIFT_RATIO + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(),
                            drivers[5].getName());
        
        // Check the models at dates
        // Cd0 model
        double eps = 1.e-14;
        Assert.assertEquals(box0, forceModel.getDragSensitive(date));
        Assert.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assert.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assert.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assert.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assert.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assert.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));


        // Add a custom-named driver
        // ----------------
        double dragCd3 = 3.;
        double dragCl3 = 0.3;
        BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd3, dragCl3,
                                                                         0.7, 0.2);
        box3.getDragParametersDrivers()[0].setName("custom-Cd");
        box3.getDragParametersDrivers()[1].setName("custom-Cl");
        forceModel.addDragSensitiveValidAfter(box3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assert.assertEquals(8,  drivers.length);
        Assert.assertEquals(dragCd3,  drivers[6].getValue(), 0.);
        Assert.assertEquals("custom-Cd", drivers[6].getName());
        Assert.assertEquals(dragCl3,  drivers[7].getValue(), 0.);
        Assert.assertEquals("custom-Cl", drivers[7].getName());
    }

    /** Test parameter derivatives for an BoxAndSolarArraySpacecraft TimeSpanDragForce.
     *  This test is more or less a copy of the same one in DragForceTest class
     *  with addition of several BoxAndSolarArraySpacecraft models valid at different dates
     *  to test that the different parameters' derivatives are computed correctly.
     */
    @Test
    public void testParametersDerivativesBox() {
        
        // Low Earth orbit definition (about 360km altitude)
        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final AbsoluteDate date = new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI());
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       date,
                                                       Constants.EIGEN5C_EARTH_MU));

        // Atmosphere
        final CelestialBody sun = CelestialBodyFactory.getSun();
        final Atmosphere atmosphere = new HarrisPriester(sun,
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
                
        // Initialize force model (first coef is valid at all epochs)
        final double dragCd  = 1.;
        final double dragCl  = 0.1;
        final BoxAndSolarArraySpacecraft box = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                               sun, 20.0, Vector3D.PLUS_J,
                                                                               dragCd, dragCl, 0.7, 0.2);
        final TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box);
        
                
        // After t2 = 4h
        final double dragCd2 = 2.;
        final double dragCl2 = 0.2;
        final double dt2 = 4 * 3600.;
        final AbsoluteDate date2 = date.shiftedBy(dt2);
        final BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                               sun, 20.0, Vector3D.PLUS_J,
                                                                               dragCd2, dragCl2, 0.7, 0.2);
        box2.getDragParametersDrivers()[0].setName("Cd2");
        box2.getDragParametersDrivers()[1].setName("Cl2");
        forceModel.addDragSensitiveValidAfter(box2, date2);
        
        // Before t3 = 1day
        final double dragCd3 = 3.;
        final double dragCl3 = 0.3;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                               sun, 20.0, Vector3D.PLUS_J,
                                                                               dragCd3, dragCl3, 0.7, 0.2);
        box3.getDragParametersDrivers()[0].setName("Cd3");
        forceModel.addDragSensitiveValidBefore(box3, date3);
        
        // Name of Cl3 is kept as default for the test
        final String nameCl3 = DragSensitive.LIFT_RATIO + TimeSpanDragForce.DATE_BEFORE + date3;
        

        Assert.assertFalse(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only 1st model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);
        checkParameterDerivative(state, forceModel, DragSensitive.LIFT_RATIO, 1.0e-4, 2.0e-11);
        checkParameterDerivative(state, forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, "Cl2", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, "Cd3", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, nameCl3, 1.0e-4, 0.);
                                 
        // Check parameter derivatives after date2: only 2nd model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, DragSensitive.LIFT_RATIO, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cd2", 1.0e-4, 2.2e-12);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cl2", 1.0e-4, 2.0e-11);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "Cd3", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, nameCl3, 1.0e-4, 0.);
        
        // Check parameter derivatives before date3: only 3nd model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, DragSensitive.LIFT_RATIO, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cl2", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "Cd3", 1.0e-4, 2.0e-12);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, nameCl3, 1.0e-4, 2.0e-11);
    }

    /** Test state Jacobian computation using finite differences in position
     * and method {@link #accelerationDerivatives}
     */
    @Test
    public void testJacobianBoxVs80Implementation()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit refOrbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                            0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                            Constants.EIGEN5C_EARTH_MU);
        CelestialBody sun = CelestialBodyFactory.getSun();
        
        // Atmosphere
        final Atmosphere atmosphere =
                        new HarrisPriester(sun,
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)));       
        // Time span drag force model initialization
        double dragCd0 = 1.;
        double dragCl0 = 0.1;
        double dragCd1 = 2.;
        double dragCl1 = 0.2;
        double dragCd2 = 3.;
        double dragCl2 = 0.3;
        double dt = 3. * 3600.;
        
        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));
        
        // Check state derivatives inside first box model
        Orbit orbit = refOrbit.shiftedBy(0.);
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.VVLH),
                                             5e-6, false);
        
        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.VVLH),
                                             5e-6, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.VVLH),
                                             5e-6, false);

    }

    /** Test state Jacobian computation using finite differences once again.
     * This time the differentiation is made using built-in numerical differentiation method.
     */
    @Test
    public void testJacobianBoxVsFiniteDifferences()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit refOrbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                            0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                            Constants.EIGEN5C_EARTH_MU);
        CelestialBody sun = CelestialBodyFactory.getSun();
        
        // Atmosphere
        final Atmosphere atmosphere =
                        new HarrisPriester(sun,
                                           new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                Constants.WGS84_EARTH_FLATTENING,
                                                                FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        
        // Time span drag force model initialization
        double dragCd0 = 1.;
        double dragCl0 = 0.1;
        double dragCd1 = 2.;
        double dragCl1 = 0.2;
        double dragCd2 = 3.;
        double dragCl2 = 0.3;
        double dt = 3. * 3600.;
        
        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));
        
        // Check state derivatives inside first box model
        Orbit orbit = refOrbit.shiftedBy(0.);
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, Propagator.DEFAULT_LAW, 1.0, 7.0e-9, false);
        
        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, Propagator.DEFAULT_LAW, 1.0, 4.0e-9, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, Propagator.DEFAULT_LAW, 1.0, 6.0e-8, false);
    }

    /** Test check state Jacobian computation. */
    @Test
    public void testGlobalStateJacobianBox()
        {

        // Initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        OrbitType integrationType = OrbitType.CARTESIAN;
        double[][] tolerances = NumericalPropagator.tolerances(0.01, orbit, integrationType);
        CelestialBody sun = CelestialBodyFactory.getSun();
        
        // Atmosphere
        final Atmosphere atmosphere = new HarrisPriester(sun,
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true)));       
        // Time span drag force model init
        double dragCd0 = 1.;
        double dragCl0 = 0.1;
        double dragCd1 = 2.;
        double dragCl1 = 0.2;
        double dragCd2 = 3.;
        double dragCl2 = 0.3;
        double dt = 1. * 3600.;
        
        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));
        
        // Check state derivatives inside first box model
        NumericalPropagator propagator =
                        new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                               tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);
        // Set target date to 0.5*dt to be inside 1st drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(0.5 * dt),
                           1e3, tolerances[0], 1.1e-9);
        
        // Check state derivatives inside 2nd box model
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to 1.5*dt to be inside 2nd drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(1.5 * dt),
                           1e3, tolerances[0], 9.7e-9);
        
        // Check state derivatives inside 3rd box model
        propagator = new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                            tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        propagator.addForceModel(forceModel);
        // Set target date to *1.5*dt to be inside 3rd drag model
        // The further away we are from the initial date, the greater the checkTolerance parameter must be set
        checkStateJacobian(propagator, state0, date.shiftedBy(-1.5 * dt),
                           1e3, tolerances[0], 4.9e-9);
    }

    /** Testing if the propagation between the FieldPropagation and the propagation is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the Taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 1.2);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngle.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();

        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getX(), finPVC_R.getPosition().getX(), FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getY(), finPVC_R.getPosition().getY(), FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertEquals(finPVC_DS.toPVCoordinates().getPosition().getZ(), finPVC_R.getPosition().getZ(), FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);

        long number = 23091991;
        RandomGenerator RG = new Well19937a(number);
        GaussianRandomGenerator NGG = new GaussianRandomGenerator(RG);
        UncorrelatedRandomVectorGenerator URVG = new UncorrelatedRandomVectorGenerator(new double[] {0.0 , 0.0 , 0.0 , 0.0 , 0.0 , 0.0 },
                                                                                       new double[] {1e3, 0.005, 0.005, 0.01, 0.01, 0.01},
                                                                                       NGG);
        double a_R = a_0.getReal();
        double e_R = e_0.getReal();
        double i_R = i_0.getReal();
        double R_R = R_0.getReal();
        double O_R = O_0.getReal();
        double n_R = n_0.getReal();
        for (int ii = 0; ii < 1; ii++){
            double[] rand_next = URVG.nextVector();
            double a_shift = a_R + rand_next[0];
            double e_shift = e_R + rand_next[1];
            double i_shift = i_R + rand_next[2];
            double R_shift = R_R + rand_next[3];
            double O_shift = O_R + rand_next[4];
            double n_shift = n_R + rand_next[5];

            KeplerianOrbit shiftedOrb = new KeplerianOrbit(a_shift, e_shift, i_shift, R_shift, O_shift, n_shift,
                                                           PositionAngle.MEAN,
                                                           EME,
                                                           J2000.toAbsoluteDate(),
                                                           Constants.EIGEN5C_EARTH_MU
                                                           );

            SpacecraftState shift_iSR = new SpacecraftState(shiftedOrb);

            NumericalPropagator shift_NP = new NumericalPropagator(RIntegrator);

            shift_NP.setInitialState(shift_iSR);

            shift_NP.addForceModel(forceModel);

            SpacecraftState finalState_shift = shift_NP.propagate(target.toAbsoluteDate());


            PVCoordinates finPVC_shift = finalState_shift.getPVCoordinates();

            //position check

            FieldVector3D<DerivativeStructure> pos_DS = finPVC_DS.getPosition();
            double x_DS = pos_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double z_DS = pos_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);

            //System.out.println(pos_DS.getX().getPartialDerivative(1));

            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 1e-5);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 1e-5);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 1e-5);

            //velocity check

            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vz_DS = vel_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 1e-7);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 1e-7);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 1e-7);
            //acceleration check

            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double az_DS = acc_DS.getZ().taylor(rand_next[0], rand_next[1], rand_next[2], rand_next[3], rand_next[4], rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 1e-5);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 1e-5);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 1e-5);
        }


    }


    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngle.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(10.0, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }
    
    @Test
    //@Ignore
    public void testSeveralDragForceModel() {
        
        AbsoluteDate initialDate = new AbsoluteDate(2004, 1, 1, 0, 0, 0., TimeScalesFactory.getUTC());
        Frame frame       = FramesFactory.getEME2000();
        double rpe         = 160.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double rap         = 180.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double inc         = FastMath.toRadians(0.);
        double aop         = FastMath.toRadians(0.);
        double raan        = FastMath.toRadians(0.);
        double mean        = FastMath.toRadians(180.);  
        double mass        = 100.;
        KeplerianOrbit orbit = new KeplerianOrbit(0.5 * (rpe + rap), (rap - rpe) / (rpe + rap),
                                                  inc, aop, raan, mean, PositionAngle.MEAN,
                                                  frame, initialDate, Constants.EIGEN5C_EARTH_MU);

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 2.6e-10, 200000, 26000);
        
        // Simulation duration
        final double duration = 3600.;
        
        // Drag area
        final double dragArea = 10.;
        
        
        // Arrays of Cds, date as frac of duration and drivers' selection
        final double[] Cds  = new double[] {1., 2., 3.};
        final double[] dt   = new double[] {0., 1./2., 3./4.};
        final boolean[] sel = new boolean[] {true, true, true};
        
        
        // Arrays of drag model
        int nModel = Cds.length;
        final IsotropicDrag[] shapes = new IsotropicDrag[nModel];
        final AbsoluteDate[]  dates  = new AbsoluteDate[nModel];
        for (int i = 0; i < nModel; i++) {
            shapes[i] = new IsotropicDrag(dragArea, Cds[i]);
            dates[i]  = initialDate.shiftedBy(duration * dt[i]);
            for (ParameterDriver driver : shapes[i].getDragParametersDrivers()) {
                //driver.setName("Cd - " + dates[i].getComponents(TimeScalesFactory.getUTC()).getTime().toString());
                driver.setSelected(sel[i]);
            }
        }        
        
        // Build time span drag force model
        TimeSpanDragForce dragForce = new TimeSpanDragForce(atmosphere, shapes[0]);
        for (int i = 0; i < nModel; i++) {
            dragForce.addDragSensitiveValidAfter(shapes[i], dates[i]);    
        }
        
        // Check values
        for(ParameterDriver driver : dragForce.getParametersDrivers()) {
            System.out.println(driver.getName() + " = " + driver.getValue());
        }

        
        DragForce singleDragForce = new DragForce(atmosphere, new IsotropicDrag(dragArea, 1.));
        for (ParameterDriver driver : singleDragForce.getParametersDrivers()) {
            driver.setName(driver.getName() + " - single");
            driver.setSelected(true);
        }
        
        
        // Set up propagator
        double[][]          tolerance  = NumericalPropagator.tolerances(0.1, orbit, OrbitType.CARTESIAN);
        AbstractIntegrator  integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerance[0], tolerance[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMu(orbit.getMu());
        propagator.addForceModel(dragForce);
        //propagator.addForceModel(singleDragForce);
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(orbit, mass)));
        AbsoluteDate finalDate = initialDate.shiftedBy(duration);
        final int nParams = partials.getSelectedParameters().getNbParams();
        
        // prepare jacob and mapper
        double[][] dYdP = new double[6][nParams];
        JacobiansMapper mapper = partials.getMapper();
        
        SpacecraftState[] states = new SpacecraftState[nModel];
        
        states[1] = propagator.propagate(dates[1].shiftedBy(-1.));
        printCd(dragForce, propagator);
        printState(propagator);
        printJacobian(partials, propagator);
        
        propagator.propagate(dates[1].shiftedBy(+1.));
        
        states[2] = propagator.propagate(dates[2].shiftedBy(-1.));
        printCd(dragForce, propagator);
        printState(propagator);
        printJacobian(partials, propagator);
        
        SpacecraftState state = propagator.propagate(finalDate);
        printCd(dragForce, propagator);
        printState(propagator);
        printJacobian(partials, propagator);
            
        double x = 1;
    }
    
    public void printCd(TimeSpanDragForce dragForce, Propagator propagator) {
        AbsoluteDate t = propagator.getInitialState().getDate();
        System.out.println("\nCd @" + t + ": " + dragForce.getDragSensitive(t).getDragParametersDrivers()[0].getValue());
    }
    
    public void printState(Propagator propagator) {
        AbsoluteDate t = propagator.getInitialState().getDate();
        System.out.println("\nState @" + t + ": " + propagator.getInitialState());
    }
    
    public void printJacobian(final PartialDerivativesEquations partials,
                              final Propagator propagator) {
        
        final int nParams = partials.getSelectedParameters().getNbParams();
        final double[][] dYdP = new double[6][nParams];
        JacobiansMapper mapper = partials.getMapper();
        mapper.getParametersJacobian(propagator.getInitialState(), dYdP);
        
        // Print drivers' names
        System.out.println("Jacobian at " + propagator.getInitialState().getDate());
        for (ParameterDriver driver : partials.getSelectedParameters().getDrivers()) {
            System.out.format(Locale.US, "\t %s", driver.getName());
        }
        System.out.println();
        
        // Get Jacobian and print it
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < nParams; j++) {
                System.out.format(Locale.US, "\t %10.5f", dYdP[i][j]);
            }
            System.out.println();
        }
    }
}


