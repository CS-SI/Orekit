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
package org.orekit.gnss.metric.messages.ssr.subtype;

import org.orekit.gnss.metric.messages.ssr.SsrHeader;

/**
 * Container for SSR IM201 header.
 * @author Bryan Cazabonne
 * @since 11.0
 */
public class SsrIm201Header extends SsrHeader {

    /** VTEC quality indicator. */
    private double vtecQualityIndicator;

    /** Number of ionospheric layers. */
    private int numberOfIonosphericLayers;

    /** Constructor. */
    public SsrIm201Header() {
        // Nothing to do ...
    }

    /**
     * Get the VTEC quality indicator.
     * @return the VTEC quality indicator in TECU
     */
    public double getVtecQualityIndicator() {
        return vtecQualityIndicator;
    }

    /**
     * Get the number of ionospheric layers.
     * @return the number of ionospheric layers
     */
    public int getNumberOfIonosphericLayers() {
        return numberOfIonosphericLayers;
    }

    /**
     * Set the VTEC quality indicator.
     * @param vtecQualityIndicator the VTEC quality indicator to set in TECU
     */
    public void setVtecQualityIndicator(final double vtecQualityIndicator) {
        this.vtecQualityIndicator = vtecQualityIndicator;
    }

    /**
     * Set the number of ionospheric layers.
     * @param numberOfIonosphericLayers the number to set
     */
    public void setNumberOfIonosphericLayers(final int numberOfIonosphericLayers) {
        this.numberOfIonosphericLayers = numberOfIonosphericLayers;
    }

}
