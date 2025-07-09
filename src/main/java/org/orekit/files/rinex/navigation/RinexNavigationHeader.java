/* Copyright 2002-2025 CS GROUP
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

import org.orekit.files.rinex.section.Label;
import org.orekit.files.rinex.section.RinexBaseHeader;
import org.orekit.files.rinex.utils.RinexFileType;
import org.orekit.files.rinex.utils.parsing.RinexUtils;
import org.orekit.gnss.SatelliteSystem;
import org.orekit.time.TimeScales;

/** Header for Rinex Navigation.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class RinexNavigationHeader extends RinexBaseHeader {

    /** Index of label in header lines. */
    private static final int LABEL_INDEX = 60;

    /** Ionospheric correction type. */
    private IonosphericCorrectionType ionosphericCorrectionType;

    /** List of time system corrections. */
    private final List<TimeSystemCorrection> timeSystemCorrections;

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

    /** {@inheritDoc} */
    @Override
    public SatelliteSystem parseSatelliteSystem(final String line, final SatelliteSystem defaultSatelliteSystem) {
        if (getFormatVersion() < 3.0) {
            // the satellite system is hidden within the entry, with GPS as default

            // look if default is overridden somewhere in the entry
            final String entry = line.substring(0, 80).toUpperCase();
            for (final SatelliteSystem satelliteSystem : SatelliteSystem.values()) {
                if (entry.contains(satelliteSystem.name())) {
                    // we found a satellite system hidden in the middle of the line
                    return satelliteSystem;
                }
            }

            // return default value
            return defaultSatelliteSystem;

        } else {
            // the satellite system is in column 40 for 3.X and later
            return SatelliteSystem.parseSatelliteSystem(line.substring(40, 41), defaultSatelliteSystem);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void parseProgramRunByDate(final String line, final TimeScales timeScales) {
        parseProgramRunByDate(RinexUtils.parseString(line,  0, 20),
                              RinexUtils.parseString(line, 20, 20),
                              RinexUtils.parseString(line, 40, 20),
                              timeScales);
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

    /** {@inheritDoc} */
    @Override
    public void checkType(final String line, final String name) {
        checkType(line, 20, name);
    }


    /** {@inheritDoc} */
    @Override
    public int getLabelIndex() {
        return LABEL_INDEX;
    }

    /** {@inheritDoc} */
    @Override
    public boolean matchFound(final Label label, final String line) {
        return label.matches(line.substring(getLabelIndex()).trim());
    }

}
