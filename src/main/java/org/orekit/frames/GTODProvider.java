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
package org.orekit.frames;

import java.io.Serializable;

import org.hipparchus.analysis.differentiation.DerivativeStructure;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UT1Scale;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;

/** Greenwich True Of Date Frame, also known as True of Date Rotating frame (TDR)
 * or Greenwich Rotating Coordinate frame (GCR).
 * <p> This frame handles the sidereal time according to IAU-82 model.</p>
 * <p> Its parent frame is the {@link TODProvider}.</p>
 * <p> The pole motion is not applied here.</p>
 * @author Pascal Parraud
 * @author Thierry Ceolin
 */
public class GTODProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20141228L;

    /** Angular velocity of the Earth, in rad/s. */
    private static final double AVE = 7.292115146706979e-5;

    /** Conventions. */
    private final IERSConventions conventions;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** GAST function. */
    private final transient TimeFunction<DerivativeStructure> gastFunction;

    /** Simple constructor.
     * @param conventions IERS conventions to use
     * @param eopHistory EOP history (may be null)
     * @exception OrekitException if EOP parameters are desired but cannot be read
     */
    protected GTODProvider(final IERSConventions conventions, final EOPHistory eopHistory)
        throws OrekitException {
        final UT1Scale ut1 = TimeScalesFactory.getUT1(eopHistory);
        this.conventions   = conventions;
        this.eopHistory    = eopHistory;
        this.gastFunction  = conventions.getGASTFunction(ut1, eopHistory);
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public GTODProvider getNonInterpolatingProvider()
        throws OrekitException {
        return new GTODProvider(conventions, eopHistory.getNonInterpolatingEOPHistory());
    }

    /** Get the transform from TOD at specified date.
     * <p>The update considers the Earth rotation from IERS data.</p>
     * @param date new value of the date
     * @return transform at the specified date
     * @exception OrekitException if the nutation model data embedded in the
     * library cannot be read
     */
    public Transform getTransform(final AbsoluteDate date) throws OrekitException {

        // compute Greenwich apparent sidereal time, in radians
        final double gast = gastFunction.value(date).getValue();

        // compute true angular rotation of Earth, in rad/s
        final double lod = (eopHistory == null) ? 0.0 : eopHistory.getLOD(date);
        final double omp = AVE * (1 - lod / Constants.JULIAN_DAY);
        final Vector3D rotationRate = new Vector3D(omp, Vector3D.PLUS_K);

        // set up the transform from parent TOD
        return new Transform(date, new Rotation(Vector3D.PLUS_K, gast, RotationConvention.FRAME_TRANSFORM), rotationRate);

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(conventions, eopHistory);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20131209L;

        /** Conventions. */
        private final IERSConventions conventions;

        /** EOP history. */
        private final EOPHistory eopHistory;

        /** Simple constructor.
         * @param conventions IERS conventions to apply
         * @param eopHistory EOP history
         */
        DataTransferObject(final IERSConventions conventions, final EOPHistory eopHistory) {
            this.conventions = conventions;
            this.eopHistory  = eopHistory;
        }

        /** Replace the deserialized data transfer object with a {@link GTODProvider}.
         * @return replacement {@link GTODProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new GTODProvider(conventions, eopHistory);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
