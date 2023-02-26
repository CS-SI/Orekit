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
package org.orekit.gnss.navigation;

import java.util.ArrayList;
import java.util.List;

import org.orekit.gnss.RinexBaseHeader;
import org.orekit.gnss.RinexFileType;
import org.orekit.gnss.navigation.RinexNavigation.TimeSystemCorrection;

/** Header for Rinex Navigation.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexNavigationHeader extends RinexBaseHeader {

    /** Ionospheric correction type. */
    private String ionosphericCorrectionType;

    /** List of time system corrections. */
    private List<TimeSystemCorrection> timeSystemCorrections;

    /** Current number of leap seconds. */
    private int numberOfLeapSeconds;

    /** Simple constructor.
     */
    public RinexNavigationHeader() {
        super(RinexFileType.NAVIGATION);
        this.timeSystemCorrections = new ArrayList<>();
    }

    /**
     * Getter for the ionospheric correction type.
     * <p>
     * Only the three first characters are given (e.g. GAL, GPS, QZS, BDS, or IRN)
     * </p>
     * @return the ionospheric correction type
     */
    public String getIonosphericCorrectionType() {
        return ionosphericCorrectionType;
    }

    /**
     * Setter for the ionospheric correction type.
     * @param ionosphericCorrectionType the ionospheric correction type to set
     */
    public void setIonosphericCorrectionType(final String ionosphericCorrectionType) {
        this.ionosphericCorrectionType = ionosphericCorrectionType;
    }

    /**
     * Getter for the time system corrections contained in the file header.
     * <p>
     * Corrections to transform the system time to UTC or oter time system.
     * </p>
     * @return the list of time system corrections
     */
    public List<TimeSystemCorrection> getTimeSystemCorrections() {
        return timeSystemCorrections;
    }

    /**
     * Add a time system correction to the list.
     * @param timeSystemCorrection the element to add
     */
    public void addTimeSystemCorrections(final TimeSystemCorrection timeSystemCorrection) {
        this.timeSystemCorrections.add(timeSystemCorrection);
    }

    /**
     * Getter for the current number of leap seconds.
     * @return the current number of leap seconds
     */
    public int getNumberOfLeapSeconds() {
        return numberOfLeapSeconds;
    }

    /**
     * Setter for the current number of leap seconds.
     * @param numberOfLeapSeconds the number of leap seconds to set
     */
    public void setNumberOfLeapSeconds(final int numberOfLeapSeconds) {
        this.numberOfLeapSeconds = numberOfLeapSeconds;
    }

}
