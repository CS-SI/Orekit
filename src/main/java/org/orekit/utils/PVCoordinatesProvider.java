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

package org.orekit.utils;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

/**
** Interface for PV coordinates providers.
 * @author Veronique Pommier
 * <p>The PV coordinates provider interface can be used by any class used for position/velocity
 * computation, for example celestial bodies or spacecraft position/velocity propagators,
 * and many others...
 * </p>
 */
public interface PVCoordinatesProvider {

    /** Get the {@link PVCoordinates} of the body in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @return time-stamped position/velocity of the body (m and m/s)
     * @exception OrekitException if position cannot be computed in given frame
     */
    TimeStampedPVCoordinates getPVCoordinates(AbsoluteDate date,
                                              Frame frame) throws OrekitException;

}
