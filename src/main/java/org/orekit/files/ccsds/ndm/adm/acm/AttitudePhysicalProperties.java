/* Copyright 2023 Luc Maisonobe
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

package org.orekit.files.ccsds.ndm.adm.acm;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.time.AbsoluteDate;

/** Spacecraft physical properties.
 * @author Luc Maisonobe
 * @since 12.0
 */
public class AttitudePhysicalProperties extends CommentsContainer {

    /** Drag coefficient. */
    private double dragCoefficient;

    /** Total mass at T₀. */
    private double wetMass;

    /** Mass without propellant. */
    private double dryMass;

    /** Reference frame for center of pressure. */
    private FrameFacade centerOfPressureReferenceFrame;

    /** Location of center of pressure. */
    private Vector3D centerOfPressure;

    /** Reference frame for inertia. */
    private FrameFacade inertiaReferenceFrame;

    /** Inertia matrix. */
    private RealMatrix inertiaMatrix;

    /** Simple constructor.
     * @param epochT0 T0 epoch from file metadata
     */
    public AttitudePhysicalProperties(final AbsoluteDate epochT0) {
        dragCoefficient = Double.NaN;
        wetMass         = Double.NaN;
        dryMass         = Double.NaN;
        inertiaMatrix   = MatrixUtils.createRealMatrix(3, 3);
    }

    /** {@inheritDoc} */
    @Override
    public void validate(final double version) {
        super.validate(version);
        if (centerOfPressureReferenceFrame != null) {
            checkNotNull(centerOfPressure, AttitudePhysicalPropertiesKey.CP.name());
        }
    }

    /** Get the drag coefficient.
     * @return the drag coefficient
     */
    public double getDragCoefficient() {
        return dragCoefficient;
    }

    /** Set the the drag coefficient.
     * @param dragCoefficient the drag coefficient
     */
    public void setDragCoefficient(final double dragCoefficient) {
        refuseFurtherComments();
        this.dragCoefficient = dragCoefficient;
    }

    /** Get the total mass at T₀.
     * @return total mass at T₀
     */
    public double getWetMass() {
        return wetMass;
    }

    /** Set the total mass at T₀.
     * @param wetMass total mass at T₀
     */
    public void setWetMass(final double wetMass) {
        refuseFurtherComments();
        this.wetMass = wetMass;
    }

    /** Get the mass without propellant.
     * @return mass without propellant
     */
    public double getDryMass() {
        return dryMass;
    }

    /** Set the mass without propellant.
     * @param dryMass mass without propellant
     */
    public void setDryMass(final double dryMass) {
        refuseFurtherComments();
        this.dryMass = dryMass;
    }

    /** Get reference frame for center of pressure.
     * @return reference frame for center of pressure
     */
    public FrameFacade getCenterOfPressureReferenceFrame() {
        return centerOfPressureReferenceFrame;
    }

    /** Set reference frame for center of pressure.
     * @param centerOfPressureReferenceFrame reference frame for center of pressure
     */
    public void setCenterOfPressureReferenceFrame(final FrameFacade centerOfPressureReferenceFrame) {
        this.centerOfPressureReferenceFrame = centerOfPressureReferenceFrame;
    }

    /** Get the location of center of pressure.
     * @return location of center of pressure
     */
    public Vector3D getCenterOfPressure() {
        return centerOfPressure;
    }

    /** Set the location of center of pressure.
     * @param centerOfPressure location of center of pressure
     */
    public void setCenterOfPressure(final Vector3D centerOfPressure) {
        this.centerOfPressure = centerOfPressure;
    }

    /** Get reference frame for inertia.
     * @return reference frame for inertia
     */
    public FrameFacade getInertiaReferenceFrame() {
        return inertiaReferenceFrame;
    }

    /** Set reference frame for inertia.
     * @param inertiaReferenceFrame reference frame for inertia
     */
    public void setInertiaReferenceFrame(final FrameFacade inertiaReferenceFrame) {
        this.inertiaReferenceFrame = inertiaReferenceFrame;
    }

    /** Get the inertia matrix.
     * @return the inertia matrix
     */
    public RealMatrix getInertiaMatrix() {
        return inertiaMatrix;
    }

    /** Set an entry in the inertia matrix.
     * <p>
     * Both I(j, k) and I(k, j) are set.
     * </p>
     * @param j row index (must be between 0 and 3 (inclusive)
     * @param k column index (must be between 0 and 3 (inclusive)
     * @param entry value of the matrix entry
     */
    public void setInertiaMatrixEntry(final int j, final int k, final double entry) {
        refuseFurtherComments();
        inertiaMatrix.setEntry(j, k, entry);
        inertiaMatrix.setEntry(k, j, entry);
    }

}
