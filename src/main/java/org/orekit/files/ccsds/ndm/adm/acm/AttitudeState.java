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

import org.hipparchus.geometry.euclidean.threed.RotationOrder;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeStamped;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.units.Unit;

/** Attitude state entry.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudeState implements TimeStamped {

    /** Type of the elements. */
    private final AttitudeElementsType attitudeType;

    /** Type of the elements rates. */
    private final RateElementsType rateType;

    /** Entry date. */
    private final AbsoluteDate date;

    /** Attitude elements. */
    private final double[] elements;

    /** Simple constructor.
     * @param attitudeType type of the elements
     * @param rateType type of the elements rates
     * (internally changed to {@link RateElementsType#NONE} if null)
     * @param date entry date
     * @param fields trajectory elements
     * @param first index of first field to consider
     */
    public AttitudeState(final AttitudeElementsType attitudeType, final RateElementsType rateType,
                         final AbsoluteDate date, final String[] fields, final int first) {

        this.attitudeType = attitudeType;
        this.rateType     = rateType == null ? RateElementsType.NONE : rateType;

        final List<Unit> attUnits  = this.attitudeType.getUnits();
        final List<Unit> rateUnits = this.rateType.getUnits();

        this.date         = date;
        this.elements     = new double[attUnits.size() + rateUnits.size()];
        for (int i = 0; i < attUnits.size(); ++i) {
            elements[i] = attUnits.get(i).toSI(Double.parseDouble(fields[first + i]));
        }
        for (int i = 0; i < rateUnits.size(); ++i) {
            elements[attUnits.size() + i] = rateUnits.get(i).toSI(Double.parseDouble(fields[attUnits.size() + first + i]));
        }
    }

    /** {@inheritDoc} */
    @Override
    public AbsoluteDate getDate() {
        return date;
    }

    /** Get attitude elements.
     * @return attitude elements
     */
    public double[] getElements() {
        return elements.clone();
    }

    /** Get the type of the elements.
     * @return type of the elements
     */
    public AttitudeElementsType getAttitudeType() {
        return attitudeType;
    }

    /** Get the type of the elements rates.
     * @return type of the elements rates
     */
    public RateElementsType getRateElementsType() {
        return rateType;
    }

    /** Get which derivatives of position are available in this state.
     * @return a value indicating if the file contains rotation rate and/or acceleration
      */
    public AngularDerivativesFilter getAvailableDerivatives() {
        return rateType == RateElementsType.NONE ?
               AngularDerivativesFilter.USE_R :
               AngularDerivativesFilter.USE_RR;
    }

    /** Convert to angular coordinates.
     * @param order rotation order for Euler angles
     * @return angular coordinates
     */
    public TimeStampedAngularCoordinates toAngular(final RotationOrder order) {
        return rateType.toAngular(getDate(), order,
                                  attitudeType.toRotation(order, elements),
                                  attitudeType.getUnits().size(), elements);
    }

}
