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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.orekit.frames.Frame;
import org.orekit.utils.FieldPVCoordinatesProvider;
import org.orekit.utils.PVCoordinatesProvider;
import org.orekit.utils.TimeStampedAngularCoordinates;
import org.orekit.utils.TimeStampedFieldAngularCoordinates;


/** This interface represents a builder for attitude.
 * <p>
 * It is intended to modify raw angular coordinates when build attitudes,
 * for example if these coordinates are not defined from the desired reference frame.
 * </p>
 * @author Luc Maisonobe
 * @since 11.0
 */
public interface AttitudeBuilder {

    /**Build a filtered attitude.
     * @param frame reference frame with respect to which attitude must be defined
     * @param pvProv provider for spacecraft position and velocity
     * @param rawAttitude raw rotation/rotation rate/rotation acceleration
     * @return filtered attitude
     */
    Attitude build(Frame frame, PVCoordinatesProvider pvProv, TimeStampedAngularCoordinates rawAttitude);

    /**Build a filtered attitude.
     * @param frame reference frame with respect to which attitude must be defined
     * @param pvProv provider for spacecraft position and velocity
     * @param rawAttitude raw rotation/rotation rate/rotation acceleration
     * @return filtered attitude
     * @param <T> the type of the field elements
     */
    <T extends CalculusFieldElement<T>> FieldAttitude<T> build(Frame frame,
                                                           FieldPVCoordinatesProvider<T> pvProv,
                                                           TimeStampedFieldAngularCoordinates<T> rawAttitude);

}
