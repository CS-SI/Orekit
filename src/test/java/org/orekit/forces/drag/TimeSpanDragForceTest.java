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
package org.orekit.forces.drag;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaFieldIntegrator;
import org.hipparchus.ode.nonstiff.ClassicalRungeKuttaIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.attitudes.LofOffset;
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
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.FieldNumericalPropagator;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.DateComponents;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.TimeSpanMap;

import java.util.List;

public class TimeSpanDragForceTest extends AbstractLegacyForceModelTest {

    /** UTC time scale. */
    private TimeScale utc;

    /** Compute acceleration derivatives around input position at input date.
     *  Using finite differences in position.
     */
    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state) {
        try {

            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<DerivativeStructure> velocity = state.getPVCoordinates().getVelocity();
            java.lang.reflect.Field atmosphereField = TimeSpanDragForce.class.getDeclaredField("atmosphere");
            atmosphereField.setAccessible(true);
            Atmosphere atmosphere = (Atmosphere) atmosphereField.get(forceModel);
            java.lang.reflect.Field spacecraftField = DragForce.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            // Get the DragSensitive model at date
            DragSensitive spacecraft = ((TimeSpanDragForce) (forceModel)).getDragSensitive(date);

            // retrieve derivation properties
            final DSFactory factory = state.getMass().getFactory();

            // get atmosphere properties in atmosphere own frame
            final Frame      atmFrame  = atmosphere.getFrame();
            final Transform  toBody    = state.getFrame().getTransformTo(atmFrame, date);
            final FieldVector3D<DerivativeStructure> posBodyDS = toBody.transformPosition(position);
            final Vector3D   posBody   = posBodyDS.toVector3D();
            final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

            // estimate density model
            if (factory.getCompiler().getOrder() > 1) {
                throw new OrekitException(OrekitMessages.OUT_OF_RANGE_DERIVATION_ORDER, factory.getCompiler().getOrder());
            }

            final DerivativeStructure rho = atmosphere.getDensity(new FieldAbsoluteDate<>(factory.getDerivativeField(), date),
                    posBodyDS, atmFrame);

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
            DerivativeStructure[] parameters = ((TimeSpanDragForce) (forceModel)).extractParameters(allParameters, state.getDate());

            // compute acceleration with all its partial derivatives
            return spacecraft.dragAcceleration(state, rho, relativeVelocity, parameters);
        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    /** Compute acceleration derivatives around input position at input date.
     *  Using finite differences in position.
     */
    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state) {
        try {

            final AbsoluteDate                       date     = state.getDate().toAbsoluteDate();
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<Gradient> velocity = state.getPVCoordinates().getVelocity();
            java.lang.reflect.Field atmosphereField = TimeSpanDragForce.class.getDeclaredField("atmosphere");
            atmosphereField.setAccessible(true);
            Atmosphere atmosphere = (Atmosphere) atmosphereField.get(forceModel);
            java.lang.reflect.Field spacecraftField = DragForce.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            // Get the DragSensitive model at date
            DragSensitive spacecraft = ((TimeSpanDragForce) (forceModel)).getDragSensitive(date);

            final int freeParameters = state.getMass().getFreeParameters();

            // get atmosphere properties in atmosphere own frame
            final Frame      atmFrame  = atmosphere.getFrame();
            final Transform  toBody    = state.getFrame().getTransformTo(atmFrame, date);
            final FieldVector3D<Gradient> posBodyG = toBody.transformPosition(position);
            final Vector3D   posBody   = posBodyG.toVector3D();
            final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

            // estimate density model
            final Gradient rho = atmosphere.getDensity(new FieldAbsoluteDate<>(posBodyG.getX().getField(), date),
                    posBodyG, atmFrame);

            // we consider that at first order the atmosphere velocity in atmosphere frame
            // does not depend on local position; however atmosphere velocity in inertial
            // frame DOES depend on position since the transform between the frames depends
            // on it, due to central body rotation rate and velocity composition.
            // So we use the transform to get the correct partial derivatives on vAtm
            final FieldVector3D<Gradient> vAtmBodyG =
                            new FieldVector3D<>(Gradient.constant(freeParameters, vAtmBody.getX()),
                                            Gradient.constant(freeParameters, vAtmBody.getY()),
                                            Gradient.constant(freeParameters, vAtmBody.getZ()));
            final FieldPVCoordinates<Gradient> pvAtmBody = new FieldPVCoordinates<>(posBodyG, vAtmBodyG);
            final FieldPVCoordinates<Gradient> pvAtm     = toBody.getInverse().transformPVCoordinates(pvAtmBody);

            // now we can compute relative velocity, it takes into account partial derivatives with respect to position
            final FieldVector3D<Gradient> relativeVelocity = pvAtm.getVelocity().subtract(velocity);


            // Extract drag parameters of the proper model
            Gradient[] allParameters = forceModel.getParameters(state.getDate().getField());
            Gradient[] parameters = ((TimeSpanDragForce) (forceModel)).extractParameters(allParameters, state.getDate());

            // compute acceleration with all its partial derivatives
            return spacecraft.dragAcceleration(state, rho, relativeVelocity, parameters);
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
        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(2,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());
        Assertions.assertEquals(dragCd0,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers.get(1).getName());

        // Extract drag model at an arbitrary epoch and check it is the one added
        IsotropicDrag isoDrag = (IsotropicDrag) forceModel.getDragSensitive(date);
        drivers = isoDrag.getDragParametersDrivers();
        Assertions.assertEquals(2, drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());
        Assertions.assertEquals(dragCd0,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers.get(1).getName());

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
        Assertions.assertEquals(6,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(utc),
                            drivers.get(0).getName());
        Assertions.assertEquals(dragCd2,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(utc),
                            drivers.get(1).getName());
        Assertions.assertEquals(1.0,  drivers.get(2).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(2).getName());
        Assertions.assertEquals(dragCd0,  drivers.get(3).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.DRAG_COEFFICIENT,  drivers.get(3).getName());
        Assertions.assertEquals(1.0,  drivers.get(4).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(utc),
                            drivers.get(4).getName());
        Assertions.assertEquals(dragCd1,  drivers.get(5).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.DRAG_COEFFICIENT + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(utc),
                            drivers.get(5).getName());

        // Check that proper models are returned at significant test dates
        // Cd0 model
        double eps = 1.e-14;
        Assertions.assertEquals(isoDrag, forceModel.getDragSensitive(date));
        Assertions.assertEquals(isoDrag, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assertions.assertEquals(isoDrag, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assertions.assertEquals(isoDrag2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assertions.assertEquals(isoDrag2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assertions.assertEquals(isoDrag1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assertions.assertEquals(isoDrag1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));

        // Add a custom-named driver
        // ------------
        double dragCd3 = 3.;
        IsotropicDrag isoDrag3 = new IsotropicDrag(dragArea, dragCd3);
        isoDrag3.getDragParametersDrivers().get(0).setName("custom-fact");
        isoDrag3.getDragParametersDrivers().get(1).setName("custom-Cd");
        forceModel.addDragSensitiveValidAfter(isoDrag3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(8,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(6).getValue(), 0.);
        Assertions.assertEquals("custom-fact", drivers.get(6).getName());
        Assertions.assertEquals(dragCd3,  drivers.get(7).getValue(), 0.);
        Assertions.assertEquals("custom-Cd", drivers.get(7).getName());


        // Test #getDragSensitiveSpan method
        Assertions.assertEquals(isoDrag, forceModel.getDragSensitiveSpan(date).getData());
        Assertions.assertEquals(isoDrag2, forceModel.getDragSensitiveSpan(date.shiftedBy(-dt - 86400.)).getData());
        Assertions.assertEquals(isoDrag1, forceModel.getDragSensitiveSpan(date.shiftedBy(+dt + 1.)).getData());
        Assertions.assertEquals(isoDrag3, forceModel.getDragSensitiveSpan(date.shiftedBy(2 * dt + 1.)).getData());

        // Test #extractDragSensitiveRange
        TimeSpanMap<DragSensitive> dragMap = forceModel.extractDragSensitiveRange(date, date.shiftedBy(dt + 1.));
        Assertions.assertEquals(isoDrag, dragMap.getSpan(date).getData());
        Assertions.assertEquals(isoDrag1, dragMap.getSpan(date.shiftedBy(dt + 86400.)).getData());
        Assertions.assertEquals(isoDrag, dragMap.getSpan(date.shiftedBy(-dt - 86400.)).getData());
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
        isotropicDrag.getDragParametersDrivers().get(0).setName("Cd");
        final TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, isotropicDrag, TimeScalesFactory.getUTC());


        // After t2 = t + 4h
        final double dragCd2 = 3.;
        final double dt2 = 4 * 3600.;
        final AbsoluteDate date2 = date.shiftedBy(dt2);
        final IsotropicDrag isotropicDrag2 = new IsotropicDrag(dragArea, dragCd2);
        isotropicDrag2.getDragParametersDrivers().get(0).setName("Cd2");
        forceModel.addDragSensitiveValidAfter(isotropicDrag2, date2);

        // Before t3 = t - 1day
        final double dragCd3 = 3.;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final IsotropicDrag isotropicDrag3 = new IsotropicDrag(dragArea, dragCd3);
        isotropicDrag3.getDragParametersDrivers().get(0).setName("Cd3");
        forceModel.addDragSensitiveValidBefore(isotropicDrag3, date3);


        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

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

    /** Test parameter derivatives for an IsotropicDrag TimeSpanDragForce.
     *  This test is more or less a copy of the same one in DragForceTest class
     *  with addition of several IsotropicDrag models valid at different dates
     *  to test that the different parameters' derivatives are computed correctly.
     */
    @Test
    public void testParameterDerivativeSphereGradient() {

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
        isotropicDrag.getDragParametersDrivers().get(0).setName("Cd");
        final TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, isotropicDrag, TimeScalesFactory.getUTC());


        // After t2 = t + 4h
        final double dragCd2 = 3.;
        final double dt2 = 4 * 3600.;
        final AbsoluteDate date2 = date.shiftedBy(dt2);
        final IsotropicDrag isotropicDrag2 = new IsotropicDrag(dragArea, dragCd2);
        isotropicDrag2.getDragParametersDrivers().get(0).setName("Cd2");
        forceModel.addDragSensitiveValidAfter(isotropicDrag2, date2);

        // Before t3 = t - 1day
        final double dragCd3 = 3.;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final IsotropicDrag isotropicDrag3 = new IsotropicDrag(dragArea, dragCd3);
        isotropicDrag3.getDragParametersDrivers().get(0).setName("Cd3");
        forceModel.addDragSensitiveValidBefore(isotropicDrag3, date3);


        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only "Cd" shouldn't be 0.
        checkParameterDerivativeGradient(state, forceModel, "Cd" , 1.0e-4, 2.0e-12);
        checkParameterDerivativeGradient(state, forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state, forceModel, "Cd3", 1.0e-4, 0.);

        // Check parameter derivatives after date2: only "Cd2" shouldn't be 0.
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, "Cd", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, "Cd2", 1.0e-4, 2.0e-12);
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, "Cd3", 1.0e-4, 0.);

        // Check parameter derivatives after date3: only "Cd3" shouldn't be 0.
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, "Cd", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, "Cd2", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, "Cd3", 1.0e-4, 2.0e-12);
    }

    /** Test state Jacobian computation. */
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
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
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
                                                                         dragCd0, 0.0,
                                                                         0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(1,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());

        // Extract drag model at an arbitrary epoch and check it is the one added
        BoxAndSolarArraySpacecraft box = (BoxAndSolarArraySpacecraft) forceModel.getDragSensitive(date);
        drivers = box.getDragParametersDrivers();
        Assertions.assertEquals(1, drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());

        // 3 BoxAndSolarArraySpacecraft models added, with one "default" in the middle
        // ----------------------------------------------
        double dragCd1 = 1.;
        double dragCd2 = 2.;
        double dt = 120.;
        // Build the force model
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd1, 0.0,
                                                                         0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd2, 0.0,
                                                                         0.7, 0.2);
        forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, date.shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, date.shiftedBy(-dt));

        // Extract the drivers and check their values and names
        drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(3,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(utc),
                                drivers.get(0).getName());

        Assertions.assertEquals(1.0,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(1).getName());

        Assertions.assertEquals(1.0,  drivers.get(2).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(utc),
                                drivers.get(2).getName());

        // Check the models at dates
        // Cd0 model
        double eps = 1.e-14;
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date));
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assertions.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assertions.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assertions.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assertions.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));

        // Add a custom-named driver
        // ----------------
        double dragCd3 = 3.;
        BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd3, 0.0,
                                                                         0.7, 0.2);
        box3.getDragParametersDrivers().get(0).setName("custom-Cd");
        forceModel.addDragSensitiveValidAfter(box3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(4,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(3).getValue(), 0.);
        Assertions.assertEquals("custom-Cd", drivers.get(3).getName());
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
        Assertions.assertFalse(forceModel.dependsOnPositionOnly());
        List<ParameterDriver> drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(1,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());

        // Extract drag model at an arbitrary epoch and check it is the one added
        BoxAndSolarArraySpacecraft box = (BoxAndSolarArraySpacecraft) forceModel.getDragSensitive(date);
        drivers = box.getDragParametersDrivers();
        Assertions.assertEquals(1, drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(0).getName());

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
        Assertions.assertEquals(3,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(0).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_BEFORE + date.shiftedBy(-dt).toString(utc),
                                drivers.get(0).getName());

        Assertions.assertEquals(1.0,  drivers.get(1).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR,  drivers.get(1).getName());

        Assertions.assertEquals(1.0,  drivers.get(2).getValue(), 0.);
        Assertions.assertEquals(DragSensitive.GLOBAL_DRAG_FACTOR + TimeSpanDragForce.DATE_AFTER + date.shiftedBy(+dt).toString(utc),
                                drivers.get(2).getName());

        // Check the models at dates
        // Cd0 model
        double eps = 1.e-14;
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date));
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(-dt)));
        Assertions.assertEquals(box0, forceModel.getDragSensitive(date.shiftedBy(+dt - eps)));
        // Cd2 model
        Assertions.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - eps)));
        Assertions.assertEquals(box2, forceModel.getDragSensitive(date.shiftedBy(-dt - 86400.)));
        // Cd1 model
        Assertions.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt)));
        Assertions.assertEquals(box1, forceModel.getDragSensitive(date.shiftedBy(+dt + 86400.)));


        // Add a custom-named driver
        // ----------------
        double dragCd3 = 3.;
        double dragCl3 = 0.3;
        BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                         sun, 20.0, Vector3D.PLUS_J,
                                                                         dragCd3, dragCl3,
                                                                         0.7, 0.2);
        box3.getDragParametersDrivers().get(0).setName("custom-Cd");
        forceModel.addDragSensitiveValidAfter(box3, date.shiftedBy(2. * dt));
        drivers = forceModel.getParametersDrivers();
        Assertions.assertEquals(4,  drivers.size());
        Assertions.assertEquals(1.0,  drivers.get(3).getValue(), 0.);
        Assertions.assertEquals("custom-Cd", drivers.get(3).getName());
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
        box2.getDragParametersDrivers().get(0).setName("factor2");
        forceModel.addDragSensitiveValidAfter(box2, date2);

        // Before t3 = 1day
        final double dragCd3 = 3.;
        final double dragCl3 = 0.3;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                               sun, 20.0, Vector3D.PLUS_J,
                                                                               dragCd3, dragCl3, 0.7, 0.2);
        box3.getDragParametersDrivers().get(0).setName("factor3");
        forceModel.addDragSensitiveValidBefore(box3, date3);

        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only 1st model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state, forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 2.2e-11);
        checkParameterDerivative(state, forceModel, "factor2", 1.0e-4, 0.);
        checkParameterDerivative(state, forceModel, "factor3", 1.0e-4, 0.);

        // Check parameter derivatives after date2: only 2nd model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "factor2", 1.0e-4, 2.2e-12);
        checkParameterDerivative(state.shiftedBy(dt2 * 1.1), forceModel, "factor3", 1.0e-4, 0.);

        // Check parameter derivatives before date3: only 3nd model parameter derivatives shouldn't be 0.
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "factor2", 1.0e-4, 0.);
        checkParameterDerivative(state.shiftedBy(dt3 * 1.1), forceModel, "factor3", 1.0e-4, 2.0e-12);

    }

    /** Test parameter derivatives for an BoxAndSolarArraySpacecraft TimeSpanDragForce.
     *  This test is more or less a copy of the same one in DragForceTest class
     *  with addition of several BoxAndSolarArraySpacecraft models valid at different dates
     *  to test that the different parameters' derivatives are computed correctly.
     */
    @Test
    public void testParametersDerivativesBoxGradient() {

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
        box2.getDragParametersDrivers().get(0).setName("factor2");
        forceModel.addDragSensitiveValidAfter(box2, date2);

        // Before t3 = 1day
        final double dragCd3 = 3.;
        final double dragCl3 = 0.3;
        final double dt3 = -86400.;
        final AbsoluteDate date3 = date.shiftedBy(dt3);
        final BoxAndSolarArraySpacecraft box3 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                                               sun, 20.0, Vector3D.PLUS_J,
                                                                               dragCd3, dragCl3, 0.7, 0.2);
        box3.getDragParametersDrivers().get(0).setName("factor3");
        forceModel.addDragSensitiveValidBefore(box3, date3);

        Assertions.assertFalse(forceModel.dependsOnPositionOnly());

        // Check parameter derivatives at initial date: only 1st model parameter derivatives shouldn't be 0.
        checkParameterDerivativeGradient(state, forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 2.2e-11);
        checkParameterDerivativeGradient(state, forceModel, "factor2", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state, forceModel, "factor3", 1.0e-4, 0.);

        // Check parameter derivatives after date2: only 2nd model parameter derivatives shouldn't be 0.
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, "factor2", 1.0e-4, 2.2e-12);
        checkParameterDerivativeGradient(state.shiftedBy(dt2 * 1.1), forceModel, "factor3", 1.0e-4, 0.);

        // Check parameter derivatives before date3: only 3nd model parameter derivatives shouldn't be 0.
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, DragSensitive.GLOBAL_DRAG_FACTOR, 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, "factor2", 1.0e-4, 0.);
        checkParameterDerivativeGradient(state.shiftedBy(dt3 * 1.1), forceModel, "factor3", 1.0e-4, 2.0e-12);

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
                                            0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
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
                                                    Utils.defaultLaw().getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             5e-6, false);

        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Utils.defaultLaw().getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             5e-6, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    Utils.defaultLaw().getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             5e-6, false);

    }

    /** Test state Jacobian computation using finite differences in position
     * and method {@link #accelerationDerivatives}
     */
    @Test
    public void testJacobianBoxVs80ImplementationGradient()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit refOrbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                            0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                            Constants.EIGEN5C_EARTH_MU);
        CelestialBody sun = CelestialBodyFactory.getSun();
        AttitudeProvider defaultLaw = Utils.defaultLaw();

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
                                                    Utils.defaultLaw().getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80ImplementationGradient(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             5e-6, false);

        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80ImplementationGradient(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
                                             5e-6, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80ImplementationGradient(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS),
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
                                            0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                            Constants.EIGEN5C_EARTH_MU);
        CelestialBody sun = CelestialBodyFactory.getSun();
        AttitudeProvider defaultLaw = Utils.defaultLaw();

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
                                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, defaultLaw, 1.0, 5.0e-6, false);

        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, defaultLaw, 1.0, 5.0e-6, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, defaultLaw, 1.0, 6.0e-6, false);
    }

    /** Test state Jacobian computation using finite differences once again.
     * This time the differentiation is made using built-in numerical differentiation method.
     */
    @Test
    public void testJacobianBoxVsFiniteDifferencesGradient()
        {

        // initialization
        AbsoluteDate date = new AbsoluteDate(new DateComponents(2003, 03, 01),
                                             new TimeComponents(13, 59, 27.816),
                                             TimeScalesFactory.getUTC());
        double i     = FastMath.toRadians(98.7);
        double omega = FastMath.toRadians(93.0);
        double OMEGA = FastMath.toRadians(15.0 * 22.5);
        Orbit refOrbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                            0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
                                            Constants.EIGEN5C_EARTH_MU);
        CelestialBody sun = CelestialBodyFactory.getSun();
        AttitudeProvider defaultLaw = Utils.defaultLaw();

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
                                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferencesGradient(state, forceModel, defaultLaw, 1.0, 5.0e-6, false);

        // Check state derivatives inside 2nd box model
        orbit = refOrbit.shiftedBy(1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferencesGradient(state, forceModel, defaultLaw, 1.0, 5.0e-6, false);

        // Check state derivatives inside 3rd box model
        orbit = refOrbit.shiftedBy(-1.1 * dt);
        state = new SpacecraftState(orbit,
                                    defaultLaw.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferencesGradient(state, forceModel, defaultLaw, 1.0, 6.0e-6, false);
    }

    /** Test state Jacobian computation. */
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
                                         0, PositionAngleType.MEAN, FramesFactory.getEME2000(), date,
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
                           1e3, tolerances[0], 5.6e-9);
    }

    /** Testing if the propagation between the FieldPropagation and the propagation is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the Taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() {
        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 1.2);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);


        // Set up force model
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
        double dt = 1000.;

        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, J2000.toAbsoluteDate().shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, J2000.toAbsoluteDate().shiftedBy(-dt));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        // -----------

        // Propagate inside 1st drag model
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 0.9 * dt, NP, FNP,
                                  1.0e-30, 6.0e-09, 2.0e-10, 5.0e-11,
                                  1, false);

        // Propagate to 2nd drag model (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 1.1 * dt, NP, FNP,
                                  1.0e-30, 2.0e-08, 8.0e-11, 1.0e-10,
                                  1, false);

        // Propagate to 3rd drag model  (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, -1.1 * dt, NP, FNP,
                                  1.0e-15, 2.0e-08, 2.0e-09, 2.0e-09,
                                  1, false);
    }

    /** Testing if the propagation between the FieldPropagation and the propagation is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the Taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldGradientTest() {
        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        final int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e6);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.01);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 1.2);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);

        Field<Gradient> field = a_0.getField();
        Gradient zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<Gradient> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);


        // Set up force model
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
        double dt = 1000.;

        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, J2000.toAbsoluteDate().shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, J2000.toAbsoluteDate().shiftedBy(-dt));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        // -----------

        // Propagate inside 1st drag model
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 0.9 * dt, NP, FNP,
                                  1.0e-30, 2.5e-02, 7.7e-2, 1.9e-4,
                                  1, false);

        // Propagate to 2nd drag model (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 1.1 * dt, NP, FNP,
                                  1.0e-30, 4.4e-02, 7.6e-5, 4.1e-4,
                                  1, false);

        // Propagate to 3rd drag model  (reset propagator first)
        FNP.resetInitialState(initialState);
        NP.resetInitialState(iSR);
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, -1.1 * dt, NP, FNP,
                                  1.0e-8, 2.4e-02, 2.3e-04, 3.9e-04,
                                  1, false);
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    it is a test to validate the previous test.
    (to test if the ForceModel it's actually
    doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        // Initial field Keplerian orbit
        // The variables are the six orbital parameters
        DSFactory factory = new DSFactory(6, 4);
        DerivativeStructure a_0 = factory.variable(0, 7e6);
        DerivativeStructure e_0 = factory.variable(1, 0.01);
        DerivativeStructure i_0 = factory.variable(2, 1.2);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        // Initial date = J2000 epoch
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        // J2000 frame
        Frame EME = FramesFactory.getEME2000();

        // Create initial field Keplerian orbit
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        // Initial field and classical S/Cs
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);
        SpacecraftState iSR = initialState.toSpacecraftState();

        // Field integrator and classical integrator
        ClassicalRungeKuttaFieldIntegrator<DerivativeStructure> integrator =
                        new ClassicalRungeKuttaFieldIntegrator<>(field, zero.add(6));
        ClassicalRungeKuttaIntegrator RIntegrator =
                        new ClassicalRungeKuttaIntegrator(6);
        OrbitType type = OrbitType.EQUINOCTIAL;

        // Field and classical numerical propagators
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);


        // Set up force model
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
        double dt = 1. * 1100.;

        // Build the force model
        BoxAndSolarArraySpacecraft box0 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd0, dragCl0, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box1 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd1, dragCl1, 0.7, 0.2);
        BoxAndSolarArraySpacecraft box2 = new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, sun, 20.0,
                                                                         Vector3D.PLUS_J, dragCd2, dragCl2, 0.7, 0.2);
        TimeSpanDragForce forceModel = new TimeSpanDragForce(atmosphere, box0);
        forceModel.addDragSensitiveValidAfter(box1, J2000.toAbsoluteDate().shiftedBy(dt));
        forceModel.addDragSensitiveValidBefore(box2, J2000.toAbsoluteDate().shiftedBy(-dt));
        FNP.addForceModel(forceModel);
        // NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(100.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assertions.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }

    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        utc = TimeScalesFactory.getUTC();
    }
}
