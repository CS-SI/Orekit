package fr.cs.orekit.propagation.numerical.forces.maneuvers;

import org.apache.commons.math.geometry.Vector3D;
import fr.cs.orekit.errors.OrekitException;
import fr.cs.orekit.propagation.SpacecraftState;
import fr.cs.orekit.propagation.numerical.OrekitSwitchingFunction;
import fr.cs.orekit.propagation.numerical.TimeDerivativesEquations;
import fr.cs.orekit.propagation.numerical.forces.ForceModel;
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

    /** Identifier for spacecraft frame. */
    public static final int SPACECRAFT = 3;

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
    private final Vector3D direction;

    /** Simple constructor for a constant direction and constant thrust.
     * @param date maneuver date
     * @param duration the duration of the thrust (s) (if negative,
     * the date is considered to be the stop date)
     * @param thrust the thrust force (N)
     * @param isp the Isp (s)
     * @param direction the acceleration direction in chosen frame.
     * @param frameType the frame in which the direction is defined
     * @exception IllegalArgumentException if frame type is not one of
     * {@link #TNW}, {@link #QSW}, {@link #INERTIAL} or {@link #SPACECRAFT}
     * @see #QSW
     * @see #TNW
     * @see #INERTIAL
     */
    public ConstantThrustManeuver(AbsoluteDate date, double duration,
                                  double thrust, double isp, Vector3D direction,
                                  int frameType)
        throws IllegalArgumentException {

        if ((frameType != QSW) && (frameType != TNW) &&
            (frameType != INERTIAL) && (frameType != SPACECRAFT)) {
            OrekitException.throwIllegalArgumentException("unsupported thrust direction frame, " +
                                                          "supported types: {0}, {1}, {2} and {3}",
                                                          new Object[] {
                                                              "QSW", "TNW", "INERTIAL", "SPACECRAFT"
                                                          });
        }

        if (duration >= 0) {
            this.startDate = date;
            this.endDate   = new AbsoluteDate(date, duration);
            this.duration  = duration;
        } else {
            this.endDate   = date;
            this.startDate = new AbsoluteDate(endDate, duration);
            this.duration  = -duration;
        }

        this.thrust     = thrust;
        this.flowRate  = -thrust / (g0 * isp);
        this.direction = direction.normalize();
        this.frameType = frameType;
        firing = false;

    }

    /** Compute the contribution of maneuver to the global acceleration.
     * @param s the current state information : date, cinematics, attitude
     * @param adder object where the contribution should be added
     * @param mu central gravitation coefficient
     * @exception OrekitException if some specific error occurs
     */
    public void addContribution(SpacecraftState s, TimeDerivativesEquations adder, double mu)
        throws OrekitException {

        if (firing) {

            // add thrust acceleration
            final double acceleration = thrust / s.getMass();
            switch (frameType) {
            case QSW :
                adder.addQSWAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            case TNW :
                adder.addTNWAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            case INERTIAL :
                adder.addXYZAcceleration(acceleration * direction.getX(),
                                         acceleration * direction.getY(),
                                         acceleration * direction.getZ());
                break;
            default :
                // the thrust is in spacecraft frame, it depends on attitude
                Vector3D inertialThrust = s.getAttitude().applyTo(direction);
                adder.addXYZAcceleration(acceleration * inertialThrust.getX(),
                                         acceleration * inertialThrust.getY(),
                                         acceleration * inertialThrust.getZ());
            }

            // add flow rate
            adder.addMassDerivative(flowRate);

        }

    }

    /** Gets the swithching functions related to start and stop passes.
     * @return start / stop switching functions
     */
    public OrekitSwitchingFunction[] getSwitchingFunctions() {
        return new OrekitSwitchingFunction[] { new StartSwitch(), new EndSwitch() };
    }

    /** This class defines the beginning of the acceleration switching function.
     * It triggers at the ignition.
     */
    private class StartSwitch implements OrekitSwitchingFunction {

        /** Serializable UID. */
        private static final long serialVersionUID = -3763244241136150814L;

        public void eventOccurred(SpacecraftState s, double mu) {
            // start the maneuver
            firing = true;
        }

        /** The G-function is the difference between the start date and the current date.
         * @param s the current state information : date, kinematics, attitude
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
            // stop the maneuver
            firing = false;
        }

        /** The G-function is the difference between the end date and the current date.
         * @param s the current state information : date, kinematics, attitude
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
