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
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScales;

/**
 * Factory for {@link GPSAlmanac}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GPSAlmanacFactory extends GNSSOrbitalElementsFactory<GPSAlmanac> {

    /** Source of the almanac. */
    private String source;

    /** SVN number. */
    private int svn;

    /** Health status. */
    private int health;

    /** Average URA. */
    private int ura;

    /** Satellite configuration. */
    private int satConfiguration;

    /** Simple constructor.
     * @param timeScales      known time scales
     * @param system          satellite system to use for interpreting week number
     * @param inertial        reference inertial frame
     * @param bodyFixed       body fixed frame (will be frozen at {@code date} to build the orbital elements
     */
    public GPSAlmanacFactory(final TimeScales timeScales, final SatelliteSystem system,
                             final Frame inertial, final Frame bodyFixed) {
        super(GNSSConstants.GPS_AV, timeScales, system, null,
              inertial, bodyFixed, GNSSConstants.GPS_MU);
    }

    /** Get the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     * @return the source of this GPS almanac
     */
    public String getSource() {
        return source;
    }

    /** Set the source of this GPS almanac.
     * <p>Sources can be SEM or YUMA, when the almanac is read from a file.</p>
     * @param source source of this GPS almanac
     */
    public void setSource(final String source) {
        this.source = source;
    }

    /** Get the satellite "SVN" reference number.
     * @return the satellite "SVN" reference number
     */
    public int getSVN() {
        return svn;
    }

    /** Set the satellite "SVN" reference number.
     * @param svn the satellite "SVN" reference number
     */
    public void setSVN(final int svn) {
        this.svn = svn;
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

    /** Get the average URA number.
     * @return the average URA number
     */
    public int getURA() {
        return ura;
    }

    /** Set the average URA number.
     * @param ura the average URA number
     */
    public void setURA(final int ura) {
        this.ura = ura;
    }

    /** Get the satellite configuration.
     * @return the satellite configuration
     */
    public int getSatConfiguration() {
        return satConfiguration;
    }

    /** Set the satellite configuration.
     * @param satConfiguration the satellite configuration
     */
    public void setSatConfiguration(final int satConfiguration) {
        this.satConfiguration = satConfiguration;
    }

    /** {@inheritDoc} */
    @Override
    public GPSAlmanac createFromDrivers() {
        return new GPSAlmanac(getTimeScales(), getSystem(), getPrn(),
                              createOrbitFromDrivers(), getADotDriver().getValue(),
                              getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                              getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                              getCucDriver().getValue(), getCusDriver().getValue(),
                              getCrcDriver().getValue(), getCrsDriver().getValue(),
                              getCicDriver().getValue(), getCisDriver().getValue(),
                              getAf0Driver().getValue(), getAf1Driver().getValue(),
                              getAf2Driver().getValue(),
                              getTGD(), getToc(),
                              getSource(), getSVN(), getHealth(), getURA(), getSatConfiguration());
    }

}
