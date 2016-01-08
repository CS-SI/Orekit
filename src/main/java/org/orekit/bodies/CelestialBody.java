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
package org.orekit.bodies;

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.utils.PVCoordinatesProvider;

/** Interface for celestial bodies like Sun, Moon or solar system planets.
 * @author Luc Maisonobe
 * @see CelestialBodyFactory
 */
public interface CelestialBody extends Serializable, PVCoordinatesProvider {

    /** Get an inertially oriented, body centered frame.
     * <p>The frame is always bound to the body center, and its axes have a
     * fixed orientation with respect to other inertial frames.</p>
     * @return an inertially oriented, body centered frame
     * @exception OrekitException if frame cannot be retrieved
     * @see #getBodyOrientedFrame()
     */
    Frame getInertiallyOrientedFrame() throws OrekitException;

    /** Get a body oriented, body centered frame.
     * <p>The frame is always bound to the body center, and its axes have a
     * fixed orientation with respect to the celestial body.</p>
     * @return a body oriented, body centered frame
     * @exception OrekitException if frame cannot be retrieved
     * @see #getInertiallyOrientedFrame()
     */
    Frame getBodyOrientedFrame() throws OrekitException;

    /** Get the name of the body.
     * @return name of the body
     */
    String getName();

    /** Get the attraction coefficient of the body.
     * @return attraction coefficient of the body (m³/s²)
     */
    double getGM();

}
