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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles a spin stabilized attitude provider.
 * <p>Spin stabilized laws are handled as wrappers for an underlying
 * non-rotating law. This underlying law is typically an instance
 * of {@link CelestialBodyPointed} with the pointing axis equal to
 * the rotation axis, but can in fact be anything.</p>
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class SpinStabilized implements AttitudeProviderModifier {

    /** Serializable UID. */
    private static final long serialVersionUID = -7025790361794748354L;

    /** Underlying non-rotating attitude provider.  */
    private final AttitudeProvider nonRotatingLaw;

    /** Start date of the rotation. */
    private final AbsoluteDate start;

    /** Rotation axis in satellite frame. */
    private final Vector3D axis;

    /** Spin rate in radians per seconds. */
    private final double rate;

    /** Spin vector. */
    private final Vector3D spin;

    /** Creates a new instance.
     * @param nonRotatingLaw underlying non-rotating attitude provider
     * @param start start date of the rotation
     * @param axis rotation axis in satellite frame
     * @param rate spin rate in radians per seconds
     */
    public SpinStabilized(final AttitudeProvider nonRotatingLaw,
                          final AbsoluteDate start,
                          final Vector3D axis, final double rate) {
        this.nonRotatingLaw = nonRotatingLaw;
        this.start          = start;
        this.axis           = axis;
        this.rate           = rate;
        this.spin           = new Vector3D(rate / axis.getNorm(), axis);
    }

    /** {@inheritDoc} */
    public AttitudeProvider getUnderlyingAttitudeProvider() {
        return nonRotatingLaw;
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {

        // get attitude from underlying non-rotating law
        final Attitude base = nonRotatingLaw.getAttitude(pvProv, date, frame);
        final Transform baseTransform = new Transform(date, base.getOrientation());

        // compute spin transform due to spin from reference to current date
        final Transform spinInfluence =
            new Transform(date,
                          new Rotation(axis,
                                       rate * date.durationFrom(start),
                                       RotationConvention.FRAME_TRANSFORM),
                          spin);

        // combine the two transforms
        final Transform combined = new Transform(date, baseTransform, spinInfluence);

        // build the attitude
        return new Attitude(date, frame,
                            combined.getRotation(), combined.getRotationRate(), combined.getRotationAcceleration());

    }

}
