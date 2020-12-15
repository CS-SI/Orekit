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

package org.orekit.files.ccsds.ndm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.files.ccsds.utils.CcsdsTimeScale;
import org.orekit.time.TimeScale;
import org.orekit.utils.IERSConventions;

/** This class gathers the meta-data present in the Navigation Data Message (ADM, ODM and TDM).
 * @author Luc Maisonobe
 * @since 11.0
 */
public class NDMMetadata {

    /** Metadata comments. The list contains a string for each line of comment. */
    private List<String> comments;

    /** Time System: used for metadata, orbit state and covariance data. */
    private CcsdsTimeScale timeSystem;

    /** Create a new meta-data.
     */
    public NDMMetadata() {
        comments = new ArrayList<String>();
    }

    /** Get the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @return the time system
     */
    public CcsdsTimeScale getTimeSystem() {
        return timeSystem;
    }

    /** Set the Time System that: for OPM, is used for metadata, state vector,
     * maneuver and covariance data, for OMM, is used for metadata, orbit state
     * and covariance data, for OEM, is used for metadata, ephemeris and
     * covariance data.
     * @param timeSystem the time system to be set
     */
    public void setTimeSystem(final CcsdsTimeScale timeSystem) {
        this.timeSystem = timeSystem;
    }

    /**
     * Get the time scale.
     * @param conventions IERS conventions to use
     * @param dataContext data context ontianing all time scales
     * @return the time scale.
     * @see #getTimeSystem()
     * @throws OrekitException if there is not corresponding time scale.
     */
    public TimeScale getTimeScale(final IERSConventions conventions, final DataContext dataContext) {
        return getTimeSystem().getTimeScale(conventions, dataContext.getTimeScales());
    }

    /** Get the metadata comment.
     * @return metadata comment
     */
    public List<String> getComments() {
        return Collections.unmodifiableList(comments);
    }

    /**
     * Add metadata comment.
     * @param comment metadata comment line
     */
    public void addComment(final String comment) {
        this.comments.add(comment);
    }

}



