/* Copyright 2022-2024 Romain Serra
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

package org.orekit.utils;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Interface for position providers (including for Field).
 * Emulates position (and derivatives) vector as a function of time.
 * @author Romain Serra
 * @since 12.1
 */
public interface ExtendedPositionProvider extends PVCoordinatesProvider {

    /** Get the position in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> field type
     * @return position
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(FieldAbsoluteDate<T> date, Frame frame);

    /** {@inheritDoc} */
    @Override
    default TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        final UnivariateDerivative2Field ud2Field = UnivariateDerivative2Field.getInstance();
        final UnivariateDerivative2 ud2Shift = new UnivariateDerivative2(0., 1., 0.);
        final FieldAbsoluteDate<UnivariateDerivative2> fieldDate = new FieldAbsoluteDate<>(ud2Field, date).shiftedBy(ud2Shift);
        final FieldVector3D<UnivariateDerivative2> ud2Position = getPosition(fieldDate, frame);
        final Vector3D position = ud2Position.toVector3D();
        final Vector3D velocity = new Vector3D(ud2Position.getX().getFirstDerivative(), ud2Position.getY().getFirstDerivative(),
            ud2Position.getZ().getFirstDerivative());
        final Vector3D acceleration = new Vector3D(ud2Position.getX().getSecondDerivative(), ud2Position.getY().getSecondDerivative(),
                ud2Position.getZ().getSecondDerivative());
        return new TimeStampedPVCoordinates(date, new PVCoordinates(position, velocity, acceleration));
    }

    /** Get the position-velocity-acceleration in the selected frame.
     * @param date current date
     * @param frame the frame where to define the position
     * @param <T> field type
     * @return position-velocity-acceleration vector
     */
    default <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                  final Frame frame) {
        final FieldAbsoluteDate<FieldUnivariateDerivative2<T>> fud2Date = date.toFUD2Field();
        final FieldVector3D<FieldUnivariateDerivative2<T>> fud2Position = getPosition(fud2Date, frame);
        final FieldVector3D<T> position = new FieldVector3D<>(fud2Position.getX().getValue(), fud2Position.getY().getValue(),
            fud2Position.getZ().getValue());
        final FieldVector3D<T> velocity = new FieldVector3D<>(fud2Position.getX().getFirstDerivative(), fud2Position.getY().getFirstDerivative(),
            fud2Position.getZ().getFirstDerivative());
        final FieldVector3D<T> acceleration = new FieldVector3D<>(fud2Position.getX().getSecondDerivative(), fud2Position.getY().getSecondDerivative(),
            fud2Position.getZ().getSecondDerivative());
        return new TimeStampedFieldPVCoordinates<>(date, position, velocity, acceleration);
    }

    /** Convert to a {@link FieldPVCoordinatesProvider} with a specific type.
     * @param <T> the type of the field elements
     * @param field field for the argument and value
     * @return converted function
     */
    default <T extends CalculusFieldElement<T>> FieldPVCoordinatesProvider<T> toFieldPVCoordinatesProvider(Field<T> field) {
        return new FieldPVCoordinatesProvider<T>() {

            @Override
            public FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date, final Frame frame) {
                return ExtendedPositionProvider.this.getPosition(date, frame);
            }

            @Override
            public TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                     final Frame frame) {
                return ExtendedPositionProvider.this.getPVCoordinates(date, frame);
            }
        };
    }

    /**
     * Method to convert as {@link ExtendedPVCoordinatesProvider}.
     * @return converted object
     * @since 13.0
     * @deprecated since 13.0. Only there to help transition out.
     */
    @Deprecated
    default ExtendedPVCoordinatesProvider toExtendedPVCoordinatesProvider() {
        return ExtendedPositionProvider.this::getPVCoordinates;
    }
}
