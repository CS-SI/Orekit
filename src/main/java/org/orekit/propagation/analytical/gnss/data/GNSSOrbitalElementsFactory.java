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
package org.orekit.propagation.analytical.gnss.data;

import org.orekit.frames.Frame;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.orbits.AbstractOrbitalParameterFactory;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

import java.util.List;

/** Factory for {@link AbstractNavigationMessage}.
 * @param <O> type of the orbital elements
 * @since 14.0
*/
public abstract class GNSSOrbitalElementsFactory<O extends GNSSOrbitalElements<O>>
    extends AbstractOrbitalParameterFactory<O> {

    /** Prefix for frozen body frame. */
    private static final String FROZEN = "frozen-";

    /** Satellite system to use for interpreting week number. */
    private final SatelliteSystem system;

    /** Known time scales. */
    private final TimeScales timeScales;

    /** Message type. */
    private final String type;

    /** Simple constructor.
     * @param template  template message from which parameter drivers will be extracted
     * @param inertial  inertial frame
     * @param bodyFixed body fixed frame, corresponding to the navigation message
     * @param date      date of the orbital parameters
     * @param mu        central attraction coefficient (m³/s²)
     */
    protected GNSSOrbitalElementsFactory(final O template,
                                         final Frame inertial, final Frame bodyFixed,
                                         final AbsoluteDate date, final double mu) {
        super(buildOrbitalDrivers(template),
              bodyFixed.getFrozenFrame(inertial, date, FROZEN + bodyFixed.getName()),
              PositionAngleType.MEAN, date, mu);
        this.timeScales = template.getTimeScales();
        this.system     = template.getSystem();
        this.type       = template.getType();
    }

    /** {@inheritDoc} */
    @Override
    public O createFromDrivers() {

        // create empty message
        final O message = createEmptyMessage(timeScales, system, type);

        // set the date
        message.setDate(getDate());

        // set the orbital elements
        final List<DelegatingDriver> drivers = getOrbitalParametersDrivers().getDrivers();
        message.getSmaDriver().setValue(drivers.get(0).getValue());
        message.getEDriver().setValue(drivers.get(1).getValue());
        message.getI0Driver().setValue(drivers.get(2).getValue());
        message.getPaDriver().setValue(drivers.get(3).getValue());
        message.getOmega0Driver().setValue(drivers.get(4).getValue());
        message.getM0Driver().setValue(drivers.get(5).getValue());

        return message;

    }

    /** {@inheritDoc} */
    @Override
    protected double[] toArray(final Orbit orbit) {

        // fix both frame and type
        final Orbit partiallyConverted = orbit.getFrame() == getFrame() ? orbit : orbit.inFrame(getFrame());
        final Orbit fullyConverted     = OrbitType.KEPLERIAN.convertType(partiallyConverted);

        // retrieve orbital parameters
        final double[] stateVector = new double[6];
        OrbitType.KEPLERIAN.mapOrbitToArray(fullyConverted, PositionAngleType.MEAN, stateVector, null);

        return stateVector;

    }

    /** Create empty message.
     * @param timeScales known time scales
     * @param system     satellite system to consider for interpreting week number
     *                   (may be different from real system, for example in Rinex nav, weeks
     *                   are always according to GPS)
     * @param type       message type
     * @return empty message
     */
    protected abstract O createEmptyMessage(TimeScales timeScales, SatelliteSystem system, String type);

    /** Build the orbital parameters drivers.
     * @param <O> type of the orbital elements
     * @param template template message from which parameter drivers will be extracted
     * @return built drivers
     */
    private static <O extends GNSSOrbitalElements<O>>
        ParameterDriversList buildOrbitalDrivers(final O template) {
        final ParameterDriversList drivers = new ParameterDriversList();
        drivers.add(template.getSmaDriver());
        drivers.add(template.getEDriver());
        drivers.add(template.getI0Driver());
        drivers.add(template.getPaDriver());
        drivers.add(template.getOmega0Driver());
        drivers.add(template.getM0Driver());
        return drivers;
    }

}
