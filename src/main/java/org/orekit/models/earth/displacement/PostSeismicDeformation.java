/* Copyright 2022-2025 Thales Alenia Space
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
package org.orekit.models.earth.displacement;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.data.BodiesElements;
import org.orekit.frames.Frame;
import org.orekit.utils.TimeSpanMap;

import java.util.List;

/** Modeling of displacement of one reference point due to post-seismic effects.
 * @see <a href="https://itrf.ign.fr/ftp/pub/itrf/itrf2020/ITRF2020-PSD-model-eqs-IGN.pdf">
 *     ITRF2020P: Equations of post-seismic deformation models</a>
 * @since 12.1
 * @author Luc Maisonobe
 */
public class PostSeismicDeformation implements StationDisplacement {

    /** Base point. */
    private final GeodeticPoint base;

    /** PSD corrections. */
    private final TimeSpanMap<List<PsdCorrection>> corrections;

    /** Simple constructor.
     * @param base base point
     * @param corrections Post-Seismic Deformation corrections
     */
    public PostSeismicDeformation(final GeodeticPoint base,
                                  final TimeSpanMap<List<PsdCorrection>> corrections) {
        this.base        = base;
        this.corrections = corrections;
    }

    /** {@inheritDoc} */
    @Override
    public Vector3D displacement(final BodiesElements elements, final Frame earthFrame, final Vector3D referencePoint) {

        // initialize displacement with zero correction
        Vector3D cumulativeCorrection = Vector3D.ZERO;

        // apply all relevant Post-Seismic Deformation corrections
        final List<PsdCorrection> correctionsAtDate = corrections.get(elements.getDate());
        if (correctionsAtDate != null) {
            for (final PsdCorrection correction : correctionsAtDate) {
                cumulativeCorrection = cumulativeCorrection.add(correction.displacement(elements.getDate(), base));
            }
        }

        return cumulativeCorrection;

    }

}
