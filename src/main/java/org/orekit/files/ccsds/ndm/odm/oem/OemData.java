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

package org.orekit.files.ccsds.ndm.odm.oem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.ndm.odm.CartesianCovariance;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Data;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * The Ephemerides data blocks class contain list of orbital data points.
 * @author sports
 */
public class OemData extends CommentsContainer implements Data {

    /** List of ephemerides data lines. */
    private List<TimeStampedPVCoordinates> ephemeridesDataLines;

    /** Enumerate for selecting which derivatives to use in {@link #ephemeridesDataLines}. */
    private CartesianDerivativesFilter cartesianDerivativesFilter;

    /** List of covariance matrices. */
    private List<CartesianCovariance> covarianceMatrices;

    /** EphemeridesBlock constructor. */
    public OemData() {
        ephemeridesDataLines       = new ArrayList<>();
        covarianceMatrices         = new ArrayList<>();
        cartesianDerivativesFilter = CartesianDerivativesFilter.USE_PVA;
    }

    /** Add a data point.
     * @param data data point to add
     * @param hasAcceleration true if the current data point has acceleration data.
     * @return always return {@code true}
     */
    public boolean addData(final TimeStampedPVCoordinates data, final boolean hasAcceleration) {
        ephemeridesDataLines.add(data);
        if (!hasAcceleration) {
            // as soon as one point misses acceleration we consider it is not available at all
            cartesianDerivativesFilter = CartesianDerivativesFilter.USE_PV;
        }
        return true;
    }

    /** Add a covariance matrix.
     * @param covarianceMatrix covariance matrix to dd
     */
    public void addCovarianceMatrix(final CartesianCovariance covarianceMatrix) {
        covarianceMatrices.add(covarianceMatrix);
    }

    /** Get the list of Ephemerides data lines.
     * @return a reference to the internal list of Ephemerides data lines
     */
    public List<TimeStampedPVCoordinates> getEphemeridesDataLines() {
        return Collections.unmodifiableList(ephemeridesDataLines);
    }

    /** Get the derivatives available in the block.
     * @return derivatives available in the block
     */
    public CartesianDerivativesFilter getAvailableDerivatives() {
        return cartesianDerivativesFilter;
    }

    /** Get an unmodifiable view of the data points.
     * @return unmodifiable view of the data points
     */
    public List<TimeStampedPVCoordinates> getCoordinates() {
        return Collections.unmodifiableList(ephemeridesDataLines);
    }

    /** Get an unmodifiable view of Covariance Matrices.
     * @return unmodifiable view of Covariance Matrices
     */
    public List<CartesianCovariance> getCovarianceMatrices() {
        return Collections.unmodifiableList(covarianceMatrices);
    }

}
