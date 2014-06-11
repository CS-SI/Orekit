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
package org.orekit.utils;

import java.util.Collection;

import org.apache.commons.math3.RealFieldElement;
import org.apache.commons.math3.analysis.interpolation.FieldHermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.FieldRotation;
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldAngularCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldAngularCoordinates<T extends RealFieldElement<T>>
    extends FieldAngularCoordinates<T> implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140611L;

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a rotation/rotation rate pair.
     * @param date coordinates date
     * @param rotation rotation
     * @param rotationRate rotation rate (rad/s)
     */
    public TimeStampedFieldAngularCoordinates(final AbsoluteDate date,
                                              final FieldRotation<T> rotation,
                                              final FieldVector3D<T> rotationRate) {
        super(rotation, rotationRate);
        this.date = date;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Revert a rotation/rotation rate pair.
     * Build a pair which reverse the effect of another pair.
     * @return a new pair whose effect is the reverse of the effect
     * of the instance
     */
    public TimeStampedFieldAngularCoordinates<T> revert() {
        return new TimeStampedFieldAngularCoordinates<T>(date,
                                                         getRotation().revert(),
                                                         getRotation().applyInverseTo(getRotationRate().negate()));
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
    public TimeStampedFieldAngularCoordinates<T> shiftedBy(final double dt) {
        final FieldAngularCoordinates<T> sac = super.shiftedBy(dt);
        return new TimeStampedFieldAngularCoordinates<T>(date.shiftedBy(dt), sac.getRotation(), sac.getRotationRate());

    }

    /** Interpolate angular coordinates.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring rotation rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * This method is based on Sergei Tanygin's paper <a
     * href="http://www.agi.com/downloads/resources/white-papers/Attitude-interpolation.pdf">Attitude
     * Interpolation</a>, changing the norm of the vector to match the modified Rodrigues
     * vector as described in Malcolm D. Shuster's paper <a
     * href="http://www.ladispe.polito.it/corsi/Meccatronica/02JHCOR/2011-12/Slides/Shuster_Pub_1993h_J_Repsurv_scan.pdf">A
     * Survey of Attitude Representations</a>. This change avoids the singularity at &pi;.
     * There is still a singularity at 2&pi;, which is handled by slightly offsetting all rotations
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first time derivatives (rotation rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the rotations.
     * </p>
     * @param date interpolation date
     * @param useRotationRates if true, use sample points rotation rates,
     * otherwise ignore them and use only rotations
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    @SuppressWarnings("unchecked")
    public static <T extends RealFieldElement<T>> TimeStampedFieldAngularCoordinates<T>
    interpolate(final AbsoluteDate date, final boolean useRotationRates,
                final Collection<TimeStampedFieldAngularCoordinates<T>> sample)
        throws OrekitException {

        // get field properties
        final T prototype = sample.iterator().next().getRotation().getQ0();
        final T zero = prototype.getField().getZero();
        final T one  = prototype.getField().getOne();

        // set up safety elements for 2PI singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear offset model canceling mean FieldRotation<T> rate
        final FieldVector3D<T> meanRate;
        if (useRotationRates) {
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                sum = sum.add(datedAC.getRotationRate());
            }
            meanRate = new FieldVector3D<T>(1.0 / sample.size(), sum);
        } else {
            FieldVector3D<T> sum = new FieldVector3D<T>(zero, zero, zero);
            TimeStampedFieldAngularCoordinates<T> previous = null;
            for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getRotation(), datedAC.getRotation(),
                                               datedAC.date.durationFrom(previous.getDate())));
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
                for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                    final T[][] rodrigues = getModifiedRodrigues(datedAC, date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(zero.add(datedAC.getDate().durationFrom(date)),
                                                rodrigues[0], rodrigues[1]);
                }
            } else {
                // populate sample with FieldRotation<T> data only, ignoring FieldRotation<T> rate
                for (final TimeStampedFieldAngularCoordinates<T> datedAC : sample) {
                    final T[][] rodrigues = getModifiedRodrigues(datedAC, date, offset, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(zero.add(datedAC.getDate().durationFrom(date)),
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
                return createFromModifiedRodrigues(date, p, offset);
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
     * @param ac coordinates to convert
     * @param offsetDate date of the linear offset model to remove
     * @param offset linear offset model to remove
     * @param threshold threshold for rotations too close to 2&pi;
     * @param <T> the type of the field elements
     * @return modified Rodrigues vector and derivative, or null if rotation is too close to 2&pi;
     */
    private static <T extends RealFieldElement<T>> T[][] getModifiedRodrigues(final TimeStampedFieldAngularCoordinates<T> ac,
                                                                              final AbsoluteDate offsetDate,
                                                                              final FieldAngularCoordinates<T> offset,
                                                                              final double threshold) {

        // remove linear offset from the current coordinates
        final double dt = ac.date.durationFrom(offsetDate);
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
     * @param date date of the angular coordinates
     * @param r modified Rodrigues vector (with first derivatives)
     * @param offset linear offset model to add (its date must be consistent with the modified Rodrigues vector)
     * @param <T> the type of the field elements
     * @return angular coordinates
     */
    private static <T extends RealFieldElement<T>> TimeStampedFieldAngularCoordinates<T>
    createFromModifiedRodrigues(final AbsoluteDate date, final T[][] r, final FieldAngularCoordinates<T> offset) {

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

        final FieldAngularCoordinates<T> ac = new FieldAngularCoordinates<T>(rotation, rate).addOffset(offset);
        return new TimeStampedFieldAngularCoordinates<T>(date, ac.getRotation(), ac.getRotationRate());

    }

}
