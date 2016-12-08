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
package org.orekit.attitudes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.FieldAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


/** This class handles attitude definition at a given date.

 * <p>This class represents the rotation between a reference frame and
 * the satellite frame, as well as the spin of the satellite (axis and
 * rotation rate).</p>
 * <p>
 * The state can be slightly shifted to close dates. This shift is based on
 * a linear extrapolation for attitude taking the spin rate into account.
 * It is <em>not</em> intended as a replacement for proper attitude propagation
 * but should be sufficient for either small time shifts or coarse accuracy.
 * </p>
 * <p>The instance <code>Attitude</code> is guaranteed to be immutable.</p>
 * @see     org.orekit.orbits.Orbit
 * @see AttitudeProvider
 * @author V&eacute;ronique Pommier-Maurussane
 */

public class FieldAttitude<T extends RealFieldElement<T>> {


    /** Reference frame. */
    private final Frame referenceFrame;

     /** Attitude and spin.  */
    private final TimeStampedFieldAngularCoordinates<T> orientation;

    /** Creates a new instance.
     * @param referenceFrame reference frame from which attitude is defined
     * @param orientation complete orientation between reference frame and satellite frame,
     * including rotation rate
     */
    public FieldAttitude(final Frame referenceFrame, final TimeStampedFieldAngularCoordinates<T> orientation) {
        this.referenceFrame = referenceFrame;
        this.orientation    = orientation;
    }

    /** Creates a new instance.
     * @param date date at which attitude is defined
     * @param referenceFrame reference frame from which attitude is defined
     * @param orientation complete orientation between reference frame and satellite frame,
     * including rotation rate
     */
    public FieldAttitude(final FieldAbsoluteDate<T> date, final Frame referenceFrame,
                    final FieldAngularCoordinates<T> orientation) {
        this(referenceFrame,
             new TimeStampedFieldAngularCoordinates<T>(date,
                                               orientation.getRotation(),
                                               orientation.getRotationRate(),
                                               orientation.getRotationAcceleration()));
    }

    /** Creates a new instance.
     * @param date date at which attitude is defined
     * @param referenceFrame reference frame from which attitude is defined
     * @param attitude rotation between reference frame and satellite frame
     * @param spin satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     * @param acceleration satellite rotation acceleration (in <strong>satellite</strong> frame)
     */
    public FieldAttitude(final FieldAbsoluteDate<T> date, final Frame referenceFrame,
                    final FieldRotation<T> attitude, final FieldVector3D<T> spin, final FieldVector3D<T> acceleration) {
        this(referenceFrame, new TimeStampedFieldAngularCoordinates<T>(date, attitude, spin, acceleration));
    }
    /** Creates a new instance.
     * @param date date at which attitude is defined
     * @param referenceFrame reference frame from which attitude is defined
     * @param attitude rotation between reference frame and satellite frame
     * @param spin satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     * @param acceleration satellite rotation acceleration (in <strong>satellite</strong> frame)
     * @param field field used by default
     */
    public FieldAttitude(final FieldAbsoluteDate<T> date, final Frame referenceFrame,
                    final Rotation attitude, final Vector3D spin, final Vector3D acceleration, final Field<T> field) {
        this(referenceFrame, new TimeStampedFieldAngularCoordinates<T>(date,
                              new FieldRotation<T>(field.getZero().add(attitude.getQ0()),
                                                  field.getZero().add(attitude.getQ1()),
                                                  field.getZero().add(attitude.getQ2()),
                                                  field.getZero().add(attitude.getQ3()), true),
                              new FieldVector3D<T>(field.getZero().add(spin.getX()),
                                                  field.getZero().add(spin.getY()),
                                                  field.getZero().add(spin.getZ())),
                              new FieldVector3D<T>(field.getZero().add(acceleration.getX()),
                                                  field.getZero().add(acceleration.getY()),
                                                  field.getZero().add(acceleration.getZ()))));
    }


    /** Get a time-shifted attitude.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a linear extrapolation for attitude taking the spin rate into account.
     * It is <em>not</em> intended as a replacement for proper attitude propagation
     * but should be sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return a new attitude, shifted with respect to the instance (which is immutable)
     */
    public FieldAttitude<T> shiftedBy(final T dt) {
        return new FieldAttitude<T>(referenceFrame, orientation.shiftedBy(dt));
    }

    /** Get a similar attitude with a specific reference frame.
     * <p>
     * If the instance reference frame is already the specified one, the instance
     * itself is returned without any object creation. Otherwise, a new instance
     * will be created with the specified reference frame. In this case, the
     * required intermediate rotation and spin between the specified and the
     * original reference frame will be inserted.
     * </p>
     * @param newReferenceFrame desired reference frame for attitude
     * @return an attitude that has the same orientation and motion as the instance,
     * but guaranteed to have the specified reference frame
     * @exception OrekitException if conversion between reference frames fails
     */
    public FieldAttitude<T> withReferenceFrame(final Frame newReferenceFrame)
        throws OrekitException {

        if (newReferenceFrame == referenceFrame) {
            // simple case, the instance is already compliant
            return this;
        }

        // we have to take an intermediate rotation into account
        final Transform t = newReferenceFrame.getTransformTo(referenceFrame, orientation.getDate().toAbsoluteDate());
        return new FieldAttitude<T>(orientation.getDate(), newReferenceFrame,
                            orientation.getRotation().compose(t.getRotation(), RotationConvention.VECTOR_OPERATOR),
                            orientation.getRotationRate().add(orientation.getRotation().applyTo(t.getRotationRate())),
                            orientation.getRotationAcceleration().add(orientation.getRotation().applyTo(t.getRotationAcceleration())));

    }

    /** Get the date of attitude parameters.
     * @return date of the attitude parameters
     */
    public FieldAbsoluteDate<T> getDate() {
        return orientation.getDate();
    }

    /** Get the reference frame.
     * @return referenceFrame reference frame from which attitude is defined.
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** Get the complete orientation including spin.
     * @return complete orientation including spin
     * @see #getRotation()
     * @see #getSpin()
     */
    public TimeStampedFieldAngularCoordinates<T> getOrientation() {
        return orientation;
    }

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     * @see #getOrientation()
     * @see #getSpin()
     */
    public FieldRotation<T> getRotation() {
        return orientation.getRotation();
    }

    /** Get the satellite spin.
     * <p>The spin vector is defined in <strong>satellite</strong> frame.</p>
     * @return spin satellite spin (axis and velocity).
     * @see #getOrientation()
     * @see #getRotation()
     */
    public FieldVector3D<T> getSpin() {
        return orientation.getRotationRate();
    }

    /** Get the satellite rotation acceleration.
     * <p>The rotation acceleration. vector is defined in <strong>satellite</strong> frame.</p>
     * @return rotation acceleration
     * @see #getOrientation()
     * @see #getRotation()
     */
    public FieldVector3D<T> getRotationAcceleration() {
        return orientation.getRotationAcceleration();
    }

    /** Get an interpolated instance.
     * <p>
     * The interpolated instance is created by polynomial Hermite interpolation
     * on Rodrigues vector ensuring rotation rate remains the exact derivative of rotation.
     * </p>
     * <p>
     * As this implementation of interpolation is polynomial, it should be used only
     * with small samples (about 10-20 points) in order to avoid <a
     * href="http://en.wikipedia.org/wiki/Runge%27s_phenomenon">Runge's phenomenon</a>
     * and numerical problems (including NaN appearing).
     * </p>
     * @param interpolationDate interpolation date
     * @param sample sample points on which interpolation should be done
     * @return a new instance, interpolated at specified date
     * @exception OrekitException if the number of point is too small for interpolating
     */
    public FieldAttitude<T> interpolate(final FieldAbsoluteDate<T> interpolationDate, final Collection<FieldAttitude<T>> sample)
        throws OrekitException {
        final List<TimeStampedFieldAngularCoordinates<T>> datedPV =
                new ArrayList<TimeStampedFieldAngularCoordinates<T>>(sample.size());
        for (final FieldAttitude<T> attitude : sample) {
            datedPV.add(attitude.orientation);
        }
        final TimeStampedFieldAngularCoordinates<T> interpolated =
                TimeStampedFieldAngularCoordinates.interpolate(interpolationDate, AngularDerivativesFilter.USE_RR, datedPV);
        return new FieldAttitude<T>(referenceFrame, interpolated);
    }
    /**
     * Converts to an Attitude instance.
     * @return Attitude with same properties
     */
    public Attitude toAttitude() {
        return new Attitude(orientation.getDate().toAbsoluteDate(), referenceFrame, orientation.toAngularCoordinates());
    }

}
