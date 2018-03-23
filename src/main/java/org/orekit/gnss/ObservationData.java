/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.gnss;

import java.util.ArrayList;

import org.orekit.time.AbsoluteDate;


/** Observation Data set.
 */
public class ObservationData {

    /** Satellite System. */
    private final SatelliteSystem satelliteSystem;

    /** PRN Number of the satellite observed. */
    private final int prnNumber;

    /** Date of the observation. */
    private AbsoluteDate tObs;

    /** List of Observation types. */
    private ArrayList<RinexFrequency> typesObs;

    /** List of observations. */
    private double[] obs;

    /** List of Loss of lock Indicators (LLI). */
    private int[] lli;

    /** List of Signal Strength. */
    private int[] sigStrength;

    /** Receiver clock offset (seconds). */
    private double rcvrClkOffset;

    /**
     * Simple constructor.
     * @param satelliteSystem Satellite system
     * @param prnNumber PRN number
     * @param tObs Observation date
     * @param rcvrClkOffset Receiver clock offset (optional, 0 by default)
     * @param typesObs List of Observation types
     * @param obs List of observations
     * @param lli List of LLI
     * @param sigStrength List of Signal Strengths
     */
    public ObservationData(final SatelliteSystem satelliteSystem,
                           final int prnNumber, final AbsoluteDate tObs,
                           final double rcvrClkOffset, final ArrayList<RinexFrequency> typesObs,
                           final double[] obs, final int[] lli, final int[] sigStrength) {
        this.satelliteSystem = satelliteSystem;
        this.prnNumber       = prnNumber;
        this.tObs            = tObs;
        this.typesObs        = typesObs;
        this.obs             = obs;
        this.lli             = lli;
        this.sigStrength     = sigStrength;
        this.rcvrClkOffset   = rcvrClkOffset;
    }

    /** Get Satellite System.
     * @return satellite system of observed satellite
     */
    public SatelliteSystem getSatelliteSystem() {
        return satelliteSystem;
    }
    /** Get PRN number.
     * @return PRN number of the observed satellite
     */
    public int getPrnNumber() {
        return prnNumber;
    }
    /** Get observation date.
     * @return date of observation
     */
    public AbsoluteDate getTObs() {
        return tObs;
    }
    /** Get list of Observation types.
     * @return list of observation types for the observed satellite
     */
    public ArrayList<RinexFrequency> getTypesObs() {
        return typesObs;
    }
    /** Get receiver clock offset.
     * @return receiver clock offset (it is optional, may be 0)
     */
    public double getRcvrClkOffset() {
        return rcvrClkOffset;
    }
    /** Get rinex observation for a specific observation type.
     * @param type Observation type for which we want to get the observation
     * @return observation for a specific observation type
     */
    public double getObs(final String type) {

        int j = 1;

        for (int i = 0; i < typesObs.size(); i++) {
            if (typesObs.get(i).equals(RinexFrequency.valueOf(type))) {
                j = i;
            }
        }
        return obs[j];
    }
    /** Get LLI for a specific observation type.
     * @param type Observation type for which we want to get the LLI
     * @return LLI for a specific observation type
     */
    public int getLli(final String type) {

        int j = 1;

        for (int i = 0; i < typesObs.size(); i++) {
            if (typesObs.get(i).equals(RinexFrequency.valueOf(type))) {
                j = i;
            }
        }
        return lli[j];
    }
    /** Get Signal Strength for a specific observation type.
     * @param type Observation type for which we want to get the signal strength
     * @return signal strength for a specific observation type
     */
    public int getSignStrength(final String type) {

        int j = 1;

        for (int i = 0; i < typesObs.size(); i++) {
            if (typesObs.get(i).equals(RinexFrequency.valueOf(type))) {
                j = i;
            }
        }
        return sigStrength[j];
    }

}
