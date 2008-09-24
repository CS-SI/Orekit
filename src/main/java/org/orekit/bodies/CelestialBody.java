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
package org.orekit.bodies;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Interface for celestial bodies like Sun, Moon or solar system planets.
 * @author Luc Maisonobe
 * @see SolarSystemBody
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface CelestialBody extends Serializable {

    /** Get the {@link PVCoordinates} of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return position/velocity of the body (m and m/s)
     * @exception OrekitException if position cannot be computed in given frame
     */
    PVCoordinates getPVCoordinates(AbsoluteDate date, Frame frame)
        throws OrekitException;

    /** Get an inertially oriented body-centered frame.
     * <p>The frame is always bound to the body center, and its axes have a
     * fixed orientation with respecto to other inertial frames.</p>
     * @return an inertially oriented body-centered frame
     */
    Frame getFrame();

    /** Get the attraction coefficient of the body.
     * @return attraction coefficient of the body (m<sup>3</sup>/s<sup>2</sup>)
     */
    double getGM();

}
