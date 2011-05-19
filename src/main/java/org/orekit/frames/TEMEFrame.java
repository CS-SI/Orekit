/* Copyright 2002-2011 CS Communication & Systèmes
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
package org.orekit.frames;

import org.apache.commons.math.geometry.euclidean.threed.Rotation;
import org.apache.commons.math.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

/** True Equator Mean Equinox Frame.
 * <p>This frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em>
 * official definition and there are some ambiguities about whether it should be used
 * as "of date" or "of epoch". This frame should therefore be used <em>only</em> for
 * TLE propagation and not for anything else, as recommended by the CCSDS Orbit Data Message
 * blue book.</p>
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
class TEMEFrame extends FactoryManagedFrame {

    /** Serializable UID. */
    private static final long serialVersionUID = 1019413109622214373L;

    /** Cached date to avoid useless calculus. */
    private AbsoluteDate cachedDate;

    /** Simple constructor.
     * @param factoryKey key of the frame within the factory
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected TEMEFrame(final Predefined factoryKey)
        throws OrekitException {

        super(FramesFactory.getTOD(false), null, true, factoryKey);

        // everything is in place, we can now synchronize the frame
        updateFrame(AbsoluteDate.J2000_EPOCH);

    }

    /** Update the frame to the given date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            final TODFrame tod = (TODFrame) getParent();
            final double eqe = tod.getEquationOfEquinoxes(date);

            // set up the transform from parent TOD
            setTransform(new Transform(new Rotation(Vector3D.PLUS_K, -eqe)));

            cachedDate = date;

        }
    }

}
