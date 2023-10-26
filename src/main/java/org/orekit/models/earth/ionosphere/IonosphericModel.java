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
package org.orekit.models.earth.ionosphere;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.TopocentricFrame;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.utils.ParameterDriversProvider;

/** Defines a ionospheric model, used to calculate the path delay imposed to
 * electro-magnetic signals between an orbital satellite and a ground station.
 * <p>
 * Since 10.0, this interface can be used for models that aspire to estimate
 * ionospheric parameters.
 * </p>
 *
 * @author Joris Olympio
 * @author Bryan Cazabonne
 * @since 7.1
 */
public interface IonosphericModel extends ParameterDriversProvider {

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param state       spacecraft state
     * @param baseFrame   base frame associated with the station
     * @param frequency   frequency of the signal in Hz
     * @param parameters  ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    double pathDelay(SpacecraftState state, TopocentricFrame baseFrame, double frequency, double[] parameters);

    /**
     * Calculates the ionospheric path delay for the signal path from a ground
     * station to a satellite.
     * <p>
     * This method is intended to be used for orbit determination issues.
     * In that respect, if the elevation is below 0° the path delay will be equal to zero.
     * </p><p>
     * For individual use of the ionospheric model (i.e. not for orbit determination), another
     * method signature can be implemented to compute the path delay for any elevation angle.
     * </p>
     * @param <T>         type of the elements
     * @param state       spacecraft state
     * @param baseFrame   base frame associated with the station
     * @param frequency   frequency of the signal in Hz
     * @param parameters  ionospheric model parameters at state date
     * @return the path delay due to the ionosphere in m
     */
    <T extends CalculusFieldElement<T>> T pathDelay(FieldSpacecraftState<T> state, TopocentricFrame baseFrame,
                                                    double frequency, T[] parameters);
}
