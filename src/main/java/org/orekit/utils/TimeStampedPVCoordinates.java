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
package org.orekit.utils;

import java.io.Serializable;
import java.util.Collection;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.analysis.interpolation.HermiteInterpolator;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link PVCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedPVCoordinates extends PVCoordinates implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140723L;

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a TimeStampedPVCoordinates pair.
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     * @param acceleration the acceleration vector (m/s²)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final Vector3D position, final Vector3D velocity, final Vector3D acceleration) {
        super(position, velocity, acceleration);
        this.date = date;
    }

    /**
     * Build from position and velocity. Acceleration is set to zero.
     *
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final Vector3D position,
                                    final Vector3D velocity) {
        this(date, position, velocity, Vector3D.ZERO);
    }

    /**
     * Build from position velocity acceleration coordinates.
     *
     * @param date coordinates date
     * @param pv position velocity, and acceleration coordinates, in meters and seconds.
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date, final PVCoordinates pv) {
        this(date, pv.getPosition(), pv.getVelocity(), pv.getAcceleration());
    }

    /** Multiplicative constructor
     * <p>Build a TimeStampedPVCoordinates from another one and a scale factor.</p>
     * <p>The TimeStampedPVCoordinates built will be a * pv</p>
     * @param date date of the built coordinates
     * @param a scale factor
     * @param pv base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a, final PVCoordinates pv) {
        super(new Vector3D(a, pv.getPosition()),
              new Vector3D(a, pv.getVelocity()),
              new Vector3D(a, pv.getAcceleration()));
        this.date = date;
    }

    /** Subtractive constructor
     * <p>Build a relative TimeStampedPVCoordinates from a start and an end position.</p>
     * <p>The TimeStampedPVCoordinates built will be end - start.</p>
     * @param date date of the built coordinates
     * @param start Starting PVCoordinates
     * @param end ending PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final PVCoordinates start, final PVCoordinates end) {
        super(end.getPosition().subtract(start.getPosition()),
              end.getVelocity().subtract(start.getVelocity()),
              end.getAcceleration().subtract(start.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from two other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2) {
        super(new Vector3D(a1, pv1.getPosition(),     a2, pv2.getPosition()),
              new Vector3D(a1, pv1.getVelocity(),     a2, pv2.getVelocity()),
              new Vector3D(a1, pv1.getAcceleration(), a2, pv2.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from three other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2,
                                    final double a3, final PVCoordinates pv3) {
        super(new Vector3D(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition()),
              new Vector3D(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity()),
              new Vector3D(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration()));
        this.date = date;
    }

    /** Linear constructor
     * <p>Build a TimeStampedPVCoordinates from four other ones and corresponding scale factors.</p>
     * <p>The TimeStampedPVCoordinates built will be a1 * u1 + a2 * u2 + a3 * u3 + a4 * u4</p>
     * @param date date of the built coordinates
     * @param a1 first scale factor
     * @param pv1 first base (unscaled) PVCoordinates
     * @param a2 second scale factor
     * @param pv2 second base (unscaled) PVCoordinates
     * @param a3 third scale factor
     * @param pv3 third base (unscaled) PVCoordinates
     * @param a4 fourth scale factor
     * @param pv4 fourth base (unscaled) PVCoordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final double a1, final PVCoordinates pv1,
                                    final double a2, final PVCoordinates pv2,
                                    final double a3, final PVCoordinates pv3,
                                    final double a4, final PVCoordinates pv4) {
        super(new Vector3D(a1, pv1.getPosition(),     a2, pv2.getPosition(),     a3, pv3.getPosition(),     a4, pv4.getPosition()),
              new Vector3D(a1, pv1.getVelocity(),     a2, pv2.getVelocity(),     a3, pv3.getVelocity(),     a4, pv4.getVelocity()),
              new Vector3D(a1, pv1.getAcceleration(), a2, pv2.getAcceleration(), a3, pv3.getAcceleration(), a4, pv4.getAcceleration()));
        this.date = date;
    }

    /** Builds a TimeStampedPVCoordinates triplet from  a {@link FieldVector3D}&lt;{@link DerivativeStructure}&gt;.
     * <p>
     * The vector components must have time as their only derivation parameter and
     * have consistent derivation orders.
     * </p>
     * @param date date of the built coordinates
     * @param p vector with time-derivatives embedded within the coordinates
     */
    public TimeStampedPVCoordinates(final AbsoluteDate date,
                                    final FieldVector3D<DerivativeStructure> p) {
        super(p);
        this.date = date;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple Taylor expansion. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedPVCoordinates shiftedBy(final double dt) {
        final PVCoordinates spv = super.shiftedBy(dt);
        return new TimeStampedPVCoordinates(date.shiftedBy(dt),
                                            spv.getPosition(), spv.getVelocity(), spv.getAcceleration());
    }

    /** Create a local provider using simply Taylor expansion through {@link #shiftedBy(double)}.
     * <p>
     * The time evolution is based on a simple Taylor expansion. It is <em>not</em> intended as a
     * replacement for proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param instanceFrame frame in which the instance is defined
     * @return provider based on Taylor expansion, for small time shifts around instance date
     */
    public PVCoordinatesProvider toTaylorProvider(final Frame instanceFrame) {
        return new PVCoordinatesProvider() {
            /** {@inheritDoc} */
            public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate d,  final Frame f)
                throws OrekitException {
                final TimeStampedPVCoordinates shifted   = shiftedBy(d.durationFrom(date));
                final Transform                transform = instanceFrame.getTransformTo(f, d);
                return transform.transformPVCoordinates(shifted);
            }
        };
    }

    /** Interpolate position-velocity.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * ensuring velocity remains the exact derivative of position.
     * </p>
     * <p>
     * Note that even if first time derivatives (velocities)
     * from sample can be ignored, the interpolated instance always includes
     * interpolated derivatives. This feature can be used explicitly to
     * compute these derivatives when it would be too complex to compute them
     * from an analytical formula: just compute a few sample points from the
     * explicit formula and set the derivatives to zero in these sample points,
     * then use interpolation to add derivatives consistent with the positions.
     * </p>
     * @param date interpolation date
     * @param filter filter for derivatives from the sample to use in interpolation
     * @param sample sample points on which interpolation should be done
     * @return a new position-velocity, interpolated at specified date
     */
    public static TimeStampedPVCoordinates interpolate(final AbsoluteDate date,
                                                       final CartesianDerivativesFilter filter,
                                                       final Collection<TimeStampedPVCoordinates> sample) {

        // set up an interpolator taking derivatives into account
        final HermiteInterpolator interpolator = new HermiteInterpolator();

        // add sample points
        switch (filter) {
            case USE_P :
                // populate sample with position data, ignoring velocity
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position = pv.getPosition();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                new double[] {
                                                              position.getX(), position.getY(), position.getZ()
                                                });
                }
                break;
            case USE_PV :
                // populate sample with position and velocity data
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position = pv.getPosition();
                    final Vector3D velocity = pv.getVelocity();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                new double[] {
                                                    position.getX(), position.getY(), position.getZ()
                                                }, new double[] {
                                                    velocity.getX(), velocity.getY(), velocity.getZ()
                                                });
                }
                break;
            case USE_PVA :
                // populate sample with position, velocity and acceleration data
                for (final TimeStampedPVCoordinates pv : sample) {
                    final Vector3D position     = pv.getPosition();
                    final Vector3D velocity     = pv.getVelocity();
                    final Vector3D acceleration = pv.getAcceleration();
                    interpolator.addSamplePoint(pv.getDate().durationFrom(date),
                                                new double[] {
                                                    position.getX(),     position.getY(),     position.getZ()
                                                }, new double[] {
                                                    velocity.getX(),     velocity.getY(),     velocity.getZ()
                                                }, new double[] {
                                                    acceleration.getX(), acceleration.getY(), acceleration.getZ()
                                                });
                }
                break;
            default :
                // this should never happen
                throw new OrekitInternalError(null);
        }

        // interpolate
        final DerivativeStructure zero = new DerivativeStructure(1, 2, 0, 0.0);
        final DerivativeStructure[] p  = interpolator.value(zero);

        // build a new interpolated instance
        return new TimeStampedPVCoordinates(date,
                                            new Vector3D(p[0].getValue(),
                                                         p[1].getValue(),
                                                         p[2].getValue()),
                                            new Vector3D(p[0].getPartialDerivative(1),
                                                         p[1].getPartialDerivative(1),
                                                         p[2].getPartialDerivative(1)),
                                            new Vector3D(p[0].getPartialDerivative(2),
                                                         p[1].getPartialDerivative(2),
                                                         p[2].getPartialDerivative(2)));

    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append(date).append(", P(").
                                  append(getPosition().getX()).append(comma).
                                  append(getPosition().getY()).append(comma).
                                  append(getPosition().getZ()).append("), V(").
                                  append(getVelocity().getX()).append(comma).
                                  append(getVelocity().getY()).append(comma).
                                  append(getVelocity().getZ()).append("), A(").
                                  append(getAcceleration().getX()).append(comma).
                                  append(getAcceleration().getY()).append(comma).
                                  append(getAcceleration().getZ()).append(")}").toString();
    }

    /** Replace the instance with a data transfer object for serialization.
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DTO(this);
    }

    /** Internal class used only for serialization. */
    private static class DTO implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20140723L;

        /** Double values. */
        private double[] d;

        /** Simple constructor.
         * @param pv instance to serialize
         */
        private DTO(final TimeStampedPVCoordinates pv) {

            // decompose date
            final double epoch  = FastMath.floor(pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH));
            final double offset = pv.getDate().durationFrom(AbsoluteDate.J2000_EPOCH.shiftedBy(epoch));

            this.d = new double[] {
                epoch, offset,
                pv.getPosition().getX(),     pv.getPosition().getY(),     pv.getPosition().getZ(),
                pv.getVelocity().getX(),     pv.getVelocity().getY(),     pv.getVelocity().getZ(),
                pv.getAcceleration().getX(), pv.getAcceleration().getY(), pv.getAcceleration().getZ()
            };

        }

        /** Replace the deserialized data transfer object with a {@link TimeStampedPVCoordinates}.
         * @return replacement {@link TimeStampedPVCoordinates}
         */
        private Object readResolve() {
            return new TimeStampedPVCoordinates(AbsoluteDate.J2000_EPOCH.shiftedBy(d[0]).shiftedBy(d[1]),
                                                new Vector3D(d[2], d[3], d[ 4]),
                                                new Vector3D(d[5], d[6], d[ 7]),
                                                new Vector3D(d[8], d[9], d[10]));
        }

    }

}
