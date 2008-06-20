/* Copyright 2002-2008 CS Communication & Systèmes
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

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles a spin stabilized attitude law.
 * <p>Spin stabilized laws are handled as wrappers for an underlying
 * non-rotating law. This underlying law is typically an instance
 * of {@link CelestialBodyPointed} with the pointing axis equal to
 * the rotation axis, but can in fact be anything.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class SpinStabilized implements AttitudeLaw {

    /** Serializable UID. */
    private static final long serialVersionUID = -7025790361794748354L;

    /** Underlying non-rotating attitude law.  */
    private final AttitudeLaw nonRotatingLaw;

    /** Start date of the rotation. */
    private final AbsoluteDate start;

    /** Rotation axis in satellite frame. */
    private final Vector3D axis;

    /** Spin rate in radians per seconds. */
    private final double rate;

    /** Spin vector. */
    private final Vector3D spin;

    /** Creates a new instance.
     * @param nonRotatingLaw underlying non-rotating attitude law
     * @param start start date of the rotation
     * @param axis rotation axis in satellite frame
     * @param rate spin rate in radians per seconds
     */
    public SpinStabilized(final AttitudeLaw nonRotatingLaw,
                          final AbsoluteDate start,
                          final Vector3D axis, final double rate) {
        this.nonRotatingLaw = nonRotatingLaw;
        this.start          = start;
        this.axis           = axis;
        this.rate           = rate;
        this.spin           = new Vector3D(rate / axis.getNorm(), axis);
    }

    /** Get the underlying non-rotating attitude law.
     * @return underlying non-rotating attitude law
     */
    public AttitudeLaw getNonRotatingLaw() {
        return nonRotatingLaw;
    }

    /** {@inheritDoc} */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {

        // get attitude from underlying non-rotating law
        final Attitude base = nonRotatingLaw.getState(date, pv, frame);
        final Transform baseTransform = new Transform(base.getRotation(), base.getSpin());

        // compute spin transform due to spin from reference to current date
        final Transform spinInfluence =
            new Transform(new Rotation(axis, rate * date.minus(start)), spin);

        // combine the two transforms
        final Transform combined = new Transform(baseTransform, spinInfluence);

        // build the attitude
        return new Attitude(frame, combined.getRotation(), combined.getRotationRate());

    }

}
