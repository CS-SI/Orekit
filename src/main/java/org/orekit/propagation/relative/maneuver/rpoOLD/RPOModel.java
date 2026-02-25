/* Copyright 2002-2026 CS GROUP
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
package org.orekit.propagation.relative.maneuver.rpoOLD;

import org.hipparchus.geometry.euclidean.threed.Vector3D;


/**
 * Enumeration used to compute the relative maneuvers based on Clohessy-Wiltshire equations (only Circular cases) or Yamanaka-Ankersen equations.
 *
 * @author Romain Cuvillon
 * @since 14.0
 */
public enum RPOModel implements RPO {
    /**
     * CW: Clohessy-Wiltshire.
     */
    CW {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */

        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_J;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.PLUS_K;
        }
    },
    /**
     * YA : Yamanaka-Ankersen.
     */
    YA {
        /** {@inheritDoc} */
        public Vector3D getRBarDirection() {
            return Vector3D.PLUS_K;
        }

        /** {@inheritDoc} */
        public Vector3D getVBarDirection() {
            return Vector3D.PLUS_I;
        }

        /** {@inheritDoc} */
        public Vector3D getOutOfPlaneDirection() {
            return Vector3D.MINUS_J;
        }
    }
}
