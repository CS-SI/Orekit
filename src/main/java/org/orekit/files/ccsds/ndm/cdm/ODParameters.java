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
    private AbsoluteDate timeLastObsStart;

    /** The end of a time interval (UTC) that contains the time of the last accepted observation. */
    private AbsoluteDate timeLastObsEnd;

    /** The recommended OD time span calculated for the object. */
    private double recommendedOdSpan;

    /** Based on the observations available and the RECOMMENDED_OD_SPAN, the actual time span used for the OD of the object. */
    private double actualOdSpan;

    /** The number of observations available for the OD of the object. */
    private int obsAvailable;

    /** The number of observations accepted for the OD of the object. */
    private int obsUsed;

    /** The number of sensor tracks available for the OD of the object. */
    private int tracksAvailable;

    /** The number of sensor tracks accepted for the OD of the object. */
    private int tracksUsed;

    /** The percentage of residuals accepted in the OD of the object (from 0 to 100). */
    private double residualsAccepted;

    /** The weighted Root Mean Square (RMS) of the residuals from a batch least squares OD. */
    private double weightedRMS;

    /** The epoch of the orbit determination used for this message (UTC). */
    private AbsoluteDate odEpoch;

    /** Simple constructor.
     */
    public ODParameters() {
        recommendedOdSpan   = Double.NaN;
        actualOdSpan        = Double.NaN;
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
    public AbsoluteDate getTimeLastObsStart() {
        return timeLastObsStart;
    }

    /**
     * Set the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @param timeLastObsStart the start of a time interval (UTC)
     */
    public void setTimeLastObsStart(final AbsoluteDate timeLastObsStart) {
        refuseFurtherComments();
        this.timeLastObsStart = timeLastObsStart;
    }

    /**
     * Get the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @return the start of a time interval (UTC)
     */
    public AbsoluteDate getTimeLastObsEnd() {
        return timeLastObsEnd;
    }

    /**
     * Set the start of a time interval (UTC) that contains the time of the last accepted observation.
     * @param timeLastObsEnd the start of a time interval (UTC)
     */
    public void setTimeLastObsEnd(final AbsoluteDate timeLastObsEnd) {
        refuseFurtherComments();
        this.timeLastObsEnd = timeLastObsEnd;
    }

    /**
     * Get the recommended OD time span calculated for the object.
     * @return the recommended OD time span (in days) calculated for the object
     */
    public double getRecommendedOdSpan() {
        return recommendedOdSpan;
    }

    /**
     * Set the recommended OD time span calculated for the object.
     * @param recommendedOdSpan recommended OD time span (in days) calculated for the object
     */
    public void setRecommendedOdSpan(final double recommendedOdSpan) {
        refuseFurtherComments();
        this.recommendedOdSpan = recommendedOdSpan;
    }

    /**
     * Get the actual OD time based on the observations available and the RECOMMENDED_OD_SPAN.
     * @return the actual OD time (in days)
     */
    public double getActualOdSpan() {
        return actualOdSpan;
    }

    /**
     * Set the actual OD time based on the observations available and the RECOMMENDED_OD_SPAN.
     * @param actualOdSpan the actual OD time (in days)
     */
    public void setActualOdSpan(final double actualOdSpan) {
        refuseFurtherComments();
        this.actualOdSpan = actualOdSpan;
    }

    /**
     * Get the number of observations available for the OD of the object.
     * @return the number of observations available
     */
    public int getObsAvailable() {
        return obsAvailable;
    }

    /**
     * Set the number of observations available for the OD of the object.
     * @param obsAvailable the number of observations available
     */
    public void setObsAvailable(final int obsAvailable) {
        refuseFurtherComments();
        this.obsAvailable = obsAvailable;
    }

    /**
     * Get the number of observations accepted for the OD of the object.
     * @return the number of observations used
     */
    public int getObsUsed() {
        return obsUsed;
    }

    /**
     * Set the number of observations accepted for the OD of the object.
     * @param obsUsed the number of observations used
     */
    public void setObsUsed(final int obsUsed) {
        refuseFurtherComments();
        this.obsUsed = obsUsed;
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
    public double getWeightedRMS() {
        return weightedRMS;
    }

    /**
     * Set the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD.
     * @param WeightedRMS the weighted Root Mean Square (RMS) of the residuals from a batch least squares OD
     */
    public void setWeightedRMS(final double WeightedRMS) {
        refuseFurtherComments();
        this.weightedRMS = WeightedRMS;
    }

    /** Get the epoch of the orbit determination used for this message.
     * @return the odEpoch the epoch of the orbit determination used for this message
     */
    public AbsoluteDate getOdEpoch() {
        return odEpoch;
    }

    /** Set the epoch of the orbit determination used for this message.
     * @param odEpoch the odEpoch to set
     */
    public void setOdEpoch(final AbsoluteDate odEpoch) {
        this.odEpoch = odEpoch;
    }

}
