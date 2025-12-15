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
 * Factory for {@link QZSSAlmanac}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class QZSSAlmanacFactory extends GNSSOrbitalElementsFactory<QZSSAlmanac> {

    /** Source of the almanac. */
    private String source;

    /** Health status. */
    private int health;

    /** Simple constructor.
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param type            message type (null if not a navigation message)
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     * @param date            date of the orbital parameters
     */
    public QZSSAlmanacFactory(final TimeScales timeScales, final SatelliteSystem system,
                              final String type, final Frame inertial, final Frame bodyFixed,
                              final AbsoluteDate date) {
        super(GNSSConstants.QZSS_AV, GNSSConstants.QZSS_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, date, GNSSConstants.QZSS_MU);
    }

    /** Get the source of this QZSS almanac.
     * @return the source of this QZSS almanac
     */
    public String getSource() {
        return source;
    }

    /** Set the source of this QZSS almanac.
     * @param source source of this QZSS almanac
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /** Get the Health status.
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /** Set the Health status.
     * @param health Health status
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /** {@inheritDoc} */
    @Override
    public QZSSAlmanac createFromDrivers() {
        return new QZSSAlmanac(getTimeScales(), getSystem(), getPrn(), getWeek(),
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
                               getSource(), getHealth());
    }

}
