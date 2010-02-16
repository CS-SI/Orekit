/* Copyright 2002-2010 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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

import java.io.Serializable;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;


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
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */

public class Attitude implements TimeStamped, Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = -947817502698754209L;

    /** Current date. */
    private final AbsoluteDate date;

    /** Reference frame. */
    private final Frame referenceFrame;

     /** Attitude defined by a rotation. */
    private final Rotation attitude;

    /** Spin (spin axis AND velocity).  */
    private final Vector3D spin;

    /** Creates a new instance.
     * @param date date at which attitude is defined
     * @param referenceFrame reference frame from which attitude is defined
     * @param attitude rotation between reference frame and satellite frame
     * @param spin satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     */
    public Attitude(final AbsoluteDate date, final Frame referenceFrame,
                    final Rotation attitude, final Vector3D spin) {
        this.date           = date;
        this.referenceFrame = referenceFrame;
        this.attitude       = attitude;
        this.spin           = spin;
    }

    /** Estimate spin between two orientations.
     * <p>Estimation is based on a simple fixed rate rotation
     * during the time interval between the two attitude.</p>
     * @param start start orientation
     * @param end end orientation
     * @param dt time elapsed between the dates of the two orientations
     * @return spin allowing to go from start to end orientation
     */
    public static Vector3D estimateSpin(final Rotation start, final Rotation end,
                                        final double dt) {
        final Rotation evolution = start.applyTo(end.revert());
        return new Vector3D(evolution.getAngle() / dt, evolution.getAxis());
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
     * @see org.orekit.time.AbsoluteDate#shiftedBy(double)
     * @see org.orekit.utils.PVCoordinates#shiftedBy(double)
     * @see org.orekit.orbits.Orbit#shiftedBy(double)
     * @see org.orekit.propagation.SpacecraftState#shiftedBy(double)
     */
    public Attitude shiftedBy(final double dt) {
        final double rate = spin.getNorm();
        if (rate == 0.0) {
            // special case for inertial attitudes
            return new Attitude(date.shiftedBy(dt), referenceFrame, attitude, spin);
        }

        // BEWARE: there is really a minus sign here, because if
        // the satellite frame rotate in one direction, the inertial vectors
        // seem to rotate in the opposite direction
        final Rotation evolution = new Rotation(spin, -rate * dt);

        return new Attitude(date.shiftedBy(dt), referenceFrame, evolution.applyTo(attitude), spin);

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
    public Attitude withReferenceFrame(final Frame newReferenceFrame)
        throws OrekitException {

        if (newReferenceFrame == referenceFrame) {
            // simple case, the instance is already compliant
            return this;
        }

        // we have to take an intermediate rotation into account
        final Transform t = newReferenceFrame.getTransformTo(referenceFrame, date);
        return new Attitude(date, newReferenceFrame,
                            attitude.applyTo(t.getRotation()),
                            spin.add(attitude.applyTo(t.getRotationRate())));

    }

    /** Get the date of attitude parameters.
     * @return date of the attitude parameters
     */
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get the reference frame.
     * @return referenceFrame reference frame from which attitude is defined.
     */
    public Frame getReferenceFrame() {
        return referenceFrame;
    }

    /** Get the attitude rotation.
     * @return attitude satellite rotation from reference frame.
     */
    public Rotation getRotation() {
        return attitude;
    }

    /** Get the satellite spin.
     * <p>The spin vector is defined in <strong>satellite</strong> frame.</p>
     * @return spin satellite spin (axis and velocity).
     */
    public Vector3D getSpin() {
        return spin;
    }

}
