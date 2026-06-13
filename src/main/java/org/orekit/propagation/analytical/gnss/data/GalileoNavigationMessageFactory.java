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
 * Factory for {@link GalileoNavigationMessage}.
 * @author Luc Maisonobe
 * @since 14.0
 */
public class GalileoNavigationMessageFactory
    extends AbstractNavigationMessageFactory<GalileoNavigationMessage> {

    /** Issue of Data of the navigation batch. */
    private int iodNav;

    /** Data source. */
    private int dataSource;

    /** E1/E5a broadcast group delay (s). */
    private double bgbE1E5a;

    /** E5b/E1 broadcast group delay (s). */
    private double bgdE5bE1;

    /** Signal in space accuracy. */
    private double sisa;

    /** Satellite health status. */
    private double svHealth;

    /** Simple constructor.
     * @param timeScales known time scales
     * @param system     satellite system to use for interpreting week number
     * @param type       message type (null if not a navigation message)
     * @param inertial   reference inertial frame
     * @param bodyFixed  body fixed frame (will be frozen at {@code date} to build the orbital elements
     */
    public GalileoNavigationMessageFactory(final TimeScales timeScales, final SatelliteSystem system,
                                           final String type, final Frame inertial, final Frame bodyFixed) {
        super(GNSSConstants.GALILEO_AV, GNSSConstants.GALILEO_WEEK_NB, timeScales, system,
              type, inertial, bodyFixed, GNSSConstants.GALILEO_MU);
    }

    /** Get the Issue Of Data (IOD).
     * @return Issue Of Data (IOD)
     */
    public int getIODNav() {
        return iodNav;
    }

    /** Set the Issue Of Data (IOD).
     * @param iodNav Issue Of Data (IOD)
     */
    public void setIODNav(final int iodNav) {
        this.iodNav = iodNav;
    }

    /** Get the data source.
     * @return the data source
     */
    public int getDataSource() {
        return dataSource;
    }

    /** Set the data source.
     * @param dataSource data source
     */
    public void setDataSource(final int dataSource) {
        this.dataSource = dataSource;
    }

    /** Get the E1/E5a broadcast group delay.
     * @return the E1/E5a broadcast group delay (s)
     */
    public double getBGDE1E5a() {
        return bgbE1E5a;
    }

    /** Set the E1/E5a broadcast group delay.
     * @param bgbE1E5a the E1/E5a broadcast group delay (s)
     */
    public void setBGDE1E5a(final double bgbE1E5a) {
        this.bgbE1E5a = bgbE1E5a;
    }

    /** Get the Broadcast Group Delay E5b/E1.
     * @return the Broadcast Group Delay E5b/E1 (s)
     */
    public double getBGDE5bE1() {
        return bgdE5bE1;
    }

    /** Set the Broadcast Group Delay E5b/E1.
     * @param bgdE5bE1 the Broadcast Group Delay E5b/E1 (s)
     */
    public void setBGDE5bE1(final double bgdE5bE1) {
        this.bgdE5bE1 = bgdE5bE1;
    }

    /** Get the signal in space accuracy (m).
     * @return the signal in space accuracy
     */
    public double getSisa() {
        return sisa;
    }

    /** Set the signal in space accuracy (m).
     * @param sisa the signal in space accuracy
     */
    public void setSisa(final double sisa) {
        this.sisa = sisa;
    }

    /** Get the SV health status.
     * @return the SV health status
     */
    public double getSvHealth() {
        return svHealth;
    }

    /** Set the SV health status.
     * @param svHealth the SV health status
     */
    public void setSvHealth(final double svHealth) {
        this.svHealth = svHealth;
    }

    /** {@inheritDoc} */
    @Override
    public GalileoNavigationMessage createFromDrivers() {
        return new GalileoNavigationMessage(getTimeScales(), getType(), getPrn(),
                                            new GNSSDate(getWeek(), getTimeDriver().getValue(), getSystem()),
                                            createOrbitFromDrivers(), getADotDriver().getValue(),
                                            getDeltaN0Driver().getValue(), getDeltaN0DotDriver().getValue(),
                                            getIDotDriver().getValue(), getOmegaDotDriver().getValue(),
                                            getCucDriver().getValue(), getCusDriver().getValue(),
                                            getCrcDriver().getValue(), getCrsDriver().getValue(),
                                            getCicDriver().getValue(), getCisDriver().getValue(),
                                            getAf0Driver().getValue(), getAf1Driver().getValue(),
                                            getAf2Driver().getValue(),
                                            getTGD(), getToc(),
                                            getEpochToc(), getTransmissionTime(),
                                            getIODNav(), getDataSource(),
                                            getBGDE1E5a(), getBGDE5bE1(),
                                            getSisa(), getSvHealth());
    }

}
