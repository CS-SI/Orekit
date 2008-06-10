/* Copyright 2002-2008 CS Communication & Systèmes
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.cs.orekit.attitudes;

import org.apache.commons.math.geometry.Rotation;

import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.frames.Transform;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles an inertial attitude law.
 * <p>Instances of this class are guaranteed to be immutable.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
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
        Transform t = frame.getTransformTo(satelliteFrame, date);
        return new Attitude(frame, t.getRotation(), t.getRotationRate());
    }

}
