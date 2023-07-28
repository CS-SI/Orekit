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

import org.orekit.files.ccsds.ndm.odm.OdmCommonMetadata;
import org.orekit.time.AbsoluteDate;

/** Metadata for Orbit Ephemeris Messages.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OemMetadata extends OdmCommonMetadata {

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
    private InterpolationMethod interpolationMethod;

    /** The interpolation degree. */
    private int interpolationDegree;

    /** Simple constructor.
     * @param defaultInterpolationDegree default interpolation degree
     */
    public OemMetadata(final int defaultInterpolationDegree) {
        this.interpolationDegree = defaultInterpolationDegree;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        checkMandatoryEntriesExceptDates(version);
        checkNotNull(startTime, OemMetadataKey.START_TIME.name());
        checkNotNull(stopTime,  OemMetadataKey.STOP_TIME.name());
    }

    /** Check is mandatory entries EXCEPT DATES have been initialized.
     * <p>
     * This method should throw an exception if some mandatory entry is missing
     * </p>
     * @param version format version
     */
    void checkMandatoryEntriesExceptDates(final double version) {
        super.validate(version);
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

    /** Get the interpolation method to be used.
     * @return the interpolation method
     */
    public InterpolationMethod getInterpolationMethod() {
        return interpolationMethod;
    }

    /** Set the interpolation method to be used.
     * @param interpolationMethod the interpolation method to be set
     */
    public void setInterpolationMethod(final InterpolationMethod interpolationMethod) {
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

    /** Copy the instance, making sure mandatory fields have been initialized.
     * @param version format version
     * @return a new copy
     */
    OemMetadata copy(final double version) {

        checkMandatoryEntriesExceptDates(version);

        // allocate new instance
        final OemMetadata copy = new OemMetadata(getInterpolationDegree());

        // copy comments
        for (String comment : getComments()) {
            copy.addComment(comment);
        }

        // copy object
        copy.setObjectName(getObjectName());
        copy.setObjectID(getObjectID());
        copy.setCenter(getCenter());

        // copy frames
        copy.setFrameEpoch(getFrameEpoch());
        copy.setReferenceFrame(getReferenceFrame());

        // copy time system only (ignore times themselves)
        copy.setTimeSystem(getTimeSystem());

        // copy interpolation (degree has already been set up at construction)
        if (getInterpolationMethod() != null) {
            copy.setInterpolationMethod(getInterpolationMethod());
        }

        return copy;

    }

}



