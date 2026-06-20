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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.orekit.annotation.Nullable;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Perturbation parameters.
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
 * @author Luc Maisonobe
 * @since 11.0
 */
public class Perturbations extends CommentsContainer {

    /** Name of atmospheric model. */
    @Nullable
    private String atmosphericModel;

    /** Gravity model name. */
    @Nullable
    private String gravityModel;

    /** Degree of the gravity model. */
    @Nullable
    private Integer gravityDegree;

    /** Order of the gravity model. */
    @Nullable
    private Integer gravityOrder;

    /** Oblate spheroid equatorial radius of central body. */
    @Nullable
    private Double equatorialRadius;

    /** Gravitational coefficient of attracting body. */
    @Nullable
    private Double gm;

    /** N-body perturbation bodies. */
    private List<BodyFacade> nBodyPerturbations;

    /** Central body angular rotation rate. */
    @Nullable
    private Double centralBodyRotation;

    /** Central body oblate spheroid oblateness. */
    @Nullable
    private Double oblateFlattening;

    /** Ocean tides model. */
    @Nullable
    private String oceanTidesModel;

    /** Solid tides model. */
    @Nullable
    private String solidTidesModel;

    /** Reduction theory used for precession and nutation modeling. */
    @Nullable
    private String reductionTheory;

    /** Albedo model. */
    @Nullable
    private String albedoModel;

    /** Albedo grid size. Optional in 502.0-B-3 with no default. */
    @Nullable
    private Integer albedoGridSize;

    /** Shadow model used for solar radiation pressure. */
    @Nullable
    private ShadowModel shadowModel;

    /** Celestial bodies casting shadow. */
    private List<BodyFacade> shadowBodies;

    /** Solar Radiation Pressure model. */
    @Nullable
    private String srpModel;

    /** Space Weather data source. */
    @Nullable
    private String spaceWeatherSource;

    /** Epoch of the Space Weather data. */
    @Nullable
    private AbsoluteDate spaceWeatherEpoch;

    /** Interpolation method for Space Weather data. */
    @Nullable
    private String interpMethodSW;

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ. */
    @Nullable
    private Double fixedGeomagneticKp;

    /** Fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ. */
    @Nullable
    private Double fixedGeomagneticAp;

    /** Fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst. */
    @Nullable
    private Double fixedGeomagneticDst;

    /** Fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7. */
    @Nullable
    private Double fixedF10P7;

    /** Fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7. */
    @Nullable
    private Double fixedF10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy M10.7. */
    @Nullable
    private Double fixedM10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7. */
    @Nullable
    private Double fixedM10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy S10.7. */
    @Nullable
    private Double fixedS10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7. */
    @Nullable
    private Double fixedS10P7Mean;

    /** Fixed (time invariant) value of the Solar Flux daily proxy Y10.7. */
    @Nullable
    private Double fixedY10P7;

    /** Fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7. */
    @Nullable
    private Double fixedY10P7Mean;

    /** Simple constructor. */
    public Perturbations() {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        // In 502.0-B-3 (p. 6-50) SHADOW_BODIES is optional with no default
        shadowBodies       = Collections.emptyList();
        nBodyPerturbations = Collections.emptyList();
    }

    /** Get name of atmospheric model.
     * @return name of atmospheric model
     */
    public Optional<String> getAtmosphericModel() {
        return Optional.ofNullable(atmosphericModel);
    }

    /** Set name of atmospheric model.
     * @param atmosphericModel name of atmospheric model
     */
    public void setAtmosphericModel(final String atmosphericModel) {
        this.atmosphericModel = atmosphericModel;
    }

    /** Get gravity model name.
     * @return gravity model name
     */
    public Optional<String> getGravityModel() {
        return Optional.ofNullable(gravityModel);
    }

    /** Get degree of the gravity model.
     * @return degree of the gravity model
     */
    public Optional<Integer> getGravityDegree() {
        return Optional.ofNullable(gravityDegree);
    }

    /** Get order of the gravity model.
     * @return order of the gravity model
     */
    public Optional<Integer> getGravityOrder() {
        return Optional.ofNullable(gravityOrder);
    }

    /** Set gravity model.
     * @param name name of the model
     * @param degree degree of the model
     * @param order order of the model
     */
    public void setGravityModel(final String name, final int degree, final int order) {
        this.gravityModel  = name;
        this.gravityDegree = degree;
        this.gravityOrder  = order;
    }

    /** Get oblate spheroid equatorial radius of central body.
     * @return oblate spheroid equatorial radius of central body
     */
    public Optional<Double> getEquatorialRadius() {
        return Optional.ofNullable(equatorialRadius);
    }

    /** Set oblate spheroid equatorial radius of central body.
     * @param equatorialRadius oblate spheroid equatorial radius of central body
     */
    public void setEquatorialRadius(final double equatorialRadius) {
        this.equatorialRadius = equatorialRadius;
    }

    /** Get gravitational coefficient of attracting body.
     * @return gravitational coefficient of attracting body
     */
    public Optional<Double> getGm() {
        return Optional.ofNullable(gm);
    }

    /** Set gravitational coefficient of attracting body.
     * @param gm gravitational coefficient of attracting body
     */
    public void setGm(final double gm) {
        this.gm = gm;
    }

    /** Get n-body perturbation bodies.
     * @return n-body perturbation bodies
     */
    public List<BodyFacade> getNBodyPerturbations() {
        return nBodyPerturbations;
    }

    /** Set n-body perturbation bodies.
     * @param nBody n-body perturbation bodies
     */
    public void setNBodyPerturbations(final List<BodyFacade> nBody) {
        this.nBodyPerturbations = nBody;
    }

    /** Get central body angular rotation rate.
     * @return central body angular rotation rate
     */
    public Optional<Double> getCentralBodyRotation() {
        return Optional.ofNullable(centralBodyRotation);
    }

    /** Set central body angular rotation rate.
     * @param centralBodyRotation central body angular rotation rate
     */
    public void setCentralBodyRotation(final double centralBodyRotation) {
        this.centralBodyRotation = centralBodyRotation;
    }

    /** Get central body oblate spheroid oblateness.
     * @return central body oblate spheroid oblateness
     */
    public Optional<Double> getOblateFlattening() {
        return Optional.ofNullable(oblateFlattening);
    }

    /** Set central body oblate spheroid oblateness.
     * @param oblateFlattening central body oblate spheroid oblateness
     */
    public void setOblateFlattening(final double oblateFlattening) {
        this.oblateFlattening = oblateFlattening;
    }

    /** Get ocean tides model.
     * @return ocean tides model
     */
    public Optional<String> getOceanTidesModel() {
        return Optional.ofNullable(oceanTidesModel);
    }

    /** Set ocean tides model.
     * @param oceanTidesModel ocean tides model
     */
    public void setOceanTidesModel(final String oceanTidesModel) {
        this.oceanTidesModel = oceanTidesModel;
    }

    /** Get solid tides model.
     * @return solid tides model
     */
    public Optional<String> getSolidTidesModel() {
        return Optional.ofNullable(solidTidesModel);
    }

    /** Set solid tides model.
     * @param solidTidesModel solid tides model
     */
    public void setSolidTidesModel(final String solidTidesModel) {
        this.solidTidesModel = solidTidesModel;
    }

    /** Get reduction theory used for precession and nutation modeling.
     * @return reduction theory used for precession and nutation modeling
     */
    public Optional<String> getReductionTheory() {
        return Optional.ofNullable(reductionTheory);
    }

    /** Set reduction theory used for precession and nutation modeling.
     * @param reductionTheory reduction theory used for precession and nutation modeling
     */
    public void setReductionTheory(final String reductionTheory) {
        this.reductionTheory = reductionTheory;
    }

    /** Get albedo model.
     * @return albedo model
     */
    public Optional<String> getAlbedoModel() {
        return Optional.ofNullable(albedoModel);
    }

    /** Set albedo model.
     * @param albedoModel albedo model
     */
    public void setAlbedoModel(final String albedoModel) {
        this.albedoModel = albedoModel;
    }

    /** Get albedo grid size.
     * @return albedo grid size
     */
    public Optional<Integer> getAlbedoGridSize() {
        return Optional.ofNullable(albedoGridSize);
    }

    /** Set albedo grid size.
     * @param albedoGridSize albedo grid size
     */
    public void setAlbedoGridSize(final Integer albedoGridSize) {
        this.albedoGridSize = albedoGridSize;
    }

    /** Get shadow model used for solar radiation pressure.
     * @return shadow model used for solar radiation pressure
     */
    public Optional<ShadowModel> getShadowModel() {
        return Optional.ofNullable(shadowModel);
    }

    /** Set shadow model used for solar radiation pressure.
     * @param shadowModel shadow model used for solar radiation pressure
     */
    public void setShadowModel(final ShadowModel shadowModel) {
        this.shadowModel = shadowModel;
    }

    /** Get celestial bodies casting shadows.
     * @return celestial bodies casting shadows
     */
    public List<BodyFacade> getShadowBodies() {
        return shadowBodies;
    }

    /** Set celestial bodies casting shadows.
     * @param shadowBodies celestial bodies casting shadows
     */
    public void setShadowBodies(final List<BodyFacade> shadowBodies) {
        this.shadowBodies = shadowBodies;
    }

    /** Get Solar Radiation Pressure model.
     * @return Solar Radiation Pressure model
     */
    public Optional<String> getSrpModel() {
        return Optional.ofNullable(srpModel);
    }

    /** Set Solar Radiation Pressure model.
     * @param srpModel Solar Radiation Pressure model
     */
    public void setSrpModel(final String srpModel) {
        this.srpModel = srpModel;
    }

    /** Get Space Weather data source.
     * @return Space Weather data source
     */
    public Optional<String> getSpaceWeatherSource() {
        return Optional.ofNullable(spaceWeatherSource);
    }

    /** Set Space Weather data source.
     * @param spaceWeatherSource Space Weather data source
     */
    public void setSpaceWeatherSource(final String spaceWeatherSource) {
        this.spaceWeatherSource = spaceWeatherSource;
    }

    /** Get epoch of the Space Weather data.
     * @return epoch of the Space Weather data
     */
    public Optional<AbsoluteDate> getSpaceWeatherEpoch() {
        return Optional.ofNullable(spaceWeatherEpoch);
    }

    /** Set epoch of the Space Weather data.
     * @param spaceWeatherEpoch epoch of the Space Weather data
     */
    public void setSpaceWeatherEpoch(final AbsoluteDate spaceWeatherEpoch) {
        this.spaceWeatherEpoch = spaceWeatherEpoch;
    }

    /** Get the interpolation method for Space Weather data.
     * @return interpolation method for Space Weather data
     */
    public Optional<String> getInterpMethodSW() {
        return Optional.ofNullable(interpMethodSW);
    }

    /** Set the interpolation method for Space Weather data.
     * @param interpMethodSW interpolation method for Space Weather data
     */
    public void setInterpMethodSW(final String interpMethodSW) {
        refuseFurtherComments();
        this.interpMethodSW = interpMethodSW;
    }

    /** Get fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ.
     * @return fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ
     */
    public Optional<Double> getFixedGeomagneticKp() {
        return Optional.ofNullable(fixedGeomagneticKp);
    }

    /** Set fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ.
     * @param fixedGeomagneticKp fixed (time invariant) value of the planetary 3-hour-range geomagnetic index Kₚ
     */
    public void setFixedGeomagneticKp(final double fixedGeomagneticKp) {
        this.fixedGeomagneticKp = fixedGeomagneticKp;
    }

    /** Get fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ.
     * @return fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ
     */
    public Optional<Double> getFixedGeomagneticAp() {
        return Optional.ofNullable(fixedGeomagneticAp);
    }

    /** Set fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ.
     * @param fixedGeomagneticAp fixed (time invariant) value of the planetary 3-hour-range geomagnetic index aₚ
     */
    public void setFixedGeomagneticAp(final double fixedGeomagneticAp) {
        this.fixedGeomagneticAp = fixedGeomagneticAp;
    }

    /** Get fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst.
     * @return fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst
     */
    public Optional<Double> getFixedGeomagneticDst() {
        return Optional.ofNullable(fixedGeomagneticDst);
    }

    /** Set fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst.
     * @param fixedGeomagneticDst fixed (time invariant) value of the planetary 1-hour-range geomagnetic index Dst
     */
    public void setFixedGeomagneticDst(final double fixedGeomagneticDst) {
        this.fixedGeomagneticDst = fixedGeomagneticDst;
    }

    /** Get fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7.
     * @return fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7
     */
    public Optional<Double> getFixedF10P7() {
        return Optional.ofNullable(fixedF10P7);
    }

    /** Set fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7.
     * @param fixedF10P7 fixed (time invariant) value of the Solar Flux Unit daily proxy F10.7
     */
    public void setFixedF10P7(final double fixedF10P7) {
        this.fixedF10P7 = fixedF10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7.
     * @return fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7
     */
    public Optional<Double> getFixedF10P7Mean() {
        return Optional.ofNullable(fixedF10P7Mean);
    }

    /** Set fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7.
     * @param fixedF10P7Mean fixed (time invariant) value of the Solar Flux Unit 81-day running center-average proxy F10.7
     */
    public void setFixedF10P7Mean(final double fixedF10P7Mean) {
        this.fixedF10P7Mean = fixedF10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy M10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy M10.7
     */
    public Optional<Double> getFixedM10P7() {
        return Optional.ofNullable(fixedM10P7);
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy M10.7.
     * @param fixedM10P7 fixed (time invariant) value of the Solar Flux daily proxy M10.7
     */
    public void setFixedM10P7(final double fixedM10P7) {
        this.fixedM10P7 = fixedM10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7
     */
    public Optional<Double> getFixedM10P7Mean() {
        return Optional.ofNullable(fixedM10P7Mean);
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7.
     * @param fixedM10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy M10.7
     */
    public void setFixedM10P7Mean(final double fixedM10P7Mean) {
        this.fixedM10P7Mean = fixedM10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy S10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy S10.7
     */
    public Optional<Double> getFixedS10P7() {
        return Optional.ofNullable(fixedS10P7);
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy S10.7.
     * @param fixedS10P7 fixed (time invariant) value of the Solar Flux daily proxy S10.7
     */
    public void setFixedS10P7(final double fixedS10P7) {
        this.fixedS10P7 = fixedS10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7
     */
    public Optional<Double> getFixedS10P7Mean() {
        return Optional.ofNullable(fixedS10P7Mean);
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7.
     * @param fixedS10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy S10.7
     */
    public void setFixedS10P7Mean(final double fixedS10P7Mean) {
        this.fixedS10P7Mean = fixedS10P7Mean;
    }

    /** Get fixed (time invariant) value of the Solar Flux daily proxy Y10.7.
     * @return fixed (time invariant) value of the Solar Flux daily proxy Y10.7
     */
    public Optional<Double> getFixedY10P7() {
        return Optional.ofNullable(fixedY10P7);
    }

    /** Set fixed (time invariant) value of the Solar Flux daily proxy Y10.7.
     * @param fixedY10P7 fixed (time invariant) value of the Solar Flux daily proxy Y10.7
     */
    public void setFixedY10P7(final double fixedY10P7) {
        this.fixedY10P7 = fixedY10P7;
    }

    /** Get fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7.
     * @return fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7
     */
    public Optional<Double> getFixedY10P7Mean() {
        return Optional.ofNullable(fixedY10P7Mean);
    }

    /** Set fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7.
     * @param fixedY10P7Mean fixed (time invariant) value of the Solar Flux 81-day running center-average proxy Y10.7
     */
    public void setFixedY10P7Mean(final double fixedY10P7Mean) {
        this.fixedY10P7Mean = fixedY10P7Mean;
    }

}
