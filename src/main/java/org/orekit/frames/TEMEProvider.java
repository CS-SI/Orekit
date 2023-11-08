/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitInternalError;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeScalarFunction;
import org.orekit.time.TimeScales;
import org.orekit.time.TimeVectorFunction;
import org.orekit.utils.IERSConventions;

/** True Equator Mean Equinox Frame.
 * <p>This frame is used for the SGP4 model in TLE propagation. This frame has <em>no</em>
 * official definition and there are some ambiguities about whether it should be used
 * as "of date" or "of epoch". This frame should therefore be used <em>only</em> for
 * TLE propagation and not for anything else, as recommended by the CCSDS Orbit Data Message
 * blue book.</p>
 * @author Luc Maisonobe
 */
class TEMEProvider implements EOPBasedTransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20131209L;

    /** Conventions. */
    private final IERSConventions conventions;

    /** EOP history. */
    private final EOPHistory eopHistory;

    /** Function computing the mean obliquity. */
    private final transient TimeScalarFunction obliquityFunction;

    /** Function computing the nutation angles. */
    private final transient TimeVectorFunction nutationFunction;

    /**
     * Simple constructor.
     *  @param conventions IERS conventions to apply
     * @param eopHistory  EOP history or {@code null} if no corrections should be
     *                    applied.
     * @param timeScales  other time scales used in computing the transform.
     */
    TEMEProvider(final IERSConventions conventions,
                 final EOPHistory eopHistory,
                 final TimeScales timeScales) {
        this.conventions       = conventions;
        this.eopHistory        = eopHistory;
        this.obliquityFunction = conventions.getMeanObliquityFunction(timeScales);
        this.nutationFunction  = conventions.getNutationFunction(timeScales);
    }

    /**
     * Private constructor.
     *
     * @param conventions       IERS conventions to apply
     * @param eopHistory        EOP history
     * @param obliquityFunction to use.
     * @param nutationFunction  to use.
     */
    private TEMEProvider(final IERSConventions conventions,
                         final EOPHistory eopHistory,
                         final TimeScalarFunction obliquityFunction,
                         final TimeVectorFunction nutationFunction) {
        this.conventions = conventions;
        this.eopHistory = eopHistory;
        this.obliquityFunction = obliquityFunction;
        this.nutationFunction = nutationFunction;
    }

    /** {@inheritDoc} */
    @Override
    public EOPHistory getEOPHistory() {
        return eopHistory;
    }

    /** {@inheritDoc} */
    @Override
    public TEMEProvider getNonInterpolatingProvider() {
        return new TEMEProvider(conventions, eopHistory.getEOPHistoryWithoutCachedTidalCorrection(),
                obliquityFunction, nutationFunction);
    }

    /** {@inheritDoc} */
    @Override
    public Transform getTransform(final AbsoluteDate date) {
        final double eqe = getEquationOfEquinoxes(date);
        return new Transform(date, new Rotation(Vector3D.PLUS_K, eqe, RotationConvention.FRAME_TRANSFORM));
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {
        final T eqe = getEquationOfEquinoxes(date);
        return new FieldTransform<>(date, new FieldRotation<>(FieldVector3D.getPlusK(date.getField()),
                                                              eqe,
                                                              RotationConvention.FRAME_TRANSFORM));
    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @return equation of the equinoxes
     */
    private double getEquationOfEquinoxes(final AbsoluteDate date) {

        // compute nutation angles
        final double[] angles = nutationFunction.value(date);

        // nutation in longitude
        double dPsi = angles[0];

        if (eopHistory != null) {
            // apply the corrections for the nutation parameters
            final double[] correction = eopHistory.getEquinoxNutationCorrection(date);
            dPsi += correction[0];
        }

        // mean obliquity of ecliptic
        final double moe = obliquityFunction.value(date);

        // original definition of equation of equinoxes
        final double eqe = dPsi * FastMath.cos(moe);

        // apply correction if needed
        return eqe + angles[2];

    }

    /** Get the Equation of the Equinoxes at the current date.
     * @param  date the date
     * @param <T> type of the field elements
     * @return equation of the equinoxes
     */
    private <T extends CalculusFieldElement<T>> T getEquationOfEquinoxes(final FieldAbsoluteDate<T> date) {

        // compute nutation angles
        final T[] angles = nutationFunction.value(date);

        // nutation in longitude
        T dPsi = angles[0];

        if (eopHistory != null) {
            // apply the corrections for the nutation parameters
            final T[] correction = eopHistory.getEquinoxNutationCorrection(date);
            dPsi = dPsi.add(correction[0]);
        }

        // mean obliquity of ecliptic
        final T moe = obliquityFunction.value(date);

        // original definition of equation of equinoxes
        final T eqe = dPsi.multiply(moe.cos());

        // apply correction if needed
        return eqe.add(angles[2]);

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes only the frame key.
     * </p>
     * @return data transfer object that will be serialized
     */
    @DefaultDataContext
    private Object writeReplace() {
        return new DataTransferObject(conventions, eopHistory);
    }

    /** Internal class used only for serialization. */
    @DefaultDataContext
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

        /** Replace the deserialized data transfer object with a {@link TEMEProvider}.
         * @return replacement {@link TEMEProvider}
         */
        private Object readResolve() {
            try {
                // retrieve a managed frame
                return new TEMEProvider(conventions, eopHistory,
                        DataContext.getDefault().getTimeScales());
            } catch (OrekitException oe) {
                throw new OrekitInternalError(oe);
            }
        }

    }

}
