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
package org.orekit.orbits;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.analysis.differentiation.FDSFactory;
import org.hipparchus.analysis.differentiation.FieldDerivativeStructure;
import org.hipparchus.analysis.interpolation.FieldHermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.MathArrays;
import org.hipparchus.util.MathUtils;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;


/**
 * This class handles equinoctial orbital parameters, which can support both
 * circular and equatorial orbits.
 * <p>
 * The parameters used internally are the equinoctial elements which can be
 * related to Keplerian elements as follows:
 *   <pre>
 *     a
 *     ex = e cos(ω + Ω)
 *     ey = e sin(ω + Ω)
 *     hx = tan(i/2) cos(Ω)
 *     hy = tan(i/2) sin(Ω)
 *     lv = v + ω + Ω
 *   </pre>
 * where ω stands for the Perigee Argument and Ω stands for the
 * Right Ascension of the Ascending Node.
 * <p>
 * The conversion equations from and to Keplerian elements given above hold only
 * when both sides are unambiguously defined, i.e. when orbit is neither equatorial
 * nor circular. When orbit is either equatorial or circular, the equinoctial
 * parameters are still unambiguously defined whereas some Keplerian elements
 * (more precisely ω and Ω) become ambiguous. For this reason, equinoctial
 * parameters are the recommended way to represent orbits.
 * </p>
 * <p>
 * The instance <code>EquinoctialOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    CartesianOrbit
 * @author Mathieu Rom&eacute;ro
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @since 9.0
 */
public class FieldEquinoctialOrbit<T extends RealFieldElement<T>> extends FieldOrbit<T> {

    /** Factory for first time derivatives. */
    private static final Map<Field<? extends RealFieldElement<?>>, FDSFactory<? extends RealFieldElement<?>>> FACTORIES =
                    new HashMap<>();

    /** Semi-major axis (m). */
    private final T a;

    /** First component of the eccentricity vector. */
    private final T ex;

    /** Second component of the eccentricity vector. */
    private final T ey;

    /** First component of the inclination vector. */
    private final T hx;

    /** Second component of the inclination vector. */
    private final T hy;

    /** True longitude argument (rad). */
    private final T lv;

    /** Semi-major axis derivative (m/s). */
    private final T aDot;

    /** First component of the eccentricity vector derivative. */
    private final T exDot;

    /** Second component of the eccentricity vector derivative. */
    private final T eyDot;

    /** First component of the inclination vector derivative. */
    private final T hxDot;

    /** Second component of the inclination vector derivative. */
    private final T hyDot;

    /** True longitude argument derivative (rad/s). */
    private final T lvDot;

    /** Partial Cartesian coordinates (position and velocity are valid, acceleration may be missing). */
    private FieldPVCoordinates<T> partialPV;

    /** Field used by this class.*/
    private Field<T> field;

    /**Zero.*/
    private T zero;

    /**One.*/
    private T one;

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω + Ω), first component of eccentricity vector
     * @param ey e sin(ω + Ω), second component of eccentricity vector
     * @param hx tan(i/2) cos(Ω), first component of inclination vector
     * @param hy tan(i/2) sin(Ω), second component of inclination vector
     * @param l  (M or E or v) + ω + Ω, mean, eccentric or true longitude argument (rad)
     * @param type type of longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldEquinoctialOrbit(final T a, final T ex, final T ey,
                                 final T hx, final T hy, final T l,
                                 final PositionAngle type,
                                 final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(a, ex, ey, hx, hy, l,
             null, null, null, null, null, null,
             type, frame, date, mu);
    }

    /** Creates a new instance.
     * @param a  semi-major axis (m)
     * @param ex e cos(ω + Ω), first component of eccentricity vector
     * @param ey e sin(ω + Ω), second component of eccentricity vector
     * @param hx tan(i/2) cos(Ω), first component of inclination vector
     * @param hy tan(i/2) sin(Ω), second component of inclination vector
     * @param l  (M or E or v) + ω + Ω, mean, eccentric or true longitude argument (rad)
     * @param aDot  semi-major axis derivative (m/s)
     * @param exDot d(e cos(ω + Ω))/dt, first component of eccentricity vector derivative
     * @param eyDot d(e sin(ω + Ω))/dt, second component of eccentricity vector derivative
     * @param hxDot d(tan(i/2) cos(Ω))/dt, first component of inclination vector derivative
     * @param hyDot d(tan(i/2) sin(Ω))/dt, second component of inclination vector derivative
     * @param lDot  d(M or E or v) + ω + Ω)/dr, mean, eccentric or true longitude argument  derivative (rad/s)
     * @param type type of longitude argument
     * @param frame the frame in which the parameters are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldEquinoctialOrbit(final T a, final T ex, final T ey,
                                 final T hx, final T hy, final T l,
                                 final T aDot, final T exDot, final T eyDot,
                                 final T hxDot, final T hyDot, final T lDot,
                                 final PositionAngle type,
                                 final Frame frame, final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        super(frame, date, mu);
        field = a.getField();
        zero = field.getZero();
        one = field.getOne();
        if (ex.getReal() * ex.getReal() + ey.getReal() * ey.getReal() >= 1.0) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

        this.a     = a;
        this.aDot  = aDot;
        this.ex    = ex;
        this.exDot = exDot;
        this.ey    = ey;
        this.eyDot = eyDot;
        this.hx    = hx;
        this.hxDot = hxDot;
        this.hy    = hy;
        this.hyDot = hyDot;

        if (hasDerivatives()) {
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> exDS = factory.build(ex, exDot);
            final FieldDerivativeStructure<T> eyDS = factory.build(ey, eyDot);
            final FieldDerivativeStructure<T> lDS  = factory.build(l,  lDot);
            final FieldDerivativeStructure<T> lvDS;
            switch (type) {
                case MEAN :
                    lvDS = eccentricToTrue(meanToEccentric(lDS, exDS, eyDS), exDS, eyDS);
                    break;
                case ECCENTRIC :
                    lvDS = eccentricToTrue(lDS, exDS, eyDS);
                    break;
                case TRUE :
                    lvDS = lDS;
                    break;
                default : // this should never happen
                    throw new OrekitInternalError(null);
            }
            this.lv    = lvDS.getValue();
            this.lvDot = lvDS.getPartialDerivative(1);
        } else {
            switch (type) {
                case MEAN :
                    this.lv = eccentricToTrue(meanToEccentric(l, ex, ey), ex, ey);
                    break;
                case ECCENTRIC :
                    this.lv = eccentricToTrue(l, ex, ey);
                    break;
                case TRUE :
                    this.lv = l;
                    break;
                default :
                    throw new OrekitInternalError(null);
            }
            this.lvDot = null;
        }

        this.partialPV = null;

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the position, velocity and acceleration
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldEquinoctialOrbit(final TimeStampedFieldPVCoordinates<T> pvCoordinates,
                                 final Frame frame, final T mu)
        throws IllegalArgumentException {
        super(pvCoordinates, frame, mu);

        field = pvCoordinates.getPosition().getX().getField();
        zero = field.getZero();
        one = field.getOne();

        //  compute semi-major axis
        final FieldVector3D<T> pvP = pvCoordinates.getPosition();
        final FieldVector3D<T> pvV = pvCoordinates.getVelocity();
        final FieldVector3D<T> pvA = pvCoordinates.getAcceleration();
        final T r2 = pvP.getNormSq();
        final T r  = r2.sqrt();
        final T V2 = pvV.getNormSq();
        final T rV2OnMu = r.multiply(V2).divide(mu);

        if (rV2OnMu.getReal() > 2) {
            throw new OrekitIllegalArgumentException(OrekitMessages.HYPERBOLIC_ORBIT_NOT_HANDLED_AS,
                                                     getClass().getName());
        }

        // compute inclination vector
        final FieldVector3D<T> w = pvCoordinates.getMomentum().normalize();
        final T d = one.divide(one.add(w.getZ()));
        hx =  d.negate().multiply(w.getY());
        hy =  d.multiply(w.getX());

        // compute true longitude argument
        final T cLv = (pvP.getX().subtract(d.multiply(pvP.getZ()).multiply(w.getX()))).divide(r);
        final T sLv = (pvP.getY().subtract(d.multiply(pvP.getZ()).multiply(w.getY()))).divide(r);
        lv = sLv.atan2(cLv);


        // compute semi-major axis
        a = r.divide(rV2OnMu.negate().add(2));

        // compute eccentricity vector
        final T eSE = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(mu).sqrt());
        final T eCE = rV2OnMu.subtract(1);
        final T e2  = eCE.multiply(eCE).add(eSE.multiply(eSE));
        final T f   = eCE.subtract(e2);
        final T g   = e2.negate().add(1).sqrt().multiply(eSE);
        ex = a.multiply(f.multiply(cLv).add( g.multiply(sLv))).divide(r);
        ey = a.multiply(f.multiply(sLv).subtract(g.multiply(cLv))).divide(r);

        partialPV = pvCoordinates;

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

        if (hasNonKeplerianAcceleration(pvCoordinates, mu)) {
            // we have a relevant acceleration, we can compute derivatives

            final T[][] jacobian = MathArrays.buildArray(a.getField(), 6, 6);
            getJacobianWrtCartesian(PositionAngle.MEAN, jacobian);

            final FieldVector3D<T> keplerianAcceleration    = new FieldVector3D<>(r.multiply(r2).reciprocal().multiply(mu.negate()), pvP);
            final FieldVector3D<T> nonKeplerianAcceleration = pvA.subtract(keplerianAcceleration);
            final T   aX                       = nonKeplerianAcceleration.getX();
            final T   aY                       = nonKeplerianAcceleration.getY();
            final T   aZ                       = nonKeplerianAcceleration.getZ();
            aDot  = jacobian[0][3].multiply(aX).add(jacobian[0][4].multiply(aY)).add(jacobian[0][5].multiply(aZ));
            exDot = jacobian[1][3].multiply(aX).add(jacobian[1][4].multiply(aY)).add(jacobian[1][5].multiply(aZ));
            eyDot = jacobian[2][3].multiply(aX).add(jacobian[2][4].multiply(aY)).add(jacobian[2][5].multiply(aZ));
            hxDot = jacobian[3][3].multiply(aX).add(jacobian[3][4].multiply(aY)).add(jacobian[3][5].multiply(aZ));
            hyDot = jacobian[4][3].multiply(aX).add(jacobian[4][4].multiply(aY)).add(jacobian[4][5].multiply(aZ));

            // in order to compute true anomaly derivative, we must compute
            // mean anomaly derivative including Keplerian motion and convert to true anomaly
            final T lMDot = getKeplerianMeanMotion().
                            add(jacobian[5][3].multiply(aX)).add(jacobian[5][4].multiply(aY)).add(jacobian[5][5].multiply(aZ));
            @SuppressWarnings("unchecked")
            final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
            final FieldDerivativeStructure<T> exDS = factory.build(ex, exDot);
            final FieldDerivativeStructure<T> eyDS = factory.build(ey, eyDot);
            final FieldDerivativeStructure<T> lMDS = factory.build(getLM(), lMDot);
            final FieldDerivativeStructure<T> lvDS = eccentricToTrue(meanToEccentric(lMDS, exDS, eyDS), exDS, eyDS);
            lvDot = lvDS.getPartialDerivative(1);

        } else {
            // acceleration is either almost zero or NaN,
            // we assume acceleration was not known
            // we don't set up derivatives
            aDot  = null;
            exDot = null;
            eyDot = null;
            hxDot = null;
            hyDot = null;
            lvDot = null;
        }

    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvCoordinates the position end velocity
     * @param frame the frame in which are defined the {@link FieldPVCoordinates}
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if eccentricity is equal to 1 or larger or
     * if frame is not a {@link Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldEquinoctialOrbit(final FieldPVCoordinates<T> pvCoordinates, final Frame frame,
                            final FieldAbsoluteDate<T> date, final T mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<>(date, pvCoordinates), frame, mu);
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldEquinoctialOrbit(final FieldOrbit<T> op) {
        super(op.getFrame(), op.getDate(), op.getMu());

        a     = op.getA();
        ex    = op.getEquinoctialEx();
        ey    = op.getEquinoctialEy();
        hx    = op.getHx();
        hy    = op.getHy();
        lv    = op.getLv();

        if (!FACTORIES.containsKey(a.getField())) {
            FACTORIES.put(a.getField(), new FDSFactory<>(a.getField(), 1, 1));
        }

        aDot  = op.getADot();
        exDot = op.getEquinoctialExDot();
        eyDot = op.getEquinoctialEyDot();
        hxDot = op.getHxDot();
        hyDot = op.getHyDot();
        lvDot = op.getLvDot();

        field = a.getField();
        zero  = field.getZero();
        one   = field.getOne();
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.EQUINOCTIAL;
    }

    /** {@inheritDoc} */
    public T getA() {
        return a;
    }

    /** {@inheritDoc} */
    public T getADot() {
        return aDot;
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        return ex;
    }

    /** {@inheritDoc} */
    public T getEquinoctialExDot() {
        return exDot;
    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        return ey;
    }

    /** {@inheritDoc} */
    public T getEquinoctialEyDot() {
        return eyDot;
    }

    /** {@inheritDoc} */
    public T getHx() {
        return hx;
    }

    /** {@inheritDoc} */
    public T getHxDot() {
        return hxDot;
    }

    /** {@inheritDoc} */
    public T getHy() {
        return hy;
    }

    /** {@inheritDoc} */
    public T getHyDot() {
        return hyDot;
    }

    /** {@inheritDoc} */
    public T getLv() {
        return lv;
    }

    /** {@inheritDoc} */
    public T getLvDot() {
        return lvDot;
    }

    /** {@inheritDoc} */
    public T getLE() {
        return trueToEccentric(lv, ex, ey);
    }

    /** {@inheritDoc} */
    public T getLEDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> lVDS = factory.build(lv, lvDot);
        final FieldDerivativeStructure<T> exDS = factory.build(ex, exDot);
        final FieldDerivativeStructure<T> eyDS = factory.build(ey, eyDot);
        final FieldDerivativeStructure<T> lEDS = trueToEccentric(lVDS, exDS, eyDS);
        return lEDS.getPartialDerivative(1);

    }

    /** {@inheritDoc} */
    public T getLM() {
        return eccentricToMean(trueToEccentric(lv, ex, ey), ex, ey);
    }

    /** {@inheritDoc} */
    public T getLMDot() {

        if (!hasDerivatives()) {
            return null;
        }

        @SuppressWarnings("unchecked")
        final FDSFactory<T> factory = (FDSFactory<T>) FACTORIES.get(a.getField());
        final FieldDerivativeStructure<T> lVDS = factory.build(lv, lvDot);
        final FieldDerivativeStructure<T> exDS = factory.build(ex, exDot);
        final FieldDerivativeStructure<T> eyDS = factory.build(ey, eyDot);
        final FieldDerivativeStructure<T> lMDS = eccentricToMean(trueToEccentric(lVDS, exDS, eyDS), exDS, eyDS);
        return lMDS.getPartialDerivative(1);

    }

    /** Get the longitude argument.
     * @param type type of the angle
     * @return longitude argument (rad)
     */
    public T getL(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getLM() :
                                              ((type == PositionAngle.ECCENTRIC) ? getLE() :
                                                                                   getLv());
    }

    /** Get the longitude argument derivative.
     * @param type type of the angle
     * @return longitude argument derivative (rad/s)
     */
    public T getLDot(final PositionAngle type) {
        return (type == PositionAngle.MEAN) ? getLMDot() :
                                              ((type == PositionAngle.ECCENTRIC) ? getLEDot() :
                                                                                   getLvDot());
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasDerivatives() {
        return aDot != null;
    }

    /** Computes the true longitude argument from the eccentric longitude argument.
     * @param lE = E + ω + Ω eccentric longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @param <T> Type of the field elements
     * @return the true longitude argument
     */
    public static <T extends RealFieldElement<T>> T eccentricToTrue(final T lE, final T ex, final T ey) {
        final T epsilon = ex.multiply(ex).add(ey.multiply(ey)).negate().add(1).sqrt();
        final T cosLE   = lE.cos();
        final T sinLE   = lE.sin();
        final T num     = ex.multiply(sinLE).subtract(ey.multiply(cosLE));
        final T den     = epsilon.add(1).subtract(ex.multiply(cosLE)).subtract(ey.multiply(sinLE));
        return lE.add(num.divide(den).atan().multiply(2));
    }

    /** Computes the eccentric longitude argument from the true longitude argument.
     * @param lv = v + ω + Ω true longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @param <T> Type of the field elements
     * @return the eccentric longitude argument
     */
    public static <T extends RealFieldElement<T>> T trueToEccentric(final T lv, final T ex, final T ey) {
        final T epsilon = ex.multiply(ex).add(ey.multiply(ey)).negate().add(1).sqrt();
        final T cosLv   = lv.cos();
        final T sinLv   = lv.sin();
        final T num     = ey.multiply(cosLv).subtract(ex.multiply(sinLv));
        final T den     = epsilon.add(1).add(ex.multiply(cosLv)).add(ey.multiply(sinLv));
        return lv.add(num.divide(den).atan().multiply(2));
    }

    /** Computes the eccentric longitude argument from the mean longitude argument.
     * @param lM = M + ω + Ω mean longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @param <T> Type of the field elements
     * @return the eccentric longitude argument
     */
    public static <T extends RealFieldElement<T>> T meanToEccentric(final T lM, final T ex, final T ey) {
        // Generalization of Kepler equation to equinoctial parameters
        // with lE = PA + RAAN + E and
        //      lM = PA + RAAN + M = lE - ex.sin(lE) + ey.cos(lE)
        T lE = lM;
        T shift = lM.getField().getZero();
        T lEmlM = lM.getField().getZero();
        T cosLE = lE.cos();
        T sinLE = lE.sin();
        int iter = 0;
        do {
            final T f2 = ex.multiply(sinLE).subtract(ey.multiply(cosLE));
            final T f1 = ex.multiply(cosLE).add(ey.multiply(sinLE)).negate().add(1);
            final T f0 = lEmlM.subtract(f2);

            final T f12 = f1.multiply(2.0);
            shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            lEmlM = lEmlM.subtract(shift);
            lE     = lM.add(lEmlM);
            cosLE  = lE.cos();
            sinLE  = lE.sin();

        } while ((++iter < 50) && (FastMath.abs(shift.getReal()) > 1.0e-12));

        return lE;

    }

    /** Computes the mean longitude argument from the eccentric longitude argument.
     * @param lE = E + ω + Ω mean longitude argument (rad)
     * @param ex first component of the eccentricity vector
     * @param ey second component of the eccentricity vector
     * @param <T> Type of the field elements
     * @return the mean longitude argument
     */
    public static <T extends RealFieldElement<T>> T eccentricToMean(final T lE, final T ex, final T ey) {
        return lE.subtract(ex.multiply(lE.sin())).add(ey.multiply(lE.cos()));
    }

    /** {@inheritDoc} */
    public T getE() {
        return ex.multiply(ex).add(ey.multiply(ey)).sqrt();
    }

    /** {@inheritDoc} */
    public T getEDot() {

        if (!hasDerivatives()) {
            return null;
        }

        return ex.multiply(exDot).add(ey.multiply(eyDot)).divide(ex.multiply(ex).add(ey.multiply(ey)).sqrt());

    }

    /** {@inheritDoc} */
    public T getI() {
        return hx.multiply(hx).add(hy.multiply(hy)).sqrt().atan().multiply(2);
    }

    /** {@inheritDoc} */
    public T getIDot() {

        if (!hasDerivatives()) {
            return null;
        }

        final T h2 = hx.multiply(hx).add(hy.multiply(hy));
        final T h  = h2.sqrt();
        return hx.multiply(hxDot).add(hy.multiply(hyDot)).multiply(2).divide(h.multiply(h2.add(1)));

    }

    /** Compute position and velocity but not acceleration.
     */
    private void computePVWithoutA() {

        if (partialPV != null) {
            // already computed
            return;
        }

        // get equinoctial parameters
        final T lE = getLE();

        // inclination-related intermediate parameters
        final T hx2   = hx.multiply(hx);
        final T hy2   = hy.multiply(hy);
        final T factH = one.divide(hx2.add(1.0).add(hy2));

        // reference axes defining the orbital plane
        final T ux = hx2.add(1.0).subtract(hy2).multiply(factH);
        final T uy = hx.multiply(hy).multiply(factH).multiply(2);
        final T uz = hy.multiply(-2).multiply(factH);

        final T vx = uy;
        final T vy = (hy2.subtract(hx2).add(1)).multiply(factH);
        final T vz =  hx.multiply(factH).multiply(2);

        // eccentricity-related intermediate parameters
        final T ex2  = ex.multiply(ex);
        final T exey = ex.multiply(ey);
        final T ey2  = ey.multiply(ey);
        final T e2   = ex2.add(ey2);
        final T eta  = one.subtract(e2).sqrt().add(1);
        final T beta = one.divide(eta);

        // eccentric longitude argument
        final T cLe    = lE.cos();
        final T sLe    = lE.sin();
        final T exCeyS = ex.multiply(cLe).add(ey.multiply(sLe));

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(one.subtract(beta.multiply(ey2)).multiply(cLe).add(beta.multiply(exey).multiply(sLe)).subtract(ex));
        final T y      = a.multiply(one.subtract(beta.multiply(ex2)).multiply(sLe).add(beta .multiply(exey).multiply(cLe)).subtract(ey));

        final T factor = getMu().divide(a).sqrt().divide(one.subtract(exCeyS));
        final T xdot   = factor.multiply(sLe.negate().add(beta.multiply(ey).multiply(exCeyS)));
        final T ydot   = factor.multiply(cLe.subtract(beta.multiply(ex).multiply(exCeyS)));

        final FieldVector3D<T> position =
                        new FieldVector3D<>(x.multiply(ux).add(y.multiply(vx)),
                                            x.multiply(uy).add(y.multiply(vy)),
                                            x.multiply(uz).add(y.multiply(vz)));
        final FieldVector3D<T> velocity =
                        new FieldVector3D<>(xdot.multiply(ux).add(ydot.multiply(vx)), xdot.multiply(uy).add(ydot.multiply(vy)), xdot.multiply(uz).add(ydot.multiply(vz)));

        partialPV = new FieldPVCoordinates<>(position, velocity);

    }

    /** Compute non-Keplerian part of the acceleration from first time derivatives.
     * <p>
     * This method should be called only when {@link #hasDerivatives()} returns true.
     * </p>
     * @return non-Keplerian part of the acceleration
     */
    private FieldVector3D<T> nonKeplerianAcceleration() {

        final T[][] dCdP = MathArrays.buildArray(a.getField(), 6, 6);
        getJacobianWrtParameters(PositionAngle.MEAN, dCdP);

        final T nonKeplerianMeanMotion = getLMDot().subtract(getKeplerianMeanMotion());
        final T nonKeplerianAx =     dCdP[3][0].multiply(aDot).
                                 add(dCdP[3][1].multiply(exDot)).
                                 add(dCdP[3][2].multiply(eyDot)).
                                 add(dCdP[3][3].multiply(hxDot)).
                                 add(dCdP[3][4].multiply(hyDot)).
                                 add(dCdP[3][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAy =     dCdP[4][0].multiply(aDot).
                                 add(dCdP[4][1].multiply(exDot)).
                                 add(dCdP[4][2].multiply(eyDot)).
                                 add(dCdP[4][3].multiply(hxDot)).
                                 add(dCdP[4][4].multiply(hyDot)).
                                 add(dCdP[4][5].multiply(nonKeplerianMeanMotion));
        final T nonKeplerianAz =     dCdP[5][0].multiply(aDot).
                                 add(dCdP[5][1].multiply(exDot)).
                                 add(dCdP[5][2].multiply(eyDot)).
                                 add(dCdP[5][3].multiply(hxDot)).
                                 add(dCdP[5][4].multiply(hyDot)).
                                 add(dCdP[5][5].multiply(nonKeplerianMeanMotion));

        return new FieldVector3D<>(nonKeplerianAx, nonKeplerianAy, nonKeplerianAz);

    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initPVCoordinates() {

        // position and velocity
        computePVWithoutA();

        // acceleration
        final T r2 = partialPV.getPosition().getNormSq();
        final FieldVector3D<T> keplerianAcceleration = new FieldVector3D<>(r2.multiply(r2.sqrt()).reciprocal().multiply(getMu().negate()),
                                                                           partialPV.getPosition());
        final FieldVector3D<T> acceleration = hasDerivatives() ?
                                              keplerianAcceleration.add(nonKeplerianAcceleration()) :
                                              keplerianAcceleration;

        return new TimeStampedFieldPVCoordinates<>(getDate(), partialPV.getPosition(), partialPV.getVelocity(), acceleration);

    }

    /** {@inheritDoc} */
    public FieldEquinoctialOrbit<T> shiftedBy(final double dt) {
        return shiftedBy(getDate().getField().getZero().add(dt));
    }

    /** {@inheritDoc} */
    public FieldEquinoctialOrbit<T> shiftedBy(final T dt) {

        // use Keplerian-only motion
        final FieldEquinoctialOrbit<T> keplerianShifted = new FieldEquinoctialOrbit<>(a, ex, ey, hx, hy,
                                                                                      getLM().add(getKeplerianMeanMotion().multiply(dt)),
                                                                                      PositionAngle.MEAN, getFrame(),
                                                                                      getDate().shiftedBy(dt), getMu());

        if (hasDerivatives()) {

            // extract non-Keplerian acceleration from first time derivatives
            final FieldVector3D<T> nonKeplerianAcceleration = nonKeplerianAcceleration();

            // add quadratic effect of non-Keplerian acceleration to Keplerian-only shift
            keplerianShifted.computePVWithoutA();
            final FieldVector3D<T> fixedP   = new FieldVector3D<>(one, keplerianShifted.partialPV.getPosition(),
                                                                  dt.multiply(dt).multiply(0.5), nonKeplerianAcceleration);
            final T   fixedR2 = fixedP.getNormSq();
            final T   fixedR  = fixedR2.sqrt();
            final FieldVector3D<T> fixedV  = new FieldVector3D<>(one, keplerianShifted.partialPV.getVelocity(),
                                                                 dt, nonKeplerianAcceleration);
            final FieldVector3D<T> fixedA  = new FieldVector3D<>(fixedR2.multiply(fixedR).reciprocal().multiply(getMu().negate()),
                                                                 keplerianShifted.partialPV.getPosition(),
                                                                 one, nonKeplerianAcceleration);

            // build a new orbit, taking non-Keplerian acceleration into account
            return new FieldEquinoctialOrbit<>(new TimeStampedFieldPVCoordinates<>(keplerianShifted.getDate(),
                                                                                   fixedP, fixedV, fixedA),
                                               keplerianShifted.getFrame(), keplerianShifted.getMu());

        } else {
            // Keplerian-only motion is all we can do
            return keplerianShifted;
        }

    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on equinoctial elements, without derivatives (which means the interpolation
     * falls back to Lagrange interpolation only).
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * <p>
     * If orbit interpolation on large samples is needed, using the {@link
     * org.orekit.propagation.analytical.Ephemeris} class is a better way than using this
     * low-level interpolation. The Ephemeris class automatically handles selection of
     * a neighboring sub-sample with a predefined number of point from a large global sample
     * in a thread-safe way.
     * </p>
     */
    public FieldEquinoctialOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Stream<FieldOrbit<T>> sample) {

        // first pass to check if derivatives are available throughout the sample
        final List<FieldOrbit<T>> list = sample.collect(Collectors.toList());
        boolean useDerivatives = true;
        for (final FieldOrbit<T> orbit : list) {
            useDerivatives = useDerivatives && orbit.hasDerivatives();
        }

        // set up an interpolator
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<>();

        // second pass to feed interpolator
        FieldAbsoluteDate<T> previousDate = null;
        T                    previousLm   = zero.add(Double.NaN);
        for (final FieldOrbit<T> orbit : list) {
            final FieldEquinoctialOrbit<T> equi = (FieldEquinoctialOrbit<T>) OrbitType.EQUINOCTIAL.convertType(orbit);
            final T continuousLm;
            if (previousDate == null) {
                continuousLm = (T) equi.getLM();
            } else {
                final T dt       = (T) equi.getDate().durationFrom(previousDate);
                final T keplerLm = previousLm.add((T) equi.getKeplerianMeanMotion().multiply(dt));
                continuousLm = MathUtils.normalizeAngle((T) equi.getLM(), keplerLm);
            }
            previousDate = equi.getDate();
            previousLm   = continuousLm;
            final T[] toAdd = MathArrays.buildArray(field, 6);
            toAdd[0] = (T) equi.getA();
            toAdd[1] = (T) equi.getEquinoctialEx();
            toAdd[2] = (T) equi.getEquinoctialEy();
            toAdd[3] = (T) equi.getHx();
            toAdd[4] = (T) equi.getHy();
            toAdd[5] = (T) continuousLm;
            if (useDerivatives) {
                final T[] toAddDot = MathArrays.buildArray(one.getField(), 6);
                toAddDot[0] = equi.getADot();
                toAddDot[1] = equi.getEquinoctialExDot();
                toAddDot[2] = equi.getEquinoctialEyDot();
                toAddDot[3] = equi.getHxDot();
                toAddDot[4] = equi.getHyDot();
                toAddDot[5] = equi.getLMDot();
                interpolator.addSamplePoint(equi.getDate().durationFrom(date),
                                            toAdd, toAddDot);
            } else {
                interpolator.addSamplePoint((T) equi.getDate().durationFrom(date),
                                            toAdd);
            }
        }

        // interpolate
        final T[][] interpolated = interpolator.derivatives(zero, 1);

        // build a new interpolated instance
        return new FieldEquinoctialOrbit<>(interpolated[0][0], interpolated[0][1], interpolated[0][2],
                                           interpolated[0][3], interpolated[0][4], interpolated[0][5],
                                           interpolated[1][0], interpolated[1][1], interpolated[1][2],
                                           interpolated[1][3], interpolated[1][4], interpolated[1][5],
                                           PositionAngle.MEAN, getFrame(), date, getMu());

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianMeanWrtCartesian() {

        final T[][] jacobian = MathArrays.buildArray(field, 6, 6);

        // compute various intermediate parameters
        computePVWithoutA();
        final FieldVector3D<T> position = partialPV.getPosition();
        final FieldVector3D<T> velocity = partialPV.getVelocity();
        final T r2         = position.getNormSq();
        final T r          = r2.sqrt();
        final T r3         = r.multiply(r2);

        final T mu         = getMu();
        final T sqrtMuA    = a.multiply(mu).sqrt();
        final T a2         = a.multiply(a);

        final T e2         = ex.multiply(ex).add(ey.multiply(ey));
        final T oMe2       = one.subtract(e2);
        final T epsilon    = oMe2.sqrt();
        final T beta       = one.divide(epsilon.add(1));
        final T ratio      = epsilon.multiply(beta);

        final T hx2        = hx.multiply(hx);
        final T hy2        = hy.multiply(hy);
        final T hxhy       = hx.multiply(hy);

        // precomputing equinoctial frame unit vectors (f, g, w)
        final FieldVector3D<T> f  = new FieldVector3D<>(hx2.subtract(hy2).add(1), hxhy.multiply(2), hy.multiply(-2)).normalize();
        final FieldVector3D<T> g  = new FieldVector3D<>(hxhy.multiply(2), hy2.add(1).subtract(hx2), hx.multiply(2)).normalize();
        final FieldVector3D<T> w  = FieldVector3D.crossProduct(position, velocity).normalize();

        // coordinates of the spacecraft in the equinoctial frame
        final T x    = FieldVector3D.dotProduct(position, f);
        final T y    = FieldVector3D.dotProduct(position, g);
        final T xDot = FieldVector3D.dotProduct(velocity, f);
        final T yDot = FieldVector3D.dotProduct(velocity, g);

        // drDot / dEx = dXDot / dEx * f + dYDot / dEx * g
        final T c1  = a.divide(sqrtMuA.multiply(epsilon));
        final T c1N = c1.negate();
        final T c2  = a.multiply(sqrtMuA).multiply(beta).divide(r3);
        final T c3  = sqrtMuA.divide(r3.multiply(epsilon));
        final FieldVector3D<T> drDotSdEx = new FieldVector3D<>(c1.multiply(xDot).multiply(yDot).subtract(c2.multiply(ey).multiply(x)).subtract(c3.multiply(x).multiply(y)), f,
                                                               c1N.multiply(xDot).multiply(xDot).subtract(c2.multiply(ey).multiply(y)).add(c3.multiply(x).multiply(x)), g);

        // drDot / dEy = dXDot / dEy * f + dYDot / dEy * g
        final FieldVector3D<T> drDotSdEy = new FieldVector3D<>(c1.multiply(yDot).multiply(yDot).add(c2.multiply(ex).multiply(x)).subtract(c3.multiply(y).multiply(y)), f,
                                                               c1N.multiply(xDot).multiply(yDot).add(c2.multiply(ex).multiply(y)).add(c3.multiply(x).multiply(y)), g);

        // da
        final FieldVector3D<T> vectorAR = new FieldVector3D<>(a2.multiply(2).divide(r3), position);
        final FieldVector3D<T> vectorARDot = new FieldVector3D<>(a2.multiply(2).divide(mu), velocity);
        fillHalfRow(one, vectorAR,    jacobian[0], 0);
        fillHalfRow(one, vectorARDot, jacobian[0], 3);

        // dEx
        final T d1 = a.negate().multiply(ratio).divide(r3);
        final T d2 = (hy.multiply(xDot).subtract(hx.multiply(yDot))).divide(sqrtMuA.multiply(epsilon));
        final T d3 = hx.multiply(y).subtract(hy.multiply(x)).divide(sqrtMuA);
        final FieldVector3D<T> vectorExRDot =
            new FieldVector3D<>(x.multiply(2).multiply(yDot).subtract(xDot.multiply(y)).divide(mu), g, y.negate().multiply(yDot).divide(mu), f, ey.negate().multiply(d3).divide(epsilon), w);
        fillHalfRow(ex.multiply(d1), position, ey.negate().multiply(d2), w, epsilon.divide(sqrtMuA), drDotSdEy, jacobian[1], 0);
        fillHalfRow(one, vectorExRDot, jacobian[1], 3);

        // dEy
        final FieldVector3D<T> vectorEyRDot =
            new FieldVector3D<>(xDot.multiply(2).multiply(y).subtract(x.multiply(yDot)).divide(mu), f, x.negate().multiply(xDot).divide(mu), g, ex.multiply(d3).divide(epsilon), w);
        fillHalfRow(ey.multiply(d1), position, ex.multiply(d2), w, epsilon.negate().divide(sqrtMuA), drDotSdEx, jacobian[2], 0);
        fillHalfRow(one, vectorEyRDot, jacobian[2], 3);

        // dHx
        final T h = (hx2.add(1).add(hy2)).divide(sqrtMuA.multiply(2).multiply(epsilon));
        fillHalfRow( h.negate().multiply(xDot), w, jacobian[3], 0);
        fillHalfRow( h.multiply(x),    w, jacobian[3], 3);

       // dHy
        fillHalfRow( h.negate().multiply(yDot), w, jacobian[4], 0);
        fillHalfRow( h.multiply(y),    w, jacobian[4], 3);

        // dLambdaM
        final T l = ratio.negate().divide(sqrtMuA);
        fillHalfRow(one.negate().divide(sqrtMuA), velocity, d2, w, l.multiply(ex), drDotSdEx, l.multiply(ey), drDotSdEy, jacobian[5], 0);
        fillHalfRow(zero.add(-2).divide(sqrtMuA), position, ex.multiply(beta), vectorEyRDot, ey.negate().multiply(beta), vectorExRDot, d3, w, jacobian[5], 3);

        return jacobian;

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianEccentricWrtCartesian() {

        // start by computing the Jacobian with mean angle
        final T[][] jacobian = computeJacobianMeanWrtCartesian();

        // Differentiating the Kepler equation lM = lE - ex sin lE + ey cos lE leads to:
        // dlM = (1 - ex cos lE - ey sin lE) dE - sin lE dex + cos lE dey
        // which is inverted and rewritten as:
        // dlE = a/r dlM + sin lE a/r dex - cos lE a/r dey
        final T le    = getLE();
        final T cosLe = le.cos();
        final T sinLe = le.sin();
        final T aOr   = one.divide(one.subtract(ex.multiply(cosLe)).subtract(ey.multiply(sinLe)));

        // update longitude row
        final T[] rowEx = jacobian[1];
        final T[] rowEy = jacobian[2];
        final T[] rowL  = jacobian[5];

        for (int j = 0; j < 6; ++j) {
            rowL[j] = aOr.multiply(rowL[j].add(sinLe.multiply(rowEx[j])).subtract(cosLe.multiply(rowEy[j])));
        }
        return jacobian;

    }

    /** {@inheritDoc} */
    protected T[][] computeJacobianTrueWrtCartesian() {

        // start by computing the Jacobian with eccentric angle
        final T[][] jacobian = computeJacobianEccentricWrtCartesian();

        // Differentiating the eccentric longitude equation
        // tan((lV - lE)/2) = [ex sin lE - ey cos lE] / [sqrt(1-ex^2-ey^2) + 1 - ex cos lE - ey sin lE]
        // leads to
        // cT (dlV - dlE) = cE dlE + cX dex + cY dey
        // with
        // cT = [d^2 + (ex sin lE - ey cos lE)^2] / 2
        // d  = 1 + sqrt(1-ex^2-ey^2) - ex cos lE - ey sin lE
        // cE = (ex cos lE + ey sin lE) (sqrt(1-ex^2-ey^2) + 1) - ex^2 - ey^2
        // cX =  sin lE (sqrt(1-ex^2-ey^2) + 1) - ey + ex (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // cY = -cos lE (sqrt(1-ex^2-ey^2) + 1) + ex + ey (ex sin lE - ey cos lE) / sqrt(1-ex^2-ey^2)
        // which can be solved to find the differential of the true longitude
        // dlV = (cT + cE) / cT dlE + cX / cT deX + cY / cT deX
        final T le        = getLE();
        final T cosLe     = le.cos();
        final T sinLe     = le.sin();
        final T eSinE     = ex.multiply(sinLe).subtract(ey.multiply(cosLe));
        final T ecosE     = ex.multiply(cosLe).add(ey.multiply(sinLe));
        final T e2        = ex.multiply(ex).add(ey.multiply(ey));
        final T epsilon   = one.subtract(e2).sqrt();
        final T onePeps   = epsilon.add(1);
        final T d         = onePeps.subtract(ecosE);
        final T cT        = d.multiply(d).add(eSinE.multiply(eSinE)).divide(2);
        final T cE        = ecosE.multiply(onePeps).subtract(e2);
        final T cX        = ex.multiply(eSinE).divide(epsilon).subtract(ey).add(sinLe.multiply(onePeps));
        final T cY        = ey.multiply(eSinE).divide(epsilon).add( ex).subtract(cosLe.multiply(onePeps));
        final T factorLe  = cT.add(cE).divide(cT);
        final T factorEx  = cX.divide(cT);
        final T factorEy  = cY.divide(cT);

        // update longitude row
        final T[] rowEx = jacobian[1];
        final T[] rowEy = jacobian[2];
        final T[] rowL = jacobian[5];
        for (int j = 0; j < 6; ++j) {
            rowL[j] = factorLe.multiply(rowL[j]).add(factorEx.multiply(rowEx[j])).add(factorEy.multiply(rowEy[j]));
        }

        return jacobian;

    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final T gm,
                                      final T[] pDot) {
        final T oMe2;
        final T ksi;
        final T n = gm.divide(a).sqrt().divide(a);
        switch (type) {
            case MEAN :
                pDot[5] = pDot[5].add(n);
                break;
            case ECCENTRIC :
                oMe2 = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey));
                ksi  = ex.multiply(lv.cos()).add(1).add(ey.multiply(lv.sin()));
                pDot[5] = pDot[5].add(n.multiply(ksi).divide(oMe2));
                break;
            case TRUE :
                oMe2 = one.subtract(ex.multiply(ex)).subtract(ey.multiply(ey));
                ksi  =  ex.multiply(lv.cos()).add(1).add(ey.multiply(lv.sin()));
                pDot[5] = pDot[5].add(n.multiply(ksi).multiply(ksi).divide(oMe2.multiply(oMe2.sqrt())));
                break;
            default :
                throw new OrekitInternalError(null);
        }
    }

    /**  Returns a string representation of this equinoctial parameters object.
     * @return a string representation of this object
     */
    public String toString() {
        return new StringBuffer().append("equinoctial parameters: ").append('{').
                                  append("a: ").append(a.getReal()).
                                  append("; ex: ").append(ex.getReal()).append("; ey: ").append(ey.getReal()).
                                  append("; hx: ").append(hx.getReal()).append("; hy: ").append(hy.getReal()).
                                  append("; lv: ").append(FastMath.toDegrees(lv.getReal())).
                                  append(";}").toString();
    }

    /**
     * Normalize an angle in a 2&pi; wide interval around a center value.
     * <p>This method has three main uses:</p>
     * <ul>
     *   <li>normalize an angle between 0 and 2&pi;:<br>
     *       {@code a = MathUtils.normalizeAngle(a, FastMath.PI);}</li>
     *   <li>normalize an angle between -&pi; and +&pi;<br>
     *       {@code a = MathUtils.normalizeAngle(a, 0.0);}</li>
     *   <li>compute the angle between two defining angular positions:<br>
     *       {@code angle = MathUtils.normalizeAngle(end, start) - start;}</li>
     * </ul>
     * <p>Note that due to numerical accuracy and since &pi; cannot be represented
     * exactly, the result interval is <em>closed</em>, it cannot be half-closed
     * as would be more satisfactory in a purely mathematical view.</p>
     * @param a angle to normalize
     * @param center center of the desired 2&pi; interval for the result
     * @param <T> the type of the field elements
     * @return a-2k&pi; with integer k and center-&pi; &lt;= a-2k&pi; &lt;= center+&pi;
     * @deprecated replaced by {@link MathUtils#normalizeAngle(RealFieldElement, RealFieldElement)}
     */
    @Deprecated
    public static <T extends RealFieldElement<T>> T normalizeAngle(final T a, final T center) {
        return a.subtract(2 * FastMath.PI * FastMath.floor((a.getReal() + FastMath.PI - center.getReal()) / (2 * FastMath.PI)));
    }

    @Override
    public EquinoctialOrbit toOrbit() {
        if (hasDerivatives()) {
            return new EquinoctialOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                        hx.getReal(), hy.getReal(), lv.getReal(),
                                        aDot.getReal(), exDot.getReal(), eyDot.getReal(),
                                        hxDot.getReal(), hyDot.getReal(), lvDot.getReal(),
                                        PositionAngle.TRUE, getFrame(),
                                        getDate().toAbsoluteDate(), getMu().getReal());
        } else {
            return new EquinoctialOrbit(a.getReal(), ex.getReal(), ey.getReal(),
                                        hx.getReal(), hy.getReal(), lv.getReal(),
                                        PositionAngle.TRUE, getFrame(),
                                        getDate().toAbsoluteDate(), getMu().getReal());
        }
    }

}
