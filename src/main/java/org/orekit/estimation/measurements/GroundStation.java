/* Copyright 2002-2015 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements;

import org.orekit.bodies.GeodeticPoint;
import org.orekit.estimation.Parameter;
import org.orekit.frames.TopocentricFrame;

/** Class modeling a ground station that can perform some measurements.
 * @author Luc Maisonobe
 * @since 7.1
 */
public class GroundStation {

    /** Suffix for ground station position offset parameter name. */
    public static final String OFFSET_SUFFIX = "-offset";

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Position offset parameter. */
    private final Parameter positionOffset;

    /** Simple constructor.
     * @param baseFrame base frame associated with the station
     */
    public GroundStation(final TopocentricFrame baseFrame) {
        this.baseFrame = baseFrame;
        positionOffset = new Parameter(baseFrame.getName() + OFFSET_SUFFIX, new double[3], false);
    }

    /** Get the base frame associated with the station.
     * <p>
     * The base frame corresponds to a null position offset
     * </p>
     * @return base frame associated with the station
     */
    public TopocentricFrame getBaseFrame() {
        return baseFrame;
    }

    /** Get the offset frame associated with the station.
     * <p>
     * The offset frame takes the position offset into account
     * </p>
     * @return offset frame associated with the station
     */
    public TopocentricFrame getOffsetFrame() {
        final GeodeticPoint gp    = baseFrame.getPoint();
        final double[]      delta = positionOffset.getValue();
        return new TopocentricFrame(baseFrame.getParentShape(),
                                    new GeodeticPoint(gp.getLatitude()  + delta[0],
                                                      gp.getLongitude() + delta[1],
                                                      gp.getAltitude()  + delta[2]),
                                    baseFrame.getName());
    }

    /** Get the position offset parameter.
     * @return position offset parameter
     */
    public Parameter getPositionOffset() {
        return positionOffset;
    }

}
