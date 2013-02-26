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
package org.orekit.utils;

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math3.ExtendedFieldElement;
import org.apache.commons.math3.analysis.interpolation.FieldHermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for FieldRotation<T>/FieldRotation<T> rate pairs, using {@link T}.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a simple linear model. It is <em>not</em> intended as a replacement for
 * proper attitude propagation but should be sufficient for either small
 * time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link FieldPVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 6.0
 * @see AngularCoordinates
 */
public class FieldAngularCoordinates<T extends ExtendedFieldElement<T>>
     implements TimeShiftable<FieldAngularCoordinates<T>>, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130222L;

    /** FieldRotation<T>. */
    private final FieldRotation<T> rotation;

    /** FieldRotation<T> rate. */
    private final FieldVector3D<T> rotationRate;

    /** Builds a FieldRotation<T>/FieldRotation<T> rate pair.
     * @param rotation FieldRotation<T>
     * @param rotationRate FieldRotation<T> rate (rad/s)
     */
    public FieldAngularCoordinates(final FieldRotation<T> rotation, final FieldVector3D<T> rotationRate) {
        this.rotation     = rotation;
        this.rotationRate = rotationRate;
    }

    /** Estimate FieldRotation<T> rate between two orientations.
     * <p>Estimation is based on a simple fixed rate FieldRotation<T>
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return FieldRotation<T> rate allowing to go from start to end orientations
     */
    public static <T extends ExtendedFieldElement<T>> FieldVector3D<T> estimateRate(final FieldRotation<T> start, final FieldRotation<T> end, final double dt) {
        final FieldRotation<T> evolution = start.applyTo(end.revert());
        return new FieldVector3D<T>(evolution.getAngle().divide(dt), evolution.getAxis());
    }

    /** Revert a FieldRotation<T>/FieldRotation<T> rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public FieldAngularCoordinates<T> revert() {
        return new FieldAngularCoordinates<T>(rotation.revert(), rotation.applyInverseTo(rotationRate.negate()));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper attitude propagation but should be sufficient for either small
     * time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public FieldAngularCoordinates<T> shiftedBy(final double dt) {
        final T rate = rotationRate.getNorm();
        if (rate.getReal() == 0.0) {
            // special case for fixed FieldRotation<T>s
            return this;
        }

        // BEWARE: there is really a minus sign here, because if
        // the target frame rotates in one direction, the vectors in the origin
        // frame seem to rotate in the opposite direction
        final FieldRotation<T> evolution = new FieldRotation<T>(rotationRate, rate.negate().multiply(dt));

        return new FieldAngularCoordinates<T>(evolution.applyTo(rotation), rotationRate);

    }

    /** Get the FieldRotation<T>.
     * @return the FieldRotation<T>.
     */
    public FieldRotation<T> getRotation() {
        return rotation;
    }

    /** Get the FieldRotation<T> rate.
     * @return the FieldRotation<T> rate vector (rad/s).
     */
    public FieldVector3D<T> getRotationRate() {
        return rotationRate;
    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset FieldRotation<T> is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(FieldAngularCoordinates) addOffset} and
     * {@link #subtractOffset(FieldAngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(FieldAngularCoordinates)
     */
    public FieldAngularCoordinates<T> addOffset(final FieldAngularCoordinates<T> offset) {
        return new FieldAngularCoordinates<T>(rotation.applyTo(offset.rotation),
                                              rotationRate.add(rotation.applyTo(offset.rotationRate)));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset Rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(FieldAngularCoordinates) addOffset} and
     * {@link #subtractOffset(FieldAngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(FieldAngularCoordinates)
     */
    public FieldAngularCoordinates<T> subtractOffset(final FieldAngularCoordinates<T> offset) {
        return addOffset(offset.revert());
    }

    /** Convert to a constant angular coordinates without derivatives.
     * @return a constant angular coordinates
     */
    public AngularCoordinates toAngularCoordinates() {
        return new AngularCoordinates(rotation.toRotation(), rotationRate.toVector3D());
    }

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring FieldRotation<T> rate remains the exact derivative of FieldRotation<T>.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at &pi;.
     * There is still a singularity at 2&pi;, which is handled by slightly offsetting all FieldRotation<T>s
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first time derivatives (FieldRotation<T> rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the FieldRotation<T>s.
     * </p>
     * @param date interpolation date
     * @param useFieldRotation<T>Rates if true, use sample points FieldRotation<T> rates,
     * otherwise ignore them and use only FieldRotation<T>s
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    @SuppressWarnings("unchecked")
    public static <T extends ExtendedFieldElement<T>> FieldAngularCoordinates<T> interpolate(final AbsoluteDate date, final boolean useRotationRates,
                                                                                             final Collection<Pair<AbsoluteDate, FieldAngularCoordinates<T>>> sample) {

        // get field properties
        final T prototype = sample.iterator().next().getValue().getRotation().getQ0();
        final T zero = prototype.getField().getZero();
        final T one  = prototype.getField().getOne();

        // set up safety elements for 2PI singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear offset model canceling mean FieldRotation<T> rate
        final FieldVector3D<T> meanRate;
        if (useRotationRates) {
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            for (final Pair<AbsoluteDate, FieldAngularCoordinates<T>> datedAC : sample) {
                sum = sum.add(datedAC.getValue().getRotationRate());
            }
            meanRate = new FieldVector3D<T>(1.0 / sample.size(), sum);
        } else {
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            Pair<AbsoluteDate, FieldAngularCoordinates<T>> previous = null;
            for (final Pair<AbsoluteDate, FieldAngularCoordinates<T>> datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getValue().getRotation(),
                                                         datedAC.getValue().getRotation(),
                                                         datedAC.getKey().durationFrom(previous.getKey().getDate())));
                }
                previous = datedAC;
            }
            meanRate = new FieldVector3D<T>(1.0 / (sample.size() - 1), sum);
        }
        FieldAngularCoordinates<T> offset =
                new FieldAngularCoordinates<T>(new FieldRotation<T>(one, zero, zero, zero, false), meanRate);

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

            // add sample points
            if (useRotationRates) {
                // populate sample with FieldRotation<T> and FieldRotation<T> rate data
                for (final Pair<AbsoluteDate, FieldAngularCoordinates<T>> datedAC : sample) {
                    final T[][] rodrigues = getModifiedRodrigues(datedAC.getKey(), datedAC.getValue(),
                                                                 date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(zero.add(datedAC.getKey().getDate().durationFrom(date)),
                                                rodrigues[0], rodrigues[1]);
                }
            } else {
                // populate sample with FieldRotation<T> data only, ignoring FieldRotation<T> rate
                for (final Pair<AbsoluteDate, FieldAngularCoordinates<T>> datedAC : sample) {
                    final T[][] rodrigues = getModifiedRodrigues(datedAC.getKey(), datedAC.getValue(),
                                                                 date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(zero.add(datedAC.getKey().getDate().durationFrom(date)),
                                                rodrigues[0]);
                }
            }

            if (restart) {
                // interpolation failed, some intermediate FieldRotation<T> was too close to 2PI
                // we need to offset all FieldRotation<T>s to avoid the singularity
                offset = offset.addOffset(new FieldAngularCoordinates<T>(new FieldRotation<T>(new FieldVector3D<T>(one, zero, zero),
                                                                                              zero.add(epsilon)),
                                                                         new FieldVector3D<T>(one, zero, zero)));
            } else {
                // interpolation succeeded with the current offset
                final T[][] p = interpolator.derivatives(zero, 1);
                return createFromModifiedRodrigues(p, offset);
            }

        }

        // this should never happen
        throw OrekitException.createInternalError(null);

    }

    /** Convert rotation and rate to modified Rodrigues vector and derivative.
     * <p>
     * The modified Rodrigues vector is tan(&theta;/4) u where &theta; and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param date date of the angular coordinates
     * @param ac coordinates to convert
     * @param offsetDate date of the linear offset model to remove
     * @param offset linear offset model to remove
     * @param threshold threshold for rotations too close to 2&pi;
     * @return modified Rodrigues vector and derivative, or null if rotation is too close to 2&pi;
     */
    private static <T extends ExtendedFieldElement<T>> T[][] getModifiedRodrigues(final AbsoluteDate date, final FieldAngularCoordinates<T> ac,
                                                                                  final AbsoluteDate offsetDate, final FieldAngularCoordinates<T> offset,
                                                                                  final double threshold) {

        // remove linear offset from the current coordinates
        final double dt = date.durationFrom(offsetDate);
        final FieldAngularCoordinates<T> fixed = ac.subtractOffset(offset.shiftedBy(dt));

        // check modified Rodrigues vector singularity
        T q0 = fixed.getRotation().getQ0();
        T q1 = fixed.getRotation().getQ1();
        T q2 = fixed.getRotation().getQ2();
        T q3 = fixed.getRotation().getQ3();
        if (q0.getReal() < threshold && FastMath.abs(dt) * fixed.getRotationRate().getNorm().getReal() > 1.0e-3) {
            // this is an intermediate point that happens to be 2PI away from reference
            // we need to change the linear offset model to avoid this point
            return null;
        }

        // make sure all interpolated points will be on the same branch
        if (q0.getReal() < 0) {
            q0 = q0.negate();
            q1 = q1.negate();
            q2 = q2.negate();
            q3 = q3.negate();
        }

        final T x  = fixed.getRotationRate().getX();
        final T y  = fixed.getRotationRate().getY();
        final T z  = fixed.getRotationRate().getZ();

        // derivatives of the quaternion
        final T q0Dot = q0.linearCombination(q1, x, q2, y,  q3, z).multiply(-0.5);
        final T q1Dot = q1.linearCombination(q0, x, q2, z, q3.negate(), y).multiply(0.5);
        final T q2Dot = q2.linearCombination(q0, y, q3, x, q1.negate(), z).multiply(0.5);
        final T q3Dot = q3.linearCombination(q0, z, q1, y, q2.negate(), x).multiply(0.5);

        final T inv = q0.add(1).reciprocal();
        final T[][] rodrigues = MathArrays.buildArray(q0.getField(), 2, 3);
        rodrigues[0][0] = inv.multiply(q1);
        rodrigues[0][1] = inv.multiply(q2);
        rodrigues[0][2] = inv.multiply(q3);
        rodrigues[1][0] = inv.multiply(q1Dot.subtract(inv.multiply(q1).multiply(q0Dot)));
        rodrigues[1][1] = inv.multiply(q2Dot.subtract(inv.multiply(q2).multiply(q0Dot)));
        rodrigues[1][2] = inv.multiply(q3Dot.subtract(inv.multiply(q3).multiply(q0Dot)));

        return rodrigues;

    }

    /** Convert a modified Rodrigues vector and derivative to angular coordinates.
     * @param r modified Rodrigues vector (with first derivatives)
     * @param offset linear offset model to add (its date must be consistent with the modified Rodrigues vector)
     * @return angular coordinates
     */
    private static <T extends ExtendedFieldElement<T>> FieldAngularCoordinates<T> createFromModifiedRodrigues(final T[][] r,
                                                                                                              final FieldAngularCoordinates<T> offset) {

        // rotation
        final T rSquared = r[0][0].multiply(r[0][0]).add(r[0][1].multiply(r[0][1])).add(r[0][2].multiply(r[0][2]));
        final T inv      = rSquared.add(1).reciprocal();
        final T ratio    = inv.multiply(rSquared.subtract(1).negate());
        final FieldRotation<T> rotation          = new FieldRotation<T>(ratio,
                                                            inv.multiply(2).multiply(r[0][0]),
                                                            inv.multiply(2).multiply(r[0][1]),
                                                            inv.multiply(2).multiply(r[0][2]),
                                                            false);

        // rotation rate
        final FieldVector3D<T> p    = new FieldVector3D<T>(r[0]);
        final FieldVector3D<T> pDot = new FieldVector3D<T>(r[1]);
        final FieldVector3D<T> rate = new FieldVector3D<T>(inv.multiply(ratio).multiply(4), pDot,
                                                           inv.multiply(inv).multiply(-8), FieldVector3D.crossProduct(p, pDot),
                                                           inv.multiply(inv).multiply(8).multiply(FieldVector3D.dotProduct(p, pDot)), p);

        return new FieldAngularCoordinates<T>(rotation, rate).addOffset(offset);

    }

}
