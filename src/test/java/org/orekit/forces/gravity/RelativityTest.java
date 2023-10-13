/* Contributed in the public domain.
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
package org.orekit.forces.gravity;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.differentiation.Gradient;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.util.FastMath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.LofOffset;
import org.orekit.forces.AbstractLegacyForceModelTest;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.LOFType;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
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
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;

/** Unit tests for {@link Relativity}. */
public class RelativityTest extends AbstractLegacyForceModelTest {

    /** speed of light */
    private static final double c = Constants.SPEED_OF_LIGHT;
    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** inertial frame */
    private static final Frame frame = FramesFactory.getGCRF();

    @Override
    protected FieldVector3D<DerivativeStructure> accelerationDerivatives(final ForceModel forceModel,
                                                                         final FieldSpacecraftState<DerivativeStructure> state)
        {
        try {
            final FieldVector3D<DerivativeStructure> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<DerivativeStructure> velocity = state.getPVCoordinates().getVelocity();
            double gm = forceModel.
                        getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).
                        getValue(state.getDate().toAbsoluteDate());
            //radius
            final DerivativeStructure r2 = position.getNormSq();
            final DerivativeStructure r = r2.sqrt();
            //speed squared
            final DerivativeStructure s2 = velocity.getNormSq();
            final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
            //eq. 3.146
            return new FieldVector3D<>(r.reciprocal().multiply(4 * gm).subtract(s2),
                                       position,
                                       position.dotProduct(velocity).multiply(4),
                                       velocity).scalarMultiply(r2.multiply(r).multiply(c2).reciprocal().multiply(gm));

        } catch (IllegalArgumentException | SecurityException e) {
            return null;
        }
    }

    @Override
    protected FieldVector3D<Gradient> accelerationDerivativesGradient(final ForceModel forceModel,
                                                                      final FieldSpacecraftState<Gradient> state)
        {
        try {
            final FieldVector3D<Gradient> position = state.getPVCoordinates().getPosition();
            final FieldVector3D<Gradient> velocity = state.getPVCoordinates().getVelocity();
            double gm = forceModel.
                        getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).
                        getValue(state.getDate().toAbsoluteDate());
            //radius
            final Gradient r2 = position.getNormSq();
            final Gradient r = r2.sqrt();
            //speed squared
            final Gradient s2 = velocity.getNormSq();
            final double c2 = Constants.SPEED_OF_LIGHT * Constants.SPEED_OF_LIGHT;
            //eq. 3.146
            return new FieldVector3D<>(r.reciprocal().multiply(4 * gm).subtract(s2),
                                       position,
                                       position.dotProduct(velocity).multiply(4),
                                       velocity).scalarMultiply(r2.multiply(r).multiply(c2).reciprocal().multiply(gm));

        } catch (IllegalArgumentException | SecurityException e) {
            return null;
        }
    }

    /** set orekit data */
    @BeforeAll
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * check the acceleration from relativity
     */
    @Test
    public void testAcceleration() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        Relativity relativity = new Relativity(gm);
        Assertions.assertFalse(relativity.dependsOnPositionOnly());
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                frame,
                date,
                gm
        ));

        //action
        Vector3D acceleration = relativity.acceleration(s, relativity.getParameters(s.getDate()));

        //verify
        //force is ~1e-8 so this give ~3 sig figs.
        double tol = 2e-11;
        Vector3D circularApproximation = p.normalize().scalarMultiply(
                gm / p.getNormSq() * 3 * v.getNormSq() / (c * c));
        Assertions.assertEquals(
                0,
                acceleration.subtract(circularApproximation).getNorm(),
                tol);
        //check derivatives
        FieldSpacecraftState<DerivativeStructure> sDS = toDS(s, new LofOffset(s.getFrame(), LOFType.LVLH_CCSDS));
        final Vector3D actualDerivatives = relativity
                .acceleration(sDS, relativity.getParameters(sDS.getDate().getField(), sDS.getDate()))
                .toVector3D();
        Assertions.assertEquals(
                0,
                actualDerivatives.subtract(circularApproximation).getNorm(),
                tol);
    }

    @Test
    public void testJacobianVs80Implementation() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        Relativity relativity = new Relativity(gm);
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                frame,
                date,
                gm
        ));

        checkStateJacobianVs80Implementation(s, relativity,
                                             new LofOffset(s.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-50, false);
    }

    @Test
    public void testJacobianVs80ImplementationGradient() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        Relativity relativity = new Relativity(gm);
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                frame,
                date,
                gm
        ));

        checkStateJacobianVs80ImplementationGradient(s, relativity,
                                             new LofOffset(s.getFrame(), LOFType.LVLH_CCSDS),
                                             1.0e-50, false);
    }

    /**
     * Check a nearly circular orbit.
     */
    @Test
    public void testAccelerationCircular() {
        double gm = Constants.EIGEN5C_EARTH_MU;
        double re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        Relativity relativity = new Relativity(gm);
        final CircularOrbit orbit = new CircularOrbit(
                re + 500e3, 0, 0, FastMath.toRadians(41.2), -1, 3, PositionAngleType.TRUE,
                frame,
                date,
                gm
        );
        SpacecraftState state = new SpacecraftState(orbit);

        //action
        Vector3D acceleration = relativity.acceleration(state, relativity.getParameters(state.getDate()));

        //verify
        //force is ~1e-8 so this give ~7 sig figs.
        double tol = 2e-10;
        PVCoordinates pv = state.getPVCoordinates();
        Vector3D p = pv.getPosition();
        Vector3D v = pv.getVelocity();
        Vector3D circularApproximation = p.normalize().scalarMultiply(
                gm / p.getNormSq() * 3 * v.getNormSq() / (c * c));
        Assertions.assertEquals(
                0,
                acceleration.subtract(circularApproximation).getNorm(),
                tol);
        //check derivatives
        FieldSpacecraftState<DerivativeStructure> sDS = toDS(state, new LofOffset(state.getFrame(), LOFType.LVLH_CCSDS));
        FieldVector3D<DerivativeStructure> gradient =
                relativity.acceleration(sDS, relativity.getParameters(sDS.getDate().getField(), sDS.getDate()));
        Assertions.assertEquals(
                0,
                gradient.toVector3D().subtract(circularApproximation).getNorm(),
                tol);
        double r = p.getNorm();
        double s = v.getNorm();
        final double[] actualdx = gradient.getX().getAllDerivatives();
        final double x = p.getX();
        final double vx = v.getX();
        double expectedDxDx = gm / (c * c * r * r * r * r * r) *
                (-13 * x * x * s * s + 3 * r * r * s * s + 4 * r * r * vx * vx);
        Assertions.assertEquals(expectedDxDx, actualdx[1], 2);
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() {
        DSFactory factory = new DSFactory(6, 5);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);
        DerivativeStructure mu  = factory.constant(Constants.EIGEN5C_EARTH_MU);

        Field<DerivativeStructure> field = a_0.getField();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 mu);

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final Relativity forceModel = new Relativity(Constants.EIGEN5C_EARTH_MU);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagation(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-15, 5.0e-10, 3.0e-11, 3.0e-10,
                                  1, false);
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldGradientTest() {

        final int freeParameters = 6;
        Gradient a_0 = Gradient.variable(freeParameters, 0, 7e7);
        Gradient e_0 = Gradient.variable(freeParameters, 1, 0.4);
        Gradient i_0 = Gradient.variable(freeParameters, 2, 85 * FastMath.PI / 180);
        Gradient R_0 = Gradient.variable(freeParameters, 3, 0.7);
        Gradient O_0 = Gradient.variable(freeParameters, 4, 0.5);
        Gradient n_0 = Gradient.variable(freeParameters, 5, 0.1);
        Gradient mu  = Gradient.constant(freeParameters, Constants.EIGEN5C_EARTH_MU);

        Field<Gradient> field = a_0.getField();

        FieldAbsoluteDate<Gradient> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<Gradient> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                      PositionAngleType.MEAN,
                                                                      EME,
                                                                      J2000,
                                                                      mu);

        FieldSpacecraftState<Gradient> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();
        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<Gradient> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<Gradient> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final Relativity forceModel = new Relativity(Constants.EIGEN5C_EARTH_MU);

        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);

        // Do the test
        checkRealFieldPropagationGradient(FKO, PositionAngleType.MEAN, 1005., NP, FNP,
                                  1.0e-15, 1.3e-2, 2.9e-4, 4.4e-3,
                                  1, false);
    }

    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
        it is a test to validate the previous test.
        (to test if the ForceModel it's actually
        doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() {
        DSFactory factory = new DSFactory(6, 0);
        DerivativeStructure a_0 = factory.variable(0, 7e7);
        DerivativeStructure e_0 = factory.variable(1, 0.4);
        DerivativeStructure i_0 = factory.variable(2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = factory.variable(3, 0.7);
        DerivativeStructure O_0 = factory.variable(4, 0.5);
        DerivativeStructure n_0 = factory.variable(5, 0.1);

        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();

        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<>(field);

        Frame EME = FramesFactory.getEME2000();

        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                 PositionAngleType.MEAN,
                                                                                 EME,
                                                                                 J2000,
                                                                                 zero.add(Constants.EIGEN5C_EARTH_MU));

        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<>(FKO);

        SpacecraftState iSR = initialState.toSpacecraftState();

        OrbitType type = OrbitType.KEPLERIAN;
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), type);


        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);

        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<>(field, integrator);
        FNP.setOrbitType(type);
        FNP.setInitialState(initialState);

        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setOrbitType(type);
        NP.setInitialState(iSR);

        final Relativity forceModel = new Relativity(Constants.EIGEN5C_EARTH_MU);

        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);

        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(1000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assertions.assertEquals(0,
                            Vector3D.distance(finPVC_DS.toPVCoordinates().getPosition(), finPVC_R.getPosition()),
                            8.0e-13 * finPVC_R.getPosition().getNorm());
    }
    /**
     * check against example in Tapley, Schutz, and Born, p 65-66. They predict a
     * progression of perigee of 11 arcsec/year. To get the same results we must set the
     * propagation tolerances to 1e-5.
     */
    @Test
    public void testSmallEffectOnOrbit() {
        //setup
        final double gm = Constants.EIGEN5C_EARTH_MU;
        Orbit orbit =
                new KeplerianOrbit(
                        7500e3, 0.025, FastMath.toRadians(41.2), 0, 0, 0, PositionAngleType.TRUE,
                        frame,
                        date,
                        gm
                );
        double[][] tol = NumericalPropagator.tolerances(0.00001, orbit, OrbitType.CARTESIAN);
        AbstractIntegrator integrator = new DormandPrince853Integrator(1, 3600, tol[0], tol[1]);
        NumericalPropagator propagator = new NumericalPropagator(integrator);
        propagator.setOrbitType(OrbitType.CARTESIAN);
        propagator.addForceModel(new Relativity(gm));
        propagator.setInitialState(new SpacecraftState(orbit));

        //action: propagate a period
        AbsoluteDate end = orbit.getDate().shiftedBy(30 * Constants.JULIAN_DAY);
        PVCoordinates actual = propagator.getPVCoordinates(end, frame);

        //verify
        KeplerianOrbit endOrbit = new KeplerianOrbit(actual, frame, end, gm);
        KeplerianOrbit startOrbit = new KeplerianOrbit(orbit);
        double dp = endOrbit.getPerigeeArgument() - startOrbit.getPerigeeArgument();
        double dtYears = end.durationFrom(orbit.getDate()) / Constants.JULIAN_YEAR;
        double dpDeg = FastMath.toDegrees(dp);
        //change in argument of perigee in arcseconds per year
        double arcsecPerYear = dpDeg * 3600 / dtYears;
        Assertions.assertEquals(11, arcsecPerYear, 0.5);
    }

    /**
     * check {@link Relativity#setParameter(String, double)}, and {@link
     * Relativity#getParameter(String)}
     */
    @Test
    public void testGetSetGM() {
        //setup
        Relativity relativity = new Relativity(Constants.EIGEN5C_EARTH_MU);

        //actions + verify
        Assertions.assertEquals(
                Constants.EIGEN5C_EARTH_MU,
                relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue(),
                0);
        relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).setValue(1);
        Assertions.assertEquals(
                1,
                relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue(),
                0);
    }

}
