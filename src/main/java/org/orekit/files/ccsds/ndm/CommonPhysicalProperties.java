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
package org.orekit.files.ccsds.ndm;

import org.hipparchus.complex.Quaternion;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OrbitRelativeFrame;
import org.orekit.files.ccsds.ndm.cdm.AdditionalParameters;
import org.orekit.files.ccsds.ndm.odm.ocm.OrbitPhysicalProperties;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Container for common physical properties for both {@link OrbitPhysicalProperties} and {@link AdditionalParameters}.
 *
 * @author Maxime Journot
 * @since 11.3
 */
public class CommonPhysicalProperties extends CommentsContainer {

    /** Optimally Enclosing Box parent reference frame. */
    private FrameFacade oebParentFrame;

    /** Optimally Enclosing Box parent reference frame epoch. */
    private AbsoluteDate oebParentFrameEpoch;

    /** Quaternion defining Optimally Enclosing Box. */
    private final double[] oebQ;

    /** Maximum physical dimension of Optimally Enclosing Box. */
    private double oebMax;

    /** Intermediate physical dimension of Optimally Enclosing Box. */
    private double oebIntermediate;

    /** Minimum physical dimension of Optimally Enclosing Box. */
    private double oebMin;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction. */
    private double oebAreaAlongMax;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction. */
    private double oebAreaAlongIntermediate;

    /** Cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction. */
    private double oebAreaAlongMin;

        /** Typical (50th percentile) radar cross-section. */
    private double rcs;

    /** Minimum radar cross-section. */
    private double minRcs;

    /** Maximum radar cross-section. */
    private double maxRcs;

    /** Typical (50th percentile) visual magnitude. */
    private double vmAbsolute;

    /** Minimum apparent visual magnitude. */
    private double vmApparentMin;

    /** Typical (50th percentile) apparent visual magnitude. */
    private double vmApparent;

    /** Maximum apparent visual magnitude. */
    private double vmApparentMax;

    /** Typical (50th percentile) coefficient of reflectivity. */
    private double reflectance;

    /** Simple constructor.
     */
    public CommonPhysicalProperties() {

        oebParentFrame           = new FrameFacade(null, null, OrbitRelativeFrame.RIC, null,
                                                   OrbitRelativeFrame.RIC.name());
        oebParentFrameEpoch      = AbsoluteDate.ARBITRARY_EPOCH;
        oebQ                     = new double[4];
        oebMax                   = Double.NaN;
        oebIntermediate          = Double.NaN;
        oebMin                   = Double.NaN;
        oebAreaAlongMax          = Double.NaN;
        oebAreaAlongIntermediate = Double.NaN;
        oebAreaAlongMin          = Double.NaN;
        rcs                      = Double.NaN;
        minRcs                   = Double.NaN;
        maxRcs                   = Double.NaN;
        vmAbsolute               = Double.NaN;
        vmApparentMin            = Double.NaN;
        vmApparent               = Double.NaN;
        vmApparentMax            = Double.NaN;
        reflectance              = Double.NaN;
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
    }

    /** Get the Optimally Enclosing Box parent reference frame.
     * @return Optimally Enclosing Box parent reference frame
     */
    public FrameFacade getOebParentFrame() {
        return oebParentFrame;
    }

    /** Set the Optimally Enclosing Box parent reference frame.
     * @param oebParentFrame Optimally Enclosing Box parent reference frame
     */
    public void setOebParentFrame(final FrameFacade oebParentFrame) {
        refuseFurtherComments();
        this.oebParentFrame = oebParentFrame;
    }

    /** Get the Optimally Enclosing Box parent reference frame epoch.
     * @return Optimally Enclosing Box parent reference frame epoch
     */
    public AbsoluteDate getOebParentFrameEpoch() {
        return oebParentFrameEpoch;
    }

    /** Set the Optimally Enclosing Box parent reference frame epoch.
     * @param oebParentFrameEpoch Optimally Enclosing Box parent reference frame epoch
     */
    public void setOebParentFrameEpoch(final AbsoluteDate oebParentFrameEpoch) {
        refuseFurtherComments();
        this.oebParentFrameEpoch = oebParentFrameEpoch;
    }

    /** Get the quaternion defining Optimally Enclosing Box.
     * @return quaternion defining Optimally Enclosing Box
     */
    public Quaternion getOebQ() {
        return new Quaternion(oebQ[0], oebQ[1], oebQ[2], oebQ[3]);
    }

    /** set the component of quaternion defining Optimally Enclosing Box.
     * @param i index of the component
     * @param qI component of quaternion defining Optimally Enclosing Box
     */
    public void setOebQ(final int i, final double qI) {
        refuseFurtherComments();
        oebQ[i] = qI;
    }

    /** Get the maximum physical dimension of the OEB.
     * @return maximum physical dimension of the OEB.
     */
    public double getOebMax() {
        return oebMax;
    }

    /** Set the maximum physical dimension of the OEB.
     * @param oebMax maximum physical dimension of the OEB.
     */
    public void setOebMax(final double oebMax) {
        refuseFurtherComments();
        this.oebMax = oebMax;
    }

    /** Get the intermediate physical dimension of the OEB.
     * @return intermediate physical dimension of the OEB.
     */
    public double getOebIntermediate() {
        return oebIntermediate;
    }

    /** Set the intermediate physical dimension of the OEB.
     * @param oebIntermediate intermediate physical dimension of the OEB.
     */
    public void setOebIntermediate(final double oebIntermediate) {
        refuseFurtherComments();
        this.oebIntermediate = oebIntermediate;
    }

    /** Get the minimum physical dimension of the OEB.
     * @return dimensions the minimum physical dimension of the OEB.
     */
    public double getOebMin() {
        return oebMin;
    }

    /** Set the minimum physical dimension of the OEB.
     * @param oebMin the minimum physical dimension of the OEB.
     */
    public void setOebMin(final double oebMin) {
        refuseFurtherComments();
        this.oebMin = oebMin;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     */
    public double getOebAreaAlongMax() {
        return oebAreaAlongMax;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     * @param oebAreaAlongMax cross-sectional area of Optimally Enclosing Box when viewed along the maximum OEB direction.
     */
    public void setOebAreaAlongMax(final double oebAreaAlongMax) {
        refuseFurtherComments();
        this.oebAreaAlongMax = oebAreaAlongMax;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     */
    public double getOebAreaAlongIntermediate() {
        return oebAreaAlongIntermediate;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     * @param oebAreaAlongIntermediate cross-sectional area of Optimally Enclosing Box when viewed along the intermediate OEB direction.
     */
    public void setOebAreaAlongIntermediate(final double oebAreaAlongIntermediate) {
        refuseFurtherComments();
        this.oebAreaAlongIntermediate = oebAreaAlongIntermediate;
    }

    /** Get the cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     * @return cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     */
    public double getOebAreaAlongMin() {
        return oebAreaAlongMin;
    }

    /** Set the cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     * @param oebAreaAlongMin cross-sectional area of Optimally Enclosing Box when viewed along the minimum OEB direction.
     */
    public void setOebAreaAlongMin(final double oebAreaAlongMin) {
        refuseFurtherComments();
        this.oebAreaAlongMin = oebAreaAlongMin;
    }


    /** Get the typical (50th percentile) radar cross-section.
     * @return typical (50th percentile) radar cross-section
     */
    public double getRcs() {
        return rcs;
    }

    /** Set the typical (50th percentile) radar cross-section.
     * @param rcs typical (50th percentile) radar cross-section
     */
    public void setRcs(final double rcs) {
        refuseFurtherComments();
        this.rcs = rcs;
    }

    /** Get the minimum radar cross-section.
     * @return minimum radar cross-section
     */
    public double getMinRcs() {
        return minRcs;
    }

    /** Set the minimum radar cross-section.
     * @param minRcs minimum radar cross-section
     */
    public void setMinRcs(final double minRcs) {
        refuseFurtherComments();
        this.minRcs = minRcs;
    }

    /** Get the maximum radar cross-section.
     * @return maximum radar cross-section
     */
    public double getMaxRcs() {
        return maxRcs;
    }

    /** Set the maximum radar cross-section.
     * @param maxRcs maximum radar cross-section
     */
    public void setMaxRcs(final double maxRcs) {
        refuseFurtherComments();
        this.maxRcs = maxRcs;
    }

    /** Get the typical (50th percentile) visual magnitude.
     * @return typical (50th percentile) visual magnitude
     */
    public double getVmAbsolute() {
        return vmAbsolute;
    }

    /** Set the typical (50th percentile) visual magnitude.
     * @param vmAbsolute typical (50th percentile) visual magnitude
     */
    public void setVmAbsolute(final double vmAbsolute) {
        refuseFurtherComments();
        this.vmAbsolute = vmAbsolute;
    }

    /** Get the minimum apparent visual magnitude.
     * @return minimum apparent visual magnitude
     */
    public double getVmApparentMin() {
        return vmApparentMin;
    }

    /** Set the minimum apparent visual magnitude.
     * @param vmApparentMin minimum apparent visual magnitude
     */
    public void setVmApparentMin(final double vmApparentMin) {
        refuseFurtherComments();
        this.vmApparentMin = vmApparentMin;
    }

    /** Get the typical (50th percentile) apparent visual magnitude.
     * @return typical (50th percentile) apparent visual magnitude
     */
    public double getVmApparent() {
        return vmApparent;
    }

    /** Set the typical (50th percentile) apparent visual magnitude.
     * @param vmApparent typical (50th percentile) apparent visual magnitude
     */
    public void setVmApparent(final double vmApparent) {
        refuseFurtherComments();
        this.vmApparent = vmApparent;
    }

    /** Get the maximum apparent visual magnitude.
     * @return maximum apparent visual magnitude
     */
    public double getVmApparentMax() {
        return vmApparentMax;
    }

    /** Set the maximum apparent visual magnitude.
     * @param vmApparentMax maximum apparent visual magnitude
     */
    public void setVmApparentMax(final double vmApparentMax) {
        refuseFurtherComments();
        this.vmApparentMax = vmApparentMax;
    }

    /** Get the typical (50th percentile) coefficient of reflectance.
     * @return typical (50th percentile) coefficient of reflectance
     */
    public double getReflectance() {
        return reflectance;
    }

    /** Set the typical (50th percentile) coefficient of reflectance.
     * @param reflectance typical (50th percentile) coefficient of reflectance
     */
    public void setReflectance(final double reflectance) {
        refuseFurtherComments();
        this.reflectance = reflectance;
    }
}
