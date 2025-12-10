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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Position provider from a TLE.
 *
 * @author Romain Serra
 * @see ExtendedPositionProvider
 *
 * @since 14.0
 */
public class TleExtendedPositionProvider implements ExtendedPositionProvider {

    /** Reference orbit. */
    private final TLE tle;

    /** TEME frame. */
    private final Frame teme;

    /** Cached propagator. */
    private TLEPropagator propagator;

    /**
     * Constructor.
     * @param tle reference TLE
     * @param frames frames used for TEME
     */
    public TleExtendedPositionProvider(final TLE tle, final Frames frames) {
        this.tle = tle;
        this.teme = frames.getTEME();
    }

    /**
     * Constructor with default TEME frame.
     * @param tle reference TLE
     */
    @DefaultDataContext
    public TleExtendedPositionProvider(final TLE tle) {
        this(tle, FramesFactory.getFrames());
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return getPropagator().getPosition(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        return getPropagator().getVelocity(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return getPropagator().getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getPosition(final FieldAbsoluteDate<T> date,
                                                                            final Frame frame) {
        return buildFieldPropagator(date.getField()).getPosition(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldVector3D<T> getVelocity(final FieldAbsoluteDate<T> date, final Frame frame) {
        return buildFieldPropagator(date.getField()).getVelocity(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> TimeStampedFieldPVCoordinates<T> getPVCoordinates(final FieldAbsoluteDate<T> date,
                                                                                                 final Frame frame) {
        return buildFieldPropagator(date.getField()).getPVCoordinates(date, frame);
    }

    /** Private getter for the cached propagator.
     * @return propagator
     */
    private TLEPropagator getPropagator() {
        if (propagator == null) {
            propagator = buildPropagator();
        }
        return propagator;
    }

    /**
     * Build propagator.
     * @return TLE propagator
     */
    private TLEPropagator buildPropagator() {
        return TLEPropagator.selectExtrapolator(tle, teme);
    }

    /**
     * Build propagator.
     * @param field field type
     * @return Field TLE propagator
     * @param <T> field
     */
    private <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> buildFieldPropagator(final Field<T> field) {
        return FieldTLEPropagator.selectExtrapolator(new FieldTLE<>(field, tle), teme);
    }
}
