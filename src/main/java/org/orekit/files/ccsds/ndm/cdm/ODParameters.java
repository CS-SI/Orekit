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
package org.orekit.files.ccsds.ndm.cdm;

import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/**
 * Container for OD parameters data block.
 * @author Melina Vanel
 * @since 11.2
 */
public class ODParameters extends CommentsContainer {
    /** The start of a time interval (UTC) that contains the time of the last accepted observation. */
    private AbsoluteDate timeLastobStart;

    /** The end of a time interval (UTC) that contains the time of the last accepted observation. */
    private AbsoluteDate timeLastobEnd;

    /** The recommended OD time span calculated for the object. */
    private double recommendedODSpan;

    /** Based on the observations available and the RECOMMENDED_OD_SPAN, the actual time span used for the OD of the object. */
    private double actualODSpan;

    /** The number of observations available for the OD of the object. */
    private int OBSavailable;

    /** The number of observations accepted for the OD of the object. */
    private int OBSused;

    /** The number of sensor tracks available for the OD of the object. */
    private int tracksAvailable;

    /** The number of sensor tracks accepted for the OD of the object. */
    private int tracksUsed;

    /** The percentage of residuals accepted in the OD of the object (from 0 to 100). */
    private double residualsAccepted;

    /** The weighted Root Mean Square (RMS) of the residuals from a batch least squares OD. */
    private double weightedRMS;

    /** Simple constructor.
     */
    public ODParameters() {
        recommendedODSpan   = Double.NaN;
        actualODSpan        = Double.NaN;
        residualsAccepted   = Double.NaN;
        weightedRMS         = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);

    }

    /**
     * Get the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @return the start of a time interval (UTC)
     */
    public AbsoluteDate getTimeLastobStart() {
        return timeLastobStart;
    }

    /**
     * Set the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @param timeLastobStart the start of a time interval (UTC)
     */
    public void setTimeLastobStart(final AbsoluteDate timeLastobStart) {
        refuseFurtherComments();
        this.timeLastobStart = timeLastobStart;
    }

    /**
     * Get the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @return the start of a time interval (UTC)
     */
    public AbsoluteDate getTimeLastobEnd() {
        return timeLastobEnd;
    }

    /**
     * Set the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @param timeLastobEnd the start of a time interval (UTC)
     */
    public void setTimeLastobEnd(final AbsoluteDate timeLastobEnd) {
        refuseFurtherComments();
        this.timeLastobEnd = timeLastobEnd;
    }

    /**
     * Get the recommended OD time span calculated for the object.
     * @return the recommended OD time span (in days) calculated for the object
     */
    public double getRecommendedODSpan() {
        return recommendedODSpan;
    }

    /**
     * Set the recommended OD time span calculated for the object.
     * @param recommendedODSpan recommended OD time span (in days) calculated for the object
     */
    public void setRecommendedODSpan(final double recommendedODSpan) {
        refuseFurtherComments();
        this.recommendedODSpan = recommendedODSpan;
    }

    /**
     * Get the actual OD time based on the observations available and the RECOMMENDED_OD_SPAN.
     * @return the actual OD time (in days)
     */
    public double getActualODSpan() {
        return actualODSpan;
    }

    /**
     * Set the actual OD time based on the observations available and the RECOMMENDED_OD_SPAN.
     * @param actualODSpan the actual OD time (in days)
     */
    public void setActualODSpan(final double actualODSpan) {
        refuseFurtherComments();
        this.actualODSpan = actualODSpan;
    }

    /**
     * Get the number of observations available for the OD of the object.
     * @return the number of observations available
     */
    public int getObsAvailable() {
        return OBSavailable;
    }

    /**
     * Set the number of observations available for the OD of the object.
     * @param obsAvailable the number of observations available
     */
    public void setObsAvailable(final int obsAvailable) {
        refuseFurtherComments();
        this.OBSavailable = obsAvailable;
    }

    /**
     * Get the number of observations accepted for the OD of the object.
     * @return the number of observations used
     */
    public int getObsUsed() {
        return OBSused;
    }

    /**
     * Set the number of observations accepted for the OD of the object.
     * @param Obsused the number of observations used
     */
    public void setObsUsed(final int Obsused) {
        refuseFurtherComments();
        this.OBSused = Obsused;
    }

    /**
     * Get the number of sensor tracks available for the OD of the object.
     * @return the number of sensor tracks available
     */
    public int getTracksAvailable() {
        return tracksAvailable;
    }

    /**
     * Set the number of sensor tracks available for the OD of the object.
     * @param tracksAvailable the number of sensor tracks available
     */
    public void setTracksAvailable(final int tracksAvailable) {
        refuseFurtherComments();
        this.tracksAvailable = tracksAvailable;
    }

    /**
     * Get the number of sensor tracks used for the OD of the object.
     * @return the number of sensor tracks used
     */
    public int getTracksUsed() {
        return tracksUsed;
    }

    /**
     * Set the number of sensor tracks used for the OD of the object.
     * @param tracksUsed the number of sensor tracks used
     */
    public void setTracksUsed(final int tracksUsed) {
        refuseFurtherComments();
        this.tracksUsed = tracksUsed;
    }

    /**
     * Get the percentage of residuals accepted in the OD of the object (from 0 to 100).
     * @return the percentage of residuals accepted in the OD
     */
    public double getResidualsAccepted() {
        return residualsAccepted;
    }

    /**
     * Set the percentage of residuals accepted in the OD of the object (from 0 to 100).
     * @param residualsAccepted the percentage of residuals accepted in the OD to be set
     */
    public void setResidualsAccepted(final double residualsAccepted) {
        refuseFurtherComments();
        this.residualsAccepted = residualsAccepted;
    }

    /**
     * Get the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD.
     * @return the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD
     */
    public double getWeightedRMSS() {
        return weightedRMS;
    }

    /**
     * Set the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD.
     * @param WeightedRMS the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD
     */
    public void setWeightedRMSS(final double WeightedRMS) {
        refuseFurtherComments();
        this.weightedRMS = WeightedRMS;
    }

}
