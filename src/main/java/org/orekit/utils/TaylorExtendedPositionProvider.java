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

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.Field;

/**
 * Class providing position, including Field, according to 2nd order Taylor expansion.
 * @author Romain Serra
 * @see AbsolutePVCoordinates
 * @since 14.0
 */
public class TaylorExtendedPositionProvider extends AbstractExtendedPositionProvider<AbsolutePVCoordinates> {

    /**
     * Constructor.
     * @param absolutePVCoordinates absolute Cartesian coordinates
     */
    public TaylorExtendedPositionProvider(final AbsolutePVCoordinates absolutePVCoordinates) {
        super(absolutePVCoordinates);
    }

    /** {@inheritDoc} */
    @Override
    protected <T extends CalculusFieldElement<T>> FieldAbsolutePVCoordinates<T> getFieldProvider(final Field<T> field) {
        return new FieldAbsolutePVCoordinates<>(field, getProvider());
    }
}
