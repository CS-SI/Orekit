/* Copyright 2002-2017 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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

import java.util.ArrayList;
import java.util.List;

import org.hipparchus.Field;
import org.hipparchus.RealFieldElement;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.TimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Generator to use field transforms in {@link GenericTimeStampedCache}.
 * @see GenericTimeStampedCache
 * @since 9.0
 * @author Luc Maisonobe
 */
public class FieldTransformGenerator<T extends RealFieldElement<T>> implements TimeStampedGenerator<FieldTransform<T>> {

    /** Field to which the elements belong. */
    private final Field<T> field;

    /** Underlying cache. */
    private final TimeStampedCache<Transform> cache;

    /** Step size. */
    private final double step;

    /** simple constructor.
     * @param field field to which the elements belong
     * @param cache underlying cache
     * @param step step size
     */
    public FieldTransformGenerator(final Field<T> field,
                                   final TimeStampedCache<Transform> cache,
                                   final double step) {
        this.field = field;
        this.cache = cache;
        this.step  = step;
    }

    /** {@inheritDoc} */
    public List<FieldTransform<T>> generate(final AbsoluteDate existingDate, final AbsoluteDate date) {

        try {

            final List<FieldTransform<T>> generated = new ArrayList<>();

            if (existingDate == null) {

                // no prior existing transforms, just generate a first set
                cache.getNeighbors(date).forEach(tr -> generated.add(new FieldTransform<>(field, tr)));

            } else {

                // some transforms have already been generated
                // add the missing ones up to specified date
                AbsoluteDate previous;
                final AbsoluteDate last;
                if (date.compareTo(existingDate) > 0) {
                    // forward generation
                    previous = existingDate;
                    last     = date;
                } else {
                    // backward generation
                    previous = date;
                    last     = existingDate;
                }

                do {
                    final AbsoluteDate target = previous.shiftedBy(step);
                    cache.getNeighbors(target).
                        filter(tr -> tr.getDate().durationFrom(target) >= 0).
                        forEach(tr -> generated.add(new FieldTransform<>(field, tr)));
                    previous = generated.get(generated.size() - 1).getDate();
                } while (previous.compareTo(last) <= 0);

            }

            // return the generated transforms
            return generated;
        } catch (OrekitException oe) {
            throw new OrekitExceptionWrapper(oe);
        }

    }

}
