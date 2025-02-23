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
package org.orekit.files.ccsds.ndm.adm.aem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * The Attitude Ephemerides data blocks class contain list of attitude data points.
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 * @author Bryan Cazabonne
 */
public class AemData extends CommentsContainer implements Data {

    /** List of data lines. */
    private final List<TimeStampedAngularCoordinates> attitudeDataLines;

    /**
     * Constructor.
     */
    public AemData() {
        attitudeDataLines = new ArrayList<>();
    }

    /** Add a data point.
     * @param data data point to add
     * @return always return {@code true}
     */
    public boolean addData(final TimeStampedAngularCoordinates data) {
        refuseFurtherComments();
        attitudeDataLines.add(data);
        return true;
    }

    /** Get an unmodifiable view of the data points.
     * @return unmodifiable view of the data points
     */
    public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
        return Collections.unmodifiableList(attitudeDataLines);
    }

}
