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
import org.apache.commons.math3.util.FastMath;
import org.orekit.data.FundamentalNutationArguments;
import org.orekit.data.NutationFunction;
import org.orekit.errors.OrekitException;
import org.orekit.errors.TimeStampedCacheException;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.IERSConventions;

/** Provider for True of Date (ToD) frame.
 * <p>This frame handles nutation effects according to selected IERS conventions.</p>
 * <p>Transform is computed with reference to the {@link MODProvider Mean of Date} frame.</p>
 * @author Pascal Parraud
 */
class TODProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130729L;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Generator for fundamental nutation arguments. */
    private final FundamentalNutationArguments nutationArguments;

    /** Function computing the nutation angles. */
    private final NutationFunction<double[]> nutationFunction;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param applyEOPCorr if true, EOP correction is applied (here, pole correction and LOD)
     * @exception OrekitException if IERS conventions tables cannot be read
     */
    public TODProvider(final IERSConventions conventions, final boolean applyEOPCorr)
        throws OrekitException {
        this.eopHistory        = applyEOPCorr ? FramesFactory.getEOPHistory(conventions) : null;
        this.nutationArguments = conventions.getNutationArguments();
        this.nutationFunction  = conventions.getNutationFunction();
    }

    /** Get the LoD (Length of Day) value.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the value is desired
     * @return LoD in seconds (0 if date is outside covered range)
     * @exception TimeStampedCacheException if EOP data cannot be retrieved
     */
    double getLOD(final AbsoluteDate date) throws TimeStampedCacheException {
        return (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
    }

    /** Get the pole IERS Reference Pole correction.
     * <p>The data provided comes from the IERS files. It is smoothed data.</p>
     * @param date date at which the correction is desired
     * @return pole correction ({@link PoleCorrection#NULL_CORRECTION
     * PoleCorrection.NULL_CORRECTION} if date is outside covered range)
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        return (eopHistory == null) ? PoleCorrection.NULL_CORRECTION : eopHistory.getPoleCorrection(date);
    }

    /** Get the transform from Mean Of Date at specified date.
     * <p>The update considers the nutation effects from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute nutation angles
        final double[] angles = nutationFunction.value(nutationArguments.evaluateAll(date));

        // compute the mean obliquity of the ecliptic
        final double moe = angles[2];

        // get the corrections for the nutation parameters
        final NutationCorrection nutCorr = (eopHistory == null) ?
                                           NutationCorrection.NULL_CORRECTION :
                                           eopHistory.getNutationCorrection(date);

        final double deps = angles[1] + nutCorr.getDdeps();
        final double dpsi = angles[0] + nutCorr.getDdpsi();

        // compute the true obliquity of the ecliptic
        final double toe = moe + deps;

        // set up the elementary rotations for nutation
        final Rotation r1 = new Rotation(Vector3D.PLUS_I,  toe);
        final Rotation r2 = new Rotation(Vector3D.PLUS_K,  dpsi);
        final Rotation r3 = new Rotation(Vector3D.PLUS_I, -moe);

        // complete nutation
        final Rotation precession = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent MOD
        return new Transform(date, precession);

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @return equation of the equinoxes
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEquationOfEquinoxes(final AbsoluteDate date)
        throws OrekitException {

        // compute nutation angles
        final double[] angles = nutationFunction.value(nutationArguments.evaluateAll(date));

        // nutation in longitude
        final double dPsi = angles[0];

        // mean obliquity of ecliptic
        final double moe = angles[2];

        // original definition of equation of equinoxes
        final double eqe = dPsi * FastMath.cos(moe);

        // apply correction if needed
        return eqe + angles[3];

    }

}
