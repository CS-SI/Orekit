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

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;

/** Class modeling a ground station that can perform some measurements.
 * <p>
 * This class adds a position offset parameter to a base {@link TopocentricFrame
 * topocentric frame}.
 * </p>
 * @author Luc Maisonobe
 * @since 7.1
 */
public class GroundStation extends Parameter {

    /** Suffix for ground station position offset parameter name. */
    public static final String OFFSET_SUFFIX = "-offset";

    /** Base frame associated with the station. */
    private final TopocentricFrame baseFrame;

    /** Offset frame associated with the station, taking offset parameter into account. */
    private TopocentricFrame offsetFrame;

    /** Simple constructor.
     * @param baseFrame base frame associated with the station
     * @exception OrekitException if some frame transforms cannot be computed
     */
    public GroundStation(final TopocentricFrame baseFrame)
        throws OrekitException {

        super(baseFrame.getName() + OFFSET_SUFFIX);
        this.baseFrame = baseFrame;

        // position offset parameter
        setValue(0.0, 0.0, 0.0);

    }

    /** {@inheritDoc} */
    @Override
    public void valueChanged(final double[] newValue) throws OrekitException {

        // estimate new origin for offset frame, in body frame
        final Frame     bodyFrame    = baseFrame.getParent();
        final Transform baseToBody   = baseFrame.getTransformTo(bodyFrame, null);
        final Vector3D  origin       = baseToBody.transformPosition(new Vector3D(newValue[0],
                                                                                 newValue[1],
                                                                                 newValue[2]));
        final GeodeticPoint originGP = baseFrame.getParentShape().transform(origin, bodyFrame, null);

        // create a new topocentric frame at parameterized origin
        offsetFrame = new TopocentricFrame(baseFrame.getParentShape(), originGP,
                                           baseFrame.getName() + OFFSET_SUFFIX);

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
        return offsetFrame;
    }

}
