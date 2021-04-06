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

package org.orekit.files.ccsds.ndm.odm.ocm;

import java.util.List;

import org.orekit.data.DataContext;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.CelestialBodyFrame;
import org.orekit.files.ccsds.definitions.ElementsType;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.units.Unit;

/** Metadata for orbit state history.
 * @author Luc Maisonobe
 * @since 11.0
 */
public class OrbitStateHistoryMetadata extends CommentsContainer {

    /** Orbit identification number. */
    private String orbID;

    /** Identification number of previous orbit. */
    private String orbPrevID;

    /** Identification number of next orbit. */
    private String orbNextID;

    /** Basis of this orbit state time history data. */
    private String orbBasis;

    /** Identification number of the orbit determination or simulation upon which this orbit is based. */
    private String orbBasisID;

    /** Interpolation method. */
    private InterpolationMethod interpolationMethod;

    /** Interpolation degree. */
    private int interpolationDegree;

    /** Type of averaging (Osculating, mean Brouwer, other...). */
    private String orbAveraging;

    /** Origin of reference frame. */
    private BodyFacade center;

    /** Reference frame of the orbit. */
    private FrameFacade orbReferenceFrame;

    /** Epoch of the orbit reference frame. */
    private AbsoluteDate orbFrameEpoch;

    /** Start of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStartTime;

    /** End of useable time span covered by ephemerides data, it may be
     * necessary to allow for proper interpolation. */
    private AbsoluteDate useableStopTime;

    /** Orbit element set type. */
    private ElementsType orbType;

    /** Units of orbit element set. */
    private List<Unit> orbUnits;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     * @param dataContext data context
     */
    OrbitStateHistoryMetadata(final AbsoluteDate epochT0, final DataContext dataContext) {
        // we don't call the setXxx() methods in order to avoid
        // calling refuseFurtherComments as a side effect
        orbBasis            = "PREDICTED";
        interpolationMethod = InterpolationMethod.HERMITE;
        interpolationDegree = 3;
        orbAveraging        = "OSCULATING";
        center              = new BodyFacade("EARTH",
                                             dataContext.getCelestialBodies().getEarth());
        orbReferenceFrame   = new FrameFacade(dataContext.getFrames().getICRF(),
                                              CelestialBodyFrame.ICRF, null, null,
                                              CelestialBodyFrame.ICRF.name());
        orbFrameEpoch       = epochT0;
        orbType             = ElementsType.CARTPV;
    }

    /** {@inheritDoc} */
    @Override
    public void checkMandatoryEntries() {
        super.checkMandatoryEntries();
        if (orbUnits != null) {
            orbType.checkUnits(orbUnits);
        }
    }

    /** Get orbit identification number.
     * @return orbit identification number
     */
    public String getOrbID() {
        return orbID;
    }

    /** Set orbit identification number.
     * @param orbID orbit identification number
     */
    public void setOrbID(final String orbID) {
        refuseFurtherComments();
        this.orbID = orbID;
    }

    /** Get identification number of previous orbit.
     * @return identification number of previous orbit
     */
    public String getOrbPrevID() {
        return orbPrevID;
    }

    /** Set identification number of previous orbit.
     * @param orbPrevID identification number of previous orbit
     */
    public void setOrbPrevID(final String orbPrevID) {
        refuseFurtherComments();
        this.orbPrevID = orbPrevID;
    }

    /** Get identification number of next orbit.
     * @return identification number of next orbit
     */
    public String getOrbNextID() {
        return orbNextID;
    }

    /** Set identification number of next orbit.
     * @param orbNextID identification number of next orbit
     */
    public void setOrbNextID(final String orbNextID) {
        refuseFurtherComments();
        this.orbNextID = orbNextID;
    }

    /** Get basis of this orbit state time history data.
     * @return basis of this orbit state time history data
     */
    public String getOrbBasis() {
        return orbBasis;
    }

    /** Set basis of this orbit state time history data.
     * @param orbBasis basis of this orbit state time history data
     */
    public void setOrbBasis(final String orbBasis) {
        refuseFurtherComments();
        this.orbBasis = orbBasis;
    }

    /** Get identification number of the orbit determination or simulation upon which this orbit is based.
     * @return identification number of the orbit determination or simulation upon which this orbit is based
     */
    public String getOrbBasisID() {
        return orbBasisID;
    }

    /** Set identification number of the orbit determination or simulation upon which this orbit is based.
     * @param orbBasisID identification number of the orbit determination or simulation upon which this orbit is based
     */
    public void setOrbBasisID(final String orbBasisID) {
        refuseFurtherComments();
        this.orbBasisID = orbBasisID;
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

    /** Get type of averaging (Osculating, mean Brouwer, other.
     * @return type of averaging (Osculating, mean Brouwer, other
     .). */
    public String getOrbAveraging() {
        return orbAveraging;
    }

    /** Set type of averaging (Osculating, mean Brouwer, other.
     * @param orbAveraging type of averaging (Osculating, mean Brouwer, other
     .). */
    public void setOrbAveraging(final String orbAveraging) {
        refuseFurtherComments();
        this.orbAveraging = orbAveraging;
    }

    /** Get the origin of reference frame.
     * @return the origin of reference frame.
     */
    public BodyFacade getCenter() {
        return center;
    }

    /** Set the origin of reference frame.
     * @param center origin of reference frame to be set
     */
    public void setCenter(final BodyFacade center) {
        refuseFurtherComments();
        this.center = center;
    }

    /** Get reference frame of the orbit.
     * @return reference frame of the orbit
     */
    public FrameFacade getOrbReferenceFrame() {
        return orbReferenceFrame;
    }

    /** Set reference frame of the orbit.
     * @param orbReferenceFrame the reference frame to be set
     */
    public void setOrbReferenceFrame(final FrameFacade orbReferenceFrame) {
        refuseFurtherComments();
        this.orbReferenceFrame = orbReferenceFrame;
    }

    /** Get epoch of the {@link #getOrbRefFrame() orbit reference frame}.
     * @return epoch of the {@link #getOrbRefFrame() orbit reference frame}
     */
    public AbsoluteDate getOrbFrameEpoch() {
        return orbFrameEpoch;
    }

    /** Set epoch of the {@link #getOrbRefFrame() orbit reference frame}.
     * @param orbFrameEpoch epoch of the {@link #getOrbRefFrame() orbit reference frame}
     */
    public void setOrbFrameEpoch(final AbsoluteDate orbFrameEpoch) {
        refuseFurtherComments();
        this.orbFrameEpoch = orbFrameEpoch;
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

    /** Get orbit element set type.
     * @return orbit element set type
     */
    public ElementsType getOrbType() {
        return orbType;
    }

    /** Set orbit element set type.
     * @param orbType orbit element set type
     */
    public void setOrbType(final ElementsType orbType) {
        refuseFurtherComments();
        this.orbType = orbType;
    }

    /** Get orbit element set units.
     * @return orbit element set units
     */
    public List<Unit> getOrbUnits() {
        return orbUnits;
    }

    /** Set orbit element set units.
     * @param orbUnits orbit element set units
     */
    public void setOrbUnits(final List<Unit> orbUnits) {
        refuseFurtherComments();
        this.orbUnits = orbUnits;
    }

}
