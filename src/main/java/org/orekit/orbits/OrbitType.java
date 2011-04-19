/* Copyright 2002-2011 CS Communication & Systèmes
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.orbits;

/** Enumerate for {@link Orbit orbital} parameters types.
 */
public enum OrbitType {

    /** Type for propagation in {@link CartesianOrbit Cartesian parameters}. */
    CARTESIAN {
        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CartesianOrbit(orbit);
        }
    },

    /** Type for propagation in {@link CircularOrbit circular parameters}. */
    CIRCULAR {
        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new CircularOrbit(orbit);
        }
    },

    /** Type for propagation in {@link EquinoctialOrbit equinoctial parameters}. */
    EQUINOCTIAL {
        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new EquinoctialOrbit(orbit);
        }
    },

    /** Type for propagation in {@link KeplerianOrbit Keplerian parameters}. */
    KEPLERIAN {
        /** {@inheritDoc} */
        public Orbit convertType(final Orbit orbit) {
            return (orbit.getType() == this) ? orbit : new KeplerianOrbit(orbit);
        }
    };

    /** Convert an orbit to the instance type.
     * <p>
     * The returned orbit is the specified instance itself if its type already matches,
     * otherwise, a new orbit of the proper type created
     * </p>
     * @param orbit orbit to convert
     * @return converted orbit with type guaranteed to match (so it can be cast safely)
     */
    public abstract Orbit convertType(final Orbit orbit);

}
