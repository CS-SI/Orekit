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

import org.apache.commons.math.geometry.Rotation;
import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Transform;
import org.orekit.orbits.Orbit;
import org.orekit.time.AbsoluteDate;


/**
 * This class handles an inertial attitude law.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public class InertialLaw implements AttitudeLaw {


    /** Dummy attitude law, perfectly aligned with the EME2000 frame. */
    public static final InertialLaw EME2000_ALIGNED =
        new InertialLaw(Rotation.IDENTITY);

    /** Serializable UID. */
    private static final long serialVersionUID = -7550347669304660626L;

    /** Fixed satellite frame. */
    private final Frame satelliteFrame;

    /** Creates new instance.
     * @param rotation rotation from EME2000 to the desired satellite frame
     */
    public InertialLaw(final Rotation rotation) {
        satelliteFrame =
            new Frame(FramesFactory.getEME2000(), new Transform(rotation), null, false);
    }

    /** {@inheritDoc} */
    public Attitude getAttitude(final Orbit orbit)
        throws OrekitException {
        final AbsoluteDate date = orbit.getDate();
        final Frame frame = orbit.getFrame();
        final Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(date, frame, t.getRotation(), t.getRotationRate());
    }

}
