/* Copyright 2002-2013 CS Systèmes d'Information
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
import org.orekit.utils.IERSConventions;


/** International Terrestrial Reference Frame, based on old equinox conventions.
 * <p> Handles pole motion effects and depends on {@link GTODProvider}, its
 * parent frame.</p>
 * @author Luc Maisonobe
 */
class ITRFEquinoxProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130801L;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param conventions conventions to apply
     * @exception OrekitException if EOP parameters cannot be read
     */
    protected ITRFEquinoxProvider(final IERSConventions conventions) throws OrekitException {
        eopHistory = FramesFactory.getEOPHistory(conventions);
    }

    /** Get the transform from GTOD at specified date.
     * <p>The update considers the pole motion from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // pole correction parameters
        final PoleCorrection pCorr = eopHistory.getPoleCorrection(date);

        // elementary rotations due to pole motion in terrestrial frame
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, -pCorr.getYp());
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -pCorr.getXp());

        // complete pole motion in terrestrial frame
        final Rotation wRot = r2.applyTo(r1);

        // set up the transform from parent GTOD
        return new Transform(date, wRot.revert(), Vector3D.ZERO);

    }

}
