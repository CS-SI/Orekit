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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.AbstractIntegrator;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.util.FastMath;
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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
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
    public void testGetSetGM() {
        //setup
        Relativity relativity = new Relativity(Constants.EIGEN5C_EARTH_MU);

        //actions + verify
        Assert.assertEquals(
                Constants.EIGEN5C_EARTH_MU,
                relativity.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                0);
        relativity.setParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT, 1);
        Assert.assertEquals(
                1,
                relativity.getParameter(NewtonianAttraction.CENTRAL_ATTRACTION_COEFFICIENT),
                0);
    }

}
