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

import java.io.Serializable;

import org.orekit.errors.OrekitException;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;


/** This interface represents an attitude law model set.
 * <p>An attitude law provides a way to compute an {@link Attitude Attitude}
 * state at any date given a center of gravity position and velocity.</p>
 * @author V&eacute;ronique Pommier-Maurussane
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface AttitudeLaw extends Serializable {

    /** Compute the attitude state at given date.
     * @param date date when attitude state shall be computed
     * @param pv satellite position/velocity at date, in given frame
     * @param frame frame in which satellite position/velocity are given
     * @return attitude state at date, from specified frame to satellite
     * frame
     * @throws OrekitException if some specific error occurs
     */
    Attitude getState(AbsoluteDate date, PVCoordinates pv, Frame frame)
        throws OrekitException;

}
