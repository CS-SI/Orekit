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

import java.io.Serializable;
import java.util.Collection;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.analysis.interpolation.HermiteInterpolator;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.nonstiff.LutherIntegrator;
import org.apache.commons.math3.ode.nonstiff.RungeKuttaIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for rotation/rotation rate pairs.
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * an approximate solution of the fixed acceleration motion. It is <em>not</em>
 * intended as a replacement for proper attitude propagation but should be
 * sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>
 * This class is the angular counterpart to {@link PVCoordinates}.
 * </p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class AngularCoordinates implements TimeShiftable<AngularCoordinates>, Serializable {

    /** Fixed orientation parallel with reference frame (identity rotation, zero rotation rate and acceleration). */
    public static final AngularCoordinates IDENTITY =
            new AngularCoordinates(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);

    /** Serializable UID. */
    private static final long serialVersionUID = 20140414L;

    /** Integrator used for shifting instances. */
    private static final RungeKuttaIntegrator LUTHER = new LutherIntegrator(Double.NaN);

    /** Rotation. */
    private final Rotation rotation;

    /** Rotation rate. */
    private final Vector3D rotationRate;

    /** Rotation acceleration. */
    private final Vector3D rotationAcceleration;

    /** Simple constructor.
     * <p> Sets the Coordinates to default : Identity, Ω = (0 0 0), dΩ/dt = (0 0 0).</p>
     */
    public AngularCoordinates() {
        this(Rotation.IDENTITY, Vector3D.ZERO, Vector3D.ZERO);
    }

    /** Builds a rotation/rotation rate pair.
     * @param rotation rotation
     * @param rotationRate rotation rate (rad/s)
     */
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate) {
        this(rotation, rotationRate, Vector3D.ZERO);
    }

    /** Builds a rotation/rotation rate/rotation acceleration triplet.
     * @param rotation rotation
     * @param rotationRate rotation rate Ω (rad/s)
     * @param rotationAcceleration rotation acceleration dΩ/dt (rad²/s²)
     */
    public AngularCoordinates(final Rotation rotation, final Vector3D rotationRate, final Vector3D rotationAcceleration) {
        this.rotation             = rotation;
        this.rotationRate         = rotationRate;
        this.rotationAcceleration = rotationAcceleration;
    }

    /** Estimate rotation rate between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two orientations.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return rotation rate allowing to go from start to end orientations
     */
    public static Vector3D estimateRate(final Rotation start, final Rotation end, final double dt) {
        final Rotation evolution = start.applyTo(end.revert());
        return new Vector3D(evolution.getAngle() / dt, evolution.getAxis());
    }

    /** Revert a rotation/rotation rate/ rotation acceleration triplet.
     * Build a triplet which reverse the effect of another triplet.
     * @return a new triplet whose effect is the reverse of the effect
     * of the instance
     */
    public AngularCoordinates revert() {
        return new AngularCoordinates(rotation.revert(),
                                      rotation.applyInverseTo(rotationRate.negate()),
                                      rotation.applyInverseTo(rotationAcceleration.negate()));
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * an approximate solution of the fixed acceleration motion. It is <em>not</em>
     * intended as a replacement for proper attitude propagation but should be
     * sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public AngularCoordinates shiftedBy(final double dt) {

        if (rotationRate.getNormSq() == 0.0 && rotationAcceleration.getNormSq() == 0.0) {
            // special case for fixed rotations
            return this;
        }

        // perform a few steps of 6th order Luther integrator with large step sizes
        final FirstOrderDifferentialEquations ode = new FirstOrderDifferentialEquations() {

            /** {@inheritDoc} */
            @Override
            public int getDimension() {
                return 4;
            }

            /** {@inheritDoc} */
            @Override
            public void computeDerivatives(final double t, final double[] q, final double[] qDot) {
                final double omegaX = rotationRate.getX() + t * rotationAcceleration.getX();
                final double omegaY = rotationRate.getY() + t * rotationAcceleration.getY();
                final double omegaZ = rotationRate.getZ() + t * rotationAcceleration.getZ();
                qDot[0] = 0.5 * MathArrays.linearCombination(-q[1], omegaX, -q[2], omegaY, -q[3], omegaZ);
                qDot[1] = 0.5 * MathArrays.linearCombination( q[0], omegaX, -q[3], omegaY,  q[2], omegaZ);
                qDot[2] = 0.5 * MathArrays.linearCombination( q[3], omegaX,  q[0], omegaY, -q[1], omegaZ);
                qDot[3] = 0.5 * MathArrays.linearCombination(-q[2], omegaX,  q[1], omegaY,  q[0], omegaZ);
            }

        };

        double[] q = new double[] {
            rotation.getQ0(), rotation.getQ1(), rotation.getQ2(), rotation.getQ3()
        };

        // limit single steps to (large) π/12 angles
        final double dtMax = FastMath.PI / (12 * rotationRate.getNorm());
        final int    n     = FastMath.max(1, (int) FastMath.ceil(dt / dtMax));
        final double h     = dt / n;
        for (int i = 0; i < n; ++i) {
            q = LUTHER.singleStep(ode, i * h, q, (i + 1) * h);
        }

        return new AngularCoordinates(new Rotation(q[0], q[1], q[2], q[3], true),
                                      new Vector3D(1, rotationRate, dt, rotationAcceleration),
                                      rotationAcceleration);
    }

    /** Get the rotation.
     * @return the rotation.
     */
    public Rotation getRotation() {
        return rotation;
    }

    /** Get the rotation rate.
     * @return the rotation rate vector Ω (rad/s).
     */
    public Vector3D getRotationRate() {
        return rotationRate;
    }

    /** Get the rotation acceleration.
     * @return the rotation acceleration vector dΩ/dt (rad²/s²).
     */
    public Vector3D getRotationAcceleration() {
        return rotationAcceleration;
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
    public AngularCoordinates addOffset(final AngularCoordinates offset) {
        return new AngularCoordinates(rotation.applyTo(offset.rotation),
                                      rotationRate.add(rotation.applyTo(offset.rotationRate)),
                                      rotationAcceleration.add(rotation.applyTo(offset.rotationAcceleration)));
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
    public AngularCoordinates subtractOffset(final AngularCoordinates offset) {
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
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     * @deprecated as of 7.1, replaced with {@link #interpolate(AbsoluteDate, RRASampleFilter, Collection)}
     */
    @Deprecated
    public static AngularCoordinates interpolate(final AbsoluteDate date, final boolean useRotationRates,
                                                 final Collection<Pair<AbsoluteDate, AngularCoordinates>> sample)
        throws OrekitException {
        return interpolate(date,
                           useRotationRates ? RRASampleFilter.SAMPLE_R : RRASampleFilter.SAMPLE_RR,
                           sample);
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
     * Note that even if first time derivatives (rotation rates)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the rotations.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives to extract from sample
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public static AngularCoordinates interpolate(final AbsoluteDate date, final RRASampleFilter filter,
                                                 final Collection<Pair<AbsoluteDate, AngularCoordinates>> sample)
        throws OrekitException {

        // set up safety elements for 2π singularity avoidance
        final double epsilon   = 2 * FastMath.PI / sample.size();
        final double threshold = FastMath.min(-(1.0 - 1.0e-4), -FastMath.cos(epsilon / 4));

        // set up a linear offset model canceling mean rotation rate
        final Vector3D meanRate;
        if (filter == RRASampleFilter.SAMPLE_R) {
            if (sample.size() < 2) {
                throw new OrekitException(OrekitMessages.NOT_ENOUGH_DATA_FOR_INTERPOLATION,
                                          sample.size());
            }
            Vector3D sum = Vector3D.ZERO;
            Pair<AbsoluteDate, AngularCoordinates> previous = null;
            for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                if (previous != null) {
                    sum = sum.add(estimateRate(previous.getValue().getRotation(),
                                               datedAC.getValue().getRotation(),
                                               datedAC.getKey().durationFrom(previous.getKey().getDate())));
                }
                previous = datedAC;
            }
            meanRate = new Vector3D(1.0 / (sample.size() - 1), sum);
        } else {
            Vector3D sum = Vector3D.ZERO;
            for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                sum = sum.add(datedAC.getValue().getRotationRate());
            }
            meanRate = new Vector3D(1.0 / sample.size(), sum);
        }
        Rotation bias = Rotation.IDENTITY;

        boolean restart = true;
        for (int i = 0; restart && i < sample.size() + 2; ++i) {

            // offset adaptation parameters
            restart = false;

            // set up an interpolator taking derivatives into account
            final HermiteInterpolator interpolator = new HermiteInterpolator();

            // add sample points
            switch (filter) {
            case SAMPLE_RRA: {
                // populate sample with rotation and rotation rate data
                for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                    final double[] rodrigues = getModifiedRodrigues(datedAC.getValue(),
                                                                    bias, meanRate, datedAC.getKey().durationFrom(date),
                                                                    threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                new double[] {
                                                    rodrigues[0], rodrigues[1], rodrigues[2]
                                                },
                                                new double[] {
                                                    rodrigues[3], rodrigues[4], rodrigues[5]
                                                },
                                                new double[] {
                                                    rodrigues[6], rodrigues[7], rodrigues[8]
                                                });
                }
                break;
            }
            case SAMPLE_RR: {
                // populate sample with rotation and rotation rate data
                for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                    final double[] rodrigues = getModifiedRodrigues(datedAC.getValue(),
                                                                    bias, meanRate, datedAC.getKey().durationFrom(date),
                                                                    threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                new double[] {
                                                    rodrigues[0], rodrigues[1], rodrigues[2]
                                                },
                                                new double[] {
                                                    rodrigues[3], rodrigues[4], rodrigues[5]
                                                });
                }
                break;
            }
            case SAMPLE_R: {
                // populate sample with rotation data only, ignoring rotation rate
                for (final Pair<AbsoluteDate, AngularCoordinates> datedAC : sample) {
                    final double[] rodrigues = getModifiedRodrigues(datedAC.getValue(),
                                                                    bias, meanRate, datedAC.getKey().durationFrom(date),
                                                                    threshold);
                    if (rodrigues == null) {
                        // the sample point is close to a modified Rodrigues vector singularity
                        // we need to change the linear offset model to avoid this
                        restart = true;
                        break;
                    }
                    interpolator.addSamplePoint(datedAC.getKey().getDate().durationFrom(date),
                                                new double[] {
                                                    rodrigues[0], rodrigues[1], rodrigues[2]
                                                });
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
                bias = bias.applyTo(new Rotation(Vector3D.PLUS_I, epsilon));
            } else {
                // interpolation succeeded with the current offset
                final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
                final DerivativeStructure[] p = interpolator.value(zero);
                return createFromModifiedRodrigues(new double[] {
                    p[0].getValue(), p[1].getValue(), p[2].getValue(),
                    p[0].getPartialDerivative(1), p[1].getPartialDerivative(1), p[2].getPartialDerivative(1),
                    p[0].getPartialDerivative(2), p[1].getPartialDerivative(2), p[2].getPartialDerivative(2)
                }, bias, meanRate);
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
     * @param ac coordinates to convert
     * @param bias rotation bias to remove
     * @param meanRate rotation rate to remove
     * @param dateOffset date shift of the linear offset model to remove
     * @param threshold threshold for rotations too close to 2π
     * @return modified Rodrigues vector and derivatives, or null if rotation is too close to 2π
     */
    private static double[] getModifiedRodrigues(final AngularCoordinates ac,
                                                 final Rotation bias, final Vector3D meanRate, final double dateOffset,
                                                 final double threshold) {

        // remove linear offset from the current coordinates
        final double meanRateNorm = meanRate.getNorm();
        final Rotation offset;
        if (meanRateNorm == 0.0) {
            offset = bias;
        } else {
            offset = new Rotation(meanRate, -dateOffset * meanRateNorm).applyTo(bias);
        }
        final Rotation fixedRotation     = ac.getRotation().applyTo(offset.revert());
        final Vector3D fixedRotationRate = ac.getRotationRate().subtract(meanRate);
        final Vector3D fixedAcceleration = ac.getRotationAcceleration();

        // check modified Rodrigues vector singularity
        double q0 = fixedRotation.getQ0();
        double q1 = fixedRotation.getQ1();
        double q2 = fixedRotation.getQ2();
        double q3 = fixedRotation.getQ3();
        if (q0 < threshold && FastMath.abs(dateOffset) * fixedRotationRate.getNorm() > 1.0e-3) {
            // this is an intermediate point that happens to be 2π away from reference
            // we need to change the linear offset model to avoid this point
            return null;
        }

        // make sure all interpolated points will be on the same branch
        if (q0 < 0) {
            q0 = -q0;
            q1 = -q1;
            q2 = -q2;
            q3 = -q3;
        }

        final double oX    = fixedRotationRate.getX();
        final double oY    = fixedRotationRate.getY();
        final double oZ    = fixedRotationRate.getZ();
        final double oXDot = fixedAcceleration.getX();
        final double oYDot = fixedAcceleration.getY();
        final double oZDot = fixedAcceleration.getZ();

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

        return new double[] {
            r1,       r2,       r3,
            r1Dot,    r2Dot,    r3Dot,
            r1DotDot, r2DotDot, r3DotDot
        };

    }

    /** Convert a modified Rodrigues vector and derivatives to angular coordinates.
     * @param r modified Rodrigues vector (with first and second times derivatives)
     * @param bias rotation bias to remove (may be null for no offset)
     * (its date must be consistent with the modified Rodrigues vector)
     * @param meanRate rotation rate to remove (may be null for no offset)
     * @return angular coordinates
     */
    private static AngularCoordinates createFromModifiedRodrigues(final double[] r,
                                                                  final Rotation bias, final Vector3D meanRate) {

        // rotation
        final double rSquared = r[0] * r[0] + r[1] * r[1] + r[2] * r[2];
        final double oPQ0     = 2 / (1 + rSquared);
        final double q0       = oPQ0 - 1;
        final double q1       = oPQ0 * r[0];
        final double q2       = oPQ0 * r[1];
        final double q3       = oPQ0 * r[2];

        // rotation rate
        final double oPQ02    = oPQ0 * oPQ0;
        final double q0Dot    = -oPQ02 * MathArrays.linearCombination(r[0], r[3], r[1], r[4],  r[2], r[5]);
        final double q1Dot    = oPQ0 * r[3] + r[0] * q0Dot;
        final double q2Dot    = oPQ0 * r[4] + r[1] * q0Dot;
        final double q3Dot    = oPQ0 * r[5] + r[2] * q0Dot;
        final double oX       = 2 * MathArrays.linearCombination(-q1, q0Dot,  q0, q1Dot,  q3, q2Dot, -q2, q3Dot);
        final double oY       = 2 * MathArrays.linearCombination(-q2, q0Dot, -q3, q1Dot,  q0, q2Dot,  q1, q3Dot);
        final double oZ       = 2 * MathArrays.linearCombination(-q3, q0Dot,  q2, q1Dot, -q1, q2Dot,  q0, q3Dot);

        // rotation acceleration
        final double q0DotDot = (1 - q0) / oPQ0 * q0Dot * q0Dot -
                                oPQ02 * MathArrays.linearCombination(r[0], r[6], r[1], r[7], r[2], r[8]) -
                                (q1Dot * q1Dot + q2Dot * q2Dot + q3Dot * q3Dot);
        final double q1DotDot = MathArrays.linearCombination(oPQ0, r[6], 2 * r[3], q0Dot, r[0], q0DotDot);
        final double q2DotDot = MathArrays.linearCombination(oPQ0, r[7], 2 * r[4], q0Dot, r[1], q0DotDot);
        final double q3DotDot = MathArrays.linearCombination(oPQ0, r[8], 2 * r[5], q0Dot, r[2], q0DotDot);
        final double oXDot    = 2 * MathArrays.linearCombination(-q1, q0DotDot,  q0, q1DotDot,  q3, q2DotDot, -q2, q3DotDot);
        final double oYDot    = 2 * MathArrays.linearCombination(-q2, q0DotDot, -q3, q1DotDot,  q0, q2DotDot,  q1, q3DotDot);
        final double oZDot    = 2 * MathArrays.linearCombination(-q3, q0DotDot,  q2, q1DotDot, -q1, q2DotDot,  q0, q3DotDot);

        return new AngularCoordinates(new Rotation(q0, q1, q2, q3, false).applyTo(bias),
                                       new Vector3D(oX + meanRate.getX(), oY + meanRate.getY(), oZ + meanRate.getZ()),
                                       new Vector3D(oXDot, oYDot, oZDot));

    }

}
