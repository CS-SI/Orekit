/* Copyright 2002-2020 CS GROUP
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

import org.orekit.files.ccsds.ndm.NDMData;
import org.orekit.utils.AngularDerivativesFilter;
import org.orekit.utils.TimeStampedAngularCoordinates;

/**
 * The Attitude Ephemerides data blocks class contain list of attitude data points.
 * @author Bryan Cazabonne
 */
public class AEMData implements NDMData {

    /** List of data lines. */
    private final List<TimeStampedAngularCoordinates> attitudeDataLines;

    /** Data Lines comments. The list contains a string for each line of comment. */
    private List<String> attitudeDataLinesComment;

    /** Enumerate for selecting which derivatives to use in {@link #attitudeDataLines}. */
    private AngularDerivativesFilter angularDerivativesFilter;

    /**
     * Constructor.
     */
    public AEMData() {
        attitudeDataLines        = new ArrayList<>();
        attitudeDataLinesComment = new ArrayList<>();
    }

    /** Add a data point.
     * @param data data point to add
     */
    public void addData(final TimeStampedAngularCoordinates data) {
        attitudeDataLines.add(data);
    }

    /** Get an unmodifiable view of the data points.
     * @return unmodifiable vie of the data points
     */
    public List<TimeStampedAngularCoordinates> getAngularCoordinates() {
        return Collections.unmodifiableList(attitudeDataLines);
    }

    /** Get the derivatives available in the block.
     * @return derivatives available in the block
     */
    public AngularDerivativesFilter getAvailableDerivatives() {
        return angularDerivativesFilter;
    }

    /** Update the {@link #getAvailableDerivatives() available derivatives}.
     * @param pointAngularDerivativesFilter enumerate for selecting which derivatives to use in attitude data.
     */
    void updateAngularDerivativesFilter(final AngularDerivativesFilter pointAngularDerivativesFilter) {
        this.angularDerivativesFilter = pointAngularDerivativesFilter;
    }

    /** Get the attitude data lines comment.
     * @return the comment
     */
    public List<String> getAttitudeDataLinesComment() {
        return attitudeDataLinesComment;
    }

    /** Add an attitude data line comment.
     * @param comment comment line to add
     */
    public void addComment(final String comment) {
        this.attitudeDataLinesComment.add(comment);
    }

}
