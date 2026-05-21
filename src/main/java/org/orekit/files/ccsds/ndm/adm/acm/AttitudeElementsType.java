/* Copyright 2022-2026 Luc Maisonobe
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

import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.RotationConvention;
import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.utils.units.Unit;

/** Attitude element set type used in CCSDS {@link Acm Attitude Comprehensive Messages}.
 * @author Luc Maisonobe
 * @since 12.0
 */
public enum AttitudeElementsType {

    // CHECKSTYLE: stop MultipleStringLiterals check

    /** Quaternion. */
    QUATERNION("Quaternion",
               "n/a", "n/a", "n/a", "n/a") {

        /** {@inheritDoc} */
        @Override
        public Rotation toRotation(final RotationOrder order, final double[] elements) {
            return new Rotation(elements[3], elements[0], elements[1], elements[2], true);
        }

        /** {@inheritDoc} */
        @Override
        public double[] toRawElements(final Rotation rotation, final RotationOrder order) {
            return new double[] {
                rotation.getQ1(), rotation.getQ2(), rotation.getQ3(), rotation.getQ0()
            };
        }

    },

    /** Euler angles. */
    EULER_ANGLES("Euler angles",
                 "°", "°", "°") {

        /** {@inheritDoc} */
        @Override
        public Rotation toRotation(final RotationOrder order, final double[] elements) {
            return new Rotation(order, RotationConvention.FRAME_TRANSFORM,
                                elements[0], elements[1], elements[2]);
        }

        /** {@inheritDoc} */
        @Override
        public double[] toRawElements(final Rotation rotation, final RotationOrder order) {
            return rotation.getAngles(order, RotationConvention.FRAME_TRANSFORM);
        }

    },

    /** Direction cosine matrix. */
    DCM("Direction cosine matrix",
        "n/a", "n/a", "n/a", "n/a", "n/a", "n/a", "n/a", "n/a", "n/a") {

        /** {@inheritDoc} */
        @Override
        public Rotation toRotation(final RotationOrder order, final double[] elements) {
            return new Rotation(new double[][] {
                { elements[0], elements[3], elements[6] },
                { elements[1], elements[4], elements[7] },
                { elements[2], elements[5], elements[8] }
            }, 1.0e-10);
        }

        /** {@inheritDoc} */
        @Override
        public double[] toRawElements(final Rotation rotation, final RotationOrder order) {
            final double[][] matrix = rotation.getMatrix();
            return new double[] {
                matrix[0][0], matrix[1][0], matrix[2][0],
                matrix[0][1], matrix[1][1], matrix[2][1],
                matrix[0][2], matrix[1][2], matrix[2][2]
            };
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
    AttitudeElementsType(final String description, final String... unitsSpecifications) {
        this.description = description;
        this.units       = Stream.of(unitsSpecifications).
                           map(Unit::parse).
                           collect(Collectors.toList());
    }

    /** Get the elements units.
     * @return elements units
     */
    public List<Unit> getUnits() {
        return units;
    }

    /** Convert to rotation.
     * @param order rotation order for Euler angles
     * @param elements elements values in SI units
     * @return rotation
     */
    public abstract Rotation toRotation(RotationOrder order, double[] elements);

    /** Convert to raw elements array.
     * @param rotation rotation
     * @param order rotation order for Euler angles
     * @return elements elements values in SI units
     * @since 14.0
     */
    public abstract double[] toRawElements(Rotation rotation, RotationOrder order);

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return description;
    }

}
