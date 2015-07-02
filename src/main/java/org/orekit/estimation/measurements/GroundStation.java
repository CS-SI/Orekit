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
import org.apache.commons.math3.util.FastMath;
import org.orekit.bodies.GeodeticPoint;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Parameter;
import org.orekit.frames.Frame;
import org.orekit.frames.TopocentricFrame;
import org.orekit.frames.Transform;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;

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

    /** Compute propagation delay on the downlink leg.
     * @param state of the spacecraft, close to reception date
     * @param groundArrivalDate date at which the associated measurement
     * is received on ground
     * @return positive delay between emission date on spacecraft and
     * signal reception date on ground
     * @exception OrekitException if some frame transforms fails
     */
    public double downlinkTimeOfFlight(final SpacecraftState state, final AbsoluteDate groundArrivalDate)
        throws OrekitException {

        // station position at signal arrival date, in inertial frame
        // (the station is not there at signal departure date, but will
        //  be there at the signal arrival)
        final Transform t = offsetFrame.getTransformTo(state.getFrame(), groundArrivalDate);
        final Vector3D arrival = t.transformPosition(Vector3D.ZERO);

        // initialize emission date search loop assuming the state is already correct
        // this will be true for all but the first orbit determination iteration,
        // and even for the first iteration the loop will converge very fast
        final double offset = groundArrivalDate.durationFrom(state.getDate());
        double delay = offset;

        // search signal transit date, computing the signal travel in inertial frame
        double delta;
        int count = 0;
        do {
            final double previous  = delay;
            final Vector3D transit = state.shiftedBy(offset - delay).getPVCoordinates().getPosition();
            delay                  = Vector3D.distance(transit, arrival) / Constants.SPEED_OF_LIGHT;
            delta                  = FastMath.abs(delay - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay));

        return delay;

    }

    /** Compute propagation delay on the uplink leg.
     * @param state of the spacecraft at signal transit date on board
     * @return positive delay between emission date on ground and
     * signal reception date on board
     * @exception OrekitException if some frame transforms fails
     */
    public double uplinkTimeOfFlight(final SpacecraftState state)
        throws OrekitException {

        // spacecraft position at signal transit date, in inertial frame
        // (the spacecraft is not there at signal departure date, but will
        //  be there at the signal transit)
        final Vector3D transit = state.getPVCoordinates().getPosition();

        // search signal departure date, computing the signal travel in inertial frame
        double delta;
        double delay = 0;
        int count = 0;
        do {
            final double       previous      = delay;
            final AbsoluteDate departureDate = state.getDate().shiftedBy(-delay);
            final Transform    t             = offsetFrame.getTransformTo(state.getFrame(), departureDate);
            final Vector3D     departure     = t.transformPosition(Vector3D.ZERO);
            delay = Vector3D.distance(departure, transit) / Constants.SPEED_OF_LIGHT;
            delta = FastMath.abs(delay - previous);
        } while (count++ < 10 && delta >= 2 * FastMath.ulp(delay));

        return delay;

    }

}
