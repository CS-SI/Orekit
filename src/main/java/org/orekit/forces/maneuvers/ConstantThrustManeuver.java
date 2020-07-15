/* Copyright 2002-2020 CS GROUP
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
package org.orekit.forces.maneuvers;

import org.hipparchus.RealFieldElement;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.forces.maneuvers.propulsion.AbstractConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.propulsion.BasicConstantThrustPropulsionModel;
import org.orekit.forces.maneuvers.trigger.DateBasedManeuverTriggers;
import org.orekit.propagation.FieldSpacecraftState;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** This class implements a simple maneuver with constant thrust.
 * <p>The maneuver is defined by a direction in satellite frame.
 * The current attitude of the spacecraft, defined by the current
 * spacecraft state, will be used to compute the thrust direction in
 * inertial frame. A typical case for tangential maneuvers is to use a
 * {@link org.orekit.attitudes.LofOffset LOF aligned} attitude provider
 * for state propagation and a velocity increment along the +X satellite axis.</p>
 * @author Fabien Maussion
 * @author V&eacute;ronique Pommier-Maurussane
 * @author Luc Maisonobe
 * @author Maxime Journot
 */
public class ConstantThrustManeuver extends Maneuver {

    /** Parameter name for thrust.
     * @deprecated as of 10.2,
     *             replace by {@link BasicConstantThrustPropulsionModel#THRUST}*/
    @Deprecated
    public static final String THRUST = "thrust";

    /** Parameter name for flow rate.
     * @deprecated as of 10.2,
     *             replace by {@link BasicConstantThrustPropulsionModel#FLOW_RATE}*/
    @Deprecated
    public static final String FLOW_RATE = "flow rate";

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses the propulsion model {@link BasicConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * Calling this constructor is equivalent to call {@link
     * #ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, String)
     * ConstantThrustManeuver(date, duration, thrust, isp, direction, "")},
     * hence not using any prefix for the parameters drivers names.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param direction the acceleration direction in satellite frame.
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction) {
        this(date, duration, thrust, isp, direction, "");
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses the propulsion model {@link BasicConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * Calling this constructor is equivalent to call {@link
     * #ConstantThrustManeuver(AbsoluteDate, double, double, double, Vector3D, String)
     * ConstantThrustManeuver(date, duration, thrust, isp, direction, "")},
     * hence not using any prefix for the parameters drivers names.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param direction the acceleration direction in satellite frame.
     * @since 9.2
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final AttitudeProvider attitudeOverride, final Vector3D direction) {
        this(date, duration, thrust, isp, attitudeOverride, direction, "");
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses the propulsion model {@link BasicConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * The name of the maneuver is used to distinguish the parameter drivers.
     * A typical use case is to use something like "1A-" or "2B-" as a prefix corresponding to the
     * name of the thruster to use, so separate parameters can be adjusted
     * for the different thrusters involved during an orbit determination
     * where maneuvers parameters are estimated.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param direction the acceleration direction in satellite frame
     * @param name name of the maneuver, used as a prefix for the {@link #getParametersDrivers() parameters drivers}
     * @since 9.0
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final Vector3D direction,
                                  final String name) {
        this(date, duration, thrust, isp, null, direction, name);
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses the propulsion model {@link BasicConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * The name of the maneuver is used to distinguish the parameter drivers.
     * A typical use case is to use something like "1A-" or "2B-" as a prefix corresponding to the
     * name of the thruster to use, so separate parameters can be adjusted
     * for the different thrusters involved during an orbit determination
     * where maneuvers parameters are estimated.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp engine specific impulse (s)
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param direction the acceleration direction in satellite frame
     * @param name name of the maneuver, used as a prefix for the {@link #getParametersDrivers() parameters drivers}
     * @since 9.2
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final double thrust, final double isp,
                                  final AttitudeProvider attitudeOverride, final Vector3D direction,
                                  final String name) {
        this(date, duration, attitudeOverride, new BasicConstantThrustPropulsionModel(thrust, isp, direction, name));
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses an {@link AbstractConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * The names of the maneuver (and thus its parameter drivers) are extracted
     * from the propulsion model.
     * </p>
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param constantThrustPropulsionModel user-defined constant thrust propulsion model
     */
    public ConstantThrustManeuver(final AbsoluteDate date, final double duration,
                                  final AttitudeProvider attitudeOverride,
                                  final AbstractConstantThrustPropulsionModel constantThrustPropulsionModel) {
        this(attitudeOverride, new DateBasedManeuverTriggers(date, duration), constantThrustPropulsionModel);
    }

    /** Simple constructor for a constant direction and constant thrust.
     * <p>
     * It uses an {@link AbstractConstantThrustPropulsionModel} and
     * the maneuver triggers {@link DateBasedManeuverTriggers}
     * </p><p>
     * The names of the maneuver (and thus its parameter drivers) are extracted
     * from the propulsion model or the maneuver triggers.
     * Propulsion model name is evaluated first, if it isn't empty, it becomes the name of the maneuver.
     * In that case the name in the maneuver triggers should be the same or empty, otherwise this could be
     * misleading when retrieving estimated parameters by their names.
     * </p>
     * @param attitudeOverride the attitude provider to use for the maneuver, or
     * null if the attitude from the propagator should be used
     * @param dateBasedManeuverTriggers user-defined maneuver triggers object based on a start and end date
     * @param constantThrustPropulsionModel user-defined constant thrust propulsion model
     */
    public ConstantThrustManeuver(final AttitudeProvider attitudeOverride,
                                  final DateBasedManeuverTriggers dateBasedManeuverTriggers,
                                  final AbstractConstantThrustPropulsionModel constantThrustPropulsionModel) {
        super(attitudeOverride, dateBasedManeuverTriggers, constantThrustPropulsionModel);
    }

    /** Get the thrust vector (N) in S/C frame.
     * @return thrust vector (N) in S/C frame.
     */
    public Vector3D getThrustVector() {
        return ((AbstractConstantThrustPropulsionModel) (getPropulsionModel())).getThrustVector();
    }

    /** Get the thrust.
     * @return thrust force (N).
     */
    public double getThrust() {
        return getThrustVector().getNorm();
    }

    /** Get the specific impulse.
     * @return specific impulse (s).
     */
    public double getISP() {
        return ((AbstractConstantThrustPropulsionModel) (getPropulsionModel())).getIsp();
    }

    /** Get the flow rate.
     * @return flow rate (negative, kg/s).
     */
    public double getFlowRate() {
        return ((AbstractConstantThrustPropulsionModel) (getPropulsionModel())).getFlowRate();
    }

    /** Get the direction.
     * @return the direction
     * @since 9.2
     */
    public Vector3D getDirection() {
        return getThrustVector().normalize();
    }

    /** Get the start date.
     * @return the start date
     * @since 9.2
     */
    public AbsoluteDate getStartDate() {
        return ((DateBasedManeuverTriggers) (getManeuverTriggers())).getStartDate();
    }

    /** Get the end date.
     * @return the end date
     * @since 9.2
     */
    public AbsoluteDate getEndDate() {
        return ((DateBasedManeuverTriggers) (getManeuverTriggers())).getEndDate();
    }

    /** Get the duration of the maneuver (s).
     * duration = endDate - startDate
     * @return the duration of the maneuver (s)
     * @since 9.2
     */
    public double getDuration() {
        return ((DateBasedManeuverTriggers) (getManeuverTriggers())).getDuration();
    }

    /** Check if maneuvering is on.
     * @param s current state
     * @return true if maneuver is on at this state
     * @since 10.1
     */
    public boolean isFiring(final SpacecraftState s) {
        return isFiring(s.getDate());
    }

    /** Check if maneuvering is on.
     * @param s current state
     * @param <T> type of the field elements
     * @return true if maneuver is on at this state
     * @since 10.1
     */
    public <T extends RealFieldElement<T>> boolean isFiring(final FieldSpacecraftState<T> s) {
        return isFiring(s.getDate().toAbsoluteDate());
    }

    /** Check if maneuvering is on.
     * @param date current date
     * @return true if maneuver is on at this date
     * @since 10.1
     */
    public boolean isFiring(final AbsoluteDate date) {
        return getManeuverTriggers().isFiring(date, new double[] {});
    }
}
