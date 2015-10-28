/* Copyright 2002-2015 CS Systèmes d'Information
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
import org.orekit.utils.Constants;


/** International Terrestrial Reference Frame.
 * <p> Handles pole motion effects and depends on {@link TIRFProvider}, its
 * parent frame.</p>
 * @author Luc Maisonobe
 */
class ITRFProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130922L;

    /** S' rate in radians per julian century.
     * Approximately -47 microarcsecond per julian century (Lambert and Bizouard, 2002)
     */
    private static final double S_PRIME_RATE = -47e-6 * Constants.ARC_SECONDS_TO_RADIANS;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Simple constructor.
     * @param eopHistory EOP history
     */
    ITRFProvider(final EOPHistory eopHistory) {
        this.eopHistory = eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public ITRFProvider getNonInterpolatingProvider()
        throws OrekitException {
        return new ITRFProvider(eopHistory.getNonInterpolatingEOPHistory());
    }

    /** Get the transform from TIRF 2000 at specified date.
     * <p>The update considers the pole motion from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // offset from J2000 epoch in julian centuries
        final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
        final double ttc =  tts / Constants.JULIAN_CENTURY;

        // pole correction parameters
        final PoleCorrection eop = eopHistory.getPoleCorrection(date);

        // elementary rotations due to pole motion in terrestrial frame
        final Rotation r1 = new Rotation(Vector3D.PLUS_I, -eop.getYp());
        final Rotation r2 = new Rotation(Vector3D.PLUS_J, -eop.getXp());
        final Rotation r3 = new Rotation(Vector3D.PLUS_K, S_PRIME_RATE * ttc);

        // complete pole motion in terrestrial frame
        final Rotation wRot = r3.applyTo(r2.applyTo(r1));

        // combined effects
        final Rotation combined = wRot.revert();

        // set up the transform from parent TIRF
        return new Transform(date, combined, Vector3D.ZERO);

    }

}
