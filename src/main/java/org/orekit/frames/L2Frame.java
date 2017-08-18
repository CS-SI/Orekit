/* Copyright 2002-2017 CS Systèmes d'Information
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
package org.orekit.frames;

import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;

/** Class to create a L2 centered frame with {@link L2TransformProvider}.
 *  Parent frame is always set as primaryBody.getInertiallyOrientedFrame()
 * @author Luc Maisonabe
 * @author Julio Hernanz
 */
public class L2Frame extends Frame {

    /** Serializable UID.*/
    private static final long serialVersionUID = 20170811L;

    /** Simple constructor. Calls to {@link Frame#Frame(Frame, TransformProvider, String) Frame(Frame, TransformProvider, String)}
     * @param primaryBody Celestial body with bigger mass, m1.
     * @param secondaryBody Celestial body with smaller mass, m2.
     * @throws OrekitException If frame cannot be retrieved in L2TransformProvider.
     * @throws IllegalArgumentException If parent frame is null.
     */
    public L2Frame(final CelestialBody primaryBody, final CelestialBody secondaryBody) throws IllegalArgumentException, OrekitException {
        super(primaryBody.getInertiallyOrientedFrame(), new L2TransformProvider(primaryBody, secondaryBody),
              primaryBody.getName() + "-" + secondaryBody.getName() + "-L2", true);
    }

}
