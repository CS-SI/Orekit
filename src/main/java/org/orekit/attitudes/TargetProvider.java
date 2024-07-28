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
package org.orekit.attitudes;

import org.hipparchus.CalculusFieldElement;
import org.hipparchus.analysis.differentiation.FieldUnivariateDerivative2;
import org.hipparchus.analysis.differentiation.UnivariateDerivative2;
import org.hipparchus.geometry.euclidean.threed.FieldVector3D;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.frames.Frame;
import org.orekit.utils.ExtendedPVCoordinatesProvider;
import org.orekit.utils.TimeStampedFieldPVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Provider for target vector.
 * @author Luc Maisonobe
 * @since 12.2
 */
public interface TargetProvider
{

    /**
     * Get a target vector.
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame, with second order time derivative
     */
    FieldVector3D<UnivariateDerivative2> getTargetDirection(ExtendedPVCoordinatesProvider sun,
                                                            OneAxisEllipsoid earth,
                                                            TimeStampedPVCoordinates pv,
                                                            Frame frame);

    /**
     * Get a target vector.
     * @param <T>   type of the field element
     * @param sun   Sun model
     * @param earth Earth model
     * @param pv    spacecraft position and velocity
     * @param frame inertial frame
     * @return target direction in the spacecraft state frame, with second order time derivative
     */
    <T extends CalculusFieldElement<T>> FieldVector3D<FieldUnivariateDerivative2<T>> getTargetDirection(ExtendedPVCoordinatesProvider sun,
                                                                                                        OneAxisEllipsoid earth,
                                                                                                        TimeStampedFieldPVCoordinates<T> pv,
                                                                                                        Frame frame);

}
