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
package org.orekit.files.rinex.navigation;

import java.util.ArrayList;
import java.util.List;

import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.utils.RinexFileType;

/** Header for Rinex Navigation.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexNavigationHeader extends RinexBaseHeader {

    /** Ionospheric correction type. */
    private IonosphericCorrectionType ionosphericCorrectionType;

    /** List of time system corrections. */
    private List<TimeSystemCorrection> timeSystemCorrections;

    /** Number of merged files. */
    private int mergedFiles;

    /** Current number of leap seconds. */
    private int numberOfLeapSeconds;

    /** Simple constructor.
     */
    public RinexNavigationHeader() {
        super(RinexFileType.NAVIGATION);
        this.timeSystemCorrections = new ArrayList<>();
        this.mergedFiles           = -1;
        this.numberOfLeapSeconds   = -1;
    }

    /**
     * Getter for the ionospheric correction type.
     * @return the ionospheric correction type
     */
    public IonosphericCorrectionType getIonosphericCorrectionType() {
        return ionosphericCorrectionType;
    }

    /**
     * Setter for the ionospheric correction type.
     * @param ionosphericCorrectionType the ionospheric correction type to set
     */
    public void setIonosphericCorrectionType(final IonosphericCorrectionType ionosphericCorrectionType) {
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
     * Getter for the number of merged files.
     * @return the number of merged files
     */
    public int getMergedFiles() {
        return mergedFiles;
    }

    /**
     * Setter for the number of merged files.
     * @param mergedFiles the number of merged files
     */
    public void setMergedFiles(final int mergedFiles) {
        this.mergedFiles = mergedFiles;
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
