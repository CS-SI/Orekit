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
package org.orekit.orbits;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.exception.MathIllegalStateException;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.FieldPVCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;


/** This class holds cartesian orbital parameters.

 * <p>
 * The parameters used internally are the cartesian coordinates:
 *   <ul>
 *     <li>x</li>
 *     <li>y</li>
 *     <li>z</li>
 *     <li>xDot</li>
 *     <li>yDot</li>
 *     <li>zDot</li>
 *   </ul>
 * contained in {@link PVCoordinates}.
 * </p>

 * <p>
 * Note that the implementation of this class delegates all non-cartesian related
 * computations ({@link #getA()}, {@link #getEquinoctialEx()}, ...) to an underlying
 * instance of the {@link EquinoctialOrbit} class. This implies that using this class
 * only for analytical computations which are always based on non-cartesian
 * parameters is perfectly possible but somewhat sub-optimal.
 * </p>
 * <p>
 * The instance <code>CartesianOrbit</code> is guaranteed to be immutable.
 * </p>
 * @see    Orbit
 * @see    KeplerianOrbit
 * @see    CircularOrbit
 * @see    EquinoctialOrbit
 * @author Luc Maisonobe
 * @author Guylaine Prat
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 */
public class FieldCartesianOrbit<T extends RealFieldElement<T>> extends FieldOrbit<T> {

    /** Underlying equinoctial orbit to which high-level methods are delegated. */
    private transient FieldEquinoctialOrbit<T> equinoctial;

    /** Field used by this class.*/
    private Field<T> field;

    /** Zero. (could be usefull)*/
    private T zero;

    /** One. (could be useful)*/
    private T one;

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position, velocity and acceleration of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCartesianOrbit(final TimeStampedFieldPVCoordinates<T> pvaCoordinates,
                          final Frame frame, final double mu)
        throws IllegalArgumentException {
        super(pvaCoordinates, frame, mu);
        equinoctial = null;
        field = pvaCoordinates.getPosition().getX().getField();
        zero = field.getZero();
        one = field.getOne();
    }

    /** Constructor from Cartesian parameters.
     *
     * <p> The acceleration provided in {@code pvCoordinates} is accessible using
     * {@link #getPVCoordinates()} and {@link #getPVCoordinates(Frame)}. All other methods
     * use {@code mu} and the position to compute the acceleration, including
     * {@link #shiftedBy(RealFieldElement)} and {@link #getPVCoordinates(FieldAbsoluteDate, Frame)}.
     *
     * @param pvaCoordinates the position and velocity of the satellite.
     * @param frame the frame in which the {@link PVCoordinates} are defined
     * (<em>must</em> be a {@link Frame#isPseudoInertial pseudo-inertial frame})
     * @param date date of the orbital parameters
     * @param mu central attraction coefficient (m³/s²)
     * @exception IllegalArgumentException if frame is not a {@link
     * Frame#isPseudoInertial pseudo-inertial frame}
     */
    public FieldCartesianOrbit(final FieldPVCoordinates<T> pvaCoordinates, final Frame frame,
                               final FieldAbsoluteDate<T> date, final double mu)
        throws IllegalArgumentException {
        this(new TimeStampedFieldPVCoordinates<T>(date, pvaCoordinates), frame, mu);
        field = pvaCoordinates.getPosition().getX().getField();
        zero = field.getZero();
        one = field.getOne();
    }

    /** Constructor from any kind of orbital parameters.
     * @param op orbital parameters to copy
     */
    public FieldCartesianOrbit(final FieldOrbit<T> op) {
        super(op.getPVCoordinates(), op.getFrame(), op.getMu());
        if (op instanceof FieldEquinoctialOrbit) {
            equinoctial = (FieldEquinoctialOrbit<T>) op;
        } else if (op instanceof FieldCartesianOrbit) {
            equinoctial = ((FieldCartesianOrbit<T>) op).equinoctial;
        } else {
            equinoctial = null;
        }
        field = op.getA().getField();
        zero = field.getZero();
        one = field.getOne();
    }

    /** {@inheritDoc} */
    public OrbitType getType() {
        return OrbitType.CARTESIAN;
    }

    /** Lazy evaluation of equinoctial parameters. */
    private void initEquinoctial() {
        if (equinoctial == null) {
            equinoctial = new FieldEquinoctialOrbit<T>(getPVCoordinates(), getFrame(), getDate(), getMu());
        }
    }

    /** {@inheritDoc} */
    public T getA() {
        // lazy evaluation of semi-major axis
        final T r  = getPVCoordinates().getPosition().getNorm();
        final T V2 = getPVCoordinates().getVelocity().getNormSq();
        return r.divide(r.negate().multiply(V2).divide(getMu()).add(2));
    }

    /** {@inheritDoc} */
    public T getE() {
        final FieldVector3D<T> pvP   = getPVCoordinates().getPosition();
        final FieldVector3D<T> pvV   = getPVCoordinates().getVelocity();
        final T rV2OnMu = pvP.getNorm().multiply(pvV.getNormSq()).divide(getMu());
        final T eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(getA().multiply(getMu()).sqrt());
        final T eCE     = rV2OnMu.subtract(1);
        return (eCE.multiply(eCE).add(eSE.multiply(eSE))).sqrt();
    }

    /** {@inheritDoc} */
    public T getI() {
        return FieldVector3D.angle(new FieldVector3D<T>(zero, zero, one), getPVCoordinates().getMomentum());
    }

    /** {@inheritDoc} */
    public T getEquinoctialEx() {
        initEquinoctial();
        return equinoctial.getEquinoctialEx();
    }

    /** {@inheritDoc} */
    public T getEquinoctialEy() {
        initEquinoctial();
        return equinoctial.getEquinoctialEy();
    }

    /** {@inheritDoc} */
    public T getHx() {
        final FieldVector3D<T> w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX().getReal() * w.getX().getReal() + w.getY().getReal() * w.getY().getReal()) == 0) && w.getZ().getReal() < 0) {
            return zero.add(Double.NaN);
        }
        return w.getY().negate().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getHy() {
        final FieldVector3D<T> w = getPVCoordinates().getMomentum().normalize();
        // Check for equatorial retrograde orbit
        if (((w.getX().getReal() * w.getX().getReal() + w.getY().getReal() * w.getY().getReal()) == 0) && w.getZ().getReal() < 0) {
            return zero.add(Double.NaN);
        }
        return  w.getX().divide(w.getZ().add(1));
    }

    /** {@inheritDoc} */
    public T getLv() {
        initEquinoctial();
        return equinoctial.getLv();
    }

    /** {@inheritDoc} */
    public T getLE() {
        initEquinoctial();
        return equinoctial.getLE();
    }

    /** {@inheritDoc} */
    public T getLM() {
        initEquinoctial();
        return equinoctial.getLM();
    }

    /** {@inheritDoc} */
    protected TimeStampedFieldPVCoordinates<T> initFieldPVCoordinates() {
        // nothing to do here, as the canonical elements are already the cartesian ones
        return getPVCoordinates();
    }

    /** {@inheritDoc} */
    public FieldCartesianOrbit<T> shiftedBy(final T dt) {
        final FieldPVCoordinates<T> shiftedPV = (getA().getReal() < 0) ? shiftPVHyperbolic(dt) : shiftPVElliptic(dt);
        return new FieldCartesianOrbit<T>(shiftedPV, getFrame(), getDate().shiftedBy(dt), getMu());
    }

    /** {@inheritDoc}
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
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
    public FieldCartesianOrbit<T> interpolate(final FieldAbsoluteDate<T> date, final Collection<FieldOrbit<T>> sample) {
        final List<TimeStampedFieldPVCoordinates<T>> datedPV = new ArrayList<TimeStampedFieldPVCoordinates<T>>(sample.size());
        for (final FieldOrbit<T> o : sample) {
            datedPV.add(new TimeStampedFieldPVCoordinates<T>(o.getDate(),
                                                     o.getPVCoordinates().getPosition(),
                                                     o.getPVCoordinates().getVelocity(),
                                                     o.getPVCoordinates().getAcceleration()));
        }
        final TimeStampedFieldPVCoordinates<T> interpolated =
                TimeStampedFieldPVCoordinates.interpolate(date, CartesianDerivativesFilter.USE_PVA, datedPV);
        return new FieldCartesianOrbit<T>(interpolated, getFrame(), date, getMu());
    }

    /** Compute shifted position and velocity in elliptic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private FieldPVCoordinates<T> shiftPVElliptic(final T dt) {

        // preliminary computation
        final FieldVector3D<T> pvP   = getPVCoordinates().getPosition();
        final FieldVector3D<T> pvV   = getPVCoordinates().getVelocity();
        final T r       = pvP.getNorm();
        final T rV2OnMu = r.multiply(pvV.getNormSq()).divide(getMu());
        final T a       = getA();
        final T eSE     = FieldVector3D.dotProduct(pvP, pvV).divide(a.multiply(getMu()).sqrt());
        final T eCE     = rV2OnMu.subtract(1);
        final T e2      = eCE.multiply(eCE).add(eSE.multiply(eSE));

        // we can use any arbitrary reference 2D frame in the orbital plane
        // in order to simplify some equations below, we use the current position as the u axis
        final FieldVector3D<T> u     = pvP.normalize();
        final FieldVector3D<T> v     = FieldVector3D.crossProduct(getPVCoordinates().getMomentum(), u).normalize();

        // the following equations rely on the specific choice of u explained above,
        // some coefficients that vanish to 0 in this case have already been removed here
        final T ex      = eCE.subtract(e2).multiply(a).divide(r);
        final T ey      = one.subtract(e2).sqrt().negate().multiply(eSE).multiply(a).divide(r);
        final T beta    = one.divide(one.subtract(e2).sqrt().add(1));
        final T thetaE0 = ey.add(eSE.multiply(beta).multiply(ex)).atan2(r.divide(a).add(ex).subtract(eSE.multiply(beta).multiply(ey)));
        final T thetaM0 = thetaE0.subtract(ex.multiply(thetaE0.sin())).add(ey.multiply(thetaE0.cos()));

        // compute in-plane shifted eccentric argument
        final T thetaM1 = thetaM0.add(getKeplerianMeanMotion().multiply(dt));
        final T thetaE1 = meanToEccentric(thetaM1, ex, ey);
        final T cTE     = thetaE1.cos();
        final T sTE     = thetaE1.sin();

        // compute shifted in-plane cartesian coordinates
        final T exey   = ex.multiply(ey);
        final T exCeyS = ex.multiply(cTE).add(ey.multiply(sTE));
        final T x      = a.multiply(one.subtract(beta.multiply(ey).multiply(ey)).multiply(cTE).add(beta.multiply(exey).multiply(sTE)).subtract(ex));
        final T y      = a.multiply(one.subtract(beta.multiply(ex).multiply(ex)).multiply(sTE).add(beta.multiply(exey).multiply(cTE)).subtract(ey));
        final T factor = zero.add(getMu()).divide(a).sqrt().divide(one.subtract(exCeyS));
        final T xDot   = factor.multiply(beta.multiply(ey).multiply(exCeyS).subtract(sTE));
        final T yDot   = factor.multiply(cTE.subtract(beta.multiply(ex).multiply(exCeyS)));

        final FieldVector3D<T> shiftedP = new FieldVector3D<T>(x, u, y, v);
        final T   r2       = x.multiply(x).add(y.multiply(y));
        final FieldVector3D<T> shiftedV = new FieldVector3D<T>(xDot, u, yDot, v);
        final FieldVector3D<T> shiftedA = new FieldVector3D<T>(zero.add(-getMu()).divide(r2.multiply(r2.sqrt())), shiftedP);
        return new FieldPVCoordinates<T>(shiftedP, shiftedV, shiftedA);

    }

    /** Compute shifted position and velocity in hyperbolic case.
     * @param dt time shift
     * @return shifted position and velocity
     */
    private FieldPVCoordinates<T> shiftPVHyperbolic(final T dt) {

        final FieldPVCoordinates<T> pv = getPVCoordinates();
        final FieldVector3D<T> pvP   = pv.getPosition();
        final FieldVector3D<T> pvV   = pv.getVelocity();
        final FieldVector3D<T> pvM   = pv.getMomentum();
        final T r       = pvP.getNorm();
        final T rV2OnMu = r.multiply(pvV.getNormSq()).divide(getMu());
        final T a       = getA();
        final T muA     = a.multiply(getMu());
        final T e       = one.subtract(FieldVector3D.dotProduct(pvM, pvM).divide(muA)).sqrt();
        final T sqrt    = e.add(1).divide(e.subtract(1)).sqrt();

        // compute mean anomaly
        final T eSH     = FieldVector3D.dotProduct(pvP, pvV).divide(muA.negate().sqrt());
        final T eCH     = rV2OnMu.subtract(1);
        final T H0      = eCH.add(eSH).divide(eCH.subtract(eSH)).log().divide(2);
        final T M0      = e.multiply(H0.sinh()).subtract(H0);

        // find canonical 2D frame with p pointing to perigee
        final T v0      = sqrt.multiply(H0.divide(2).tanh()).atan().multiply(2);
        final FieldVector3D<T> p     = new FieldRotation<T>(pvM, v0, RotationConvention.FRAME_TRANSFORM).applyTo(pvP).normalize();
        final FieldVector3D<T> q     = FieldVector3D.crossProduct(pvM, p).normalize();

        // compute shifted eccentric anomaly
        final T M1      = M0.add(getKeplerianMeanMotion().multiply(dt));
        final T H1      = meanToHyperbolicEccentric(M1, e);

        // compute shifted in-plane cartesian coordinates
        final T cH     = H1.cosh();
        final T sH     = H1.sinh();
        final T sE2m1  = e.subtract(1).multiply(e.add(1)).sqrt();

        // coordinates of position and velocity in the orbital plane
        final T x      = a.multiply(cH.subtract(e));
        final T y      = a.negate().multiply(sE2m1).multiply(sH);
        final T factor = zero.add(getMu()).divide(a.negate()).sqrt().divide(e.multiply(cH).subtract(1));
        final T xDot   = factor.negate().multiply(sH);
        final T yDot   =  factor.multiply(sE2m1).multiply(cH);

        final FieldVector3D<T> shiftedP = new FieldVector3D<T>(x, p, y, q);
        final T   r2       = x.multiply(x).add(y.multiply(y));
        final FieldVector3D<T> shiftedV = new FieldVector3D<T>(xDot, p, yDot, q);
        final FieldVector3D<T> shiftedA = new FieldVector3D<T>(zero.add(-getMu()).divide(r2.multiply(r2.sqrt())), shiftedP);
        return new FieldPVCoordinates<T>(shiftedP, shiftedV, shiftedA);

    }

    /** Computes the eccentric in-plane argument from the mean in-plane argument.
     * @param thetaM = mean in-plane argument (rad)
     * @param ex first component of eccentricity vector
     * @param ey second component of eccentricity vector
     * @return the eccentric in-plane argument.
     */
    private T meanToEccentric(final T thetaM, final T ex, final T ey) {
        // Generalization of Kepler equation to in-plane parameters
        // with thetaE = eta + E and
        //      thetaM = eta + M = thetaE - ex.sin(thetaE) + ey.cos(thetaE)
        // and eta being counted from an arbitrary reference in the orbital plane
        T thetaE        = thetaM;
        T thetaEMthetaM = zero;
        int    iter          = 0;
        do {
            final T cosThetaE = thetaE.cos();
            final T sinThetaE = thetaE.sin();

            final T f2 = ex.multiply(sinThetaE).subtract(ey.multiply(cosThetaE));
            final T f1 = one.subtract(ex.multiply(cosThetaE)).subtract(ey.multiply(sinThetaE));
            final T f0 = thetaEMthetaM.subtract(f2);

            final T f12 = f1.multiply(2.0);
            final T shift = f0.multiply(f12).divide(f1.multiply(f12).subtract(f0.multiply(f2)));

            thetaEMthetaM = thetaEMthetaM.subtract(shift);
            thetaE         = thetaM.add(thetaEMthetaM);

            if (FastMath.abs(shift.getReal()) <= 1.0e-12) {
                return thetaE;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(LocalizedCoreFormats.CONVERGENCE_FAILED);

    }

    /** Computes the hyperbolic eccentric anomaly from the mean anomaly.
     * <p>
     * The algorithm used here for solving hyperbolic Kepler equation is
     * Danby's iterative method (3rd order) with Vallado's initial guess.
     * </p>
     * @param M mean anomaly (rad)
     * @param ecc eccentricity
     * @return the hyperbolic eccentric anomaly
     */
    private T meanToHyperbolicEccentric(final T M, final T ecc) {

        // Resolution of hyperbolic Kepler equation for keplerian parameters

        // Initial guess
        T H;
        if (ecc.getReal() < 1.6) {
            if ((-FastMath.PI < M.getReal() && M.getReal() < 0.) || M.getReal() > FastMath.PI) {
                H = M.subtract(ecc);
            } else {
                H = M.add(ecc);
            }
        } else {
            if (ecc.getReal() < 3.6 && FastMath.abs(M.getReal()) > FastMath.PI) {
                H = M.subtract(ecc.copySign(M));
            } else {
                H = M.divide(ecc.subtract(1.));
            }
        }

        // Iterative computation
        int iter = 0;
        do {
            final T f3  = ecc.multiply(H.cosh());
            final T f2  = ecc.multiply(H.sinh());
            final T f1  = f3.subtract(1.);
            final T f0  = f2.subtract(H).subtract(M);
            final T f12 = f1.multiply(2);
            final T d   = f0.divide(f12);
            final T fdf = f1.subtract(d.multiply(f2));
            final T ds  = f0.divide(fdf);

            final T shift = f0.divide(fdf.add(ds.multiply(ds).multiply(f3.divide(6.))));

            H = H.subtract(shift);

            if (FastMath.abs(shift.getReal()) <= 1.0e-12) {
                return H;
            }

        } while (++iter < 50);

        throw new MathIllegalStateException(OrekitMessages.UNABLE_TO_COMPUTE_HYPERBOLIC_ECCENTRIC_ANOMALY,
                                            iter);
    }

    @Override
    public void getJacobianWrtCartesian(final PositionAngle type, final T[][] jacobian) {
        // this is the fastest way to set the 6x6 identity matrix
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                jacobian[i][j] = zero;
            }
            jacobian[i][i] = one;
        }
    }

    @Override
    protected T[][] computeJacobianMeanWrtCartesian() {
        // not used
        return null;
    }
    @Override
    protected T[][] computeJacobianEccentricWrtCartesian() {
        // not used
        return null;
    }

    @Override
    protected T[][] computeJacobianTrueWrtCartesian() {
        // not used
        return null;
    }

    /** {@inheritDoc} */
    public void addKeplerContribution(final PositionAngle type, final double gm,
                                      final T[] pDot) {

        final FieldPVCoordinates<T> pv = getPVCoordinates();

        // position derivative is velocity
        final FieldVector3D<T> velocity = pv.getVelocity();
        pDot[0] = pDot[0].add(velocity.getX());
        pDot[1] = pDot[1].add(velocity.getY());
        pDot[2] = pDot[2].add(velocity.getZ());

        // velocity derivative is Newtonian acceleration
        final FieldVector3D<T> position = pv.getPosition();
        final T r2         = position.getNormSq();
        final T coeff      = r2.multiply(r2.sqrt()).reciprocal().negate().multiply(gm);
        pDot[3] = pDot[3].add(coeff.multiply(position.getX()));
        pDot[4] = pDot[4].add(coeff.multiply(position.getY()));
        pDot[5] = pDot[5].add(coeff.multiply(position.getZ()));

    }

    /**  Returns a string representation of this Orbit object.
     * @return a string representation of this object
     */
    public String toString() {
        return "cartesian parameters: " + getPVCoordinates().toString();
    }

    @Override
    public CartesianOrbit toOrbit() {
        final PVCoordinates PVC = getPVCoordinates().toPVCoordinates();
        final AbsoluteDate AD = getPVCoordinates().getDate().toAbsoluteDate();
        final TimeStampedPVCoordinates TSPVC = new TimeStampedPVCoordinates(AD, PVC);
        return new CartesianOrbit(TSPVC, getFrame(), getMu());
    }

}
