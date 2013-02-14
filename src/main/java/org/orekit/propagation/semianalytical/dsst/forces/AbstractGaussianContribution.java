/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.semianalytical.dsst.forces;

import org.apache.commons.math3.analysis.UnivariateVectorFunction;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.GaussQuadrature;

/** Common handling of {@link DSSTForceModel} methods for Gaussian contributions to DSST propagation.
 * <p>
 * This abstract class allows to provide easily a subset of {@link DSSTForceModel} methods
 * for specific Gaussian contributions (i.e. atmospheric drag and solar radiation pressure).
 * </p><p>
 * Gaussian contributions can be expressed as: da<sub>i</sub>/dt = &delta;a<sub>i</sub>/&delta;v . q<br>
 * where:
 * <ul>
 * <li>a<sub>i</sub> are the six equinoctial elements</li>
 * <li>v is the velocity vector</li>
 * <li>q is the perturbing acceleration due to the considered force</li>
 * </ul>
 * The averaging process and other considerations lead to integrate this contribution
 * over the true longitude L possibly taking into account some limits.
 * </p><p>
 * Only two methods must be implemented by derived classes:
 * {@link #getAcceleration(SpacecraftState, Vector3D, Vector3D)} and
 * {@link #getLLimits(SpacecraftState)}.
 * </p>
 * @author Pascal Parraud
 */
public abstract class AbstractGaussianContribution implements DSSTForceModel {

    /** Propagation orbit type. */
    protected static final OrbitType ORBIT_TYPE = OrbitType.EQUINOCTIAL;

    /** Position angle type. */
    protected static final PositionAngle ANGLE_TYPE = PositionAngle.MEAN;

    // CHECKSTYLE: stop VisibilityModifierCheck

    /** Retrograde factor. */
    protected double I;

    /** a. */
    protected double a;
    /** e<sub>x</sub>. */
    protected double k;
    /** e<sub>y</sub>. */
    protected double h;
    /** h<sub>x</sub>. */
    protected double q;
    /** h<sub>y</sub>. */
    protected double p;

    /** Eccentricity. */
    protected double ecc;

    /** Kepler mean motion: n = sqrt(&mu; / a<sup>3</sup>). */
    protected double n;

    /** Equinoctial frame f vector. */
    protected Vector3D f;
    /** Equinoctial frame g vector. */
    protected Vector3D g;
    /** Equinoctial frame w vector. */
    protected Vector3D w;

    /** A = sqrt(&mu; * a). */
    protected double A;
    /** B = sqrt(1 - h<sup>2</sup> - k<sup>2</sup>). */
    protected double B;
    /** C = 1 + p<sup>2</sup> + q<sup>2</sup>. */
    protected double C;

    /** 2 / (n<sup>2</sup> * a) . */
    protected double ton2a;
    /** 1 / A .*/
    protected double ooA;
    /** 1 / (A * B) .*/
    protected double ooAB;
    /** C / (2 * A * B) .*/
    protected double co2AB;
    /** 1 / (1 + B) .*/
    protected double ooBpo;
    /** 1 / &mu; .*/
    protected double ooMu;

    // CHECKSTYLE: resume VisibilityModifierCheck

    /** Gauss integrator. */
    private final GaussQuadrature integrator;

    /** Build a new instance.
     *  @param quadrature_order order for Gauss quadrature
     */
    protected AbstractGaussianContribution(final int quadrature_order) {
        this.integrator = new GaussQuadrature(quadrature_order);
    }

    /** {@inheritDoc} */
    public void initialize(final AuxiliaryElements aux)
        throws OrekitException {
        // Nothing to do for gaussian contributions at the beginning of the propagation.
    }

    /** {@inheritDoc} */
    public void initializeStep(final AuxiliaryElements aux)
        throws OrekitException {

        // Equinoctial elements
        a  = aux.getSma();
        k  = aux.getK();
        h  = aux.getH();
        q  = aux.getQ();
        p  = aux.getP();

        // Retrograde factor
        I = aux.getRetrogradeFactor();

        // Eccentricity
        ecc = aux.getEcc();

        // Equinoctial coefficients
        A = aux.getA();
        B = aux.getB();
        C = aux.getC();

        // Equinoctial frame vectors
        f = aux.getVectorF();
        g = aux.getVectorG();
        w = aux.getVectorW();

        // Kepler mean motion
        n = A / (a * a);

        // 1 / A
        ooA = 1. / A;
        // 1 / AB
        ooAB = ooA / B;
        // C / 2AB
        co2AB = C * ooAB / 2.;
        // 1 / (1 + B)
        ooBpo = 1. / (1. + B);
        // 2 / (n² * a)
        ton2a = 2. / (n * n * a);
        // 1 / mu
        ooMu  = 1. / aux.getMu();
    }

    /** {@inheritDoc} */
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {

        double[] meanElementRate = new double[6];
        // Computes the limits for the integral
        final double[] ll = getLLimits(state);
        // Computes integrated mean element rates if Llow < Lhigh
        if (ll[0] < ll[1]) {
            meanElementRate = integrator.integrate(new IntegrableFunction(state), ll[0], ll[1]);
            // Constant multiplier for integral
            final double coef = 1. / (2. * FastMath.PI * B);
            // Corrects mean element rates
            for (int i = 0; i < 6; i++) {
                meanElementRate[i] *= coef;
            }
        }
        return meanElementRate;
    }

    /** Compute the acceleration due to the non conservative perturbing force.
     *
     *  @param state current state information: date, kinematics, attitude
     *  @param position spacecraft position
     *  @param velocity spacecraft velocity
     *  @return the perturbing acceleration
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract Vector3D getAcceleration(final SpacecraftState state,
                                                final Vector3D position,
                                                final Vector3D velocity) throws OrekitException;

    /** Compute the limits in L, the true longitude, for integration.
     *
     *  @param  state current state information: date, kinematics, attitude
     *  @return the integration limits in L
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract double[] getLLimits(final SpacecraftState state) throws OrekitException;

    /** Internal class for numerical quadrature. */
    private class IntegrableFunction implements UnivariateVectorFunction {

        /** Current state. */
        private final SpacecraftState state;

        /** Build a new instance.
         *  @param  state current state information: date, kinematics, attitude
         */
        public IntegrableFunction(final SpacecraftState state) {
            this.state = state;
        }

        /** {@inheritDoc} */
        public double[] value(final double x) {
            final double cosL = FastMath.cos(x);
            final double sinL = FastMath.sin(x);
            final double roa  = B * B / (1. + h * sinL + k * cosL);
            final double roa2 = roa * roa;
            final double r    = a * roa;
            final double X    = r * cosL;
            final double Y    = r * sinL;
            final double naob = n * a / B;
            final double Xdot = -naob * (h + sinL);
            final double Ydot =  naob * (k + cosL);
            final Vector3D pos = new Vector3D(X, f, Y, g);
            final Vector3D vel = new Vector3D(Xdot, f, Ydot, g);
            // Compute acceleration
            Vector3D acc = Vector3D.ZERO;
            try {
                acc = getAcceleration(state, pos, vel);
            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
            // Compute mean elements rates
            final double[] val = new double[6];
            // da/dt
            val[0] = roa2 * getAoV(vel).dotProduct(acc);
            // dex/dt
            val[1] = roa2 * getKoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dey/dt
            val[2] = roa2 * getHoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dhx/dt
            val[3] = roa2 * getQoV(X).dotProduct(acc);
            // dhy/dt
            val[4] = roa2 * getPoV(Y).dotProduct(acc);
            // d&lambda;/dt
            val[5] = roa2 * getLoV(X, Y, Xdot, Ydot).dotProduct(acc);

            return val;
        }

        /** Compute &delta;a/&delta;v.
         *  @param vel satellite velocity
         *  @return &delta;a/&delta;v
         */
        private Vector3D getAoV(final Vector3D vel) {
            return new Vector3D(ton2a, vel);
        }

        /** Compute &delta;h/&delta;v.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;h/&delta;v
         */
        private Vector3D getHoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = (2. * Xdot * Y - X * Ydot) * ooMu;
            final double kg = X * Xdot * ooMu;
            final double kw = k * (I * q * Y - p * X) * ooAB;
            return new Vector3D(kf, f, -kg, g, kw, w);
        }

        /** Compute &delta;k/&delta;v.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;k/&delta;v
         */
        private Vector3D getKoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = Y * Ydot * ooMu;
            final double kg = (2. * X * Ydot - Xdot * Y) * ooMu;
            final double kw = h * (I * q * Y - p * X) * ooAB;
            return new Vector3D(-kf, f, kg, g, -kw, w);
        }

        /** Compute &delta;p/&delta;v.
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @return &delta;p/&delta;v
         */
        private Vector3D getPoV(final double Y) {
            return new Vector3D(co2AB * Y, w);
        }

        /** Compute &delta;q/&delta;v.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @return &delta;q/&delta;v
         */
        private Vector3D getQoV(final double X) {
            return new Vector3D(I * co2AB * X, w);
        }

        /** Compute &delta;&lambda;/&delta;v.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return &delta;&lambda;/&delta;v
         */
        private Vector3D getLoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final Vector3D pos = new Vector3D(X, f, Y, g);
            final Vector3D v2  = new Vector3D(k, getHoV(X, Y, Xdot, Ydot), -h, getKoV(X, Y, Xdot, Ydot));
            return new Vector3D(-2. * ooA, pos, ooBpo, v2, (I * q * Y - p * X) * ooA, w);
        }

    }
}
