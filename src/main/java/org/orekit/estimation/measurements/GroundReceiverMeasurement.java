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
package org.orekit.estimation.measurements;

import java.util.Collections;

import org.orekit.time.AbsoluteDate;

/** Base class modeling a measurement where receiver is a ground station.
 * @author Thierry Ceolin
 * @author Luc Maisonobe
 * @author Maxime Journot
 * @since 12.0
 */
public abstract class GroundReceiverMeasurement<T extends GroundReceiverMeasurement<T>> extends AbstractMeasurement<T> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Flag indicating whether it is a two-way measurement. */
    private final boolean twoway;

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 12.0
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double observed, final double sigma, final double baseWeight,
                                     final ObservableSatellite satellite) {
        super(date, observed, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getClockDriftDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        if (!twoWay) {
            // for one way measurements, the satellite clock offset affects the measurement
            addParameterDriver(satellite.getClockOffsetDriver());
            addParameterDriver(satellite.getClockDriftDriver());
        }
        this.station = station;
        this.twoway  = twoWay;
    }

    /** Simple constructor.
     * @param station ground station from which measurement is performed
     * @param twoWay flag indicating whether it is a two-way measurement
     * @param date date of the measurement
     * @param observed observed value
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param satellite satellite related to this measurement
     * @since 12.0
     */
    public GroundReceiverMeasurement(final GroundStation station, final boolean twoWay, final AbsoluteDate date,
                                     final double[] observed, final double[] sigma, final double[] baseWeight,
                                     final ObservableSatellite satellite) {
        super(date, observed, sigma, baseWeight, Collections.singletonList(satellite));
        addParameterDriver(station.getClockOffsetDriver());
        addParameterDriver(station.getClockDriftDriver());
        addParameterDriver(station.getEastOffsetDriver());
        addParameterDriver(station.getNorthOffsetDriver());
        addParameterDriver(station.getZenithOffsetDriver());
        addParameterDriver(station.getPrimeMeridianOffsetDriver());
        addParameterDriver(station.getPrimeMeridianDriftDriver());
        addParameterDriver(station.getPolarOffsetXDriver());
        addParameterDriver(station.getPolarDriftXDriver());
        addParameterDriver(station.getPolarOffsetYDriver());
        addParameterDriver(station.getPolarDriftYDriver());
        if (!twoWay) {
            // for one way measurements, the satellite clock offset affects the measurement
            addParameterDriver(satellite.getClockOffsetDriver());
            addParameterDriver(satellite.getClockDriftDriver());
        }
        this.station = station;
        this.twoway  = twoWay;
    }

    /** Get the ground station from which measurement is performed.
     * @return ground station from which measurement is performed
     */
    public GroundStation getStation() {
        return station;
    }

    /** Check if the instance represents a two-way measurement.
     * @return true if the instance represents a two-way measurement
     */
    public boolean isTwoWay() {
        return twoway;
    }

}
