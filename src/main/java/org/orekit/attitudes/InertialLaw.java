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
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.Transform;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/**
 * This class handles an inertial attitude law.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class InertialLaw implements AttitudeLaw {

    /** Dummy attitude law, perfectly aligned with the J<sub>2000</sub> frame. */
    public static final InertialLaw J2000_ALIGNED =
        new InertialLaw(Rotation.IDENTITY);

    /** Serializable UID. */
    private static final long serialVersionUID = -8661629460150215557L;

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     * @param rotation rotation from J2000 to the desired satellite frame
     */
    public InertialLaw(final Rotation rotation) {
        satelliteFrame = new Frame(Frame.getJ2000(), new Transform(rotation), null);
    }

    /** {@inheritDoc} */
    public Attitude getState(final AbsoluteDate date,
                             final PVCoordinates pv, final Frame frame)
        throws OrekitException {
        final Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(frame, t.getRotation(), t.getRotationRate());
    }

}
