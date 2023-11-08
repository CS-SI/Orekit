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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.Collections;
import java.util.List;

import org.orekit.files.ccsds.definitions.OdMethodFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Orbit determination data.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitDetermination extends CommentsContainer {

    /** Identification number. */
    private String id;

    /** Identification of previous orbit determination. */
    private String prevId;

    /** Orbit determination method. */
    private OdMethodFacade method;

    /** Time tag for orbit determination solved-for state. */
    private AbsoluteDate epoch;

    /** Time elapsed between first accepted observation on epoch. */
    private double timeSinceFirstObservation;

    /** Time elapsed between last accepted observation on epoch. */
    private double timeSinceLastObservation;

    /** Sime span of observation recommended for the OD of the object. */
    private double recommendedOdSpan;

    /** Actual time span used for the OD of the object. */
    private double actualOdSpan;

    /** Number of observations available within the actual OD span. */
    private int obsAvailable;

    /** Number of observations accepted within the actual OD span. */
    private int obsUsed;

    /** Number of sensors tracks available for the OD within the actual OD span. */
    private int tracksAvailable;

    /** Number of sensors tracks accepted for the OD within the actual OD span. */
    private int tracksUsed;

    /** Maximum time between observations in the OD of the object. */
    private double maximumObsGap;

    /** Positional error ellipsoid 1σ major eigenvalue at the epoch of OD. */
    private double epochEigenMaj;

    /** Positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD. */
    private double epochEigenInt;

    /** Positional error ellipsoid 1σ minor eigenvalue at the epoch of OD. */
    private double epochEigenMin;

    /** Maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM. */
    private double maxPredictedEigenMaj;

    /** Minimum predicted minor eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM. */
    private double minPredictedEigenMin;

    /** Confidence metric. */
    private double confidence;

    /** Generalize Dilution Of Precision. */
    private double gdop;

    /** Number of solved-for states. */
    private int solveN;

    /** Description of state elements solved-for. */
    private List<String> solveStates;

    /** Number of consider parameters. */
    private int considerN;

    /** Description of consider parameters. */
    private List<String> considerParameters;

    /** Specific Energy Dissipation Rate.
     * @since 12.0
     */
    private double sedr;

    /** Number of sensors used. */
    private int sensorsN;

    /** Description of sensors used. */
    private List<String> sensors;

    /** Weighted RMS residual ratio. */
    private double weightedRms;

    /** Observation data types used. */
    private List<String> dataTypes;

    /** Simple constructor.
     */
    public OrbitDetermination() {
        sedr               = Double.NaN;
        solveStates        = Collections.emptyList();
        considerParameters = Collections.emptyList();
        sensors            = Collections.emptyList();
        dataTypes          = Collections.emptyList();
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        checkNotNull(id,     OrbitDeterminationKey.OD_ID.name());
        checkNotNull(method, OrbitDeterminationKey.OD_METHOD.name());
        checkNotNull(epoch,  OrbitDeterminationKey.OD_EPOCH.name());
    }

    /** Get identification number.
     * @return identification number
     */
    public String getId() {
        return id;
    }

    /** Set identification number.
     * @param id identification number
     */
    public void setId(final String id) {
        this.id = id;
    }

    /** Get identification of previous orbit determination.
     * @return identification of previous orbit determination
     */
    public String getPrevId() {
        return prevId;
    }

    /** Set identification of previous orbit determination.
     * @param prevId identification of previous orbit determination
     */
    public void setPrevId(final String prevId) {
        this.prevId = prevId;
    }

    /** Get orbit determination method.
     * @return orbit determination method
     */
    public OdMethodFacade getMethod() {
        return method;
    }

    /** Set orbit determination method.
     * @param method orbit determination method
     */
    public void setMethod(final OdMethodFacade method) {
        this.method = method;
    }

    /** Get time tag for orbit determination solved-for state.
     * @return time tag for orbit determination solved-for state
     */
    public AbsoluteDate getEpoch() {
        return epoch;
    }

    /** Set time tag for orbit determination solved-for state.
     * @param epoch time tag for orbit determination solved-for state
     */
    public void setEpoch(final AbsoluteDate epoch) {
        this.epoch = epoch;
    }

    /** Get time elapsed between first accepted observation on epoch.
     * @return time elapsed between first accepted observation on epoch
     */
    public double getTimeSinceFirstObservation() {
        return timeSinceFirstObservation;
    }

    /** Set time elapsed between first accepted observation on epoch.
     * @param timeSinceFirstObservation time elapsed between first accepted observation on epoch
     */
    public void setTimeSinceFirstObservation(final double timeSinceFirstObservation) {
        this.timeSinceFirstObservation = timeSinceFirstObservation;
    }

    /** Get time elapsed between last accepted observation on epoch.
     * @return time elapsed between last accepted observation on epoch
     */
    public double getTimeSinceLastObservation() {
        return timeSinceLastObservation;
    }

    /** Set time elapsed between last accepted observation on epoch.
     * @param timeSinceLastObservation time elapsed between last accepted observation on epoch
     */
    public void setTimeSinceLastObservation(final double timeSinceLastObservation) {
        this.timeSinceLastObservation = timeSinceLastObservation;
    }

    /** Get time span of observation recommended for the OD of the object.
     * @return time span of observation recommended for the OD of the object
     */
    public double getRecommendedOdSpan() {
        return recommendedOdSpan;
    }

    /** Set time span of observation recommended for the OD of the object.
     * @param recommendedOdSpan time span of observation recommended for the OD of the object
     */
    public void setRecommendedOdSpan(final double recommendedOdSpan) {
        this.recommendedOdSpan = recommendedOdSpan;
    }

    /** Get actual time span used for the OD of the object.
     * @return actual time span used for the OD of the object
     */
    public double getActualOdSpan() {
        return actualOdSpan;
    }

    /** Set actual time span used for the OD of the object.
     * @param actualOdSpan actual time span used for the OD of the object
     */
    public void setActualOdSpan(final double actualOdSpan) {
        this.actualOdSpan = actualOdSpan;
    }

    /** Get number of observations available within the actual OD span.
     * @return number of observations available within the actual OD span
     */
    public int getObsAvailable() {
        return obsAvailable;
    }

    /** Set number of observations available within the actual OD span.
     * @param obsAvailable number of observations available within the actual OD span
     */
    public void setObsAvailable(final int obsAvailable) {
        this.obsAvailable = obsAvailable;
    }

    /** Get number of observations accepted within the actual OD span.
     * @return number of observations accepted within the actual OD span
     */
    public int getObsUsed() {
        return obsUsed;
    }

    /** Set number of observations accepted within the actual OD span.
     * @param obsUsed number of observations accepted within the actual OD span
     */
    public void setObsUsed(final int obsUsed) {
        this.obsUsed = obsUsed;
    }

    /** Get number of sensors tracks available for the OD within the actual OD span.
     * @return number of sensors tracks available for the OD within the actual OD span
     */
    public int getTracksAvailable() {
        return tracksAvailable;
    }

    /** Set number of sensors tracks available for the OD within the actual OD span.
     * @param tracksAvailable number of sensors tracks available for the OD within the actual OD span
     */
    public void setTracksAvailable(final int tracksAvailable) {
        this.tracksAvailable = tracksAvailable;
    }

    /** Get number of sensors tracks accepted for the OD within the actual OD span.
     * @return number of sensors tracks accepted for the OD within the actual OD span
     */
    public int getTracksUsed() {
        return tracksUsed;
    }

    /** Set number of sensors tracks accepted for the OD within the actual OD span.
     * @param tracksUsed number of sensors tracks accepted for the OD within the actual OD span
     */
    public void setTracksUsed(final int tracksUsed) {
        this.tracksUsed = tracksUsed;
    }

    /** Get maximum time between observations in the OD of the object.
     * @return maximum time between observations in the OD of the object
     */
    public double getMaximumObsGap() {
        return maximumObsGap;
    }

    /** Set maximum time between observations in the OD of the object.
     * @param maximumObsGap maximum time between observations in the OD of the object
     */
    public void setMaximumObsGap(final double maximumObsGap) {
        this.maximumObsGap = maximumObsGap;
    }

    /** Get positional error ellipsoid 1σ major eigenvalue at the epoch of OD.
     * @return positional error ellipsoid 1σ major eigenvalue at the epoch of OD
     */
    public double getEpochEigenMaj() {
        return epochEigenMaj;
    }

    /** Set positional error ellipsoid 1σ major eigenvalue at the epoch of OD.
     * @param epochEigenMaj positional error ellipsoid 1σ major eigenvalue at the epoch of OD
     */
    public void setEpochEigenMaj(final double epochEigenMaj) {
        this.epochEigenMaj = epochEigenMaj;
    }

    /** Get positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD.
     * @return positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD
     */
    public double getEpochEigenInt() {
        return epochEigenInt;
    }

    /** Set positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD.
     * @param epochEigenInt positional error ellipsoid 1σ intermediate eigenvalue at the epoch of OD
     */
    public void setEpochEigenInt(final double epochEigenInt) {
        this.epochEigenInt = epochEigenInt;
    }

    /** Get positional error ellipsoid 1σ minor eigenvalue at the epoch of OD.
     * @return positional error ellipsoid 1σ minor eigenvalue at the epoch of OD
     */
    public double getEpochEigenMin() {
        return epochEigenMin;
    }

    /** Set positional error ellipsoid 1σ minor eigenvalue at the epoch of OD.
     * @param epochEigenMin positional error ellipsoid 1σ minor eigenvalue at the epoch of OD
     */
    public void setEpochEigenMin(final double epochEigenMin) {
        this.epochEigenMin = epochEigenMin;
    }

    /** Get maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM.
     * @return maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM
     */
    public double getMaxPredictedEigenMaj() {
        return maxPredictedEigenMaj;
    }

    /** Set maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM.
     * @param maxPredictedEigenMaj maximum predicted major eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM
     */
    public void setMaxPredictedEigenMaj(final double maxPredictedEigenMaj) {
        this.maxPredictedEigenMaj = maxPredictedEigenMaj;
    }

    /** Get minimum predicted minor eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM.
     * @return minimum predicted v eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM
     */
    public double getMinPredictedEigenMin() {
        return minPredictedEigenMin;
    }

    /** Set minimum predicted minor eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM.
     * @param minPredictedEigenMin minimum predicted minor eigenvalue of 1σ positional error ellipsoid over entire time span of the OCM
     */
    public void setMinPredictedEigenMin(final double minPredictedEigenMin) {
        this.minPredictedEigenMin = minPredictedEigenMin;
    }

    /** Get confidence metric.
     * @return confidence metric
     */
    public double getConfidence() {
        return confidence;
    }

    /** Set confidence metric.
     * @param confidence confidence metric
     */
    public void setConfidence(final double confidence) {
        this.confidence = confidence;
    }

    /** Get generalize Dilution Of Precision.
     * @return generalize Dilution Of Precision
     */
    public double getGdop() {
        return gdop;
    }

    /** Set generalize Dilution Of Precision.
     * @param gdop generalize Dilution Of Precision
     */
    public void setGdop(final double gdop) {
        this.gdop = gdop;
    }

    /** Get number of solved-for states.
     * @return number of solved-for states
     */
    public int getSolveN() {
        return solveN;
    }

    /** Set number of solved-for states.
     * @param solveN number of solved-for states
     */
    public void setSolveN(final int solveN) {
        this.solveN = solveN;
    }

    /** Get description of state elements solved-for.
     * @return description of state elements solved-for
     */
    public List<String> getSolveStates() {
        return solveStates;
    }

    /** Set description of state elements solved-for.
     * @param solveStates description of state elements solved-for
     */
    public void setSolveStates(final List<String> solveStates) {
        this.solveStates = solveStates;
    }

    /** Get number of consider parameters.
     * @return number of consider parameters
     */
    public int getConsiderN() {
        return considerN;
    }

    /** Set number of consider parameters.
     * @param considerN number of consider parameters
     */
    public void setConsiderN(final int considerN) {
        this.considerN = considerN;
    }

    /** Get description of consider parameters.
     * @return description of consider parameters
     */
    public List<String> getConsiderParameters() {
        return considerParameters;
    }

    /** Set description of consider parameters.
     * @param considerParameters description of consider parameters
     */
    public void setConsiderParameters(final List<String> considerParameters) {
        this.considerParameters = considerParameters;
    }

    /** Get Specific Energy Dissipation Rate.
     * @return Specific Energy Dissipation Rate
     * @since 12.0
     */
    public double getSedr() {
        return sedr;
    }

    /** Set Specific Energy Dissipation Rate.
     * @param sedr Specific Energy Dissipation Rate (W/kg)
     * @since 12.0
     */
    public void setSedr(final double sedr) {
        this.sedr = sedr;
    }

    /** Get number of sensors used.
     * @return number of sensors used
     */
    public int getSensorsN() {
        return sensorsN;
    }

    /** Set number of sensors used.
     * @param sensorsN number of sensors used
     */
    public void setSensorsN(final int sensorsN) {
        this.sensorsN = sensorsN;
    }

    /** Get description of sensors used.
     * @return description of sensors used
     */
    public List<String> getSensors() {
        return sensors;
    }

    /** Set description of sensors used.
     * @param sensors description of sensors used
     */
    public void setSensors(final List<String> sensors) {
        this.sensors = sensors;
    }

    /** Get weighted RMS residual ratio.
     * @return weighted RMS residual ratio
     */
    public double getWeightedRms() {
        return weightedRms;
    }

    /** Set weighted RMS residual ratio.
     * @param weightedRms weighted RMS residual ratio
     */
    public void setWeightedRms(final double weightedRms) {
        this.weightedRms = weightedRms;
    }

    /** Get observation data types used.
     * @return observation data types used
     */
    public List<String> getDataTypes() {
        return dataTypes;
    }

    /** Set observation data types used.
     * @param dataTypes observation data types used
     */
    public void setDataTypes(final List<String> dataTypes) {
        this.dataTypes = dataTypes;
    }

}
