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
package org.orekit.propagation.analytical.tle;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.frames.Frame;
import org.orekit.frames.Frames;
import org.orekit.frames.FramesFactory;
import org.orekit.utils.AbstractExtendedPositionProvider;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Position provider from a TLE.
 *
 * @author Romain Serra
 * @see ExtendedPositionProvider
 * @see TLE
 *
 * @since 14.0
 */
public class TleExtendedPositionProvider extends AbstractExtendedPositionProvider<TLEPropagator> {

    /** Reference orbit. */
    private final TLE tle;

    /** TEME frame. */
    private final Frame teme;

    /**
     * Constructor.
     * @param tle reference TLE
     * @param frames frames used for TEME
     */
    public TleExtendedPositionProvider(final TLE tle, final Frames frames) {
        super(TLEPropagator.selectExtrapolator(tle, frames.getTEME()));
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
    protected <T extends CalculusFieldElement<T>> FieldTLEPropagator<T> getFieldProvider(final Field<T> field) {
        return FieldTLEPropagator.selectExtrapolator(new FieldTLE<>(field, tle), teme);
    }
}
