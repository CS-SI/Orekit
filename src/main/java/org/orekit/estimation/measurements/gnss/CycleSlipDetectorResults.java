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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.orekit.gnss.Frequency;
import org.orekit.time.AbsoluteDate;

/**
 * This class is used to contains all the data computed within cycle-slip detector.
 * All these parameters are what user can get from the detectors.
 * @author David Soulard
 * @since 10.2
 */
public class CycleSlipDetectorResults {

    /** Integer corresponding to the PRN number of the satellite. */
    private String satellite;

    /** Date from which for this satellite cycle-slip detection begins. */
    private Map<Frequency, AbsoluteDate> begin;

    /** Date up to  which for this satellite  cycle-slip detection is valid. */
    private Map<Frequency, AbsoluteDate> end;

    /** List of date at which cycle slip occurs. */
    private Map<Frequency, List<AbsoluteDate>> results;

    /**
     * Constructor.
     * @param satellite name of the satellite considered: "system - PRN" (e.g. "GPS - 07" for the satellite GPS 7.
     * @param date current date
     * @param freq frequency corresponding to the measurements.
     */
    CycleSlipDetectorResults(final String satellite, final AbsoluteDate date, final Frequency freq) {
        this.begin     = new HashMap<>();
        this.end       = new HashMap<>();
        this.results   = new HashMap<>();
        this.satellite = satellite;
        begin.put(freq, date);
        end.put(freq, date);
        results.put(freq, new ArrayList<AbsoluteDate>());
    }

    /**
     * Get the satellite name.
     * @return satellite name
     */
    public String getSatelliteName() {
        return satellite;
    }

    /**
     * Return the end date at the given frequency.
     * <p>
     * For dual-Frequency cycle-slip detector, the {@link Frequency} contained
     * in the map is the higher frequency (e.g. for L1-L2 the frequency in the map will be L1)
     * </p>
     * @param f frequency
     * @return date of end of validity of the detectors
     */
    public AbsoluteDate getEndDate(final Frequency f) {
        return end.get(f);
    }

    /**
     * Return the date of validity beginning of the detector.
     * @param f frequency
     * @return AbsoluteDate
     */
    public AbsoluteDate getBeginDate(final Frequency f) {
        return begin.get(f);
    }

    /**
     * Get the cycle slip Map with contains the results.
     * <p>
     * For dual-Frequency cycle-slip detector, the {@link Frequency} contained
     * in the map is the higher frequency (e.g. for L1-L2 the frequency in the map will be L1)
     * </p>
     * @return cycle slip map containing the results
     */
    public Map<Frequency, List<AbsoluteDate>> getCycleSlipMap() {
        return results;
    }

    /**
     * Add a new cycle-slip date into the Map at the given frequency.
     * @param f frequency of the measurement used to detect the cycle-slip
     * @param date date of the cycle-slip detected.
     */
    void addCycleSlipDate(final Frequency f, final AbsoluteDate date) {
        final List<AbsoluteDate> newList = results.get(f);
        newList.add(date);
        results.put(f, newList);
    }

    /**
     * Knowing the satellite already exist, adding data at another frequency.
     * @param f frequency corresponding to the data
     * @param date date of measurement
     */
    void addAtOtherFrequency(final Frequency f, final AbsoluteDate date) {
        begin.put(f, date);
        end.put(f, date);
        results.put(f, new ArrayList<AbsoluteDate>());
    }

    /**
     * Setter for the ending data.
     * @param f frequency at which the measurement at current date is taken.
     * @param date new date of end
     */
    void setDate(final Frequency f, final AbsoluteDate date) {
        end.put(f, date);
    }

}
