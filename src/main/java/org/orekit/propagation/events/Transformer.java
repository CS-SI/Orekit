/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.orekit.propagation.events;

import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;


/** Transformer for {@link EventHandler#g(double, double[]) g functions}.
 * <p>This class is heavily based on the class with the same name from the
 * Hipparchus library. The changes performed consist in replacing
 * raw types (double and double arrays) with space dynamics types
 * ({@link AbsoluteDate}, {@link SpacecraftState}).</p>
 * @see EventSlopeFilter
 * @see FilterType
 * @since 6.0
 */
enum Transformer {

    /** Transformer computing transformed = 0.
     * <p>
     * This transformer is used when we initialize the filter, until we get at
     * least one non-zero value to select the proper transformer.
     * </p>
     */
    UNINITIALIZED {
        /**  {@inheritDoc} */
        protected double transformed(final double g) {
            return 0;
        }
    },

    /** Transformer computing transformed = g.
     * <p>
     * When this transformer is applied, the roots of the original function
     * are preserved, with the same {@code increasing/decreasing} status.
     * </p>
     */
    PLUS {
        /**  {@inheritDoc} */
        protected double transformed(final double g) {
            return g;
        }
    },

    /** Transformer computing transformed = -g.
     * <p>
     * When this transformer is applied, the roots of the original function
     * are preserved, with reversed {@code increasing/decreasing} status.
     * </p>
     */
    MINUS {
        /**  {@inheritDoc} */
        protected double transformed(final double g) {
            return -g;
        }
    },

    /** Transformer computing transformed = min(-{@link Precision#SAFE_MIN}, -g, +g).
     * <p>
     * When this transformer is applied, the transformed function is
     * guaranteed to be always strictly negative (i.e. there are no roots).
     * </p>
     */
    MIN {
        /**  {@inheritDoc} */
        protected double transformed(final double g) {
            return FastMath.min(-Precision.SAFE_MIN, FastMath.min(-g, +g));
        }
    },

    /** Transformer computing transformed = max(+{@link Precision#SAFE_MIN}, -g, +g).
     * <p>
     * When this transformer is applied, the transformed function is
     * guaranteed to be always strictly positive (i.e. there are no roots).
     * </p>
     */
    MAX {
        /**  {@inheritDoc} */
        protected double transformed(final double g) {
            return FastMath.max(+Precision.SAFE_MIN, FastMath.max(-g, +g));
        }
    };

    /** Transform value of function g.
     * @param g raw value of function g
     * @return transformed value of function g
     */
    protected abstract double transformed(double g);

}
