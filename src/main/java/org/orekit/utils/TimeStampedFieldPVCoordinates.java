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
import org.apache.commons.math3.geometry.euclidean.threed.FieldVector3D;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;

/** {@link TimeStamped time-stamped} version of {@link FieldPVCoordinates}.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @param <T> the type of the field elements
 * @author Luc Maisonobe
 * @since 7.0
 */
public class TimeStampedFieldPVCoordinates<T extends RealFieldElement<T>>
    extends FieldPVCoordinates<T> implements TimeStamped {

    /** Serializable UID. */
    private static final long serialVersionUID = 20140611L;

    /** The date. */
    private final AbsoluteDate date;

    /** Builds a PVCoordinates pair.
     * @param date coordinates date
     * @param position the position vector (m)
     * @param velocity the velocity vector (m/s)
     */
    public TimeStampedFieldPVCoordinates(final AbsoluteDate date,
                                         final FieldVector3D<T> position, final FieldVector3D<T> velocity) {
        super(position, velocity);
        this.date = date;
    }

    /** {@inheritDoc} */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get a time-shifted state.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a simple linear model. It is <em>not</em> intended as a replacement for
     * proper orbit propagation (it is not even Keplerian!) but should be sufficient
     * for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new state, shifted with respect to the instance (which is immutable)
     */
    public TimeStampedFieldPVCoordinates<T> shiftedBy(final double dt) {
        return new TimeStampedFieldPVCoordinates<T>(date.shiftedBy(dt),
                                                    new FieldVector3D<T>(1, getPosition(), dt, getVelocity()),
                                                    getVelocity());
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
     * @param useVelocities if true, use sample points velocities,
     * otherwise ignore them and use only positions
     * @param sample sample points on which interpolation should be done
     * @param <T> the type of the field elements
     * @return a new position-velocity, interpolated at specified date
     */
    @SuppressWarnings("unchecked")
    public static <T extends RealFieldElement<T>> TimeStampedFieldPVCoordinates<T>
    interpolate(final AbsoluteDate date, final boolean useVelocities,
                    final Collection<TimeStampedFieldPVCoordinates<T>> sample) {

        // get field properties
        final T prototype = sample.iterator().next().getPosition().getX();
        final T zero      = prototype.getField().getZero();

        // set up an interpolator taking derivatives into account
        final FieldHermiteInterpolator<T> interpolator = new FieldHermiteInterpolator<T>();

        // add sample points
        if (useVelocities) {
            // populate sample with position and velocity data
            for (final TimeStampedFieldPVCoordinates<T> datedPV : sample) {
                final FieldVector3D<T> position = datedPV.getPosition();
                final FieldVector3D<T> velocity = datedPV.getVelocity();
                interpolator.addSamplePoint(zero.add(datedPV.getDate().durationFrom(date)),
                                            position.toArray(), velocity.toArray());
            }
        } else {
            // populate sample with position data, ignoring velocity
            for (final TimeStampedFieldPVCoordinates<T> datedPV : sample) {
                final FieldVector3D<T> position = datedPV.getPosition();
                interpolator.addSamplePoint(zero.add(datedPV.getDate().durationFrom(date)),
                                            position.toArray());
            }
        }

        // interpolate
        final T[][] p = interpolator.derivatives(zero, 1);

        // build a new interpolated instance
        return new TimeStampedFieldPVCoordinates<T>(date,
                                                    new FieldVector3D<T>(p[0]),
                                                    new FieldVector3D<T>(p[1]));

    }

    /** Return a string representation of this position/velocity pair.
     * @return string representation of this position/velocity pair
     */
    public String toString() {
        final String comma = ", ";
        return new StringBuffer().append('{').append(date).append(", P(").
                                  append(getPosition().getX().getReal()).append(comma).
                                  append(getPosition().getY().getReal()).append(comma).
                                  append(getPosition().getZ().getReal()).append("), V(").
                                  append(getVelocity().getX().getReal()).append(comma).
                                  append(getVelocity().getY().getReal()).append(comma).
                                  append(getVelocity().getZ().getReal()).append(")}").toString();
    }

}
