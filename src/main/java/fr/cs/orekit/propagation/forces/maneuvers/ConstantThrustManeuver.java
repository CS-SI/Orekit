package fr.cs.orekit.propagation.forces.maneuvers;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.forces.ForceModel;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.TimeDerivativesEquations;
import fr.cs.orekit.time.AbsoluteDate;


/** This class implements a simple maneuver with constant thrust.
 *
 * @author F. Maussion
 */
public class ConstantThrustManeuver implements ForceModel {

    /** Identifier for QSW frame. */
    public static final int QSW = 0;

    /** Identifier for TNW frame. */
    public static final int TNW = 1;

    /** Identifier for inertial frame. */
    public static final int INERTIAL = 2;

    /** Reference gravity acceleration constant (m/s<sup>2</sup>) */
    private static final double g0 = 9.80665;

    /** State of the engine. */
    private boolean firing;

    /** Frame type. */
    private final int frameType;

    /** Start of the maneuver. */
    private final AbsoluteDate startDate;

    /** End of the maneuver. */
    private final AbsoluteDate endDate;

    /** Duration (s). */
    private final double duration;

    /** Engine thrust. */
    private final double thrust;

    /** Engine flow-rate. */
    private final double flowRate;

    /** Direction of the acceleration in selected frame. */
    private Vector3D direction;

    /** Thrust direction provider. */
    private ThrustForceDirection variableDir;

    /** Simple constructor for a constant direction and constant thrust.
     * @param startDate the instant of ignition
     * @param duration the duration of the thrust (s)
     * @param thrust the thrust force (N)
     * @param isp the Isp (s)
     * @param direction the acceleration direction in choosed frame.
     * @param frameType the frame in which is defined the direction
     * @exception IllegalArgumentException if frame type is not one of
     * {@link #TNW}, {@link #QSW} or  {@link #INERTIAL}
     * @see #QSW
     * @see #TNW
     * @see #INERTIAL
     */
    public ConstantThrustManeuver(AbsoluteDate startDate, double duration,
                                  double thrust, double isp, Vector3D direction,
                                  int frameType)
        throws IllegalArgumentException {

        if ((frameType != QSW) && (frameType != TNW) && (frameType != INERTIAL)) {
            OrekitException.throwIllegalArgumentException("unsupported thrust direction frame, " +
                                                          "supported types: {0}, {1} and {2}",
                                                          new Object[] {
                                                              "QSW", "TNW", "INERTIAL"
                                                          });
        }

        if (duration >= 0) {
            this.startDate = startDate;
            this.endDate   = new AbsoluteDate(startDate , duration);
            this.duration  = duration;
        } else {
            this.endDate   = startDate;
            this.startDate = new AbsoluteDate(startDate , duration);
            this.duration  = -duration;
        }

        this.thrust     = thrust;
        this.flowRate  = -thrust / (g0*isp);
        this.direction = direction.normalize();
        this.frameType = frameType;
        firing = false;

    }

    /** Constructor for a variable direction and constant thrust.
     * @param startDate the instant of ignition
     * @param duration the duration of the thrust (s)
     * @param thrust the thrust force (N)
     * @param isp the specific impulse (s)
     * @param direction the variable acceleration direction.
     */
    public ConstantThrustManeuver(AbsoluteDate startDate, double duration,
                                  double thrust, double isp, ThrustForceDirection direction) {

        this(startDate, duration, thrust, isp, null, direction.getType());
        this.variableDir = direction;
    }

    /** Compute the contribution of maneuver to the global acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @param mu central gravitation coefficient
     * @throws OrekitException if some specific error occurs
     */
    public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
        throws OrekitException {

        if (firing) {
            if (variableDir != null) {
                direction = variableDir.getDirection(s).normalize();
            }

            final double acc = thrust/s.getMass();
            final Vector3D acceleration = new Vector3D(acc, direction);

            if (frameType == QSW) {
                adder.addQSWAcceleration(acceleration.getX(),
                                         acceleration.getY(), acceleration.getZ());
            } else if (frameType == TNW) {
                adder.addTNWAcceleration(acceleration.getX(),
                                         acceleration.getY(), acceleration.getZ());
            } else {
                adder.addXYZAcceleration(acceleration.getX(),
                                         acceleration.getY(), acceleration.getZ());
            }

            adder.addMassDerivative(flowRate);
        }

    }

    /** Gets the swithching functions related to start and stop passes.
     * @return start / stop switching functions
     */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[] { new StartSwitch(), new EndSwitch() };
    }

    /** This class defines the begining of the acceleration switching function.
     * It triggers at the ignition.
     */
    private class StartSwitch implements OrekitSwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -3763244241136150814L;

        public void eventOccurred(SpacecraftState s, double mu) {
            firing = true;
        }

        /** The G-function is the difference between the start date and the current date.
         * @param s the current state information : date, cinematics, attitude
         * @param mu central gravitation coefficient
         */
        public double g(SpacecraftState s, double mu) throws OrekitException {
            return startDate.minus(s.getDate());
        }

        public double getMaxCheckInterval() {
            return duration;
        }

        public double getThreshold() {
            // convergence threshold in seconds
            return 1.0e-4;
        }

        public int getMaxIterationCount() {
            return 10;
        }

    }

    /** This class defines the end of the acceleration switching function.
     * It triggers at the end of the maneuver.
     */
    private class EndSwitch implements OrekitSwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -4081671157610680754L;

        public void eventOccurred(SpacecraftState s, double mu) {
            firing = false;
        }

        /** The G-function is the difference between the end date and the currentdate.
         * @param s the current state information : date, cinematics, attitude
         * @param mu central gravitation coefficient
         */
        public double g(SpacecraftState s, double mu) throws OrekitException {
            return endDate.minus(s.getDate());
        }

        public double getMaxCheckInterval() {
            return duration;
        }

        public double getThreshold() {
            // convergence threshold in seconds
            return 1.0e-4;
        }

        public int getMaxIterationCount() {
            return 10;
        }

    }

}
