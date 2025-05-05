/* Copyright 2022-2025 Romain Serra
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
package org.orekit.models.earth.atmosphere;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.FieldAbsoluteDate;
import org.orekit.utils.ExtendedPositionProvider;

/**
 * Abstract class for atmospheric models using the Sun's position.
 * @author Romain Serra
 * @since 13.0
 */
public abstract class AbstractSunInfluencedAtmosphere implements Atmosphere {

    /** Sun's position provider. */
    private final ExtendedPositionProvider sun;

    /**
     * Constructor.
     * @param sun position provider.
     */
    protected AbstractSunInfluencedAtmosphere(final ExtendedPositionProvider sun) {
        this.sun = sun;
    }

    /**
     * Getter for Sun's position provider.
     * @return position provider
     */
    protected ExtendedPositionProvider getSun() {
        return sun;
    }

    /**
     * Method returning the Sun's position vector.
     * @param date date of output position
     * @param frame frame of output position
     * @return Sun's position
     */
    protected Vector3D getSunPosition(final AbsoluteDate date, final Frame frame) {
        return sun.getPosition(date, frame);
    }

    /**
     * Method returning the Sun's position vector (Field version).
     * @param date date of output position
     * @param frame frame of output position
     * @param <T> field type
     * @return Sun's position
     */
    protected <T extends CalculusFieldElement<T>> FieldVector3D<T> getSunPosition(final FieldAbsoluteDate<T> date,
                                                                                  final Frame frame) {
        return sun.getPosition(date, frame);
    }
}
