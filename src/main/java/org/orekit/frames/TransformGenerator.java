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

import java.util.ArrayList;
import java.util.List;

import org.orekit.time.AbsoluteDate;
import org.orekit.utils.GenericTimeStampedCache;
import org.orekit.utils.TimeStampedGenerator;

/** Generator to use transforms in {@link GenericTimeStampedCache}.
 * @see GenericTimeStampedCache
 * @since 9.0
 * @author Luc Maisonobe
 */
public class TransformGenerator implements TimeStampedGenerator<Transform> {

    /** Number of neighbors. */
    private int neighborsSize;

    /** Underlying provider. */
    private final TransformProvider provider;

    /** Step size. */
    private final double step;

    /** simple constructor.
     * @param neighborsSize number of neighbors
     * @param provider underlying provider
     * @param step step size
     */
    public TransformGenerator(final int neighborsSize,
                              final TransformProvider provider,
                              final double step) {
        this.neighborsSize = neighborsSize;
        this.provider      = provider;
        this.step          = step;
    }

    /** {@inheritDoc} */
    public List<Transform> generate(final AbsoluteDate existingDate, final AbsoluteDate date) {

        final List<Transform> generated = new ArrayList<>();

        if (existingDate == null) {

            // no prior existing transforms, just generate a first one
            for (int i = 0; i < neighborsSize; ++i) {
                generated.add(provider.getTransform(date.shiftedBy(i * step)));
            }

        } else {

            // some transforms have already been generated
            // add the missing ones up to specified date
            AbsoluteDate t = existingDate;
            if (date.compareTo(t) > 0) {
                // forward generation
                do {
                    t = t.shiftedBy(step);
                    generated.add(generated.size(), provider.getTransform(t));
                } while (t.compareTo(date) <= 0);
            } else {
                // backward generation
                do {
                    t = t.shiftedBy(-step);
                    generated.add(0, provider.getTransform(t));
                } while (t.compareTo(date) >= 0);
            }

        }

        // return the generated transforms
        return generated;

    }

}
