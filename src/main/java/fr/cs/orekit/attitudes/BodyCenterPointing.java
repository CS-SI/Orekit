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

import fr.cs.orekit.frames.Frame;
import fr.cs.orekit.time.AbsoluteDate;
import fr.cs.orekit.utils.PVCoordinates;

/**
 * This class handles body center pointing attitude law.

 * <p>
 * This class represents the attitude law where the satellite z axis is
 * pointing to the body frame center.</p>
 * <p>
 * The object <code>BodyCenterPointing</code> is guaranteed to be immutable.
 * </p>
 * @see     GroundPointing
 * @author Véronique Pommier-Maurussane
 * @version $Revision$ $Date$
 */
public class BodyCenterPointing extends GroundPointing {

    /** Serializable UID. */
    private static final long serialVersionUID = -1080963665744011149L;

    /** Creates new instance.
     * @param bodyFrame Body frame
     */
    public BodyCenterPointing(final Frame bodyFrame) {
        super(bodyFrame);
    }

    /** Get target expressed in body frame at given date.
     * @param date computation date.
     * @param pv satellite position-velocity vector at given date in given frame.
     * @param frame the frame in which satellite position-velocity is given.
     * @return target position/velocity in body frame.
     *
     * <p>User should check that position/velocity and frame is consistent with given frame.
     * </p>
     */
    protected PVCoordinates getTargetInBodyFrame(final AbsoluteDate date,
                                                 final PVCoordinates pv, final Frame frame) {
        return PVCoordinates.ZERO;
    }

}
