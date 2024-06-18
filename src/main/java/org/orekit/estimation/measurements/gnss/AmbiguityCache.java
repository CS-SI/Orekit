/* Copyright 2002-2024 Luc Maisonobe
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
package org.orekit.estimation.measurements.gnss;

import org.hipparchus.util.Precision;

import java.util.HashMap;
import java.util.Map;

/** Cache for {@link AmbiguityDriver}.
 * @author Luc Maisonobe
 * @since 12.1
 */
public class AmbiguityCache {

    /** Cache map. */
    private final Map<Key, AmbiguityDriver> cache;

    /** Simple constructor.
     */
    public AmbiguityCache() {
        cache = new HashMap<>();
    }

    /** Get a cached driver for ambiguity.
     * <p>
     * A new parameter driver is created and cached the first time an
     * emitter/receiver/wavelength triplet is used; after that, the cached
     * driver will be returned when the same triplet is passed again
     * </p>
     * @param emitter emitter id
     * @param receiver receiver id
     * @param wavelength signal wavelength
     * @return parameter driver for the emitter/receiver/wavelength triplet
     */
    public AmbiguityDriver getAmbiguity(final String emitter, final String receiver, final double wavelength) {
        return cache.computeIfAbsent(new Key(emitter, receiver, wavelength),
                                     k -> new AmbiguityDriver(emitter, receiver, wavelength));
    }

    /** Key for the map. */
    private static class Key {

        /** Emitter id. */
        private final String emitter;

        /** Receiver id. */
        private final String receiver;

        /** Wavelength. */
        private final double wavelength;

        /** Simple constructor.
         * @param emitter emitter id
         * @param receiver receiver id
         * @param wavelength signal wavelength
         */
        Key(final String emitter, final String receiver, final double wavelength) {
            this.emitter    = emitter;
            this.receiver   = receiver;
            this.wavelength = wavelength;
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return (emitter.hashCode() ^ receiver.hashCode()) ^ Double.hashCode(wavelength);
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(final Object object) {
            if (object instanceof Key) {
                final Key other = (Key) object;
                return emitter.equals(other.emitter) && receiver.equals(other.receiver) &&
                       Precision.equals(wavelength, other.wavelength, 1);
            }
            return false;
        }

    }

}
