/* Copyright 2002-2019 CS Systèmes d'Information
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
package org.orekit.estimation.measurements.gnss;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.orekit.gnss.Frequency;
import org.orekit.time.AbsoluteDate;

/**
 * This class is used to contains all the data computed within cycle-slip detector.
 * All thses attributes are what user can get from the detectors.
 * @author David Soulard
 *
 */
public class CycleSlipDetectorResults {

    /** Integer corresponding to the PRN number of the satellite.*/
    private String satellite;

    /** Date from which for this satellite cycle-slip detection begins.*/
    private Map<Frequency, AbsoluteDate> begin;

    /** Date up to  which for this satellite  cycle-slip detection is valid.*/
    private Map<Frequency, AbsoluteDate> end;

    /** List of date at which cycle slip occurs.*/
    private Map<Frequency, ArrayList<AbsoluteDate>> results;

    /**
     * Constructor.
     * @param satellite name of the satellite considered: "system - PRN" (e.g. "GPS - 07" for the satellite GPS 7.
     * @param date current date
     * @param freq frequency corresponding to the measurements.
     */
    CycleSlipDetectorResults(final String satellite, final AbsoluteDate date, final Frequency freq) {
        this.satellite  = satellite;
        final Map<Frequency, AbsoluteDate> beginMap = new HashMap<>();
        beginMap.put(freq, date);
        this.begin      = beginMap;
        this.end        = new HashMap<Frequency, AbsoluteDate>();
        end.put(freq, date);
        this.results    = new HashMap<>();;
        results.put(freq, new ArrayList<AbsoluteDate>());
    }

    /**
     * Get the satellite name.
     * @return satellite name
     */
    protected String getSatelliteName() {
        return satellite;
    }

    /**
     * Return the end date at the given frequency.
     * @param f frequency
     * @return date of end of validity of the detectors
     */
    protected AbsoluteDate getEndDate(final Frequency f) {
        return end.get(f);
    }

    /**
     * Get beginning date of validity of the cycle slip detector.
     * @return absoluteDate of beginning of validity of the cycle slip detector
     */
    protected Map<Frequency, AbsoluteDate> getBegin() {
        return begin;
    }

    /**
     * Return the date of validity begining of the detector.
     * @param f frequency
     * @return AbsoluteDate
     */
    protected AbsoluteDate getBeginDate(final Frequency f) {
        return begin.get(f);
    }

    /**
     * Get the cycle slip Map with contains the results.
     * @return cycle slip map containing the results
     */
    protected Map<Frequency, ArrayList<AbsoluteDate>> getCycleSlipMap() {
        return results;
    }

    /**
     * Add a new cycle-slip date into the Map at the given frequency.
     * @param f frequency of the measurement used to detect the cycle-slip
     * @param date date of the cycle-slip detected.
     */
    protected void addCycleSlipDate(final Frequency f, final AbsoluteDate date) {
        final ArrayList<AbsoluteDate> newList = results.get(f);
        newList.add(date);
        results.put(f, newList);
    }

    /**
     * Knowing the satellite already exist, adding data at another frequency.
     * @param f frequency corresponding to the data
     * @param date date of measurement
     */
    protected void addAtOtherFrequency(final Frequency f, final AbsoluteDate date) {
        begin.put(f, date);
        end.put(f, date);
        results.put(f, new ArrayList<AbsoluteDate>());
    }

    /**
     * Setter for the ending data.
     * @param f : frequency at which the measurement at current date is taken.
     * @param date : new date of end
     */
    protected void setDate(final Frequency f, final AbsoluteDate date) {
        end.put(f, date);
    }

}
