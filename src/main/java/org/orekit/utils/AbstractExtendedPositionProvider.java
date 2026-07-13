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
package org.orekit.utils;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
 * Abstract class for position provider based on a given type of provider.
 *
 * @param <T> type of the pv coordinates provider
 * @author Romain Serra
 * @see ExtendedPositionProvider
 *
 * @since 14.0
 */
public abstract class AbstractExtendedPositionProvider<T extends PVCoordinatesProvider> implements ExtendedPositionProvider {

    /** Reference trajectory. */
    private final T provider;

    /** Cache for already encountered Field. */
    private final Map<Field<? extends CalculusFieldElement<?>>, FieldPVCoordinatesProvider<?>> fieldCache;

    /**
     * Constructor.
     * @param provider base coordinates provider
     */
    protected AbstractExtendedPositionProvider(final T provider) {
        this.provider = provider;
        this.fieldCache = new HashMap<>();
    }

    /**
     * Protected getter for coordinates provider.
     * @return provider
     */
    protected T getProvider() {
        return provider;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getPosition(final AbsoluteDate date, final Frame frame) {
        return provider.getPosition(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D getVelocity(final AbsoluteDate date, final Frame frame) {
        return provider.getVelocity(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public TimeStampedPVCoordinates getPVCoordinates(final AbsoluteDate date, final Frame frame) {
        return provider.getPVCoordinates(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> FieldVector3D<S> getPosition(final FieldAbsoluteDate<S> date,
                                                                            final Frame frame) {
        return getCachedFieldProviderOrBuild(date.getField()).getPosition(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> FieldVector3D<S> getVelocity(final FieldAbsoluteDate<S> date, final Frame frame) {
        return getCachedFieldProviderOrBuild(date.getField()).getVelocity(date, frame);
    }

    /** {@inheritDoc} */
    @Override
    public <S extends CalculusFieldElement<S>> TimeStampedFieldPVCoordinates<S> getPVCoordinates(final FieldAbsoluteDate<S> date,
                                                                                                 final Frame frame) {
        return getCachedFieldProviderOrBuild(date.getField()).getPVCoordinates(date, frame);
    }

    /**
     * Retrieve cached Field provider or create it.
     * @param field field type
     * @return Field provider
     * @param <S> field
     */
    @SuppressWarnings("unchecked")
    private <S extends CalculusFieldElement<S>> FieldPVCoordinatesProvider<S> getCachedFieldProviderOrBuild(final Field<S> field) {
        return (FieldPVCoordinatesProvider<S>) fieldCache.computeIfAbsent(field, ignored -> getFieldProvider(field));
    }

    /**
     * Build Field provider.
     * @param field field type
     * @return Field provider
     * @param <S> field
     */
    protected abstract <S extends CalculusFieldElement<S>> FieldPVCoordinatesProvider<S> getFieldProvider(Field<S> field);
}
