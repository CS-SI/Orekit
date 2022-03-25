/* Copyright 2002-2022 CS GROUP
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

package org.orekit.estimation.measurements.filtering;

import org.orekit.gnss.ObservationData;
import org.orekit.gnss.ObservationDataSet;

/**Class used to store modified ObservationData along with the ObservationDataSet it originates from.
 * Used for pseudo-range smoothing, in the pseudo-range smoother classes.
 *
 * @author Louis Aucouturier
 *
 */
public class ObservationDataSetUpdate {

    /** Modified observation data. */
    private ObservationData newObsData;

    /** Original observationDataSet used to obtain newObsData. */
    private ObservationDataSet ObsDataSet;


    /** Simple constructor.
     *
     * @param newObsData
     * @param obsDataSet
     */
    public ObservationDataSetUpdate(final ObservationData newObsData, final ObservationDataSet obsDataSet) {
        this.newObsData = newObsData;
        this.ObsDataSet = obsDataSet;
    }

    /** Getter for the modified ObservationData object.
     *
     * @return the newObsData
     */
    public final ObservationData getNewObsData() {
        return newObsData;
    }

    /** Getter for the original ObservationDataSet.
     *
     * @return the obsDataSet
     */
    public final ObservationDataSet getObsDataSet() {
        return ObsDataSet;
    }

    /** Setter for the modified ObservationData.
     *
     * @param newObsData the newObsData to set
     */
    public final void setNewObsData(final ObservationData newObsData) {
        this.newObsData = newObsData;
    }

    /** Setter for the ObservationDataSet.
     *
     * @param obsDataSet the obsDataSet to set
     */
    public final void setObsDataSet(final ObservationDataSet obsDataSet) {
        ObsDataSet = obsDataSet;
    }
}
