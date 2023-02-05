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

/**
 * Container for the additional covariance metadata (optional).
 */
public class AdditionalCovarianceMetadata extends CommentsContainer {

    /** The atmospheric density forecast error. */
    private double densityForecastUncertainty;

    /** The minimum suggested covariance scale factor. */
    private double cScaleFactorMin;

    /** The (median) suggested covariance scale factor. */
    private double cScaleFactor;

    /** The maximum suggested covariance scale factor. */
    private double cScaleFactorMax;

    /** The source (or origin) of the specific orbital data for this object. */
    private String screeningDataSource;

    /** The drag consider parameter (DCP) sensitivity vectors map forward expected error in the drag acceleration to actual
     * componentized position errors at TCA. */
    private double[] dcpSensitivityVectorPosition;

    /** The drag consider parameter (DCP) sensitivity vectors map forward expected error in the drag acceleration to actual
     * componentized velocity errors at TCA. */
    private double[] dcpSensitivityVectorVelocity;


    /** Simple constructor. */
    public AdditionalCovarianceMetadata() {
        densityForecastUncertainty = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
    }


    /**
     * Get the atmospheric density forecast error.
     * @return densityForecastUncertainty
     */
    public double getDensityForecastUncertainty() {
        return densityForecastUncertainty;
    }

    /**
     * Set the atmospheric density forecast error.
     * @param densityForecastUncertainty the cScaleFactorMax to set
     */
    public void setDensityForecastUncertainty(final double densityForecastUncertainty) {
        refuseFurtherComments();
        this.densityForecastUncertainty = densityForecastUncertainty;
    }

    /** Get the minimum suggested covariance scale factor.
     * @return the cScaleFactorMin
     */
    public double getcScaleFactorMin() {
        return cScaleFactorMin;
    }

    /** Set the minimum suggested covariance scale factor.
     * @param cScaleFactorMin the cScaleFactorMin to set
     */
    public void setcScaleFactorMin(final double cScaleFactorMin) {
        this.cScaleFactorMin = cScaleFactorMin;
    }

    /** Get the (median) suggested covariance scale factor.
     * @return the cScaleFactor
     */
    public double getcScaleFactor() {
        return cScaleFactor;
    }

    /** Set the (median) suggested covariance scale factor.
     * @param cScaleFactor the cScaleFactor to set
     */
    public void setcScaleFactor(final double cScaleFactor) {
        this.cScaleFactor = cScaleFactor;
    }

    /** Get the maximum suggested covariance scale factor.
     * @return the cScaleFactorMax
     */
    public double getcScaleFactorMax() {
        return cScaleFactorMax;
    }

    /** set the maximum suggested covariance scale factor.
     * @param cScaleFactorMax the cScaleFactorMax to set
     */
    public void setcScaleFactorMax(final double cScaleFactorMax) {
        this.cScaleFactorMax = cScaleFactorMax;
    }

    /** Get the source (or origin) of the specific orbital data for this object.
     * @return the screeningDataSource
     */
    public String getScreeningDataSource() {
        return screeningDataSource;
    }

    /** Set the source (or origin) of the specific orbital data for this object.
     * @param screeningDataSource the screeningDataSource to set
     */
    public void setScreeningDataSource(final String screeningDataSource) {
        this.screeningDataSource = screeningDataSource;
    }

    /** Get the DCP sensitivity vector (position errors at TCA).
     * @return the dcpSensitivityVectorPosition
     */
    public double[] getDcpSensitivityVectorPosition() {
        return dcpSensitivityVectorPosition == null ? null : dcpSensitivityVectorPosition.clone();
    }

    /** Set the DCP sensitivity vector (position errors at TCA).
     * @param dcpSensitivityVectorPosition the dcpSensitivityVectorPosition to set
     */
    public void setDcpSensitivityVectorPosition(final double[] dcpSensitivityVectorPosition) {
        this.dcpSensitivityVectorPosition = dcpSensitivityVectorPosition == null ? null : dcpSensitivityVectorPosition.clone();
    }

    /** Get the DCP sensitivity vector (velocity errors at TCA).
     * @return the dcpSensitivityVectorVelocity
     */
    public double[] getDcpSensitivityVectorVelocity() {
        return dcpSensitivityVectorVelocity == null ? null : dcpSensitivityVectorVelocity.clone();
    }

    /** Set the DCP sensitivity vector (velocity errors at TCA).
     * @param dcpSensitivityVectorVelocity the dcpSensitivityVectorVelocity to set
     */
    public void setDcpSensitivityVectorVelocity(final double[] dcpSensitivityVectorVelocity) {
        this.dcpSensitivityVectorVelocity = dcpSensitivityVectorVelocity == null ? null : dcpSensitivityVectorVelocity.clone();
    }

}
