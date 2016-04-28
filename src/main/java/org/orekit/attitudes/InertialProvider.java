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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinatesProvider;


/**
 * This class handles an inertial attitude provider.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 */
public class InertialProvider implements AttitudeProvider {


    /** Dummy attitude provider, perfectly aligned with the EME2000 frame. */
    public static final InertialProvider EME2000_ALIGNED =
        new InertialProvider(Rotation.IDENTITY);

    /** Serializable UID. */
    private static final long serialVersionUID = -818658655669855332L;

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     * @param rotation rotation from EME2000 to the desired satellite frame
     */
    public InertialProvider(final Rotation rotation) {
        satelliteFrame =
            new Frame(FramesFactory.getEME2000(), new Transform(AbsoluteDate.J2000_EPOCH, rotation),
                      null, false);
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final PVCoordinatesProvider pvProv,
                                final AbsoluteDate date, final Frame frame)
        throws OrekitException {
        final Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(date, frame, t.getRotation(), t.getRotationRate(), t.getRotationAcceleration());
    }

}
