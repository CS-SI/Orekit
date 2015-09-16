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

import java.io.Serializable;

import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeFunction;
import org.orekit.utils.IERSConventions;

/** Provider for True of Date (ToD) frame.
 * <p>This frame handles nutation effects according to selected IERS conventions.</p>
 * <p>Transform is computed with reference to the {@link MODProvider Mean of Date} frame.</p>
 * @author Pascal Parraud
 */
class TODProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** Conventions. */
    private final IERSConventions conventions;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Function computing the mean obliquity. */
    private final transient TimeFunction<Double> obliquityFunction;

    /** Function computing the nutation angles. */
    private final transient TimeFunction<double[]> nutationFunction;

    /** Simple constructor.
     * @param conventions IERS conventions to apply
     * @param eopHistory EOP history
     * @exception OrekitException if IERS conventions tables cannot be read
     */
    TODProvider(final IERSConventions conventions, final EOPHistory eopHistory)
        throws OrekitException {
        this.conventions       = conventions;
        this.eopHistory        = eopHistory;
        this.obliquityFunction = conventions.getMeanObliquityFunction();
        this.nutationFunction  = conventions.getNutationFunction();
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TODProvider getNonInterpolatingProvider()
        throws OrekitException {
        return new TODProvider(conventions, eopHistory.getNonInterpolatingEOPHistory());
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
        final double[] angles = nutationFunction.value(date);

        // compute the mean obliquity of the ecliptic
        final double moe = obliquityFunction.value(date);

        double dpsi = angles[0];
        double deps = angles[1];
        if (eopHistory != null) {
            // apply the corrections for the nutation parameters
            final double[] correction = eopHistory.getEquinoxNutationCorrection(date);
            dpsi += correction[0];
            deps += correction[1];
        }

        // compute the true obliquity of the ecliptic
        final double toe = moe + deps;

        // set up the elementary rotations for nutation
        final Rotation r1 = new Rotation(Vector3D.PLUS_I,  toe);
        final Rotation r2 = new Rotation(Vector3D.PLUS_K,  dpsi);
        final Rotation r3 = new Rotation(Vector3D.PLUS_I, -moe);

        // complete nutation
        final Rotation nutation = r1.applyTo(r2.applyTo(r3));

        // set up the transform from parent MOD
        return new Transform(date, nutation);

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

        /** Replace the deserialized data transfer object with a {@link TODProvider}.
         * @return replacement {@link TODProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new TODProvider(conventions, eopHistory);
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
