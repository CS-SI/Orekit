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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link AngularCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedAngularCoordinates extends AngularCoordinates implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a rotation/rotation rate pair.
     * @param date coordinates date
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public TimeStampedAngularCoordinates(final AbsoluteDate date,
                                         final Rotation rotation,
                                         final Vector3D rotationRate,
                                         final Vector3D rotationAcceleration) {
        super(rotation, rotationRate, rotationAcceleration);
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
    public TimeStampedAngularCoordinates revert() {
        return new TimeStampedAngularCoordinates(date,
                                                 getRotation().revert(),
                                                 getRotation().applyInverseTo(getRotationRate().negate()),
                                                 getRotation().applyInverseTo(getRotationAcceleration().negate()));
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
    public TimeStampedAngularCoordinates shiftedBy(final double dt) {
        final AngularCoordinates sac = super.shiftedBy(dt);
        return new TimeStampedAngularCoordinates(date.shiftedBy(dt),
                                                 sac.getRotation(), sac.getRotationRate(), sac.getRotationAcceleration());

    }

    /** Add an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.addOffset(b)} and {@code
     * b.addOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #subtractOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates addOffset(final AngularCoordinates offset) {
        return new TimeStampedAngularCoordinates(date,
                                                 getRotation().applyTo(offset.getRotation()),
                                                 getRotationRate().add(getRotation().applyTo(offset.getRotationRate())),
                                                 getRotationAcceleration().add(getRotation().applyTo(offset.getRotationAcceleration())));
    }

    /** Subtract an offset from the instance.
     * <p>
     * We consider here that the offset rotation is applied first and the
     * instance is applied afterward. Note that angular coordinates do <em>not</em>
     * commute under this operation, i.e. {@code a.subtractOffset(b)} and {@code
     * b.subtractOffset(a)} lead to <em>different</em> results in most cases.
     * </p>
     * <p>
     * The two methods {@link #addOffset(AngularCoordinates) addOffset} and
     * {@link #subtractOffset(AngularCoordinates) subtractOffset} are designed
     * so that round trip applications are possible. This means that both {@code
     * ac1.subtractOffset(ac2).addOffset(ac2)} and {@code
     * ac1.addOffset(ac2).subtractOffset(ac2)} return angular coordinates equal to ac1.
     * </p>
     * @param offset offset to subtract
     * @return new instance, with offset subtracted
     * @see #addOffset(AngularCoordinates)
     */
    @Override
    public TimeStampedAngularCoordinates subtractOffset(final AngularCoordinates offset) {
        return addOffset(offset.revert());
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
     * Survey of Attitude Representations</a>. This change avoids the singularity at π.
     * There is still a singularity at 2π, which is handled by slightly offsetting all rotations
     * when this singularity is detected.
     * </p>
     * <p>
     * Note that even if first and second time derivatives (rotation rates and acceleration)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the rotations.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public static TimeStampedAngularCoordinates interpolate(final AbsoluteDate date,
                                                            final AngularDerivativesFilter filter,
                                                            final Collection<TimeStampedAngularCoordinates> sample)
        throws OrekitException {

        // set up safety elements for 2π singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear model canceling mean rotation rate
        final Vector3D meanRate;
        if (filter != AngularDerivativesFilter.USE_R) {
            Vector3D sum = Vector3D.ZERO;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                sum = sum.add(datedAC.getRotationRate());
            }
            meanRate = new Vector3D(1.0 / sample.size(), sum);
        } else {
            if (sample.size() < 2) {
                throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
                                          sample.size());
            }
            Vector3D sum = Vector3D.ZERO;
            TimeStampedAngularCoordinates previous = null;
            for (final TimeStampedAngularCoordinates datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getRotation(), datedAC.getRotation(),
                                               datedAC.date.durationFrom(previous.date)));
                }
                previous = datedAC;
            }
            meanRate = new Vector3D(1.0 / (sample.size() - 1), sum);
        }
        TimeStampedAngularCoordinates offset =
            new TimeStampedAngularCoordinates(date, Rotation.IDENTITY, meanRate, Vector3D.ZERO);

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            final double[] previous = new double[] {
                1.0, 0.0, 0.0, 0.0
            };
            switch (filter) {
            case USE_RRA: {
                // populate sample with rotation and rotation rate data
                for (final TimeStampedAngularCoordinates ac : sample) {

                    // remove linear offset from the current coordinates
                    final double dt = ac.date.durationFrom(date);
                    final TimeStampedAngularCoordinates fixed = ac.subtractOffset(offset.shiftedBy(dt));

                    final double[][] rodrigues = getModifiedRodrigues(fixed, previous, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(ac.getDate().durationFrom(date),
                                                rodrigues[0], rodrigues[1], rodrigues[2]);
                }
                break;
            }
            case USE_RR: {
                // populate sample with rotation and rotation rate data
                for (final TimeStampedAngularCoordinates ac : sample) {

                    // remove linear offset from the current coordinates
                    final double dt = ac.date.durationFrom(date);
                    final TimeStampedAngularCoordinates fixed = ac.subtractOffset(offset.shiftedBy(dt));

                    final double[][] rodrigues = getModifiedRodrigues(fixed, previous, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(ac.getDate().durationFrom(date),
                                                rodrigues[0], rodrigues[1]);
                }
                break;
            }
            case USE_R: {
                // populate sample with rotation data only, ignoring rotation rate
                for (final TimeStampedAngularCoordinates ac : sample) {

                    // remove linear offset from the current coordinates
                    final double dt = ac.date.durationFrom(date);
                    final TimeStampedAngularCoordinates fixed = ac.subtractOffset(offset.shiftedBy(dt));

                    final double[][] rodrigues = getModifiedRodrigues(fixed, previous, threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(ac.getDate().durationFrom(date),
                                                rodrigues[0]);
                }
                break;
            }
            default :
                // this should never happen
                throw OrekitException.createInternalError(null);
            }

            if (restart) {
                // interpolation failed, some intermediate rotation was too close to 2π
                // we need to offset all rotations to avoid the singularity
                offset = offset.addOffset(new AngularCoordinates(new Rotation(Vector3D.PLUS_I, epsilon),
                                                                 Vector3D.ZERO, Vector3D.ZERO));
            } else {
                // interpolation succeeded with the current offset
                final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
                final DerivativeStructure[] p = interpolator.value(zero);
                return createFromModifiedRodrigues(new double[][] {
                    {
                        p[0].getValue(),              p[1].getValue(),              p[2].getValue()
                    }, {
                        p[0].getPartialDerivative(1), p[1].getPartialDerivative(1), p[2].getPartialDerivative(1)
                    }, {
                        p[0].getPartialDerivative(2), p[1].getPartialDerivative(2), p[2].getPartialDerivative(2)
                    }
                }, offset);
            }

        }

        // this should never happen
        throw OrekitException.createInternalError(null);

    }

    /** Convert rotation, rate and acceleration to modified Rodrigues vector and derivatives.
     * <p>
     * The modified Rodrigues vector is tan(θ/4) u where θ and u are the
     * rotation angle and axis respectively.
     * </p>
     * @param fixed coordinates to convert, with offset already fixed
     * @param previous previous quaternion used
     * @param threshold threshold for rotations too close to 2π
     * @return modified Rodrigues vector and derivatives, or null if rotation is too close to 2π
     */
    private static double[][] getModifiedRodrigues(final TimeStampedAngularCoordinates fixed,
                                                   final double[] previous, final double threshold) {

        // make sure all interpolated points will be on the same branch
        double q0 = fixed.getRotation().getQ0();
        double q1 = fixed.getRotation().getQ1();
        double q2 = fixed.getRotation().getQ2();
        double q3 = fixed.getRotation().getQ3();
        if (MathArrays.linearCombination(q0, previous[0], q1, previous[1], q2, previous[2], q3, previous[3]) < 0) {
            q0 = -q0;
            q1 = -q1;
            q2 = -q2;
            q3 = -q3;
        }
        previous[0] = q0;
        previous[1] = q1;
        previous[2] = q2;
        previous[3] = q3;

        // check modified Rodrigues vector singularity
        if (q0 < threshold) {
            // this is an intermediate point that happens to be 2π away from reference
            // we need to change the linear offset model to avoid this point
            return null;
        }

        final double oX    = fixed.getRotationRate().getX();
        final double oY    = fixed.getRotationRate().getY();
        final double oZ    = fixed.getRotationRate().getZ();
        final double oXDot = fixed.getRotationAcceleration().getX();
        final double oYDot = fixed.getRotationAcceleration().getY();
        final double oZDot = fixed.getRotationAcceleration().getZ();

        // first time-derivatives of the quaternion
        final double q0Dot = 0.5 * MathArrays.linearCombination(-q1, oX, -q2, oY, -q3, oZ);
        final double q1Dot = 0.5 * MathArrays.linearCombination( q0, oX, -q3, oY,  q2, oZ);
        final double q2Dot = 0.5 * MathArrays.linearCombination( q3, oX,  q0, oY, -q1, oZ);
        final double q3Dot = 0.5 * MathArrays.linearCombination(-q2, oX,  q1, oY,  q0, oZ);

        // second time-derivatives of the quaternion
        final double q0DotDot = -0.5 * MathArrays.linearCombination(new double[] {
            q1, q2,  q3, q1Dot, q2Dot,  q3Dot
        }, new double[] {
            oXDot, oYDot, oZDot, oX, oY, oZ
        });
        final double q1DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q2, -q3, q0Dot, q2Dot, -q3Dot
        }, new double[] {
            oXDot, oZDot, oYDot, oX, oZ, oY
        });
        final double q2DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q3, -q1, q0Dot, q3Dot, -q1Dot
        }, new double[] {
            oYDot, oXDot, oZDot, oY, oX, oZ
        });
        final double q3DotDot =  0.5 * MathArrays.linearCombination(new double[] {
            q0, q1, -q2, q0Dot, q1Dot, -q2Dot
        }, new double[] {
            oZDot, oYDot, oXDot, oZ, oY, oX
        });

        // the modified Rodrigues is tan(θ/4) u where θ and u are the rotation angle and axis respectively
        // this can be rewritten using quaternion components:
        //      r (q₁ / (1+q₀), q₂ / (1+q₀), q₃ / (1+q₀))
        // applying the derivation chain rule to previous expression gives rDot and rDotDot
        final double inv          = 1.0 / (1.0 + q0);
        final double mTwoInvQ0Dot = -2 * inv * q0Dot;

        final double r1       = inv * q1;
        final double r2       = inv * q2;
        final double r3       = inv * q3;

        final double mInvR1   = -inv * r1;
        final double mInvR2   = -inv * r2;
        final double mInvR3   = -inv * r3;

        final double r1Dot    = MathArrays.linearCombination(inv, q1Dot, mInvR1, q0Dot);
        final double r2Dot    = MathArrays.linearCombination(inv, q2Dot, mInvR2, q0Dot);
        final double r3Dot    = MathArrays.linearCombination(inv, q3Dot, mInvR3, q0Dot);

        final double r1DotDot = MathArrays.linearCombination(inv, q1DotDot, mTwoInvQ0Dot, r1Dot, mInvR1, q0DotDot);
        final double r2DotDot = MathArrays.linearCombination(inv, q2DotDot, mTwoInvQ0Dot, r2Dot, mInvR2, q0DotDot);
        final double r3DotDot = MathArrays.linearCombination(inv, q3DotDot, mTwoInvQ0Dot, r3Dot, mInvR3, q0DotDot);

        return new double[][] {
            {
                r1,       r2,       r3
            }, {
                r1Dot,    r2Dot,    r3Dot
            }, {
                r1DotDot, r2DotDot, r3DotDot
            }
        };

    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @param r modified Rodrigues vector (with first and second times derivatives)
     * @param offset linear offset model to add (its date must be consistent with the modified Rodrigues vector)
     * @return angular coordinates
     */
    private static TimeStampedAngularCoordinates createFromModifiedRodrigues(final double[][] r,
                                                                             final TimeStampedAngularCoordinates offset) {

        // rotation
        final double rSquared = r[0][0] * r[0][0] + r[0][1] * r[0][1] + r[0][2] * r[0][2];
        final double oPQ0     = 2 / (1 + rSquared);
        final double q0       = oPQ0 - 1;
        final double q1       = oPQ0 * r[0][0];
        final double q2       = oPQ0 * r[0][1];
        final double q3       = oPQ0 * r[0][2];

        // rotation rate
        final double oPQ02    = oPQ0 * oPQ0;
        final double q0Dot    = -oPQ02 * MathArrays.linearCombination(r[0][0], r[1][0], r[0][1], r[1][1],  r[0][2], r[1][2]);
        final double q1Dot    = oPQ0 * r[1][0] + r[0][0] * q0Dot;
        final double q2Dot    = oPQ0 * r[1][1] + r[0][1] * q0Dot;
        final double q3Dot    = oPQ0 * r[1][2] + r[0][2] * q0Dot;
        final double oX       = 2 * MathArrays.linearCombination(-q1, q0Dot,  q0, q1Dot,  q3, q2Dot, -q2, q3Dot);
        final double oY       = 2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot,  q0, q2Dot,  q1, q3Dot);
        final double oZ       = 2 * MathArrays.linearCombination(-q3, q0Dot,  q2, q1Dot, -q1, q2Dot,  q0, q3Dot);

        // rotation acceleration
        final double q0DotDot = (1 - q0) / oPQ0 * q0Dot * q0Dot -
                                oPQ02 * MathArrays.linearCombination(r[0][0], r[2][0], r[0][1], r[2][1], r[0][2], r[2][2]) -
                                (q1Dot * q1Dot + q2Dot * q2Dot + q3Dot * q3Dot);
        final double q1DotDot = MathArrays.linearCombination(oPQ0, r[2][0], 2 * r[1][0], q0Dot, r[0][0], q0DotDot);
        final double q2DotDot = MathArrays.linearCombination(oPQ0, r[2][1], 2 * r[1][1], q0Dot, r[0][1], q0DotDot);
        final double q3DotDot = MathArrays.linearCombination(oPQ0, r[2][2], 2 * r[1][2], q0Dot, r[0][2], q0DotDot);
        final double oXDot    = 2 * MathArrays.linearCombination(-q1, q0DotDot,  q0, q1DotDot,  q3, q2DotDot, -q2, q3DotDot);
        final double oYDot    = 2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot,  q0, q2DotDot,  q1, q3DotDot);
        final double oZDot    = 2 * MathArrays.linearCombination(-q3, q0DotDot,  q2, q1DotDot, -q1, q2DotDot,  q0, q3DotDot);

        return new TimeStampedAngularCoordinates(offset.getDate(),
                                                 new Rotation(q0, q1, q2, q3, false),
                                                 new Vector3D(oX, oY, oZ),
                                                 new Vector3D(oXDot, oYDot, oZDot)).addOffset(offset);

    }

}
