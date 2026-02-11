/* Copyright 2022-2026 Romain Serra
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
package org.orekit.propagation.events.functions;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.models.AtmosphericRefractionModel;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;


/** Event function for ground location being at night.
 * <p>
 * It is positive when ground is at night
 * (i.e. Sun is below dawn/dusk elevation angle).
 * </p>
 * @author Luc Maisonobe
 * @author Romain Serra
 * @since 14.0
 */
public class GroundAtNightEventFunction extends AbstractTopocentricEventFunction {

    /** Provider for Sun position. */
    private final ExtendedPositionProvider sun;

    /** Sun elevation below which we consider night is dark enough. */
    private final double dawnDuskElevation;

    /** Atmospheric Model used for calculations, if defined. */
    private final AtmosphericRefractionModel refractionModel;

    /** Private constructor.
     * @param topocentricFrame ground location from which measurement is performed
     * @param sun provider for Sun position
     * @param dawnDuskElevation Sun elevation below which we consider night is dark enough (rad)
     * @param refractionModel reference to refraction model (null if refraction should be ignored),
     */
    public GroundAtNightEventFunction(final TopocentricFrame topocentricFrame, final ExtendedPositionProvider sun,
                                      final double dawnDuskElevation, final AtmosphericRefractionModel refractionModel) {
        super(topocentricFrame);
        this.sun               = sun;
        this.dawnDuskElevation = dawnDuskElevation;
        this.refractionModel   = refractionModel;
    }

    @Override
    public double value(final SpacecraftState state) {
        final AbsoluteDate date     = state.getDate();
        final Frame         frame    = state.getFrame();
        final Vector3D position = sun.getPosition(date, frame);
        final double trueElevation   = getTopocentricFrame().getElevation(position, frame, date);

        final double calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = trueElevation + refractionModel.getRefraction(trueElevation);
        } else {
            calculatedElevation = trueElevation;
        }

        return dawnDuskElevation - calculatedElevation;
    }

    /** {@inheritDoc}
     * <p>
     * The function is positive when ground is at night
     * (i.e. Sun is below dawn/dusk elevation angle).
     * </p>
     * <p>
     * This function only depends on date, not on the actual position of the spacecraft.
     * </p>
     */
    @Override
    public <T extends CalculusFieldElement<T>> T value(final FieldSpacecraftState<T> state) {

        final FieldAbsoluteDate<T> date     = state.getDate();
        final Frame         frame    = state.getFrame();
        final FieldVector3D<T> position = sun.getPosition(date, frame);
        final T trueElevation   = getTopocentricFrame().getElevation(position, frame, date);

        final T calculatedElevation;
        if (refractionModel != null) {
            calculatedElevation = trueElevation.add(refractionModel.getRefraction(trueElevation.getReal()));
        } else {
            calculatedElevation = trueElevation;
        }

        return calculatedElevation.negate().add(dawnDuskElevation);

    }

    @Override
    public boolean dependsOnTimeOnly() {
        return true;
    }
}
