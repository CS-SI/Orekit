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
package org.orekit.frames;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;


/** International Terrestrial Reference Frame 2005.
 * <p> Handles pole motion effects and depends on {@link TIRF2000Frame}, its
 * parent frame.</p>
 * @author Luc Maisonobe
 * @version $Revision: 1726 $ $Date: 2008-06-20 11:18:17 +0200 (ven., 20 juin 2008) $
 */
class ITRF2005Frame extends Frame {

    /** Serializable UID. */
    private static final long serialVersionUID = 8677025885319745348L;

    /** 2&pi;. */
    private static final double TWO_PI = 2.0 * Math.PI;

    /** Radians per arcsecond. */
    private static final double RADIANS_PER_ARC_SECOND = TWO_PI / 1296000;

    /** Julian century per second. */
    private static final double JULIAN_CENTURY_PER_SECOND = 1.0 / (36525.0 * 86400.0);

    /** S' rate in radians per julian century.
     * Approximately -47 microarcsecond per julian century (Lambert and Bizouard, 2002)
     */
    private static final double S_PRIME_RATE = -47e-6 * RADIANS_PER_ARC_SECOND;

    /** Cached date to avoid useless computation. */
    private AbsoluteDate cachedDate;

    /** Simple constructor.
     * @param date the current date
     * @param name the string representation
     * @exception OrekitException if nutation cannot be computed
     */
    protected ITRF2005Frame(final AbsoluteDate date, final String name)
        throws OrekitException {
        super(getTIRF2000(), null, name);
        // everything is in place, we can now synchronize the frame
        updateFrame(date);
    }

    /** Update the frame to the given date.
     * <p>The update considers the pole motion from IERS data.</p>
     * @param date new value of the date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    protected void updateFrame(final AbsoluteDate date) throws OrekitException {

        if ((cachedDate == null) || !cachedDate.equals(date)) {

            // offset from J2000 epoch in julian centuries
            final double tts = date.durationFrom(AbsoluteDate.J2000_EPOCH);
            final double ttc =  tts * JULIAN_CENTURY_PER_SECOND;

            // get the current IERS pole correction parameters
            final PoleCorrection iCorr =
                EarthOrientationHistory.getInstance().getPoleCorrection(date);

            // compute the additional terms not included in IERS data
            final PoleCorrection tCorr = tidalCorrection(date);
            final PoleCorrection nCorr = nutationCorrection(date);

            // elementary rotations due to pole motion in terrestrial frame
            final Rotation r1 = new Rotation(Vector3D.PLUS_I, -(iCorr.getYp() + tCorr.getYp() + nCorr.getYp()));
            final Rotation r2 = new Rotation(Vector3D.PLUS_J, -(iCorr.getXp() + tCorr.getXp() + nCorr.getXp()));
            final Rotation r3 = new Rotation(Vector3D.PLUS_K, S_PRIME_RATE * ttc);

            // complete pole motion in terrestrial frame
            final Rotation wRot = r3.applyTo(r2.applyTo(r1));

            // combined effects
            final Rotation combined = wRot.revert();

            // set up the transform from parent TIRF
            setTransform(new Transform(combined, Vector3D.ZERO));
            cachedDate = date;

        }
    }

    /** Compute tidal correction to the pole motion.
     * @param date current date
     * @return tidal correction
     */
    private PoleCorrection tidalCorrection(final AbsoluteDate date) {
        // TODO compute tidal correction to pole motion
        return PoleCorrection.NULL_CORRECTION;
    }

    /** Compute nutation correction due to tidal gravity.
     * @param date current date
     * @return nutation correction
     */
    private PoleCorrection nutationCorrection(final AbsoluteDate date) {
        // this factor seems to be of order of magnitude a few tens of
        // micro arcseconds. It is computed from the classical approach
        // (not the new one used here) and hence requires computation
        // of GST, IAU2000A nutation, equations of equinox ...
        // For now, this term is ignored
        return PoleCorrection.NULL_CORRECTION;
    }

}
