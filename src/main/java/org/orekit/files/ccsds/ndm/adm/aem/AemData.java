/* Copyright 2002-2024 CS GROUP
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
