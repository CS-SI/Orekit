/* Copyright 2002-2014 CS Systèmes d'Information
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
import org.apache.commons.math3.analysis.function.Atan2;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.forces.ForceModel;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.TimeDerivativesEquations;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.utils.PVCoordinates;

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

    /** Available orders for Gauss quadrature. */
    private static final int[] GAUSS_ORDER = {12, 16, 20, 24, 32, 40, 48};

    /** Max rank in Gauss quadrature orders array. */
    private static final int MAX_ORDER_RANK = GAUSS_ORDER.length - 1;

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

    protected double lm;

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

    /** Contribution to be numerically averaged. */
    private ForceModel contribution;

    /** Gauss integrator. */
    private final double threshold;

    /** Gauss integrator. */
    private GaussQuadrature integrator;

    /** Flag for Gauss order computation. */
    private boolean isDirty;

    /** Build a new instance.
     *
     *  @param threshold tolerance for the choice of the Gauss quadrature order
     */
    protected AbstractGaussianContribution(final double threshold,
            final ForceModel contribution) {
        this.contribution = contribution;
        this.threshold  = threshold;
        this.integrator = new GaussQuadrature(GAUSS_ORDER[MAX_ORDER_RANK]);
        this.isDirty    = true;
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
        AccelerationRetriever retriever = new AccelerationRetriever(state);
        contribution.addContribution(state, retriever);

        return retriever.getAcceleration();
    }

    /** Compute the limits in L, the true longitude, for integration.
     *
     *  @param  state current state information: date, kinematics, attitude
     *  @return the integration limits in L
     *  @exception OrekitException if some specific error occurs
     */
    protected abstract double[] getLLimits(final SpacecraftState state) throws OrekitException;

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
        final double[] meanElementRate = gauss.integrate(new IntegrableFunction(state), low, high);
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

    /** Internal class for retrieving acceleration from a force model. */
    private class AccelerationRetriever implements TimeDerivativesEquations {

        private Vector3D acceleration;
        private SpacecraftState state;

        public AccelerationRetriever(SpacecraftState state) {
            this.acceleration = Vector3D.ZERO;
            this.state = state;
        }

        @Override
        public void addKeplerContribution(double mu) {
        }

        @Override
        public void addXYZAcceleration(double x, double y, double z) {
            //TODO How to be sure we are in the good frame ???
            acceleration = new Vector3D(x, y, z);
        }

        @Override
        public void addAcceleration(Vector3D gamma, Frame frame)
                throws OrekitException {
            acceleration = frame.getTransformTo(state.getFrame(),
                    state.getDate()).transformVector(gamma);
        }

        @Override
        public void addMassDerivative(double q) {
        }

        public Vector3D getAcceleration() {
            return acceleration;
        }

    }

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

            final double shiftedLm = TrueToMean(x);
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
            final Vector3D pos = new Vector3D(X, f, Y, g);
            final Vector3D vel = new Vector3D(Xdot, f, Ydot, g);

            // Compute acceleration
            Vector3D acc = Vector3D.ZERO;
            try {
                acc = getAcceleration(state.shiftedBy(dt));
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


        private double TrueToEccentric (double lv) {
            final double cosLv   = FastMath.cos(lv);
            final double sinLv   = FastMath.sin(lv);
            final double num     = h * cosLv - k * sinLv;
            final double den     = B + 1 + k * cosLv + h * sinLv;
            return lv + 2 * FastMath.atan(num / den);
        }

        private double EccentricToMean (double le) {
            return le - k * FastMath.sin(le) + h * FastMath.cos(le);
        }

        private double TrueToMean (double lv) {
            return EccentricToMean(TrueToEccentric(lv));
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

    /** Class used to {@link #integrate(UnivariateVectorFunction, double, double) integrate}
     *  a {@link org.apache.commons.math3.analysis.UnivariateVectorFunction function}
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
        public GaussQuadrature(final int numberOfPoints) {

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
}
