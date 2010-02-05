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
import org.orekit.frames.Frame;


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

public class Attitude implements Serializable {

    /** Serializable UID. */
    private static final long serialVersionUID = 5729155542976428027L;

    /** Reference frame. */
    private final Frame referenceFrame;

     /** Attitude defined by a rotation. */
    private final Rotation attitude;

    /** Spin (spin axis AND velocity).  */
    private final Vector3D spin;

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

    /** Creates a new instance.
     * @param referenceFrame reference frame from which attitude is defined
     * @param attitude rotation between reference frame and satellite frame
     * @param spin satellite spin (axis and velocity, in <strong>satellite</strong> frame)
     */
    public Attitude(final Frame referenceFrame, final Rotation attitude, final Vector3D spin) {
        this.referenceFrame = referenceFrame;
        this.attitude       = attitude;
        this.spin           = spin;
    }

    /** Time-shift the attitude.
     * <p>
     * The state can be slightly shifted to close dates. This shift is based on
     * a linear extrapolation for attitude taking the spin rate into account.
     * It is <em>not</em> intended as a replacement for proper attitude propagation
     * but should be sufficient for either small time shifts or coarse accuracy.
     * </p>
     * @param dt time shift in seconds
     * @return shifted attitude
     */
    public Attitude shift(final double dt) {
        final double rate = spin.getNorm();
        if (rate == 0.0) {
            // special case for inertial attitudes
            return this;
        }

        // BEWARE: there is really a minus sign here, because if
        // the satellite frame rotate in one direction, the inertial vectors
        // seem to rotate in the opposite direction
        final Rotation evolution = new Rotation(spin, -rate * dt);

        return new Attitude(referenceFrame, evolution.applyTo(attitude), spin);

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
