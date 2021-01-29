/* Copyright 2002-2021 CS GROUP
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

import org.orekit.files.ccsds.ndm.odm.OCommonMetadata;
import org.orekit.time.AbsoluteDate;

/** Metadata for Orbit Ephemeris Message files.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OEMMetadata extends OCommonMetadata {

    /** Start of total time span covered by ephemerides data and covariance data. */
    private AbsoluteDate startTime;

    /** End of total time span covered by ephemerides data and covariance data. */
    private AbsoluteDate stopTime;

    /** Start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStopTime;

    /** The interpolation method to be used. */
    private String interpolationMethod;

    /** The interpolation degree. */
    private int interpolationDegree;

    /** Simple constructor.
     * @param defaultInterpolationDegree default interpolation degree
     */
    public OEMMetadata(final int defaultInterpolationDegree) {
        this.interpolationDegree = defaultInterpolationDegree;
    }

    /** Get start of total time span covered by ephemerides data and
     * covariance data.
     * @return the start time
     */
    public AbsoluteDate getStartTime() {
        return startTime;
    }

    /** Set start of total time span covered by ephemerides data and
     * covariance data.
     * @param startTime the time to be set
     */
    public void setStartTime(final AbsoluteDate startTime) {
        refuseFurtherComments();
        this.startTime = startTime;
    }

    /** Get end of total time span covered by ephemerides data and covariance
     * data.
     * @return the stop time
     */
    public AbsoluteDate getStopTime() {
        return stopTime;
    }

    /** Set end of total time span covered by ephemerides data and covariance
     * data.
     * @param stopTime the time to be set
     */
    public void setStopTime(final AbsoluteDate stopTime) {
        refuseFurtherComments();
        this.stopTime = stopTime;
    }

    /** Get start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @return the useable start time
     */
    public AbsoluteDate getUseableStartTime() {
        return useableStartTime;
    }

    /** Set start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @param useableStartTime the time to be set
     */
    public void setUseableStartTime(final AbsoluteDate useableStartTime) {
        refuseFurtherComments();
        this.useableStartTime = useableStartTime;
    }

    /** Get end of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @return the useable stop time
     */
    public AbsoluteDate getUseableStopTime() {
        return useableStopTime;
    }

    /** Set end of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation.
     * @param useableStopTime the time to be set
     */
    public void setUseableStopTime(final AbsoluteDate useableStopTime) {
        refuseFurtherComments();
        this.useableStopTime = useableStopTime;
    }

    /**
     * Get the start date of this ephemeris segment.
     *
     * @return ephemeris segment start date.
     */
    public AbsoluteDate getStart() {
        // useable start time overrides start time if it is set
        final AbsoluteDate start = this.getUseableStartTime();
        if (start != null) {
            return start;
        } else {
            return this.getStartTime();
        }
    }

    /**
     * Get the stop date of this ephemeris segment.
     *
     * @return ephemeris segment stop date.
     */
    public AbsoluteDate getStop() {
        // useable stop time overrides stop time if it is set
        final AbsoluteDate stop = this.getUseableStopTime();
        if (stop != null) {
            return stop;
        } else {
            return this.getStopTime();
        }
    }

    /** Get the interpolation method to be used.
     * @return the interpolation method
     */
    public String getInterpolationMethod() {
        return interpolationMethod;
    }

    /** Set the interpolation method to be used.
     * @param interpolationMethod the interpolation method to be set
     */
    public void setInterpolationMethod(final String interpolationMethod) {
        refuseFurtherComments();
        this.interpolationMethod = interpolationMethod;
    }

    /** Get the interpolation degree.
     * @return the interpolation degree
     */
    public int getInterpolationDegree() {
        return interpolationDegree;
    }

    /** Set the interpolation degree.
     * @param interpolationDegree the interpolation degree to be set
     */
    public void setInterpolationDegree(final int interpolationDegree) {
        refuseFurtherComments();
        this.interpolationDegree = interpolationDegree;
    }

}



