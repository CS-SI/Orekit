/* Copyright 2022-2025 Romain Serra
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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Abstract class for attitude provider modifiers using an underlying ground pointing law.
 *
 * @see GroundPointing
 * @see AttitudeProviderModifier
 * @author Romain Serra
 * @since 12.1
 */
public abstract class GroundPointingAttitudeModifier extends GroundPointing implements AttitudeProviderModifier {

    /**
     * Underlying ground pointing law.
     */
    private final GroundPointing groundPointingLaw;

    /** Constructor.
     * @param inertialFrame frame in which orbital velocities are computed
     * @param bodyFrame the frame that rotates with the body
     * @param groundPointingLaw underlying ground pointing attitude law
     */
    protected GroundPointingAttitudeModifier(final Frame inertialFrame, final Frame bodyFrame,
                                             final GroundPointing groundPointingLaw) {
        super(inertialFrame, bodyFrame);
        this.groundPointingLaw = groundPointingLaw;
    }

    /**
     * Getter for underlying ground pointing law.
     * @return underlying attitude provider, which in this case is a {@link GroundPointing} instance
     */
    @Override
    public GroundPointing getUnderlyingAttitudeProvider() {
        return groundPointingLaw;
    }

    /** Compute the base system state at given date, without modifications.
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @return satellite base attitude state.
     */
    public Attitude getBaseState(final PVCoordinatesProvider pvProv,
                                 final AbsoluteDate date, final Frame frame) {
        return getUnderlyingAttitudeProvider().getAttitude(pvProv, date, frame);
    }

    /** Compute the base system state at given date, without modifications.
     * @param pvProv provider for PV coordinates
     * @param date date at which state is requested
     * @param frame reference frame from which attitude is computed
     * @param <T> type of the field elements
     * @return satellite base attitude state.
     */
    public <T extends CalculusFieldElement<T>> FieldAttitude<T> getBaseState(final FieldPVCoordinatesProvider<T> pvProv,
                                                                             final FieldAbsoluteDate<T> date, final Frame frame) {
        return getUnderlyingAttitudeProvider().getAttitude(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getTargetPV(final PVCoordinatesProvider pvProv,
                                                final AbsoluteDate date, final Frame frame) {
        return groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected Vector3D getTargetPosition(final PVCoordinatesProvider pvProv, final AbsoluteDate date, final Frame frame) {
        return groundPointingLaw.getTargetPosition(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getTargetPV(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                            final FieldAbsoluteDate<T> date,
                                                                                            final Frame frame) {
        return groundPointingLaw.getTargetPV(pvProv, date, frame);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getTargetPosition(final FieldPVCoordinatesProvider<T> pvProv,
                                                                                     final FieldAbsoluteDate<T> date,
                                                                                     final Frame frame) {
        return groundPointingLaw.getTargetPosition(pvProv, date, frame);
    }
}
