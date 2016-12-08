/* Contributed in the public domain.
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
package org.orekit.forces.gravity;

import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.AbstractIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeFieldIntegrator;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853FieldIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.RandomGenerator;
import org.hipparchus.random.UncorrelatedRandomVectorGenerator;
import org.hipparchus.random.Well19937a;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.AbstractForceModelTest;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.CircularOrbit;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
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
public class RelativityTest extends AbstractForceModelTest {

    /** speed of light */
    private static final double c = Constants.SPEED_OF_LIGHT;
    /** arbitrary date */
    private static final AbsoluteDate date = AbsoluteDate.J2000_EPOCH;
    /** inertial frame */
    private static final Frame frame = FramesFactory.getGCRF();
    /** identity rotation */
    private static final FieldRotation<DerivativeStructure> identity =
            new FieldRotation<DerivativeStructure>(
                    new DerivativeStructure(1, 1, 1),
                    new DerivativeStructure(1, 1, 0),
                    new DerivativeStructure(1, 1, 0),
                    new DerivativeStructure(1, 1, 0),
                    false
            );

    /** set orekit data */
    @BeforeClass
    public static void setUpBefore() {
        Utils.setDataRoot("regular-data");
    }

    /**
     * check the acceleration from relitivity
     *
     * @throws OrekitException on error
     */
    @Test
    public void testAcceleration() throws OrekitException {
        double gm = Constants.EIGEN5C_EARTH_MU;
        Relativity relativity = new Relativity(gm);
        AccelerationRetriever adder = new AccelerationRetriever();
        final Vector3D p = new Vector3D(3777828.75000531, -5543949.549783845, 2563117.448578311);
        final Vector3D v = new Vector3D(489.0060271721, -2849.9328929417, -6866.4671013153);
        SpacecraftState s = new SpacecraftState(new CartesianOrbit(
                new PVCoordinates(p, v),
                frame,
                date,
                gm
        ));

        //action
        relativity.addContribution(s, adder);

        //verify
        //force is ~1e-8 so this give ~3 sig figs.
        double tol = 2e-11;
        Vector3D circularApproximation = p.normalize().scalarMultiply(
                gm / p.getNormSq() * 3 * v.getNormSq() / (c * c));
        Assert.assertEquals(
                0,
                adder.getAcceleration().subtract(circularApproximation).getNorm(),
                tol);
        //check derivatives
        final DerivativeStructure mass = new DerivativeStructure(7, 1, 0);
        final Vector3D actualDerivatives = relativity
                .accelerationDerivatives(date, frame, ds(p, 0), ds(v, 3), identity, mass)
                .toVector3D();
        Assert.assertEquals(
                0,
                actualDerivatives.subtract(circularApproximation).getNorm(),
                tol);
    }

    /**
     * Check a nearly circular orbit.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testAccelerationCircular() throws OrekitException {
        double gm = Constants.EIGEN5C_EARTH_MU;
        double re = Constants.WGS84_EARTH_EQUATORIAL_RADIUS;
        Relativity relativity = new Relativity(gm);
        AccelerationRetriever adder = new AccelerationRetriever();
        final CircularOrbit orbit = new CircularOrbit(
                re + 500e3, 0, 0, FastMath.toRadians(41.2), -1, 3, PositionAngle.TRUE,
                frame,
                date,
                gm
        );
        SpacecraftState state = new SpacecraftState(orbit);

        //action
        relativity.addContribution(state, adder);

        //verify
        //force is ~1e-8 so this give ~7 sig figs.
        double tol = 2e-10;
        PVCoordinates pv = state.getPVCoordinates();
        Vector3D p = pv.getPosition();
        Vector3D v = pv.getVelocity();
        Vector3D circularApproximation = p.normalize().scalarMultiply(
                gm / p.getNormSq() * 3 * v.getNormSq() / (c * c));
        Assert.assertEquals(
                0,
                adder.getAcceleration().subtract(circularApproximation).getNorm(),
                tol);
        //check derivatives
        DerivativeStructure mass = new DerivativeStructure(7, 1, 6, 1);
        final FieldVector3D<DerivativeStructure> pDS = ds(p, 0);
        final FieldVector3D<DerivativeStructure> vDS = ds(v, 3);
        FieldVector3D<DerivativeStructure> gradient =
                relativity.accelerationDerivatives(date, frame, pDS, vDS, identity, mass);
        Assert.assertEquals(
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
        Assert.assertEquals(expectedDxDx, actualdx[1], 2);
    }

    /**
     * create a DS version of a vector.
     *
     * @param v the vector
     * @param i the start index
     * @return v as a DS vector
     */
    private static FieldVector3D<DerivativeStructure> ds(Vector3D v, int i) {
        return new FieldVector3D<DerivativeStructure>(
                new DerivativeStructure(7, 1, i, v.getX()),
                new DerivativeStructure(7, 1, i + 1, v.getY()),
                new DerivativeStructure(7, 1, i + 2, v.getZ())
        );
    }

    /**Testing if the propagation between the FieldPropagation and the propagation
     * is equivalent.
     * Also testing if propagating X+dX with the propagation is equivalent to 
     * propagation X with the FieldPropagation and then applying the taylor
     * expansion of dX to the result.*/
    @Test
    public void RealFieldTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 5, 0, 7e7);
        DerivativeStructure e_0 = new DerivativeStructure(6, 5, 1, 0.4);
        DerivativeStructure i_0 = new DerivativeStructure(6, 5, 2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = new DerivativeStructure(6, 5, 3, 0.7);
        DerivativeStructure O_0 = new DerivativeStructure(6, 5, 4, 0.5);
        DerivativeStructure n_0 = new DerivativeStructure(6, 5, 5, 0.1);
        
        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();
        
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<DerivativeStructure>(field);
        
        Frame EME = FramesFactory.getEME2000();
        
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<DerivativeStructure>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                                    PositionAngle.MEAN,
                                                                                                    EME,
                                                                                                    J2000,
                                                                                                    Constants.EIGEN5C_EARTH_MU);
        
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<DerivativeStructure>(FKO); 
        
        SpacecraftState iSR = initialState.toSpacecraftState();
        
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), OrbitType.KEPLERIAN);
        
        
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<DerivativeStructure>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);
                
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);
        FNP.setInitialState(initialState);
                
        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        
        final Relativity forceModel = new Relativity(Constants.EIGEN5C_EARTH_MU); 
        
        FNP.addForceModel(forceModel);
        NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(10000.);
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
                                                                                       new double[] {1e3, 0.01, 0.01, 0.01, 0.01, 0.01}, 
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
            double x_DS = pos_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double y_DS = pos_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double z_DS = pos_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            
            //System.out.println(pos_DS.getX().getPartialDerivative(1));

            double x = finPVC_shift.getPosition().getX();
            double y = finPVC_shift.getPosition().getY();
            double z = finPVC_shift.getPosition().getZ();
            Assert.assertEquals(x_DS, x, FastMath.abs(x - pos_DS.getX().getReal()) * 1e-8);
            Assert.assertEquals(y_DS, y, FastMath.abs(y - pos_DS.getY().getReal()) * 1e-8);
            Assert.assertEquals(z_DS, z, FastMath.abs(z - pos_DS.getZ().getReal()) * 1e-8);
            
            //velocity check
            
            FieldVector3D<DerivativeStructure> vel_DS = finPVC_DS.getVelocity();
            double vx_DS = vel_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vy_DS = vel_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double vz_DS = vel_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double vx = finPVC_shift.getVelocity().getX();
            double vy = finPVC_shift.getVelocity().getY();
            double vz = finPVC_shift.getVelocity().getZ();
            Assert.assertEquals(vx_DS, vx, FastMath.abs(vx) * 1e-9);
            Assert.assertEquals(vy_DS, vy, FastMath.abs(vy) * 1e-9);
            Assert.assertEquals(vz_DS, vz, FastMath.abs(vz) * 1e-9);
            //acceleration check
            
            FieldVector3D<DerivativeStructure> acc_DS = finPVC_DS.getAcceleration();
            double ax_DS = acc_DS.getX().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ay_DS = acc_DS.getY().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);                                                                               
            double az_DS = acc_DS.getZ().taylor(rand_next[0],rand_next[1],rand_next[2],rand_next[3],rand_next[4],rand_next[5]);
            double ax = finPVC_shift.getAcceleration().getX();
            double ay = finPVC_shift.getAcceleration().getY();
            double az = finPVC_shift.getAcceleration().getZ();
            Assert.assertEquals(ax_DS, ax, FastMath.abs(ax) * 1e-8);
            Assert.assertEquals(ay_DS, ay, FastMath.abs(ay) * 1e-8);
            Assert.assertEquals(az_DS, az, FastMath.abs(az) * 1e-8);
        }
    }
    
    /**Same test as the previous one but not adding the ForceModel to the NumericalPropagator
        it is a test to validate the previous test. 
        (to test if the ForceModel it's actually
        doing something in the Propagator and the FieldPropagator)*/
    @Test
    public void RealFieldExpectErrorTest() throws OrekitException{
        DerivativeStructure a_0 = new DerivativeStructure(6, 0, 0, 7e7);
        DerivativeStructure e_0 = new DerivativeStructure(6, 0, 1, 0.4);
        DerivativeStructure i_0 = new DerivativeStructure(6, 0, 2, 85 * FastMath.PI / 180);
        DerivativeStructure R_0 = new DerivativeStructure(6, 0, 3, 0.7);
        DerivativeStructure O_0 = new DerivativeStructure(6, 0, 4, 0.5);
        DerivativeStructure n_0 = new DerivativeStructure(6, 0, 5, 0.1);
        
        Field<DerivativeStructure> field = a_0.getField();
        DerivativeStructure zero = field.getZero();
        
        FieldAbsoluteDate<DerivativeStructure> J2000 = new FieldAbsoluteDate<DerivativeStructure>(field);
        
        Frame EME = FramesFactory.getEME2000();
        
        FieldKeplerianOrbit<DerivativeStructure> FKO = new FieldKeplerianOrbit<DerivativeStructure>(a_0, e_0, i_0, R_0, O_0, n_0,
                                                                                                    PositionAngle.MEAN,
                                                                                                    EME,
                                                                                                    J2000,
                                                                                                    Constants.EIGEN5C_EARTH_MU);
        
        FieldSpacecraftState<DerivativeStructure> initialState = new FieldSpacecraftState<DerivativeStructure>(FKO); 
        
        SpacecraftState iSR = initialState.toSpacecraftState();
        
        double[][] tolerance = NumericalPropagator.tolerances(0.001, FKO.toOrbit(), OrbitType.KEPLERIAN);
        
        
        AdaptiveStepsizeFieldIntegrator<DerivativeStructure> integrator =
                        new DormandPrince853FieldIntegrator<DerivativeStructure>(field, 0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(zero.add(60));
        AdaptiveStepsizeIntegrator RIntegrator =
                        new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        RIntegrator.setInitialStepSize(60);
                
        FieldNumericalPropagator<DerivativeStructure> FNP = new FieldNumericalPropagator<DerivativeStructure>(field, integrator);
        FNP.setInitialState(initialState);
                
        NumericalPropagator NP = new NumericalPropagator(RIntegrator);
        NP.setInitialState(iSR);
        
        final Relativity forceModel = new Relativity(Constants.EIGEN5C_EARTH_MU); 
        
        FNP.addForceModel(forceModel);
     //NOT ADDING THE FORCE MODEL TO THE NUMERICAL PROPAGATOR   NP.addForceModel(forceModel);
        
        FieldAbsoluteDate<DerivativeStructure> target = J2000.shiftedBy(10000.);
        FieldSpacecraftState<DerivativeStructure> finalState_DS = FNP.propagate(target);
        SpacecraftState finalState_R = NP.propagate(target.toAbsoluteDate());
        FieldPVCoordinates<DerivativeStructure> finPVC_DS = finalState_DS.getPVCoordinates();
        PVCoordinates finPVC_R = finalState_R.getPVCoordinates();

        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getX() - finPVC_R.getPosition().getX()) < FastMath.abs(finPVC_R.getPosition().getX()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getY() - finPVC_R.getPosition().getY()) < FastMath.abs(finPVC_R.getPosition().getY()) * 1e-11);
        Assert.assertFalse(FastMath.abs(finPVC_DS.toPVCoordinates().getPosition().getZ() - finPVC_R.getPosition().getZ()) < FastMath.abs(finPVC_R.getPosition().getZ()) * 1e-11);
    }
    /**
     * check against example in Tapley, Schutz, and Born, p 65-66. They predict a
     * progression of perigee of 11 arcsec/year. To get the same results we must set the
     * propagation tolerances to 1e-5.
     *
     * @throws OrekitException on error
     */
    @Test
    public void testSmallEffectOnOrbit() throws OrekitException {
        //setup
        final double gm = Constants.EIGEN5C_EARTH_MU;
        Orbit orbit =
                new KeplerianOrbit(
                        7500e3, 0.025, FastMath.toRadians(41.2), 0, 0, 0, PositionAngle.TRUE,
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
        Assert.assertEquals(11, arcsecPerYear, 0.5);
    }

    /**
     * check {@link Relativity#setParameter(String, double)}, and {@link
     * Relativity#getParameter(String)}
     */
    @Test
    public void testGetSetGM() throws OrekitException {
        //setup
        Relativity relativity = new Relativity(Constants.EIGEN5C_EARTH_MU);

        //actions + verify
        Assert.assertEquals(
                Constants.EIGEN5C_EARTH_MU,
                relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue(),
                0);
        relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).setValue(1);
        Assert.assertEquals(
                1,
                relativity.getParameterDriver(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT).getValue(),
                0);
    }

}
