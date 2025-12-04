/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.orbits;

import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.ParameterDriversList;

/** Factory for orbital parameters.
 * @param <P> type of the orbital parameters
 * @since 14.0
 */
public interface OrbitalParameterFactory<P extends OrbitalParameters> {

    /** Get the date of the orbital parameters.
     * @return date of the orbital parameters
     */
    AbsoluteDate getDate();

    /** Set the date of the orbital parameters.
     * @param date date of the orbital parameters
     */
    void setDate(AbsoluteDate date);

    /** Get the frame in which the orbit is propagated.
     * @return frame in which the orbit is propagated
     */
    Frame getFrame();

    /** Get the central attraction coefficient (µ - m³/s²) value.
     * @return the central attraction coefficient (µ - m³/s²) value
     */
    double getMu();

    /** Set the central attraction coefficient (µ - m³/s²) value.
     * @param mu the central attraction coefficient (µ - m³/s²) value
     */
    void setMu(double mu);

    /** Get the drivers for orbital parameters.
     * @return drivers for orbital parameters
     */
    ParameterDriversList getDrivers();

    /** Get the position scale used to scale the orbital drivers.
     * @return position scale used to scale the orbital drivers
     */
    double getPositionScale();

    /** Convert orbital parameters to state array.
     * @param parameters parameters to map
     * @return array containing the elements
     */
    double[] toArray(P parameters);

     /** Convert state array to orbital parameters.
     * @param array state as a flat array
     * (it can have more than 6 elements, extra elements are ignored)
     * @return orbital parameters corresponding to the flat array as a space dynamics object
     */
    P toParameters(double[] array);

    /** Create orbital parameters from current drivers values.
     * @return created orbital parameters
     */
    default P createFromDrivers() {
        final double[] unNormalized = new double[getDrivers().getNbParams()];
        for (int i = 0; i < unNormalized.length; ++i) {
            unNormalized[i] = getDrivers().getDrivers().get(i).getValue(getDate());
        }
        return toParameters(unNormalized);
    }

}
