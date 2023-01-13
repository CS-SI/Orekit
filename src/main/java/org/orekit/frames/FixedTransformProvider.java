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

package org.orekit.frames;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.hipparchus.Field;
import org.hipparchus.CalculusFieldElement;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;

/**
** Transform provider using fixed transform.
 * @author Luc Maisonobe
 */
public class FixedTransformProvider implements TransformProvider {

    /** Serializable UID. */
    private static final long serialVersionUID = 20170106L;

    /** Fixed transform. */
    private final Transform transform;

    /** Cached field-based transforms. */
    private final transient Map<Field<? extends CalculusFieldElement<?>>, FieldTransform<? extends CalculusFieldElement<?>>> cached;

    /** Simple constructor.
     * @param transform fixed transform
     */
    public FixedTransformProvider(final Transform transform) {
        this.transform = transform;
        this.cached    = new HashMap<>();
    }

    /** {@inheritDoc} */
    public Transform getTransform(final AbsoluteDate date) {
        return transform;
    }

    /** {@inheritDoc} */
    @Override
    public <T extends CalculusFieldElement<T>> FieldTransform<T> getTransform(final FieldAbsoluteDate<T> date) {

        @SuppressWarnings("unchecked")
        FieldTransform<T> ft = (FieldTransform<T>) cached.get(date.getField());
        if (ft == null) {
            ft = new FieldTransform<>(date.getField(), transform);
            cached.put(date.getField(), ft);
        }

        return ft;

    }

    /** Replace the instance with a data transfer object for serialization.
     * <p>
     * This intermediate class serializes nothing.
     * </p>
     * @return data transfer object that will be serialized
     */
    private Object writeReplace() {
        return new DataTransferObject(transform);
    }

    /** Internal class used only for serialization. */
    private static class DataTransferObject implements Serializable {

        /** Serializable UID. */
        private static final long serialVersionUID = 20170106L;

        /** Fixed transform. */
        private final Transform transform;

        /** Simple constructor.
         * @param transform fixed transform
         */
        private DataTransferObject(final Transform transform) {
            this.transform = transform;
        }

        /** Replace the deserialized data transfer object with a {@link FixedTransformProvider}.
         * @return replacement {@link FixedTransformProvider}
         */
        private Object readResolve() {
            return new FixedTransformProvider(transform);
        }

    }

}
