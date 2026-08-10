/* Copyright 2022-2026 Luc Maisonobe
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

import org.hipparchus.CalculusFieldElement;
import org.orekit.orbits.FieldKeplerianOrbit;
import org.orekit.time.FieldGNSSDate;
import org.orekit.time.TimeScales;

/**
 * Container for data contained in a GPS/QZNSS legacy navigation message.
 * @param <T> type of the field elements
 * @param <O> type of the orbital elements (non-field version)
 * @author Luc Maisonobe
 * @since 13.0
 */
public abstract class FieldLegacyNavigationMessage<T extends CalculusFieldElement<T>,
                                                   O extends LegacyNavigationMessage<O>>
    extends FieldAbstractNavigationMessage<T, O> {

    /** Issue of Data, Ephemeris. */
    private final int iode;

    /** Issue of Data, Clock. */
    private final int iodc;

    /** The user SV accuracy (m). */
    private final T svAccuracy;

    /** Satellite health status. */
    private final int svHealth;

    /** Fit interval. */
    private final int fitInterval;

    /** Codes on L2 channel.
     * @since 14.0
     */
    private final int l2Codes;

    /** L2 P data flags.
     * @since 14.0
     */
    private final int l2PFlags;

    /** Creates a new instance.
     * @param angularVelocity  mean angular velocity of the Earth for the GNSS model
     * @param weeksInCycle     number of weeks in the GNSS cycle
     * @param timeScales       known time scales
     * @param type             type (null if not a navigation message)
     * @param prn              PRN number of the satellite
     * @param toe              time of ephemeris (<em>must</em> be consistent with {@code orbit})
     * @param orbit            Keplerian orbit in Earth-frozen frame
     * @param nonKeplerian     15 non-Keplerian parameters (in the order given by {@link NonKeplerianDriversFactory}
     * @param tgd              group delay differential TGD for L1-L2 correction
     * @param toc              time of clock
     * @param transmissionTime transmission time
     * @param iode             issue of data, ephemeris
     * @param iodc             issue of data, clock
     * @param svAccuracy       user SV accuracy (m)
     * @param svHealth         satellite health status
     * @param fitInterval      fit interval
     * @param l2Codes          codes on L2 channel
     * @param l2PFlags         L2 P data flags.
     * @since 14.0
     */
    public FieldLegacyNavigationMessage(final double angularVelocity, final int weeksInCycle,
                                        final TimeScales timeScales, final String type, final int prn,
                                        final FieldGNSSDate<T> toe, final FieldKeplerianOrbit<T> orbit,
                                        final T[] nonKeplerian, final T tgd,
                                        final FieldGNSSDate<T> toc, final FieldGNSSDate<T>  transmissionTime,
                                        final int iode, final int iodc, final T svAccuracy,
                                        final int svHealth, final int fitInterval,
                                        final int l2Codes, final int l2PFlags) {
        super(angularVelocity, weeksInCycle, timeScales, type, prn, toe,
              orbit, nonKeplerian, tgd, toc, transmissionTime);
        this.iode        = iode;
        this.iodc        = iodc;
        this.svAccuracy  = svAccuracy;
        this.svHealth    = svHealth;
        this.fitInterval = fitInterval;
        this.l2Codes     = l2Codes;
        this.l2PFlags    = l2PFlags;
    }

    /**
     * Getter for the Issue Of Data Ephemeris (IODE).
     * @return the Issue Of Data Ephemeris (IODE)
     */
    public int getIODE() {
        return iode;
    }

    /**
     * Getter for the Issue Of Data Clock (IODC).
     * @return the Issue Of Data Clock (IODC)
     */
    public int getIODC() {
        return iodc;
    }

    /**
     * Getter for the user SV accuray (meters).
     * @return the user SV accuracy
     */
    public T getSvAccuracy() {
        return svAccuracy;
    }

    /**
     * Getter for the satellite health status.
     * @return the satellite health status
     */
    public int getSvHealth() {
        return svHealth;
    }

    /**
     * Getter for the fit interval.
     * @return the fit interval
     */
    public int getFitInterval() {
        return fitInterval;
    }

    /** Get the codes on L2 channel.
     * @return codes on L2 channel
     * @since 14.0
     */
    public int getL2Codes() {
        return l2Codes;
    }

    /** Get the L2 P data flags.
     * @return L2 P data flags
     * @since 14.0
     */
    public int getL2PFlags() {
        return l2PFlags;
    }

}
