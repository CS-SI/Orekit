/* Copyright 2002-2025 CS GROUP
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldRotation;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.time.TimeInterval;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;

/** This interface is intended for attitude ephemerides valid only during a time range.
*
* <p>This interface provides a mean to retrieve an attitude at
* any time within a given range. It should be implemented by attitude readers
* based on external data files.</p>
*
* @author Bryan Cazabonne
* @since 10.3
*/
public interface BoundedAttitudeProvider extends AttitudeProvider {

    /** Get the first date of the range.
     * @return the first date of the range
     */
    AbsoluteDate getMinDate();

    /** Get the last date of the range.
     * @return the last date of the range
     */
    AbsoluteDate getMaxDate();

    /**
     * Creates a bounded provider given a time interval and a standard attitude provider, with the same outputs.
     * @param attitudeProvider provider to be bounded
     * @param interval time interval
     * @return an instance of the interface
     * @since 13.1
     */
    static BoundedAttitudeProvider of(final AttitudeProvider attitudeProvider, final TimeInterval interval) {
        return new BoundedAttitudeProvider() {
            @Override
            public AbsoluteDate getMinDate() {
                return interval.getStartDate();
            }

            @Override
            public AbsoluteDate getMaxDate() {
                return interval.getEndDate();
            }

            @Override
            public Attitude getAttitude(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
                return attitudeProvider.getAttitude(pvProv, date, frame);
            }

            @Override
            public Rotation getAttitudeRotation(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
                return attitudeProvider.getAttitudeRotation(pvProv, date, frame);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldAttitude<T> getAttitude(final FieldPVCoordinatesProvider<T> pvProv, final FieldAbsoluteDate<T> date, final Frame frame) {
                return attitudeProvider.getAttitude(pvProv, date, frame);
            }

            @Override
            public <T extends CalculusFieldElement<T>> FieldRotation<T> getAttitudeRotation(final FieldPVCoordinatesProvider<T> pvProv, final FieldAbsoluteDate<T> date, final Frame frame) {
                return attitudeProvider.getAttitudeRotation(pvProv, date, frame);
            }
        };
    }
}
