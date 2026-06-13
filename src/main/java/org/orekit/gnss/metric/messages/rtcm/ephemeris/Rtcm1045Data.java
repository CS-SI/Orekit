/* Copyright 2002-2026 CS GROUP
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
package org.orekit.gnss.metric.messages.rtcm.ephemeris;

import org.orekit.gnss.metric.messages.common.AccuracyProvider;
import org.orekit.propagation.analytical.gnss.data.GalileoNavigationMessageFactory;

/**
 * Container for RTCM 1045 data.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class Rtcm1045Data extends Rtcm1046Data {

    /** Galileo NAV Data Validity Status. */
    private int galileoDataValidityStatus;

    /** Constructor.
     * @param satelliteId satellite ID
     * @param accuracyProvider accuracy provider
     * @param factory factory for Galileo navigation message
     * @since 14.0
     */
    public Rtcm1045Data(final int satelliteId, final AccuracyProvider accuracyProvider,
                        final GalileoNavigationMessageFactory factory) {
        super(satelliteId, accuracyProvider, factory);
    }

    /**
     * Get the Galileo data validity status.
     * @return the Galileo data validity status
     */
    public int getGalileoDataValidityStatus() {
        return galileoDataValidityStatus;
    }

    /**
     * Set the Galileo data validity status.
     * @param galileoDataValidityStatus the validity status to set
     */
    public void setGalileoDataValidityStatus(final int galileoDataValidityStatus) {
        this.galileoDataValidityStatus = galileoDataValidityStatus;
    }

}
