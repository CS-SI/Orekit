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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.nonstiff.LutherIntegrator;
import org.apache.commons.math3.ode.nonstiff.RungeKuttaIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.apache.commons.math3.util.MathArrays;
import org.apache.commons.math3.util.Pair;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeShiftable;

/** Simple container for rotation/rotation rate/rotation acceleration triplets.
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

        // limit single steps to roughly 1 degree angles
        final double maxRate = rotationRate.getNorm() + FastMath.abs(dt) * rotationAcceleration.getNorm();
        final double dtMax   = 1.0 / (60 * maxRate);
        final int    n       = FastMath.max(1, (int) FastMath.ceil(dt / dtMax));
        final double h       = dt / n;
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
     * @deprecated since 7.0 replaced with {@link TimeStampedAngularCoordinates#interpolate(AbsoluteDate, AngularDerivativesFilter, Collection)}
     */
    @Deprecated
    public static AngularCoordinates interpolate(final AbsoluteDate date, final boolean useRotationRates,
                                                 final Collection<Pair<AbsoluteDate, AngularCoordinates>> sample)
        throws OrekitException {
        final List<TimeStampedAngularCoordinates> list = new ArrayList<TimeStampedAngularCoordinates>(sample.size());
        for (final Pair<AbsoluteDate, AngularCoordinates> pair : sample) {
            list.add(new TimeStampedAngularCoordinates(pair.getFirst(),
                                                       pair.getSecond().getRotation(),
                                                       pair.getSecond().getRotationRate(),
                                                       pair.getSecond().getRotationAcceleration()));
        }
        return TimeStampedAngularCoordinates.interpolate(date,
                                                         useRotationRates ? AngularDerivativesFilter.USE_RR : AngularDerivativesFilter.USE_R,
                                                         list);
    }

}
