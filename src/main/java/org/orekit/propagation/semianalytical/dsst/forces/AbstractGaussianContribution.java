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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.io.NotSerializableException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import org.hipparchus.analysis.UnivariateVectorFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.attitudes.Attitude;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CjSjCoefficient;
import org.orekit.propagation.semianalytical.dsst.utilities.ShortPeriodicsInterpolatedCoefficient;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeSpanMap;

/** Common handling of {@link DSSTForceModel} methods for Gaussian contributions to DSST propagation.
 * <p>
 * This abstract class allows to provide easily a subset of {@link DSSTForceModel} methods
 * for specific Gaussian contributions.
 * </p><p>
 * This class implements the notion of numerical averaging of the DSST theory.
 * Numerical averaging is mainly used for non-conservative disturbing forces such as
 * atmospheric drag and solar radiation pressure.
 * </p><p>
 * Gaussian contributions can be expressed as: da<sub>i</sub>/dt = δa<sub>i</sub>/δv . q<br>
 * where:
 * <ul>
 * <li>a<sub>i</sub> are the six equinoctial elements</li>
 * <li>v is the velocity vector</li>
 * <li>q is the perturbing acceleration due to the considered force</li>
 * </ul>
 *
 * <p> The averaging process and other considerations lead to integrate this contribution
 * over the true longitude L possibly taking into account some limits.
 *
 * <p> To create a numerically averaged contribution, one needs only to provide a
 * {@link ForceModel} and to implement in the derived class the method:
 * {@link #getLLimits(SpacecraftState)}.
 * </p>
 * @author Pascal Parraud
 */
public abstract class AbstractGaussianContribution implements DSSTForceModel {

    /** Available orders for Gauss quadrature. */
    private static final int[] GAUSS_ORDER = {12, 16, 20, 24, 32, 40, 48};

    /** Max rank in Gauss quadrature orders array. */
    private static final int MAX_ORDER_RANK = GAUSS_ORDER.length - 1;

    /** Number of points for interpolation. */
    private static final int INTERPOLATION_POINTS = 3;

    /** Maximum value for j index. */
    private static final int JMAX = 12;

    /** Retrograde factor I.
     *  <p>
     *  DSST model needs equinoctial orbit as internal representation.
     *  Classical equinoctial elements have discontinuities when inclination
     *  is close to zero. In this representation, I = +1. <br>
     *  To avoid this discontinuity, another representation exists and equinoctial
     *  elements can be expressed in a different way, called "retrograde" orbit.
     *  This implies I = -1. <br>
     *  As Orekit doesn't implement the retrograde orbit, I is always set to +1.
     *  But for the sake of consistency with the theory, the retrograde factor
     *  has been kept in the formulas.
     *  </p>
     */
    private static final int I = 1;

    // CHECKSTYLE: stop VisibilityModifierCheck

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

    /** Kepler mean motion: n = sqrt(μ / a³). */
    protected double n;

    /** Mean longitude. */
    protected double lm;

    /** Equinoctial frame f vector. */
    protected Vector3D f;
    /** Equinoctial frame g vector. */
    protected Vector3D g;
    /** Equinoctial frame w vector. */
    protected Vector3D w;

    /** A = sqrt(μ * a). */
    protected double A;
    /** B = sqrt(1 - h² - k²). */
    protected double B;
    /** C = 1 + p² + q². */
    protected double C;

    /** 2 / (n² * a) . */
    protected double ton2a;
    /** 1 / A .*/
    protected double ooA;
    /** 1 / (A * B) .*/
    protected double ooAB;
    /** C / (2 * A * B) .*/
    protected double co2AB;
    /** 1 / (1 + B) .*/
    protected double ooBpo;
    /** 1 / μ .*/
    protected double ooMu;
    /** μ .*/
    protected double mu;

    // CHECKSTYLE: resume VisibilityModifierCheck

    /** Contribution to be numerically averaged. */
    private final ForceModel contribution;

    /** Gauss integrator. */
    private final double threshold;

    /** Gauss integrator. */
    private GaussQuadrature integrator;

    /** Flag for Gauss order computation. */
    private boolean isDirty;

    /** Attitude provider. */
    private AttitudeProvider attitudeProvider;

    /** Prefix for coefficients keys. */
    private final String coefficientsKeyPrefix;

    /** Short period terms. */
    private GaussianShortPeriodicCoefficients gaussianSPCoefs;

    /** Build a new instance.
     *  @param coefficientsKeyPrefix prefix for coefficients keys
     *  @param threshold tolerance for the choice of the Gauss quadrature order
     *  @param contribution the {@link ForceModel} to be numerically averaged
     */
    protected AbstractGaussianContribution(final String coefficientsKeyPrefix,
                                           final double threshold,
                                           final ForceModel contribution) {
        this.coefficientsKeyPrefix = coefficientsKeyPrefix;
        this.contribution          = contribution;
        this.threshold             = threshold;
        this.integrator            = new GaussQuadrature(GAUSS_ORDER[MAX_ORDER_RANK]);
        this.isDirty               = true;
    }

    /** {@inheritDoc} */
    @Override
    public List<ShortPeriodTerms> initialize(final AuxiliaryElements aux, final boolean meanOnly) {

        final List<ShortPeriodTerms> list = new ArrayList<ShortPeriodTerms>();
        gaussianSPCoefs = new GaussianShortPeriodicCoefficients(coefficientsKeyPrefix,
                                                                JMAX, INTERPOLATION_POINTS,
                                                                new TimeSpanMap<Slot>(new Slot(JMAX, INTERPOLATION_POINTS)));
        list.add(gaussianSPCoefs);
        return list;

    }

    /** {@inheritDoc} */
    @Override
    public void initializeStep(final AuxiliaryElements aux)
        throws OrekitException {

        // Equinoctial elements
        a  = aux.getSma();
        k  = aux.getK();
        h  = aux.getH();
        q  = aux.getQ();
        p  = aux.getP();

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
        n = aux.getMeanMotion();

        // Mean longitude
        lm = aux.getLM();

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
        // mu
        mu = aux.getMu();
        // 1 / mu
        ooMu  = 1. / mu;
    }

    /** {@inheritDoc} */
    @Override
    public double[] getMeanElementRate(final SpacecraftState state) throws OrekitException {

        double[] meanElementRate = new double[6];
        // Computes the limits for the integral
        final double[] ll = getLLimits(state);
        // Computes integrated mean element rates if Llow < Lhigh
        if (ll[0] < ll[1]) {
            meanElementRate = getMeanElementRate(state, integrator, ll[0], ll[1]);
            if (isDirty) {
                boolean next = true;
                for (int i = 0; i < MAX_ORDER_RANK && next; i++) {
                    final double[] meanRates = getMeanElementRate(state, new GaussQuadrature(GAUSS_ORDER[i]), ll[0], ll[1]);
                    if (getRatesDiff(meanElementRate, meanRates) < threshold) {
                        integrator = new GaussQuadrature(GAUSS_ORDER[i]);
                        next = false;
                    }
                }
                isDirty = false;
            }
        }
        return meanElementRate;
    }

    /** Compute the acceleration due to the non conservative perturbing force.
     *
     *  @param state current state information: date, kinematics, attitude
     *  @return the perturbing acceleration
     *  @exception OrekitException if some specific error occurs
     */
    protected Vector3D getAcceleration(final SpacecraftState state)
        throws OrekitException {
        final AccelerationRetriever retriever = new AccelerationRetriever(state);
        contribution.addContribution(state, retriever);

        return retriever.getAcceleration();
    }

    /** Compute the limits in L, the true longitude, for integration.
     *
     *  @param  state current state information: date, kinematics, attitude
     *  @return the integration limits in L
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract double[] getLLimits(SpacecraftState state) throws OrekitException;

    /** Computes the mean equinoctial elements rates da<sub>i</sub> / dt.
     *
     *  @param state current state
     *  @param gauss Gauss quadrature
     *  @param low lower bound of the integral interval
     *  @param high upper bound of the integral interval
     *  @return the mean element rates
     *  @throws OrekitException if some specific error occurs
     */
    private double[] getMeanElementRate(final SpacecraftState state,
            final GaussQuadrature gauss,
            final double low,
            final double high) throws OrekitException {
        final double[] meanElementRate = gauss.integrate(new IntegrableFunction(state, true, 0), low, high);
        // Constant multiplier for integral
        final double coef = 1. / (2. * FastMath.PI * B);
        // Corrects mean element rates
        for (int i = 0; i < 6; i++) {
            meanElementRate[i] *= coef;
        }
        return meanElementRate;
    }

    /** Estimates the weighted magnitude of the difference between 2 sets of equinoctial elements rates.
     *
     *  @param meanRef reference rates
     *  @param meanCur current rates
     *  @return estimated magnitude of weighted differences
     */
    private double getRatesDiff(final double[] meanRef, final double[] meanCur) {
        double maxDiff = FastMath.abs(meanRef[0] - meanCur[0]) / a;
        // Corrects mean element rates
        for (int i = 1; i < meanRef.length; i++) {
            final double diff = FastMath.abs(meanRef[i] - meanCur[i]);
            if (maxDiff < diff) maxDiff = diff;
        }
        return maxDiff;
    }

    /** {@inheritDoc} */
    @Override
    public void registerAttitudeProvider(final AttitudeProvider provider) {
        this.attitudeProvider = provider;
    }

    /** {@inheritDoc} */
    @Override
    public void updateShortPeriodTerms(final SpacecraftState ... meanStates)
        throws OrekitException {

        final Slot slot = gaussianSPCoefs.createSlot(meanStates);
        for (final SpacecraftState meanState : meanStates) {
            initializeStep(new AuxiliaryElements(meanState.getOrbit(), I));
            final double[][] currentRhoSigmaj = computeRhoSigmaCoefficients(meanState.getDate());
            final FourierCjSjCoefficients fourierCjSj = new FourierCjSjCoefficients(meanState, JMAX);
            final UijVijCoefficients uijvij = new UijVijCoefficients(currentRhoSigmaj, fourierCjSj, JMAX);
            gaussianSPCoefs.computeCoefficients(meanState, slot, fourierCjSj, uijvij, n, a);
        }

    }

    /**
     * Compute the auxiliary quantities ρ<sub>j</sub> and σ<sub>j</sub>.
     * <p>
     * The expressions used are equations 2.5.3-(4) from the Danielson paper. <br/>
     *  ρ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>C<sub>j</sub>(k, h) <br/>
     *  σ<sub>j</sub> = (1+jB)(-b)<sup>j</sup>S<sub>j</sub>(k, h) <br/>
     * </p>
     * @param date current date
     * @return computed coefficients
     */
    private double[][] computeRhoSigmaCoefficients(final AbsoluteDate date) {
        final double[][] currentRhoSigmaj = new double[2][3 * JMAX + 1];
        final CjSjCoefficient cjsjKH = new CjSjCoefficient(k, h);
        final double b = 1. / (1 + B);

        // (-b)<sup>j</sup>
        double mbtj = 1;

        for (int j = 1; j <= 3 * JMAX; j++) {

            //Compute current rho and sigma;
            mbtj *= -b;
            final double coef = (1 + j * B) * mbtj;
            currentRhoSigmaj[0][j] = coef * cjsjKH.getCj(j);
            currentRhoSigmaj[1][j] = coef * cjsjKH.getSj(j);
        }
        return currentRhoSigmaj;
    }

    /** Internal class for retrieving acceleration from a {@link ForceModel}. */
    private static class AccelerationRetriever implements TimeDerivativesEquations {

        /** acceleration vector. */
        private Vector3D acceleration;

        /** state. */
        private final SpacecraftState state;

        /** Simple constructor.
         *  @param state input state
         */
        AccelerationRetriever(final SpacecraftState state) {
            this.acceleration = Vector3D.ZERO;
            this.state = state;
        }

        /** {@inheritDoc} */
        @Override
        public void addKeplerContribution(final double mu) {
        }

        /** {@inheritDoc} */
        @Override
        public void addXYZAcceleration(final double x, final double y, final double z) {
            acceleration = new Vector3D(x, y, z);
        }

        /** {@inheritDoc} */
        @Override
        public void addAcceleration(final Vector3D gamma, final Frame frame)
            throws OrekitException {
            acceleration = frame.getTransformTo(state.getFrame(),
                                                state.getDate()).transformVector(gamma);
        }

        /** {@inheritDoc} */
        @Override
        public void addMassDerivative(final double q) {
        }

        /** Get the acceleration vector.
         * @return acceleration vector
         */
        public Vector3D getAcceleration() {
            return acceleration;
        }

    }

    /** Internal class for numerical quadrature. */
    private class IntegrableFunction implements UnivariateVectorFunction {

        /** Current state. */
        private final SpacecraftState state;

        /** Signal that this class is used to compute the values required by the mean element variations
         * or by the short periodic element variations. */
        private final boolean meanMode;

        /** The j index.
         * <p>
         * Used only for short periodic variation. Ignored for mean elements variation.
         * </p> */
        private final int j;

        /** Build a new instance.
         *  @param  state current state information: date, kinematics, attitude
         *  @param meanMode if true return the value associated to the mean elements variation,
         *                  if false return the values associated to the short periodic elements variation
         * @param j the j index. used only for short periodic variation. Ignored for mean elements variation.
         */
        IntegrableFunction(final SpacecraftState state, final boolean meanMode, final int j) {
            this.state = state;
            this.meanMode = meanMode;
            this.j = j;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final double x) {

            //Compute the time difference from the true longitude difference
            final double shiftedLm = trueToMean(x);
            final double dLm = shiftedLm - lm;
            final double dt = dLm / n;

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
            final Vector3D vel = new Vector3D(Xdot, f, Ydot, g);

            // Compute acceleration
            Vector3D acc = Vector3D.ZERO;
            try {

                // shift the orbit to dt
                final Orbit shiftedOrbit = state.getOrbit().shiftedBy(dt);

                // Recompose an orbit with time held fixed to be compliant with DSST theory
                final Orbit recomposedOrbit =
                        new EquinoctialOrbit(shiftedOrbit.getA(),
                                             shiftedOrbit.getEquinoctialEx(),
                                             shiftedOrbit.getEquinoctialEy(),
                                             shiftedOrbit.getHx(),
                                             shiftedOrbit.getHy(),
                                             shiftedOrbit.getLv(),
                                             PositionAngle.TRUE,
                                             shiftedOrbit.getFrame(),
                                             state.getDate(),
                                             shiftedOrbit.getMu());

                // Get the corresponding attitude
                final Attitude recomposedAttitude =
                        attitudeProvider.getAttitude(recomposedOrbit,
                                                     recomposedOrbit.getDate(),
                                                     recomposedOrbit.getFrame());

                // create shifted SpacecraftState with attitude at specified time
                final SpacecraftState shiftedState =
                        new SpacecraftState(recomposedOrbit, recomposedAttitude, state.getMass());

                acc = getAcceleration(shiftedState);

            } catch (OrekitException oe) {
                throw new OrekitExceptionWrapper(oe);
            }
            //Compute the derivatives of the elements by the speed
            final double[] deriv = new double[6];
            // da/dv
            deriv[0] = getAoV(vel).dotProduct(acc);
            // dex/dv
            deriv[1] = getKoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dey/dv
            deriv[2] = getHoV(X, Y, Xdot, Ydot).dotProduct(acc);
            // dhx/dv
            deriv[3] = getQoV(X).dotProduct(acc);
            // dhy/dv
            deriv[4] = getPoV(Y).dotProduct(acc);
            // dλ/dv
            deriv[5] = getLoV(X, Y, Xdot, Ydot).dotProduct(acc);

            // Compute mean elements rates
            double[] val = null;
            if (meanMode) {
                val = new double[6];
                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dt
                    val[i] = roa2 * deriv[i];
                }
            } else {
                val = new double[12];
                //Compute cos(j*L) and sin(j*L);
                final double cosjL = j == 1 ? cosL : FastMath.cos(j * x);
                final double sinjL = j == 1 ? sinL : FastMath.sin(j * x);

                for (int i = 0; i < 6; i++) {
                    // da<sub>i</sub>/dv * cos(jL)
                    val[i] = cosjL * deriv[i];
                    // da<sub>i</sub>/dv * sin(jL)
                    val[i + 6] = sinjL * deriv[i];
                }
            }
            return val;
        }

        /** Converts true longitude to eccentric longitude.
         * @param lv True longitude
         * @return Eccentric longitude
         */
        private double trueToEccentric (final double lv) {
            final double cosLv   = FastMath.cos(lv);
            final double sinLv   = FastMath.sin(lv);
            final double num     = h * cosLv - k * sinLv;
            final double den     = B + 1 + k * cosLv + h * sinLv;
            return lv + 2 * FastMath.atan(num / den);
        }

        /** Converts eccentric longitude to mean longitude.
         * @param le Eccentric longitude
         * @return Mean longitude
         */
        private double eccentricToMean (final double le) {
            return le - k * FastMath.sin(le) + h * FastMath.cos(le);
        }

        /** Converts true longitude to mean longitude.
         * @param lv True longitude
         * @return Eccentric longitude
         */
        private double trueToMean (final double lv) {
            return eccentricToMean(trueToEccentric(lv));
        }

        /** Compute δa/δv.
         *  @param vel satellite velocity
         *  @return δa/δv
         */
        private Vector3D getAoV(final Vector3D vel) {
            return new Vector3D(ton2a, vel);
        }

        /** Compute δh/δv.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return δh/δv
         */
        private Vector3D getHoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = (2. * Xdot * Y - X * Ydot) * ooMu;
            final double kg = X * Xdot * ooMu;
            final double kw = k * (I * q * Y - p * X) * ooAB;
            return new Vector3D(kf, f, -kg, g, kw, w);
        }

        /** Compute δk/δv.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return δk/δv
         */
        private Vector3D getKoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final double kf = Y * Ydot * ooMu;
            final double kg = (2. * X * Ydot - Xdot * Y) * ooMu;
            final double kw = h * (I * q * Y - p * X) * ooAB;
            return new Vector3D(-kf, f, kg, g, -kw, w);
        }

        /** Compute δp/δv.
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @return δp/δv
         */
        private Vector3D getPoV(final double Y) {
            return new Vector3D(co2AB * Y, w);
        }

        /** Compute δq/δv.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @return δq/δv
         */
        private Vector3D getQoV(final double X) {
            return new Vector3D(I * co2AB * X, w);
        }

        /** Compute δλ/δv.
         *  @param X satellite position component along f, equinoctial reference frame 1st vector
         *  @param Y satellite position component along g, equinoctial reference frame 2nd vector
         *  @param Xdot satellite velocity component along f, equinoctial reference frame 1st vector
         *  @param Ydot satellite velocity component along g, equinoctial reference frame 2nd vector
         *  @return δλ/δv
         */
        private Vector3D getLoV(final double X, final double Y, final double Xdot, final double Ydot) {
            final Vector3D pos = new Vector3D(X, f, Y, g);
            final Vector3D v2  = new Vector3D(k, getHoV(X, Y, Xdot, Ydot), -h, getKoV(X, Y, Xdot, Ydot));
            return new Vector3D(-2. * ooA, pos, ooBpo, v2, (I * q * Y - p * X) * ooA, w);
        }

    }

    /** Class used to {@link #integrate(UnivariateVectorFunction, double, double) integrate}
     *  a {@link org.hipparchus.analysis.UnivariateVectorFunction function}
     *  of the orbital elements using the Gaussian quadrature rule to get the acceleration.
     */
    private static class GaussQuadrature {

        // CHECKSTYLE: stop NoWhitespaceAfter

        // Points and weights for the available quadrature orders

        /** Points for quadrature of order 12. */
        private static final double[] P_12 = {
            -0.98156063424671910000,
            -0.90411725637047490000,
            -0.76990267419430470000,
            -0.58731795428661740000,
            -0.36783149899818024000,
            -0.12523340851146890000,
            0.12523340851146890000,
            0.36783149899818024000,
            0.58731795428661740000,
            0.76990267419430470000,
            0.90411725637047490000,
            0.98156063424671910000
        };

        /** Weights for quadrature of order 12. */
        private static final double[] W_12 = {
            0.04717533638651220000,
            0.10693932599531830000,
            0.16007832854334633000,
            0.20316742672306584000,
            0.23349253653835478000,
            0.24914704581340286000,
            0.24914704581340286000,
            0.23349253653835478000,
            0.20316742672306584000,
            0.16007832854334633000,
            0.10693932599531830000,
            0.04717533638651220000
        };

        /** Points for quadrature of order 16. */
        private static final double[] P_16 = {
            -0.98940093499164990000,
            -0.94457502307323260000,
            -0.86563120238783160000,
            -0.75540440835500310000,
            -0.61787624440264380000,
            -0.45801677765722737000,
            -0.28160355077925890000,
            -0.09501250983763745000,
            0.09501250983763745000,
            0.28160355077925890000,
            0.45801677765722737000,
            0.61787624440264380000,
            0.75540440835500310000,
            0.86563120238783160000,
            0.94457502307323260000,
            0.98940093499164990000
        };

        /** Weights for quadrature of order 16. */
        private static final double[] W_16 = {
            0.02715245941175405800,
            0.06225352393864777000,
            0.09515851168249283000,
            0.12462897125553388000,
            0.14959598881657685000,
            0.16915651939500256000,
            0.18260341504492360000,
            0.18945061045506847000,
            0.18945061045506847000,
            0.18260341504492360000,
            0.16915651939500256000,
            0.14959598881657685000,
            0.12462897125553388000,
            0.09515851168249283000,
            0.06225352393864777000,
            0.02715245941175405800
        };

        /** Points for quadrature of order 20. */
        private static final double[] P_20 = {
            -0.99312859918509490000,
            -0.96397192727791390000,
            -0.91223442825132600000,
            -0.83911697182221890000,
            -0.74633190646015080000,
            -0.63605368072651510000,
            -0.51086700195082700000,
            -0.37370608871541955000,
            -0.22778585114164507000,
            -0.07652652113349734000,
            0.07652652113349734000,
            0.22778585114164507000,
            0.37370608871541955000,
            0.51086700195082700000,
            0.63605368072651510000,
            0.74633190646015080000,
            0.83911697182221890000,
            0.91223442825132600000,
            0.96397192727791390000,
            0.99312859918509490000
        };

        /** Weights for quadrature of order 20. */
        private static final double[] W_20 = {
            0.01761400713915226400,
            0.04060142980038684000,
            0.06267204833410904000,
            0.08327674157670477000,
            0.10193011981724048000,
            0.11819453196151844000,
            0.13168863844917678000,
            0.14209610931838212000,
            0.14917298647260380000,
            0.15275338713072600000,
            0.15275338713072600000,
            0.14917298647260380000,
            0.14209610931838212000,
            0.13168863844917678000,
            0.11819453196151844000,
            0.10193011981724048000,
            0.08327674157670477000,
            0.06267204833410904000,
            0.04060142980038684000,
            0.01761400713915226400
        };

        /** Points for quadrature of order 24. */
        private static final double[] P_24 = {
            -0.99518721999702130000,
            -0.97472855597130950000,
            -0.93827455200273270000,
            -0.88641552700440100000,
            -0.82000198597390300000,
            -0.74012419157855440000,
            -0.64809365193697550000,
            -0.54542147138883950000,
            -0.43379350762604520000,
            -0.31504267969616340000,
            -0.19111886747361634000,
            -0.06405689286260563000,
            0.06405689286260563000,
            0.19111886747361634000,
            0.31504267969616340000,
            0.43379350762604520000,
            0.54542147138883950000,
            0.64809365193697550000,
            0.74012419157855440000,
            0.82000198597390300000,
            0.88641552700440100000,
            0.93827455200273270000,
            0.97472855597130950000,
            0.99518721999702130000
        };

        /** Weights for quadrature of order 24. */
        private static final double[] W_24 = {
            0.01234122979998733500,
            0.02853138862893380600,
            0.04427743881741981000,
            0.05929858491543691500,
            0.07334648141108027000,
            0.08619016153195320000,
            0.09761865210411391000,
            0.10744427011596558000,
            0.11550566805372553000,
            0.12167047292780335000,
            0.12583745634682825000,
            0.12793819534675221000,
            0.12793819534675221000,
            0.12583745634682825000,
            0.12167047292780335000,
            0.11550566805372553000,
            0.10744427011596558000,
            0.09761865210411391000,
            0.08619016153195320000,
            0.07334648141108027000,
            0.05929858491543691500,
            0.04427743881741981000,
            0.02853138862893380600,
            0.01234122979998733500
        };

        /** Points for quadrature of order 32. */
        private static final double[] P_32 = {
            -0.99726386184948160000,
            -0.98561151154526840000,
            -0.96476225558750640000,
            -0.93490607593773970000,
            -0.89632115576605220000,
            -0.84936761373256990000,
            -0.79448379596794250000,
            -0.73218211874028970000,
            -0.66304426693021520000,
            -0.58771575724076230000,
            -0.50689990893222950000,
            -0.42135127613063540000,
            -0.33186860228212767000,
            -0.23928736225213710000,
            -0.14447196158279646000,
            -0.04830766568773831000,
            0.04830766568773831000,
            0.14447196158279646000,
            0.23928736225213710000,
            0.33186860228212767000,
            0.42135127613063540000,
            0.50689990893222950000,
            0.58771575724076230000,
            0.66304426693021520000,
            0.73218211874028970000,
            0.79448379596794250000,
            0.84936761373256990000,
            0.89632115576605220000,
            0.93490607593773970000,
            0.96476225558750640000,
            0.98561151154526840000,
            0.99726386184948160000
        };

        /** Weights for quadrature of order 32. */
        private static final double[] W_32 = {
            0.00701861000947013600,
            0.01627439473090571200,
            0.02539206530926214200,
            0.03427386291302141000,
            0.04283589802222658600,
            0.05099805926237621600,
            0.05868409347853559000,
            0.06582222277636193000,
            0.07234579410884862000,
            0.07819389578707042000,
            0.08331192422694673000,
            0.08765209300440380000,
            0.09117387869576390000,
            0.09384439908080441000,
            0.09563872007927487000,
            0.09654008851472784000,
            0.09654008851472784000,
            0.09563872007927487000,
            0.09384439908080441000,
            0.09117387869576390000,
            0.08765209300440380000,
            0.08331192422694673000,
            0.07819389578707042000,
            0.07234579410884862000,
            0.06582222277636193000,
            0.05868409347853559000,
            0.05099805926237621600,
            0.04283589802222658600,
            0.03427386291302141000,
            0.02539206530926214200,
            0.01627439473090571200,
            0.00701861000947013600
        };

        /** Points for quadrature of order 40. */
        private static final double[] P_40 = {
            -0.99823770971055930000,
            -0.99072623869945710000,
            -0.97725994998377420000,
            -0.95791681921379170000,
            -0.93281280827867660000,
            -0.90209880696887420000,
            -0.86595950321225960000,
            -0.82461223083331170000,
            -0.77830565142651940000,
            -0.72731825518992710000,
            -0.67195668461417960000,
            -0.61255388966798030000,
            -0.54946712509512820000,
            -0.48307580168617870000,
            -0.41377920437160500000,
            -0.34199409082575850000,
            -0.26815218500725370000,
            -0.19269758070137110000,
            -0.11608407067525522000,
            -0.03877241750605081600,
            0.03877241750605081600,
            0.11608407067525522000,
            0.19269758070137110000,
            0.26815218500725370000,
            0.34199409082575850000,
            0.41377920437160500000,
            0.48307580168617870000,
            0.54946712509512820000,
            0.61255388966798030000,
            0.67195668461417960000,
            0.72731825518992710000,
            0.77830565142651940000,
            0.82461223083331170000,
            0.86595950321225960000,
            0.90209880696887420000,
            0.93281280827867660000,
            0.95791681921379170000,
            0.97725994998377420000,
            0.99072623869945710000,
            0.99823770971055930000
        };

        /** Weights for quadrature of order 40. */
        private static final double[] W_40 = {
            0.00452127709853309800,
            0.01049828453115270400,
            0.01642105838190797300,
            0.02224584919416689000,
            0.02793700698002338000,
            0.03346019528254786500,
            0.03878216797447199000,
            0.04387090818567333000,
            0.04869580763507221000,
            0.05322784698393679000,
            0.05743976909939157000,
            0.06130624249292891000,
            0.06480401345660108000,
            0.06791204581523394000,
            0.07061164739128681000,
            0.07288658239580408000,
            0.07472316905796833000,
            0.07611036190062619000,
            0.07703981816424793000,
            0.07750594797842482000,
            0.07750594797842482000,
            0.07703981816424793000,
            0.07611036190062619000,
            0.07472316905796833000,
            0.07288658239580408000,
            0.07061164739128681000,
            0.06791204581523394000,
            0.06480401345660108000,
            0.06130624249292891000,
            0.05743976909939157000,
            0.05322784698393679000,
            0.04869580763507221000,
            0.04387090818567333000,
            0.03878216797447199000,
            0.03346019528254786500,
            0.02793700698002338000,
            0.02224584919416689000,
            0.01642105838190797300,
            0.01049828453115270400,
            0.00452127709853309800
        };

        /** Points for quadrature of order 48. */
        private static final double[] P_48 = {
            -0.99877100725242610000,
            -0.99353017226635080000,
            -0.98412458372282700000,
            -0.97059159254624720000,
            -0.95298770316043080000,
            -0.93138669070655440000,
            -0.90587913671556960000,
            -0.87657202027424800000,
            -0.84358826162439350000,
            -0.80706620402944250000,
            -0.76715903251574020000,
            -0.72403413092381470000,
            -0.67787237963266400000,
            -0.62886739677651370000,
            -0.57722472608397270000,
            -0.52316097472223300000,
            -0.46690290475095840000,
            -0.40868648199071680000,
            -0.34875588629216070000,
            -0.28736248735545555000,
            -0.22476379039468908000,
            -0.16122235606889174000,
            -0.09700469920946270000,
            -0.03238017096286937000,
            0.03238017096286937000,
            0.09700469920946270000,
            0.16122235606889174000,
            0.22476379039468908000,
            0.28736248735545555000,
            0.34875588629216070000,
            0.40868648199071680000,
            0.46690290475095840000,
            0.52316097472223300000,
            0.57722472608397270000,
            0.62886739677651370000,
            0.67787237963266400000,
            0.72403413092381470000,
            0.76715903251574020000,
            0.80706620402944250000,
            0.84358826162439350000,
            0.87657202027424800000,
            0.90587913671556960000,
            0.93138669070655440000,
            0.95298770316043080000,
            0.97059159254624720000,
            0.98412458372282700000,
            0.99353017226635080000,
            0.99877100725242610000
        };

        /** Weights for quadrature of order 48. */
        private static final double[] W_48 = {
            0.00315334605230596250,
            0.00732755390127620800,
            0.01147723457923446900,
            0.01557931572294386600,
            0.01961616045735556700,
            0.02357076083932435600,
            0.02742650970835688000,
            0.03116722783279807000,
            0.03477722256477045000,
            0.03824135106583080600,
            0.04154508294346483000,
            0.04467456085669424000,
            0.04761665849249054000,
            0.05035903555385448000,
            0.05289018948519365000,
            0.05519950369998416500,
            0.05727729210040315000,
            0.05911483969839566000,
            0.06070443916589384000,
            0.06203942315989268000,
            0.06311419228625403000,
            0.06392423858464817000,
            0.06446616443595010000,
            0.06473769681268386000,
            0.06473769681268386000,
            0.06446616443595010000,
            0.06392423858464817000,
            0.06311419228625403000,
            0.06203942315989268000,
            0.06070443916589384000,
            0.05911483969839566000,
            0.05727729210040315000,
            0.05519950369998416500,
            0.05289018948519365000,
            0.05035903555385448000,
            0.04761665849249054000,
            0.04467456085669424000,
            0.04154508294346483000,
            0.03824135106583080600,
            0.03477722256477045000,
            0.03116722783279807000,
            0.02742650970835688000,
            0.02357076083932435600,
            0.01961616045735556700,
            0.01557931572294386600,
            0.01147723457923446900,
            0.00732755390127620800,
            0.00315334605230596250
        };
        // CHECKSTYLE: resume NoWhitespaceAfter

        /** Node points. */
        private final double[] nodePoints;

        /** Node weights. */
        private final double[] nodeWeights;

        /** Creates a Gauss integrator of the given order.
         *
         *  @param numberOfPoints Order of the integration rule.
         */
        GaussQuadrature(final int numberOfPoints) {

            switch(numberOfPoints) {
                case 12 :
                    this.nodePoints  = P_12.clone();
                    this.nodeWeights = W_12.clone();
                    break;
                case 16 :
                    this.nodePoints  = P_16.clone();
                    this.nodeWeights = W_16.clone();
                    break;
                case 20 :
                    this.nodePoints  = P_20.clone();
                    this.nodeWeights = W_20.clone();
                    break;
                case 24 :
                    this.nodePoints  = P_24.clone();
                    this.nodeWeights = W_24.clone();
                    break;
                case 32 :
                    this.nodePoints  = P_32.clone();
                    this.nodeWeights = W_32.clone();
                    break;
                case 40 :
                    this.nodePoints  = P_40.clone();
                    this.nodeWeights = W_40.clone();
                    break;
                case 48 :
                default :
                    this.nodePoints  = P_48.clone();
                    this.nodeWeights = W_48.clone();
                    break;
            }

        }

        /** Integrates a given function on the given interval.
         *
         *  @param f Function to integrate.
         *  @param lowerBound Lower bound of the integration interval.
         *  @param upperBound Upper bound of the integration interval.
         *  @return the integral of the weighted function.
         */
        public double[] integrate(final UnivariateVectorFunction f,
                final double lowerBound, final double upperBound) {

            final double[] adaptedPoints  = nodePoints.clone();
            final double[] adaptedWeights = nodeWeights.clone();
            transform(adaptedPoints, adaptedWeights, lowerBound, upperBound);
            return basicIntegrate(f, adaptedPoints, adaptedWeights);
        }

        /** Performs a change of variable so that the integration
         *  can be performed on an arbitrary interval {@code [a, b]}.
         *  <p>
         *  It is assumed that the natural interval is {@code [-1, 1]}.
         *  </p>
         *
         * @param points  Points to adapt to the new interval.
         * @param weights Weights to adapt to the new interval.
         * @param a Lower bound of the integration interval.
         * @param b Lower bound of the integration interval.
         */
        private void transform(final double[] points, final double[] weights,
                final double a, final double b) {
            // Scaling
            final double scale = (b - a) / 2;
            final double shift = a + scale;
            for (int i = 0; i < points.length; i++) {
                points[i]   = points[i] * scale + shift;
                weights[i] *= scale;
            }
        }

        /** Returns an estimate of the integral of {@code f(x) * w(x)},
         *  where {@code w} is a weight function that depends on the actual
         *  flavor of the Gauss integration scheme.
         *
         * @param f Function to integrate.
         * @param points  Nodes.
         * @param weights Nodes weights.
         * @return the integral of the weighted function.
         */
        private double[] basicIntegrate(final UnivariateVectorFunction f,
                final double[] points,
                final double[] weights) {
            double x = points[0];
            double w = weights[0];
            double[] v = f.value(x);
            final double[] y = new double[v.length];
            for (int j = 0; j < v.length; j++) {
                y[j] = w * v[j];
            }
            final double[] t = y.clone();
            final double[] c = new double[v.length];
            final double[] s = t.clone();
            for (int i = 1; i < points.length; i++) {
                x = points[i];
                w = weights[i];
                v = f.value(x);
                for (int j = 0; j < v.length; j++) {
                    y[j] = w * v[j] - c[j];
                    t[j] =  s[j] + y[j];
                    c[j] = (t[j] - s[j]) - y[j];
                    s[j] = t[j];
                }
            }
            return s;
        }

    }

    /** Compute the C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> coefficients.
     *  <p>
     *  Those coefficients are given in Danielson paper by expression 4.4-(6)
     *  </p>
     *  @author Petre Bazavan
     *  @author Lucian Barbulescu
     */
    private class FourierCjSjCoefficients {

        /** Maximum possible value for j. */
        private final int jMax;

        /** The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final double[][] cCoef;

        /** The C<sub>i</sub><sup>j</sup> coefficients.
         * <p>
         * the index i corresponds to the following elements: <br/>
         * - 0 for a <br>
         * - 1 for k <br>
         * - 2 for h <br>
         * - 3 for q <br>
         * - 4 for p <br>
         * - 5 for λ <br>
         * </p>
         */
        private final double[][] sCoef;

        /** Standard constructor.
         * @param state the current state
         * @param jMax maximum value for j
         * @throws OrekitException in case of an error
         */
        FourierCjSjCoefficients(final SpacecraftState state, final int jMax)
            throws OrekitException {
            //Initialise the fields
            this.jMax = jMax;

            //Allocate the arrays
            final int rows = jMax + 1;
            cCoef = new double[rows][6];
            sCoef = new double[rows][6];

            //Compute the coefficients
            computeCoefficients(state);
        }

        /**
         * Compute the Fourrier coefficients.
         * <p>
         * Only the C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup> coefficients need to be computed
         * as D<sub>i</sub><sup>m</sup> is always 0.
         * </p>
         * @param state the current state
         * @throws OrekitException in case of an error
         */
        private void computeCoefficients(final SpacecraftState state)
            throws OrekitException {
            // Computes the limits for the integral
            final double[] ll = getLLimits(state);
            // Computes integrated mean element rates if Llow < Lhigh
            if (ll[0] < ll[1]) {
                //Compute 1 / PI
                final double ooPI = 1 / FastMath.PI;

                // loop through all values of j
                for (int j = 0; j <= jMax; j++) {
                    final double[] curentCoefficients =
                            integrator.integrate(new IntegrableFunction(state, false, j), ll[0], ll[1]);

                    //divide by PI and set the values for the coefficients
                    for (int i = 0; i < 6; i++) {
                        cCoef[j][i] = ooPI * curentCoefficients[i];
                        sCoef[j][i] = ooPI * curentCoefficients[i + 6];
                    }
                }
            }
        }

        /** Get the coefficient C<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient C<sub>i</sub><sup>j</sup>
         */
        public double getCij(final int i, final int j) {
            return cCoef[j][i];
        }

        /** Get the coefficient S<sub>i</sub><sup>j</sup>.
         * @param i i index - corresponds to the required variation
         * @param j j index
         * @return the coefficient S<sub>i</sub><sup>j</sup>
         */
        public double getSij(final int i, final int j) {
            return sCoef[j][i];
        }
    }

    /** This class handles the short periodic coefficients described in Danielson 2.5.3-26.
     *
     * <p>
     * The value of M is 0. Also, since the values of the Fourier coefficient D<sub>i</sub><sup>m</sup> is 0
     * then the values of the coefficients D<sub>i</sub><sup>m</sup> for m &gt; 2 are also 0.
     * </p>
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     *
     */
    private static class GaussianShortPeriodicCoefficients implements ShortPeriodTerms {

        /** Serializable UID. */
        private static final long serialVersionUID = 20151118L;

        /** Maximum value for j index. */
        private final int jMax;

        /** Number of points used in the interpolation process. */
        private final int interpolationPoints;

        /** Prefix for coefficients keys. */
        private final String coefficientsKeyPrefix;

        /** All coefficients slots. */
        private final transient TimeSpanMap<Slot> slots;

        /** Constructor.
         *  @param coefficientsKeyPrefix prefix for coefficients keys
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         *  @param slots all coefficients slots
         */
        GaussianShortPeriodicCoefficients(final String coefficientsKeyPrefix,
                                          final int jMax, final int interpolationPoints,
                                          final TimeSpanMap<Slot> slots) {
            //Initialize fields
            this.jMax                  = jMax;
            this.interpolationPoints   = interpolationPoints;
            this.coefficientsKeyPrefix = coefficientsKeyPrefix;
            this.slots                 = slots;
        }

        /** Get the slot valid for some date.
         * @param meanStates mean states defining the slot
         * @return slot valid at the specified date
         */
        public Slot createSlot(final SpacecraftState ... meanStates) {
            final Slot         slot  = new Slot(jMax, interpolationPoints);
            final AbsoluteDate first = meanStates[0].getDate();
            final AbsoluteDate last  = meanStates[meanStates.length - 1].getDate();
            if (first.compareTo(last) <= 0) {
                slots.addValidAfter(slot, first);
            } else {
                slots.addValidBefore(slot, first);
            }
            return slot;
        }

        /** Compute the short periodic coefficients.
         *
         * @param state current state information: date, kinematics, attitude
         * @param slot coefficients slot
         * @param fourierCjSj Fourier coefficients
         * @param uijvij U and V coefficients
         * @param n Keplerian mean motion
         * @param a semi major axis
         * @throws OrekitException if an error occurs
         */
        private void computeCoefficients(final SpacecraftState state, final Slot slot,
                                         final FourierCjSjCoefficients fourierCjSj,
                                         final UijVijCoefficients uijvij,
                                         final double n, final double a)
            throws OrekitException {

            // get the current date
            final AbsoluteDate date = state.getDate();

            // compute the k₂⁰ coefficient
            final double k20 = computeK20(jMax, uijvij.currentRhoSigmaj);

            // 1. / n
            final double oon = 1. / n;
            // 3. / (2 * a * n)
            final double to2an = 1.5 * oon / a;
            // 3. / (4 * a * n)
            final double to4an = to2an / 2;

            // Compute the coefficients for each element
            final int size = jMax + 1;
            final double[]   di1        = new double[6];
            final double[]   di2        = new double[6];
            final double[][] currentCij = new double[size][6];
            final double[][] currentSij = new double[size][6];
            for (int i = 0; i < 6; i++) {

                // compute D<sub>i</sub>¹ and D<sub>i</sub>² (all others are 0)
                di1[i] = -oon * fourierCjSj.getCij(i, 0);
                if (i == 5) {
                    di1[i] += to2an * uijvij.getU1(0, 0);
                }
                di2[i] = 0.;
                if (i == 5) {
                    di2[i] += -to4an * fourierCjSj.getCij(0, 0);
                }

                //the C<sub>i</sub>⁰ is computed based on all others
                currentCij[0][i] = -di2[i] * k20;

                for (int j = 1; j <= jMax; j++) {
                    // compute the current C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup>
                    currentCij[j][i] = oon * uijvij.getU1(j, i);
                    if (i == 5) {
                        currentCij[j][i] += -to2an * uijvij.getU2(j);
                    }
                    currentSij[j][i] = oon * uijvij.getV1(j, i);
                    if (i == 5) {
                        currentSij[j][i] += -to2an * uijvij.getV2(j);
                    }

                    // add the computed coefficients to C<sub>i</sub>⁰
                    currentCij[0][i] += -(currentCij[j][i] * uijvij.currentRhoSigmaj[0][j] + currentSij[j][i] * uijvij.currentRhoSigmaj[1][j]);
                }

            }

            // add the values to the interpolators
            slot.cij[0].addGridPoint(date, currentCij[0]);
            slot.dij[1].addGridPoint(date, di1);
            slot.dij[2].addGridPoint(date, di2);
            for (int j = 1; j <= jMax; j++) {
                slot.cij[j].addGridPoint(date, currentCij[j]);
                slot.sij[j].addGridPoint(date, currentSij[j]);
            }

        }

        /** Compute the coefficient k₂⁰ by using the equation
         * 2.5.3-(9a) from Danielson.
         * <p>
         * After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:<br>
         * k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² + ρ<sub>k</sub>²)]
         * </p>
         * @param kMax max value fot k index
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and σ<sub>j</sub> coefficients
         * @return the coefficient k₂⁰
         */
        private double computeK20(final int kMax, final double[][] currentRhoSigmaj) {
            double k20 = 0.;

            for (int kIndex = 1; kIndex <= kMax; kIndex++) {
                // After inserting 2.5.3-(8) into 2.5.3-(9a) the result becomes:
                //k₂⁰ = &Sigma;<sub>k=1</sub><sup>kMax</sup>[(2 / k²) * (σ<sub>k</sub>² + ρ<sub>k</sub>²)]
                double currentTerm = currentRhoSigmaj[1][kIndex] * currentRhoSigmaj[1][kIndex] +
                                     currentRhoSigmaj[0][kIndex] * currentRhoSigmaj[0][kIndex];

                //multiply by 2 / k²
                currentTerm *= 2. / (kIndex * kIndex);

                // add the term to the result
                k20 += currentTerm;
            }

            return k20;
        }

        /** {@inheritDoc} */
        @Override
        public double[] value(final Orbit meanOrbit) {

            // select the coefficients slot
            final Slot slot = slots.get(meanOrbit.getDate());

            // Get the True longitude L
            final double L = meanOrbit.getLv();

            // Compute the center (l - λ)
            final double center =  L - meanOrbit.getLM();
            // Compute (l - λ)²
            final double center2 = center * center;

            // Initialize short periodic variations
            final double[] shortPeriodicVariation = slot.cij[0].value(meanOrbit.getDate());
            final double[] d1 = slot.dij[1].value(meanOrbit.getDate());
            final double[] d2 = slot.dij[2].value(meanOrbit.getDate());
            for (int i = 0; i < 6; i++) {
                shortPeriodicVariation[i] += center * d1[i] + center2 * d2[i];
            }

            for (int j = 1; j <= JMAX; j++) {
                final double[] c = slot.cij[j].value(meanOrbit.getDate());
                final double[] s = slot.sij[j].value(meanOrbit.getDate());
                final double cos = FastMath.cos(j * L);
                final double sin = FastMath.sin(j * L);
                for (int i = 0; i < 6; i++) {
                    // add corresponding term to the short periodic variation
                    shortPeriodicVariation[i] += c[i] * cos;
                    shortPeriodicVariation[i] += s[i] * sin;
                }
            }

            return shortPeriodicVariation;

        }

        /** {@inheritDoc} */
        public String getCoefficientsKeyPrefix() {
            return coefficientsKeyPrefix;
        }

        /** {@inheritDoc}
         * <p>
         * For Gaussian forces,there are JMAX cj coefficients,
         * JMAX sj coefficients and 3 dj coefficients. As JMAX = 12,
         * this sums up to 27 coefficients. The j index is the integer
         * multiplier for the true longitude argument in the cj and sj
         * coefficients and to the degree in  the polynomial dj coefficients.
         * </p>
         */
        @Override
        public Map<String, double[]> getCoefficients(final AbsoluteDate date, final Set<String> selected)
            throws OrekitException {

            // select the coefficients slot
            final Slot slot = slots.get(date);

            final Map<String, double[]> coefficients = new HashMap<String, double[]>(2 * JMAX + 3);
            storeIfSelected(coefficients, selected, slot.cij[0].value(date), "d", 0);
            storeIfSelected(coefficients, selected, slot.dij[1].value(date), "d", 1);
            storeIfSelected(coefficients, selected, slot.dij[2].value(date), "d", 2);
            for (int j = 1; j <= JMAX; j++) {
                storeIfSelected(coefficients, selected, slot.cij[j].value(date), "c", j);
                storeIfSelected(coefficients, selected, slot.sij[j].value(date), "s", j);
            }

            return coefficients;

        }

        /** Put a coefficient in a map if selected.
         * @param map map to populate
         * @param selected set of coefficients that should be put in the map
         * (empty set means all coefficients are selected)
         * @param value coefficient value
         * @param id coefficient identifier
         * @param indices list of coefficient indices
         */
        private void storeIfSelected(final Map<String, double[]> map, final Set<String> selected,
                                     final double[] value, final String id, final int ... indices) {
            final StringBuilder keyBuilder = new StringBuilder(getCoefficientsKeyPrefix());
            keyBuilder.append(id);
            for (int index : indices) {
                keyBuilder.append('[').append(index).append(']');
            }
            final String key = keyBuilder.toString();
            if (selected.isEmpty() || selected.contains(key)) {
                map.put(key, value);
            }
        }

        /** Replace the instance with a data transfer object for serialization.
         * @return data transfer object that will be serialized
         * @exception NotSerializableException if an additional state provider is not serializable
         */
        private Object writeReplace() throws NotSerializableException {

            // slots transitions
            final SortedSet<TimeSpanMap.Transition<Slot>> transitions     = slots.getTransitions();
            final AbsoluteDate[]                          transitionDates = new AbsoluteDate[transitions.size()];
            final Slot[]                                  allSlots        = new Slot[transitions.size() + 1];
            int i = 0;
            for (final TimeSpanMap.Transition<Slot> transition : transitions) {
                if (i == 0) {
                    // slot before the first transition
                    allSlots[i] = transition.getBefore();
                }
                if (i < transitionDates.length) {
                    transitionDates[i] = transition.getDate();
                    allSlots[++i]      = transition.getAfter();
                }
            }

            return new DataTransferObject(jMax, interpolationPoints, coefficientsKeyPrefix,
                                          transitionDates, allSlots);

        }


        /** Internal class used only for serialization. */
        private static class DataTransferObject implements Serializable {

            /** Serializable UID. */
            private static final long serialVersionUID = 20160319L;

            /** Maximum value for j index. */
            private final int jMax;

            /** Number of points used in the interpolation process. */
            private final int interpolationPoints;

            /** Prefix for coefficients keys. */
            private final String coefficientsKeyPrefix;

            /** Transitions dates. */
            private final AbsoluteDate[] transitionDates;

            /** All slots. */
            private final Slot[] allSlots;

            /** Simple constructor.
             * @param jMax maximum value for j index
             * @param interpolationPoints number of points used in the interpolation process
             * @param coefficientsKeyPrefix prefix for coefficients keys
             * @param transitionDates transitions dates
             * @param allSlots all slots
             */
            DataTransferObject(final int jMax, final int interpolationPoints,
                               final String coefficientsKeyPrefix,
                               final AbsoluteDate[] transitionDates, final Slot[] allSlots) {
                this.jMax                  = jMax;
                this.interpolationPoints   = interpolationPoints;
                this.coefficientsKeyPrefix = coefficientsKeyPrefix;
                this.transitionDates       = transitionDates;
                this.allSlots              = allSlots;
            }

            /** Replace the deserialized data transfer object with a {@link GaussianShortPeriodicCoefficients}.
             * @return replacement {@link GaussianShortPeriodicCoefficients}
             */
            private Object readResolve() {

                final TimeSpanMap<Slot> slots = new TimeSpanMap<Slot>(allSlots[0]);
                for (int i = 0; i < transitionDates.length; ++i) {
                    slots.addValidAfter(allSlots[i + 1], transitionDates[i]);
                }

                return new GaussianShortPeriodicCoefficients(coefficientsKeyPrefix, jMax, interpolationPoints, slots);

            }

        }

    }

    /** The U<sub>i</sub><sup>j</sup> and V<sub>i</sub><sup>j</sup> coefficients described by
     * equations 2.5.3-(21) and 2.5.3-(22) from Danielson.
     * <p>
     * The index i takes only the values 1 and 2<br>
     * For U only the index 0 for j is used.
     * </p>
     *
     * @author Petre Bazavan
     * @author Lucian Barbulescu
     */
    private static class UijVijCoefficients {

        /** The U₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup><br>
         * The only exception is when j = 0 when only the coefficient for fourier index = 1 (i == 0) is needed.<br>
         * Also, for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are computed, because are required
         * to compute the coefficients U₂<sup>j</sup>
         * </p>
         */
        private final double[][] u1ij;

        /** The V₁<sup>j</sup> coefficients.
         * <p>
         * The first index identifies the Fourier coefficients used<br>
         * Those coefficients are computed for all Fourier C<sub>i</sub><sup>j</sup> and S<sub>i</sub><sup>j</sup><br>
         * for fourier index = 1 (i == 0), the coefficients up to 2 * jMax are computed, because are required
         * to compute the coefficients V₂<sup>j</sup>
         * </p>
         */
        private final double[][] v1ij;

        /** The U₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as they are the only ones required.
         * </p>
         */
        private final double[] u2ij;

        /** The V₂<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients that use the Fourier index = 1 (i == 0) are computed as they are the only ones required.
         * </p>
         */
        private final double[] v2ij;

        /** The current computed values for the ρ<sub>j</sub> and σ<sub>j</sub> coefficients. */
        private final double[][] currentRhoSigmaj;

        /** The C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup> Fourier coefficients. */
        private final FourierCjSjCoefficients fourierCjSj;

        /** The maximum value for j index. */
        private final int jMax;

        /** Constructor.
         * @param currentRhoSigmaj the current computed values for the ρ<sub>j</sub> and σ<sub>j</sub> coefficients
         * @param fourierCjSj the fourier coefficients C<sub>i</sub><sup>j</sup> and the S<sub>i</sub><sup>j</sup>
         * @param jMax maximum value for j index
         */
        UijVijCoefficients(final double[][] currentRhoSigmaj, final FourierCjSjCoefficients fourierCjSj, final int jMax) {
            this.currentRhoSigmaj = currentRhoSigmaj;
            this.fourierCjSj = fourierCjSj;
            this.jMax = jMax;

            // initialize the internal arrays.
            this.u1ij = new double[6][2 * jMax + 1];
            this.v1ij = new double[6][2 * jMax + 1];
            this.u2ij = new double[jMax + 1];
            this.v2ij = new double[jMax + 1];

            //compute the coefficients
            computeU1V1Coefficients();
            computeU2V2Coefficients();
        }

        /** Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients. */
        private void computeU1V1Coefficients() {
            // generate the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients
            // for j >= 1
            // also the U₁⁰ for Fourier index = 1 (i == 0) coefficient will be computed
            u1ij[0][0] = 0;
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                for (int i = 0; i < 6; i++) {
                    //j is aready between 1 and J
                    u1ij[i][j] = fourierCjSj.getSij(i, j);
                    v1ij[i][j] = fourierCjSj.getCij(i, j);

                    // 1 - δ<sub>1j</sub> is 1 for all j > 1
                    if (j > 1) {
                        // k starts with 1 because j-J is less than or equal to 0
                        for (int kIndex = 1; kIndex <= j - 1; kIndex++) {
                            // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] +=   fourierCjSj.getCij(i, j - kIndex) * currentRhoSigmaj[1][kIndex] +
                                            fourierCjSj.getSij(i, j - kIndex) * currentRhoSigmaj[0][kIndex];

                            // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                            // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                            v1ij[i][j] +=   fourierCjSj.getCij(i, j - kIndex) * currentRhoSigmaj[0][kIndex] -
                                            fourierCjSj.getSij(i, j - kIndex) * currentRhoSigmaj[1][kIndex];
                        }
                    }

                    // since j must be between 1 and J-1 and is already between 1 and J
                    // the following sum is skiped only for j = jMax
                    if (j != jMax) {
                        for (int kIndex = 1; kIndex <= jMax - j; kIndex++) {
                            // -C<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub>
                            u1ij[i][j] +=   -fourierCjSj.getCij(i, j + kIndex) * currentRhoSigmaj[1][kIndex] +
                                            fourierCjSj.getSij(i, j + kIndex) * currentRhoSigmaj[0][kIndex];

                            // C<sub>i</sub><sup>j+k</sup> * ρ<sub>k</sub> +
                            // S<sub>i</sub><sup>j+k</sup> * σ<sub>k</sub>
                            v1ij[i][j] +=   fourierCjSj.getCij(i, j + kIndex) * currentRhoSigmaj[0][kIndex] +
                                            fourierCjSj.getSij(i, j + kIndex) * currentRhoSigmaj[1][kIndex];
                        }
                    }

                    for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                        // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                        // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                        u1ij[i][j] +=   -fourierCjSj.getCij(i, kIndex) * currentRhoSigmaj[1][j + kIndex] -
                                        fourierCjSj.getSij(i, kIndex) * currentRhoSigmaj[0][j + kIndex];

                        // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                        // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                        v1ij[i][j] +=   fourierCjSj.getCij(i, kIndex) * currentRhoSigmaj[0][j + kIndex] +
                                        fourierCjSj.getSij(i, kIndex) * currentRhoSigmaj[1][j + kIndex];
                    }

                    // divide by 1 / j
                    u1ij[i][j] *= -ooj;
                    v1ij[i][j] *= ooj;

                    // if index = 1 (i == 0) add the computed terms to U₁⁰
                    if (i == 0) {
                        //- (U₁<sup>j</sup> * ρ<sub>j</sub> + V₁<sup>j</sup> * σ<sub>j</sub>
                        u1ij[0][0] += -u1ij[0][j] * currentRhoSigmaj[0][j] - v1ij[0][j] * currentRhoSigmaj[1][j];
                    }
                }
            }

            // Terms with j > jMax are required only when computing the coefficients
            // U₂<sup>j</sup> and V₂<sup>j</sup>
            // and those coefficients are only required for Fourier index = 1 (i == 0).
            for (int j = jMax + 1; j <= 2 * jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;
                //the value of i is 0
                u1ij[0][j] = 0.;
                v1ij[0][j] = 0.;

                //k starts from j-J as it is always greater than or equal to 1
                for (int kIndex = j - jMax; kIndex <= j - 1; kIndex++) {
                    // C<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub> +
                    // S<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub>
                    u1ij[0][j] +=   fourierCjSj.getCij(0, j - kIndex) * currentRhoSigmaj[1][kIndex] +
                                    fourierCjSj.getSij(0, j - kIndex) * currentRhoSigmaj[0][kIndex];

                    // C<sub>i</sub><sup>j-k</sup> * ρ<sub>k</sub> -
                    // S<sub>i</sub><sup>j-k</sup> * σ<sub>k</sub>
                    v1ij[0][j] +=   fourierCjSj.getCij(0, j - kIndex) * currentRhoSigmaj[0][kIndex] -
                                    fourierCjSj.getSij(0, j - kIndex) * currentRhoSigmaj[1][kIndex];
                }
                for (int kIndex = 1; kIndex <= jMax; kIndex++) {
                    // C<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub> -
                    // S<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub>
                    u1ij[0][j] +=   -fourierCjSj.getCij(0, kIndex) * currentRhoSigmaj[1][j + kIndex] -
                                    fourierCjSj.getSij(0, kIndex) * currentRhoSigmaj[0][j + kIndex];

                    // C<sub>i</sub><sup>k</sup> * ρ<sub>j+k</sub> +
                    // S<sub>i</sub><sup>k</sup> * σ<sub>j+k</sub>
                    v1ij[0][j] +=   fourierCjSj.getCij(0, kIndex) * currentRhoSigmaj[0][j + kIndex] +
                                    fourierCjSj.getSij(0, kIndex) * currentRhoSigmaj[1][j + kIndex];
                }

                // divide by 1 / j
                u1ij[0][j] *= -ooj;
                v1ij[0][j] *= ooj;
            }
        }

        /** Build the U₁<sup>j</sup> and V₁<sup>j</sup> coefficients.
         * <p>
         * Only the coefficients for Fourier index = 1 (i == 0) are required.
         * </p>
         */
        private void computeU2V2Coefficients() {
            for (int j = 1; j <= jMax; j++) {
                // compute 1 / j
                final double ooj = 1. / j;

                // only the values for i == 0 are computed
                u2ij[j] = v1ij[0][j];
                v2ij[j] = u1ij[0][j];

                // 1 - δ<sub>1j</sub> is 1 for all j > 1
                if (j > 1) {
                    for (int l = 1; l <= j - 1; l++) {
                        // U₁<sup>j-l</sup> * σ<sub>l</sub> +
                        // V₁<sup>j-l</sup> * ρ<sub>l</sub>
                        u2ij[j] +=   u1ij[0][j - l] * currentRhoSigmaj[1][l] +
                                     v1ij[0][j - l] * currentRhoSigmaj[0][l];

                        // U₁<sup>j-l</sup> * ρ<sub>l</sub> -
                        // V₁<sup>j-l</sup> * σ<sub>l</sub>
                        v2ij[j] +=   u1ij[0][j - l] * currentRhoSigmaj[0][l] -
                                     v1ij[0][j - l] * currentRhoSigmaj[1][l];
                    }
                }

                for (int l = 1; l <= jMax; l++) {
                    // -U₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // U₁<sup>l</sup> * σ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * ρ<sub>l</sub> -
                    // V₁<sup>l</sup> * ρ<sub>j+l</sub>
                    u2ij[j] +=   -u1ij[0][j + l] * currentRhoSigmaj[1][l] +
                                  u1ij[0][l] * currentRhoSigmaj[1][j + l] +
                                  v1ij[0][j + l] * currentRhoSigmaj[0][l] -
                                  v1ij[0][l] * currentRhoSigmaj[0][j + l];

                    // U₁<sup>j+l</sup> * ρ<sub>l</sub> +
                    // U₁<sup>l</sup> * ρ<sub>j+l</sub> +
                    // V₁<sup>j+l</sup> * σ<sub>l</sub> +
                    // V₁<sup>l</sup> * σ<sub>j+l</sub>
                    u2ij[j] +=   u1ij[0][j + l] * currentRhoSigmaj[0][l] +
                                 u1ij[0][l] * currentRhoSigmaj[0][j + l] +
                                 v1ij[0][j + l] * currentRhoSigmaj[1][l] +
                                 v1ij[0][l] * currentRhoSigmaj[1][j + l];
                }

                // divide by 1 / j
                u2ij[j] *= -ooj;
                v2ij[j] *= ooj;
            }
        }

        /** Get the coefficient U₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient U₁<sup>j</sup> for the given Fourier index i
         */
        public double getU1(final int j, final int i) {
            return u1ij[i][j];
        }

        /** Get the coefficient V₁<sup>j</sup> for Fourier index i.
         *
         * @param j j index
         * @param i Fourier index (starts at 0)
         * @return the coefficient V₁<sup>j</sup> for the given Fourier index i
         */
        public double getV1(final int j, final int i) {
            return v1ij[i][j];
        }

        /** Get the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient U₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public double getU2(final int j) {
            return u2ij[j];
        }

        /** Get the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0).
         *
         * @param j j index
         * @return the coefficient V₂<sup>j</sup> for Fourier index = 1 (i == 0)
         */
        public double getV2(final int j) {
            return v2ij[j];
        }
    }

    /** Coefficients valid for one time slot. */
    private static class Slot implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20160319L;

        /**The coefficients D<sub>i</sub><sup>j</sup>.
         * <p>
         * Only for j = 1 and j = 2 the coefficients are not 0. <br>
         * i corresponds to the equinoctial element, as follows:
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] dij;

        /** The coefficients C<sub>i</sub><sup>j</sup>.
         * <p>
         * The index order is cij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] cij;

        /** The coefficients S<sub>i</sub><sup>j</sup>.
         * <p>
         * The index order is sij[j][i] <br/>
         * i corresponds to the equinoctial element, as follows: <br/>
         * - i=0 for a <br/>
         * - i=1 for k <br/>
         * - i=2 for h <br/>
         * - i=3 for q <br/>
         * - i=4 for p <br/>
         * - i=5 for λ <br/>
         * </p>
         */
        private final ShortPeriodicsInterpolatedCoefficient[] sij;

        /** Simple constructor.
         *  @param jMax maximum value for j index
         *  @param interpolationPoints number of points used in the interpolation process
         */
        Slot(final int jMax, final int interpolationPoints) {

            dij = new ShortPeriodicsInterpolatedCoefficient[3];
            cij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];
            sij = new ShortPeriodicsInterpolatedCoefficient[jMax + 1];

            // Initialize the C<sub>i</sub><sup>j</sup>, S<sub>i</sub><sup>j</sup> and D<sub>i</sub><sup>j</sup> coefficients
            for (int j = 0; j <= jMax; j++) {
                cij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                if (j > 0) {
                    sij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
                // Initialize only the non-zero D<sub>i</sub><sup>j</sup> coefficients
                if (j == 1 || j == 2) {
                    dij[j] = new ShortPeriodicsInterpolatedCoefficient(interpolationPoints);
                }
            }

        }

    }

}
