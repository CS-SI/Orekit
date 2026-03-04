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
package org.orekit.propagation.relative.maneuver;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.orbits.FieldOrbit;
import org.orekit.propagation.events.FieldEventDetectionSettings;
import org.orekit.propagation.events.FieldEventDetector;
import org.orekit.propagation.relative.FieldRelativeProvider;

/**
 * Interface for implementing a relative maneuver.
 *
 * @author Romain Cuvillon
 * @since 14.0
 * @param <T> Any scalar field.
 */
public interface FieldRelativeManeuver<T extends CalculusFieldElement<T>> extends FieldEventDetector<T> {
    /**
     * @return The ΔV vector of the maneuver, expressed in the target spacecraft's LOF.
     */
    FieldVector3D<T> getDeltaV();

    /**
     * Get the detection settings.
     * @return detection settings.
     */
    FieldEventDetectionSettings<T> getDetectionSettings();

    /**
     * @return relative motion provider.
     */
    FieldRelativeProvider<T> getRelativeProvider();

    /**
     * @return triggering event.
     */
    FieldEventDetector<T> getTrigger();
    /**
     * Reset True Anomaly of the target orbit in the provider to the true anomaly of the last target state.
     * It keeps the same orbit but modifies the true anomaly.
     * Useful only for Yamanaka-Ankersen model. Do nothing if CW.
     *
     * @param orbit orbit of the target.
     */
    default void resetTrueAnomalyAtManeuver(final FieldOrbit<T> orbit) {
    }
}
