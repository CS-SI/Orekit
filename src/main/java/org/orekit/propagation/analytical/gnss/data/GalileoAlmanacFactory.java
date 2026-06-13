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
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link GalileoAlmanac}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GalileoAlmanacFactory extends GNSSOrbitalElementsFactory<GalileoAlmanac> {

    /** Satellite E5a signal health status. */
    private int healthE5a;

    /** Satellite E5b signal health status. */
    private int healthE5b;

    /** Satellite E1-B/C signal health status. */
    private int healthE1;

    /** Almanac Issue Of Data. */
    private int iod;

    /** Simple constructor.
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param date            date of the orbital parameters
     */
    public GalileoAlmanacFactory(final TimeScales timeScales, final SatelliteSystem system,
                                 final String type, final Frame inertial, final Frame bodyFixed,
                                 final AbsoluteDate date) {
        super(GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, date, GNSSConstants.GALILEO_MU);
    }

    /** Get the E1-B/C signal health status.
     * @return the E1-B/C signal health status
     */
    public int getHealthE1() {
        return healthE1;
    }

    /** Set the E1-B/C signal health status.
     * @param healthE1 the E1-B/C signal health status
     */
    public void setHealthE1(final int healthE1) {
        this.healthE1 = healthE1;
    }

    /** Get the E5a signal health status.
     * @return the E5a signal health status
     */
    public int getHealthE5a() {
        return healthE5a;
    }

    /** Set the E5a signal health status.
     * @param healthE5a the E5a signal health status
     */
    public void setHealthE5a(final int healthE5a) {
        this.healthE5a = healthE5a;
    }

    /** Get the E5b signal health status.
     * @return the E5b signal health status
     */
    public int getHealthE5b() {
        return healthE5b;
    }

    /** Set the E5b signal health status.
     * @param healthE5b the E5b signal health status
     */
    public void setHealthE5b(final int healthE5b) {
        this.healthE5b = healthE5b;
    }

    /** Get the Issue of Data (IOD).
     * @return the Issue Of Data
     */
    public int getIOD() {
        return iod;
    }

    /** Set the Issue of Data (IOD).
     * @param iod the Issue Of Data
     */
    public void setIOD(final int iod) {
        this.iod = iod;
    }

    /** {@inheritDoc} */
    @Override
    public GalileoAlmanac createFromDrivers() {
        return new GalileoAlmanac(getTimeScales(), getSystem(), getPrn(), getWeek(),
                                  createOrbitFromDrivers(),
                                  getTimeDriver().getValue(), getADotDriver().getValue(),
                                  getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                                  getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                                  getCucDriver().getValue(), getCusDriver().getValue(),
                                  getCrcDriver().getValue(), getCrsDriver().getValue(),
                                  getCicDriver().getValue(), getCisDriver().getValue(),
                                  getAf0Driver().getValue(), getAf1Driver().getValue(),
                                  getAf2Driver().getValue(),
                                  getTGD(), getToc(),
                                  getHealthE5a(), getHealthE5b(), getHealthE1(), getIOD());
    }

}
