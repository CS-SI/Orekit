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
import org.orekit.time.GNSSDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link BeidouAlmanac}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class BeidouAlmanacFactory extends GNSSOrbitalElementsFactory<BeidouAlmanac> {

    /** Health status. */
    private int health;

    /** Simple constructor.
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     */
    public BeidouAlmanacFactory(final TimeScales timeScales, final SatelliteSystem system,
                                final Frame inertial, final Frame bodyFixed) {
        super(GNSSConstants.BEIDOU_AV, timeScales, system,
              null, inertial, bodyFixed, GNSSConstants.BEIDOU_MU);
    }

    /** Get the Health status.
     * @return the Health status
     */
    public int getHealth() {
        return health;
    }

    /** Set the Health status.
     * @param health the Health status
     */
    public void setHealth(final int health) {
        this.health = health;
    }

    /** {@inheritDoc} */
    @Override
    public BeidouAlmanac createFromDrivers() {
        return new BeidouAlmanac(getTimeScales(), getPrn(),
                                 new GNSSDate(getWeek(), getTimeDriver().getValue(), getSystem()),
                                 createOrbitFromDrivers(),
                                 getADotDriver().getValue(),
                                 getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                                 getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                                 getCucDriver().getValue(), getCusDriver().getValue(),
                                 getCrcDriver().getValue(), getCrsDriver().getValue(),
                                 getCicDriver().getValue(), getCisDriver().getValue(),
                                 getAf0Driver().getValue(), getAf1Driver().getValue(),
                                 getAf2Driver().getValue(),
                                 getTGD(), getToc(),
                                 getHealth());
    }

}
