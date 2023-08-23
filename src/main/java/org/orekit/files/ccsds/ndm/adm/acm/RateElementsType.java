/* Copyright 2023 Luc Maisonobe
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
package org.orekit.files.ccsds.ndm.adm.acm;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hipparchus.analysis.differentiation.UnivariateDerivative1;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.units.Unit;

/** Attitude rate element set type used in CCSDS {@link Acm Attitude Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum RateElementsType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Angular velocity. */
    ANGVEL("Angular velocity",
           "°/s", "°/s", "°/s") {
        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates toAngular(final AbsoluteDate date,
                                                       final RotationOrder order,
                                                       final Rotation rotation,
                                                       final int first,
                                                       final double[] elements) {
            return new TimeStampedAngularCoordinates(date,
                                                     rotation,
                                                     new Vector3D(elements[first], elements[first + 1], elements[first + 2]),
                                                     Vector3D.ZERO);
        }
    },

    /** Quaternion derivatives. */
    Q_DOT("Quaternion derivatives",
          "s⁻¹", "s⁻¹", "s⁻¹", "s⁻¹") {
        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates toAngular(final AbsoluteDate date,
                                                       final RotationOrder order,
                                                       final Rotation rotation,
                                                       final int first,
                                                       final double[] elements) {
            final UnivariateDerivative1 q0 = new UnivariateDerivative1(rotation.getQ0(), elements[first + 3]);
            final UnivariateDerivative1 q1 = new UnivariateDerivative1(rotation.getQ1(), elements[first]);
            final UnivariateDerivative1 q2 = new UnivariateDerivative1(rotation.getQ2(), elements[first + 1]);
            final UnivariateDerivative1 q3 = new UnivariateDerivative1(rotation.getQ3(), elements[first + 2]);
            return new TimeStampedAngularCoordinates(date, new FieldRotation<>(q0, q1, q2, q3, false));
        }
    },

    /** Euler rates. */
    EULER_RATE("Euler rates",
               "°/s", "°/s", "°/s") {
        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates toAngular(final AbsoluteDate date,
                                                       final RotationOrder order,
                                                       final Rotation rotation,
                                                       final int first,
                                                       final double[] elements) {
            final double[] euler0 = rotation.getAngles(order, RotationConvention.FRAME_TRANSFORM);
            final UnivariateDerivative1 alpha0 = new UnivariateDerivative1(euler0[0], elements[first]);
            final UnivariateDerivative1 alpha1 = new UnivariateDerivative1(euler0[1], elements[first + 1]);
            final UnivariateDerivative1 alpha2 = new UnivariateDerivative1(euler0[2], elements[first + 2]);
            return new TimeStampedAngularCoordinates(date, new FieldRotation<>(order, RotationConvention.FRAME_TRANSFORM,
                                                                               alpha0, alpha1, alpha2));
        }
    },

    /** Correction to gyro rates. */
    GYRO_BIAS("Gyro rate corrections",
              "°/s", "°/s", "°/s") {
        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates toAngular(final AbsoluteDate date,
                                                       final RotationOrder order,
                                                       final Rotation rotation,
                                                       final int first,
                                                       final double[] elements) {
            throw new OrekitException(OrekitMessages.CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE, name(), toString());
        }
    },

    /** No rates. */
    NONE("no rates") {
        /** {@inheritDoc} */
        @Override
        public TimeStampedAngularCoordinates toAngular(final AbsoluteDate date,
                                                       final RotationOrder order,
                                                       final Rotation rotation,
                                                       final int first,
                                                       final double[] elements) {
            return new TimeStampedAngularCoordinates(date, rotation, Vector3D.ZERO, Vector3D.ZERO);
        }
    };

    // CHECKSTYLE: resume MultipleStringLiterals check

    /** Description. */
    private final String description;

    /** Elements units. */
    private final List<Unit> units;

    /** Simple constructor.
     * @param description description
     * @param unitsSpecifications elements units specifications
     */
    RateElementsType(final String description, final String... unitsSpecifications) {
        this.description = description;
        this.units       = Stream.of(unitsSpecifications).
                           map(s -> Unit.parse(s)).
                           collect(Collectors.toList());
    }

    /** Get the elements units.
     * @return elements units
     */
    public List<Unit> getUnits() {
        return units;
    }

    /** Convert to angyla coordinates.
     * @param date date
     * @param order rotation order for Euler angles
     * @param rotation rotation
     * @param first index of the first element to consider
     * @param elements elements values in SI units
     * @return rotation
     */
    public abstract TimeStampedAngularCoordinates toAngular(AbsoluteDate date,
                                                            RotationOrder order,
                                                            Rotation rotation,
                                                            int first,
                                                            double[] elements);

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
