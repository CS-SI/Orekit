/* Copyright 2002-2012 CS Systèmes d'Information
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

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;

/** True Equator Mean Equinox Frame.
 * <p>This frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em>
 * official definition and there are some ambiguities about whether it should be used
 * as "of date" or "of epoch". This frame should therefore be used <em>only</em> for
 * TLE propagation and not for anything else, as recommended by the CCSDS Orbit Data Message
 * blue book.</p>
 * @author Luc Maisonobe
 */
class TEMEProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 2456075440738236022L;

    /** True Of Date provider. */
    private final TODProvider tod;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Cached transform to avoid useless computation. */
    private Transform cachedTransform;

    /** Simple constructor.
     * @param tod True Of Date provider
     */
    public TEMEProvider(final TODProvider tod) {
        this.tod = tod;
    }

    /** Get the transform from True Of Date date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public synchronized Transform getTransform(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            final double eqe = tod.getEquationOfEquinoxes(date);

            // set up the transform from parent TOD
            cachedTransform = new Transform(date, new Rotation(Vector3D.PLUS_K, -eqe));
            cachedDate = date;

        }

        return cachedTransform;

    }

}
