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

import java.io.Serializable;

import org.apache.commons.math3.analysis.differentiation.DerivativeStructure;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.commons.math3.util.MathUtils;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/** Terrestrial Intermediate Reference Frame.
 * <p> The pole motion is not considered : Pseudo Earth Fixed Frame. It handles
 * the earth rotation angle, its parent frame is the {@link CIRFProvider}</p>
 */
class TIRFProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20130801L;

    /** Tidal correction (null if tidal effects are ignored). */
    private final TidalCorrection tidalCorrection;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** IERS conventions to apply. */
    private final IERSConventions conventions;

    /** ERA function. */
    private final TimeFunction<DerivativeStructure> era;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param ignoreTidalEffects if true, tidal effects are ignored
     * @exception OrekitException if nutation cannot be computed
     */
    protected TIRFProvider(final IERSConventions conventions, final boolean ignoreTidalEffects)
        throws OrekitException {

        this.tidalCorrection = ignoreTidalEffects ? null : new TidalCorrection();
        this.eopHistory      = FramesFactory.getEOPHistory(conventions);
        this.conventions     = conventions;
        this.era             = conventions.getEarthOrientationAngleFunction();

    }

    /** Get the pole correction.
     * @param date date at which the correction is desired
     * @return pole correction including both EOP values and tidal correction
     * if they have been configured
     */
    public PoleCorrection getPoleCorrection(final AbsoluteDate date) {
        final PoleCorrection eop = eopHistory.getPoleCorrection(date);
        if (tidalCorrection == null) {
            return eop;
        } else {
            final PoleCorrection tidal = tidalCorrection.getPoleCorrection(date);
            return new PoleCorrection(eop.getXp() + tidal.getXp(),
                                      eop.getYp() + tidal.getYp());
        }
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

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the constructor parameters.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(conventions, tidalCorrection == null);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20130730L;

        /** IERS conventions to apply. */
        private final IERSConventions conventions;

        /** Indicator for tidal effects. */
        private final boolean ignoreTidalEffects;

        /** Simple constructor.
         * @param conventions IERS conventions to apply
         * @param ignoreTidalEffects if true, tidal effects are ignored
         */
        protected DataTransferObject(final IERSConventions conventions, final boolean ignoreTidalEffects) {
            this.conventions        = conventions;
            this.ignoreTidalEffects = ignoreTidalEffects;
        }

        /** Replace the deserialized data transfer object with a {@link TIRFProvider}.
         * @return replacement {@link TIRFProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new TIRFProvider(conventions, ignoreTidalEffects);
            } catch (OrekitException oe) {
                throw OrekitException.createInternalError(oe);
            }
        }

    }

}
