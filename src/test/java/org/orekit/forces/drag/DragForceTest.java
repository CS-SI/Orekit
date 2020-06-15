/* Copyright 2002-2020 CS GROUP
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
import org.hipparchus.analysis.differentiation.GradientField;
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
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.bodies.BodyShape;
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
import org.orekit.utils.TimeStampedPVCoordinates;

public class DragForceTest extends AbstractLegacyForceModelTest {

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final AbsoluteDate date, final  Frame frame,
                                                                         final FieldVector3D<DerivativeStructure> position,
                                                                         final FieldVector3D<DerivativeStructure> velocity,
                                                                         final FieldRotation<DerivativeStructure> rotation,
                                                                         final DerivativeStructure mass)
        {
        try {

            java.lang.reflect.Field atmosphereField = DragForce.class.getDeclaredField("atmosphere");
            atmosphereField.setAccessible(true);
            Atmosphere atmosphere = (Atmosphere) atmosphereField.get(forceModel);
            java.lang.reflect.Field spacecraftField = DragForce.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            DragSensitive spacecraft = (DragSensitive) spacecraftField.get(forceModel);

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

            // compute acceleration with all its partial derivatives
            return spacecraft.dragAcceleration(new FieldAbsoluteDate<>(factory.getDerivativeField(), date),
                                               frame, position, rotation, mass, rho, relativeVelocity,
                                               forceModel.getParameters(factory.getDerivativeField()));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final AbsoluteDate date, final  Frame frame,
                                                                      final FieldVector3D<Gradient> position,
                                                                      final FieldVector3D<Gradient> velocity,
                                                                      final FieldRotation<Gradient> rotation,
                                                                      final Gradient mass)
        {
        try {

            java.lang.reflect.Field atmosphereField = DragForce.class.getDeclaredField("atmosphere");
            atmosphereField.setAccessible(true);
            Atmosphere atmosphere = (Atmosphere) atmosphereField.get(forceModel);
            java.lang.reflect.Field spacecraftField = DragForce.class.getDeclaredField("spacecraft");
            spacecraftField.setAccessible(true);
            DragSensitive spacecraft = (DragSensitive) spacecraftField.get(forceModel);

            final int freeParameters = mass.getFreeParameters();

            // get atmosphere properties in atmosphere own frame
            final Frame      atmFrame  = atmosphere.getFrame();
            final Transform  toBody    = frame.getTransformTo(atmFrame, date);
            final FieldVector3D<Gradient> posBodyG = toBody.transformPosition(position);
            final Vector3D   posBody   = posBodyG.toVector3D();
            final Vector3D   vAtmBody  = atmosphere.getVelocity(date, posBody, atmFrame);

            // estimate density model by finite differences and composition
            // the following implementation works only for first order derivatives.
            // this could be improved by adding a new method
            // getDensity(AbsoluteDate, FieldVector3D<Gradient>, Frame)
            // to the Atmosphere interface
            final double delta  = 1.0;
            final double x      = posBody.getX();
            final double y      = posBody.getY();
            final double z      = posBody.getZ();
            final double rho0   = atmosphere.getDensity(date, posBody, atmFrame);
            final double dRhodX = (atmosphere.getDensity(date, new Vector3D(x + delta, y,         z),         atmFrame) - rho0) / delta;
            final double dRhodY = (atmosphere.getDensity(date, new Vector3D(x,         y + delta, z),         atmFrame) - rho0) / delta;
            final double dRhodZ = (atmosphere.getDensity(date, new Vector3D(x,         y,         z + delta), atmFrame) - rho0) / delta;
            final double[] dXdQ = posBodyG.getX().getGradient();
            final double[] dYdQ = posBodyG.getY().getGradient();
            final double[] dZdQ = posBodyG.getZ().getGradient();
            final double[] rhoAll = new double[dXdQ.length];
            for (int i = 0; i < rhoAll.length; ++i) {
                rhoAll[i] = dRhodX * dXdQ[i] + dRhodY * dYdQ[i] + dRhodZ * dZdQ[i];
            }
            final Gradient rho = new Gradient(rho0, rhoAll);

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

            // compute acceleration with all its partial derivatives
            return spacecraft.dragAcceleration(new FieldAbsoluteDate<>(GradientField.getField(freeParameters), date),
                                               frame, position, rotation, mass, rho, relativeVelocity,
                                               forceModel.getParameters(GradientField.getField(freeParameters)));

        } catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            return null;
        }
    }

    @Test
    public void testParameterDerivativeSphere() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));

        Assert.assertFalse(forceModel.dependsOnPositionOnly());

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testParameterDerivativeSphereGradient() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));

        Assert.assertFalse(forceModel.dependsOnPositionOnly());

        checkParameterDerivativeGradient(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 2.0e-12);

    }

    @Test
    public void testStateJacobianSphere()
        {

        // initialization
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

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new IsotropicDrag(2.5, 1.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 2.0e-8);

    }

    @Test
    public void testParametersDerivativesBox() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                             CelestialBodyFactory.getSun(), 20.0, Vector3D.PLUS_J,
                                                             1.2, 0.1, 0.7, 0.2));

        checkParameterDerivative(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 5.0e-13);
        checkParameterDerivative(state, forceModel, DragSensitive.LIFT_RATIO,       1.0e-4, 2.0e-11);

    }

    @Test
    public void testParametersDerivativesBoxGradient() {

        final Vector3D pos = new Vector3D(6.46885878304673824e+06, -1.88050918456274318e+06, -1.32931592294715829e+04);
        final Vector3D vel = new Vector3D(2.14718074509906819e+03, 7.38239351251748485e+03, -1.14097953925384523e+01);
        final SpacecraftState state =
                new SpacecraftState(new CartesianOrbit(new PVCoordinates(pos, vel),
                                                       FramesFactory.getGCRF(),
                                                       new AbsoluteDate(2003, 3, 5, 0, 24, 0.0, TimeScalesFactory.getTAI()),
                                                       Constants.EIGEN5C_EARTH_MU));

        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8,
                                                             CelestialBodyFactory.getSun(), 20.0, Vector3D.PLUS_J,
                                                             1.2, 0.1, 0.7, 0.2));

        checkParameterDerivativeGradient(state, forceModel, DragSensitive.DRAG_COEFFICIENT, 1.0e-4, 5.0e-13);
        checkParameterDerivativeGradient(state, forceModel, DragSensitive.LIFT_RATIO,       1.0e-4, 2.0e-11);

    }

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
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80Implementation(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.VVLH),
                                             5e-6, false);

    }

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
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVs80ImplementationGradient(state, forceModel,
                                             new LofOffset(state.getFrame(), LOFType.VVLH),
                                             5e-6, false);

    }

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
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferences(state, forceModel, Propagator.DEFAULT_LAW, 1.0, 5.0e-6, false);

    }

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
        Orbit orbit = new KeplerianOrbit(7201009.7124401, 1e-3, i , omega, OMEGA,
                                         0, PositionAngle.MEAN, FramesFactory.getEME2000(), date,
                                         Constants.EIGEN5C_EARTH_MU);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        SpacecraftState state = new SpacecraftState(orbit,
                                                    Propagator.DEFAULT_LAW.getAttitude(orbit, orbit.getDate(), orbit.getFrame()));
        checkStateJacobianVsFiniteDifferencesGradient(state, forceModel, Propagator.DEFAULT_LAW, 1.0, 5.0e-6, false);

    }

    @Test
    public void testGlobalStateJacobianBox()
        {

        // initialization
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

        NumericalPropagator propagator =
                new NumericalPropagator(new DormandPrince853Integrator(1.0e-3, 120,
                                                                       tolerances[0], tolerances[1]));
        propagator.setOrbitType(integrationType);
        final DragForce forceModel =
                new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                 new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                      Constants.WGS84_EARTH_FLATTENING,
                                                                      FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                              new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                             Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        propagator.addForceModel(forceModel);
        SpacecraftState state0 = new SpacecraftState(orbit);

        checkStateJacobian(propagator, state0, date.shiftedBy(3.5 * 3600.0),
                           1e3, tolerances[0], 3.0e-8);

    }

    @Test
    public void testIssue229() {
        AbsoluteDate initialDate = new AbsoluteDate(2004, 1, 1, 0, 0, 0., TimeScalesFactory.getUTC());
        Frame frame       = FramesFactory.getEME2000();
        double rpe         = 160.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double rap         = 2000.e3 + Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        double inc         = FastMath.toRadians(0.);
        double aop         = FastMath.toRadians(0.);
        double raan        = FastMath.toRadians(0.);
        double mean        = FastMath.toRadians(180.);
        double mass        = 100.;
        KeplerianOrbit orbit = new KeplerianOrbit(0.5 * (rpe + rap), (rap - rpe) / (rpe + rap),
                                                  inc, aop, raan, mean, PositionAngle.MEAN,
                                                  frame, initialDate, Constants.EIGEN5C_EARTH_MU);

        IsotropicDrag shape = new IsotropicDrag(10., 2.2);

        Frame itrf = FramesFactory.getITRF(IERSConventions.IERS_2010, true);
        BodyShape earthShape = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, itrf);
        Atmosphere atmosphere = new SimpleExponentialAtmosphere(earthShape, 2.6e-10, 200000, 26000);

        double[][]          tolerance  = NumericalPropagator.tolerances(0.1, orbit, OrbitType.CARTESIAN);
        AbstractIntegrator  integrator = new DormandPrince853Integrator(1.0e-3, 300, tolerance[0], tolerance[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.setMu(orbit.getMu());
        propagator.addForceModel(new DragForce(atmosphere, shape));
        PartialDerivativesEquations partials = new PartialDerivativesEquations("partials", propagator);
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(orbit, mass)));

        SpacecraftState state = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));

        double delta = 0.1;
        Orbit shifted = new CartesianOrbit(new TimeStampedPVCoordinates(orbit.getDate(),
                                                                        orbit.getPVCoordinates().getPosition().add(new Vector3D(delta, 0, 0)),
                                                                        orbit.getPVCoordinates().getVelocity()),
                                           orbit.getFrame(), orbit.getMu());
        propagator.setInitialState(partials.setInitialJacobians(new SpacecraftState(shifted, mass)));
        SpacecraftState newState = propagator.propagate(new AbsoluteDate(2004, 1, 1, 1, 30, 0., TimeScalesFactory.getUTC()));
        double[] dPVdX = new double[] {
            (newState.getPVCoordinates().getPosition().getX() - state.getPVCoordinates().getPosition().getX()) / delta,
            (newState.getPVCoordinates().getPosition().getY() - state.getPVCoordinates().getPosition().getY()) / delta,
            (newState.getPVCoordinates().getPosition().getZ() - state.getPVCoordinates().getPosition().getZ()) / delta,
            (newState.getPVCoordinates().getVelocity().getX() - state.getPVCoordinates().getVelocity().getX()) / delta,
            (newState.getPVCoordinates().getVelocity().getY() - state.getPVCoordinates().getVelocity().getY()) / delta,
            (newState.getPVCoordinates().getVelocity().getZ() - state.getPVCoordinates().getVelocity().getZ()) / delta,
        };

        double[][] dYdY0 = new double[6][6];
        partials.getMapper().getStateJacobian(state, dYdY0);
        for (int i = 0; i < 6; ++i) {
            Assert.assertEquals(dPVdX[i], dYdY0[i][0], 6.2e-6 * FastMath.abs(dPVdX[i]));
        }

    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
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
                                                                                 PositionAngle.MEAN,
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

        // Set up the force model to test
        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        // Do the test
        checkRealFieldPropagation(FKO, PositionAngle.MEAN, 1000., NP, FNP,
                                  1.0e-30, 9.0e-9, 9.0e-11, 9.0e-11,
                                  1, false);
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
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
                                                                                 PositionAngle.MEAN,
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

        // Set up the force model to test
        final DragForce forceModel =
                        new DragForce(new HarrisPriester(CelestialBodyFactory.getSun(),
                                                         new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                                                              Constants.WGS84_EARTH_FLATTENING,
                                                                              FramesFactory.getITRF(IERSConventions.IERS_2010, true))),
                                      new BoxAndSolarArraySpacecraft(1.5, 2.0, 1.8, CelestialBodyFactory.getSun(), 20.0,
                                                                     Vector3D.PLUS_J, 1.2, 0.7, 0.2));
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngle.MEAN, 1000., NP, FNP,
                                  1.0e-30, 3.2e-2, 7.7e-5, 2.8e-4,
                                  1, false);
    }

    /** Same test as the previous one but not adding the ForceModel to the NumericalPropagator
    * it is a test to validate the previous test.
    * (to test if the ForceModel it's actually
    * doing something in the Propagator and the FieldPropagator).
    */ 
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
        // NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

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

}


