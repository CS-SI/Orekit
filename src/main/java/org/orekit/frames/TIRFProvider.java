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

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.IERSConventions;

/** Terrestrial Intermediate Reference Frame.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRFProvider}</p>
 */
class TIRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130919L;

    /** Tidal correction (null if tidal effects are ignored). */
    private final TidalCorrection tidalCorrection;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** ERA function. */
    private final TimeFunction<DerivativeStructure> era;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param eopHistory EOP history
     * @param tidalCorrection model for tidal correction (may be null)
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRFProvider(final IERSConventions conventions,
                           final EOPHistory eopHistory,
                           final TidalCorrection tidalCorrection)
        throws OrekitException {

        final UT1Scale ut1   = TimeScalesFactory.getUT1(eopHistory);
        this.tidalCorrection = tidalCorrection;
        this.eopHistory      = eopHistory;
        this.era             = conventions.getEarthOrientationAngleFunction(ut1);

    }

    /** Get the EOP history.
     * @return EOP history
     */
    EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** Get the tidal correction model.
     * @return tidal correction model (may be null)
     */
    TidalCorrection getTidalCorrection() {
        return tidalCorrection;
    }

    /** Get the transform from CIRF 2000 at specified date.
     * <p>The update considers the earth rotation from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute proper rotation
        final DerivativeStructure rawERA = era.value(date);
        final double correctedERA = correctERA(date, rawERA);

        // set up the transform from parent CIRF2000
        final Rotation rotation     = new Rotation(Vector3D.PLUS_K, -correctedERA);
        final Vector3D rotationRate = new Vector3D(rawERA.getPartialDerivative(1), Vector3D.PLUS_K);
        return new Transform(date, rotation, rotationRate);

    }

    /** Get the Earth Rotation Angle at the current date.
     * @param  date the date
     * @return Earth Rotation Angle at the current date in radians
     * @exception OrekitException if nutation model cannot be computed
     */
    public double getEarthRotationAngle(final AbsoluteDate date) throws OrekitException {
        return MathUtils.normalizeAngle(correctERA(date, era.value(date)), 0);
    }

    /** Apply corrections to the Earth Rotation Angle.
     * @param date date
     * @param  rawERA raw value of ERA
     * @return corrected value of the ERA
      */
    private double correctERA(final AbsoluteDate date, final DerivativeStructure rawERA) {
        return (tidalCorrection == null) ? rawERA.getValue() : rawERA.taylor(tidalCorrection.getDUT1(date));
    }

}
