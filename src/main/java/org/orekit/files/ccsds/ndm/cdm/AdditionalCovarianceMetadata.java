/* Copyright 2002-2026 CS GROUP
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

import java.util.Optional;

import org.orekit.annotation.Nullable;
import org.orekit.files.ccsds.section.CommentsContainer;

/**
 * Container for the additional covariance metadata (optional).
 * <p>
 * Beware that the Orekit getters and setters all rely on SI units. The parsers
 * and writers take care of converting these SI units into CCSDS mandatory units.
 * The {@link org.orekit.utils.units.Unit Unit} class provides useful
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} methods in case the callers
 * already use CCSDS units instead of the API SI units. The general-purpose
 * {@link org.orekit.utils.units.Unit Unit} class (without an 's') and the
 * CCSDS-specific {@link org.orekit.files.ccsds.definitions.Units Units} class
 * (with an 's') also provide some predefined units. These predefined units and the
 * {@link org.orekit.utils.units.Unit#fromSI(double) fromSi} and
 * {@link org.orekit.utils.units.Unit#toSI(double) toSI} conversion methods are indeed
 * what the parsers and writers use for the conversions.
 * </p>
 */
public class AdditionalCovarianceMetadata extends CommentsContainer {

    /** The atmospheric density forecast error. */
    @Nullable
    private Double densityForecastUncertainty;

    /** The minimum suggested covariance scale factor. */
    @Nullable
    private Double cScaleFactorMin;

    /** The (median) suggested covariance scale factor. */
    @Nullable
    private Double cScaleFactor;

    /** The maximum suggested covariance scale factor. */
    @Nullable
    private Double cScaleFactorMax;

    /** The source (or origin) of the specific orbital data for this object. */
    @Nullable
    private String screeningDataSource;

    /** The drag consider parameter (DCP) sensitivity vectors map forward expected error in the drag acceleration to actual
     * componentized position errors at TCA. */
    @Nullable
    private double[] dcpSensitivityVectorPosition;

    /** The drag consider parameter (DCP) sensitivity vectors map forward expected error in the drag acceleration to actual
     * componentized velocity errors at TCA. */
    @Nullable
    private double[] dcpSensitivityVectorVelocity;


    /** Simple constructor. */
    public AdditionalCovarianceMetadata() {
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
    public Optional<Double> getDensityForecastUncertainty() {
        return Optional.ofNullable(densityForecastUncertainty);
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
    public Optional<Double> getcScaleFactorMin() {
        return Optional.ofNullable(cScaleFactorMin);
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
    public Optional<Double> getcScaleFactor() {
        return Optional.ofNullable(cScaleFactor);
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
    public Optional<Double> getcScaleFactorMax() {
        return Optional.ofNullable(cScaleFactorMax);
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
    public Optional<String> getScreeningDataSource() {
        return Optional.ofNullable(screeningDataSource);
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
    public Optional<double[]> getDcpSensitivityVectorPosition() {
        if (dcpSensitivityVectorPosition == null) {
            return Optional.empty();
        }
        return Optional.of(dcpSensitivityVectorPosition.clone());
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
    public Optional<double[]> getDcpSensitivityVectorVelocity() {
        if (dcpSensitivityVectorVelocity == null) {
            return Optional.empty();
        }
        return Optional.of(dcpSensitivityVectorVelocity.clone());
    }

    /** Set the DCP sensitivity vector (velocity errors at TCA).
     * @param dcpSensitivityVectorVelocity the dcpSensitivityVectorVelocity to set
     */
    public void setDcpSensitivityVectorVelocity(final double[] dcpSensitivityVectorVelocity) {
        this.dcpSensitivityVectorVelocity = dcpSensitivityVectorVelocity == null ? null : dcpSensitivityVectorVelocity.clone();
    }

}
