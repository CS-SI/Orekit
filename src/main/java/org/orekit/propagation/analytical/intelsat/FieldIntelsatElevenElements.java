/* Copyright 2002-2025 Airbus Defence and Space
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * Airbus Defence and Space licenses this file to You under the Apache License, Version 2.0
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
package org.orekit.propagation.analytical.intelsat;

import org.hipparchus.CalculusFieldElement;
import org.orekit.time.FieldAbsoluteDate;

/**
 * This class is a container for a single set of Intelsat's 11 Elements data.
 * <p>
 * Intelsat's 11 elements are defined in ITU-R S.1525 standard.
 * </p>
 *
 * @author Bryan Cazabonne
 * @since 12.1
 */
public class FieldIntelsatElevenElements<T extends CalculusFieldElement<T>> {

    /**
     * Elements epoch.
     */
    private final FieldAbsoluteDate<T> epoch;

    /**
     * Mean longitude (East of Greenwich).
     */
    private final T lm0;

    /**
     * Drift rate.
     */
    private final T lm1;

    /**
     * Drift acceleration.
     */
    private final T lm2;

    /**
     * Longitude oscillation-amplitude for the cosine term.
     */
    private final T lonC;

    /**
     * Rate of change of longitude, for the cosine term.
     */
    private final T lonC1;

    /**
     * Longitude oscillation-amplitude for the sine term.
     */
    private final T lonS;

    /**
     * Rate of change of longitude, for the sine term.
     */
    private final T lonS1;

    /**
     * Latitude oscillation-amplitude for the cosine term.
     */
    private final T latC;

    /**
     * Rate of change of latitude, for the cosine term.
     */
    private final T latC1;

    /**
     * Latitude oscillation-amplitude for the sine term.
     */
    private final T latS;

    /**
     * Rate of change of latitude, for the sine term.
     */
    private final T latS1;

    /**
     * Constructor.
     *
     * @param epoch elements epoch
     * @param lm0   mean longitude (East of Greenwich) in degrees
     * @param lm1   drift rate in degrees/day
     * @param lm2   drift acceleration in degrees/day/day
     * @param lonC  longitude oscillation-amplitude for the cosine term in degrees
     * @param lonC1 rate of change of longitude, for the cosine term, in degrees/day
     * @param lonS  longitude oscillation-amplitude for the sine term in degrees
     * @param lonS1 rate of change of longitude, for the sine term, in degrees/day
     * @param latC  latitude oscillation-amplitude for the cosine term in degrees
     * @param latC1 rate of change of latitude, for the cosine term, in degrees/day
     * @param latS  latitude oscillation-amplitude for the sine term in degrees
     * @param latS1 rate of change of latitude, for the sine term, in degrees/day
     */
    public FieldIntelsatElevenElements(final FieldAbsoluteDate<T> epoch, final T lm0, final T lm1, final T lm2, final T lonC, final T lonC1, final T lonS, final T lonS1,
                                       final T latC, final T latC1, final T latS, final T latS1) {
        this.epoch = epoch;
        this.lm0 = lm0;
        this.lm1 = lm1;
        this.lm2 = lm2;
        this.lonC = lonC;
        this.lonC1 = lonC1;
        this.lonS = lonS;
        this.lonS1 = lonS1;
        this.latC = latC;
        this.latC1 = latC1;
        this.latS = latS;
        this.latS1 = latS1;
    }

    /**
     * Get the elements epoch.
     *
     * @return elements epoch
     */
    public FieldAbsoluteDate<T> getEpoch() {
        return epoch;
    }

    /**
     * Get the mean longitude (East of Greenwich).
     *
     * @return the mean longitude (East of Greenwich) in degrees
     */
    public T getLm0() {
        return lm0;
    }

    /**
     * Get the drift rate.
     *
     * @return the drift rate in degrees/day
     */
    public T getLm1() {
        return lm1;
    }

    /**
     * Get the drift acceleration.
     *
     * @return the drift acceleration in degrees/day/day
     */
    public T getLm2() {
        return lm2;
    }

    /**
     * Get the longitude oscillation-amplitude for the cosine term.
     *
     * @return the longitude oscillation-amplitude for the cosine term in degrees
     */
    public T getLonC() {
        return lonC;
    }

    /**
     * Get the rate of change of longitude, for the cosine term.
     *
     * @return the rate of change of longitude, for the cosine term, in degrees/day
     */
    public T getLonC1() {
        return lonC1;
    }

    /**
     * Get the longitude oscillation-amplitude for the sine term.
     *
     * @return the longitude oscillation-amplitude for the sine term in degrees
     */
    public T getLonS() {
        return lonS;
    }

    /**
     * Get the rate of change of longitude, for the sine term.
     *
     * @return the rate of change of longitude, for the sine term, in degrees/day
     */
    public T getLonS1() {
        return lonS1;
    }

    /**
     * Get the latitude oscillation-amplitude for the cosine term.
     *
     * @return the latitude oscillation-amplitude for the cosine term in degrees
     */
    public T getLatC() {
        return latC;
    }

    /**
     * Get the rate of change of latitude, for the cosine term.
     *
     * @return the rate of change of latitude, for the cosine term, in degrees/day
     */
    public T getLatC1() {
        return latC1;
    }

    /**
     * Get the latitude oscillation-amplitude for the sine term.
     *
     * @return the latitude oscillation-amplitude for the sine term in degrees
     */
    public T getLatS() {
        return latS;
    }

    /**
     * Get the rate of change of latitude, for the sine term.
     *
     * @return the rate of change of latitude, for the sine term, in degrees/day
     */
    public T getLatS1() {
        return latS1;
    }
}
