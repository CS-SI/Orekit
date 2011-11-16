package org.orekit.propagation.semianalytical.dsst.dsstforcemodel;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.PropagationException;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.events.AbstractDetector;
import org.orekit.propagation.sampling.OrekitFixedStepHandler;
import org.orekit.time.AbsoluteDate;

/**
 * Orbit factory
 * 
 * @author Romain Di Costanzo
 */
public class OrbitFactory {

    /**
     * Get a heliosynchonous orbit
     * 
     * @param ae
     *            equatorial radius
     * @param alt
     *            altitude (in meters above the geoid)
     * @param eccentricity
     *            eccentricity
     * @param pa
     *            perigee argument
     * @param raan
     *            right ascention of the ascending node
     * @param meanAnomaly
     *            mean anomaly
     * @param mu
     *            &mu;
     * @param frame
     *            inertial frame
     * @param date
     *            bulletin date
     * @return a heliosynchonous orbit
     */
    public static Orbit getHeliosynchronousOrbit(final double ae,
                                                 final double alt,
                                                 final double eccentricity,
                                                 final double pa,
                                                 final double raan,
                                                 final double meanAnomaly,
                                                 final double mu,
                                                 final Frame frame,
                                                 final AbsoluteDate date) {
        // Get inclination :
        final double a = ae + alt;
        final double period = 2.0 * FastMath.PI * a * FastMath.sqrt(a / mu);
        final double rotationPerDay = 86400d / period;
        // Daily precession in degrees for heliosynchonism
        final double daj = 365 / 365.25;
        // Daily precession for the current satellite
        final double da = daj / rotationPerDay;

        final double coeff = -0.58 * Math.pow(ae / a, 2);
        final double i = Math.acos(da / coeff);
        final double h = eccentricity * Math.sin(pa + raan);
        final double k = eccentricity * Math.cos(pa + raan);
        final double p = Math.tan(i / 2) * Math.sin(raan);
        final double q = Math.tan(i / 2) * Math.cos(raan);
        final double mean = meanAnomaly + pa + raan;
        return new EquinoctialOrbit(a, k, h, q, p, mean, PositionAngle.MEAN, frame, date, mu);

    }

    /**
     * Get a geostationnary orbit <br>
     * 
     * @param mu
     * @param frame
     *            inertial frame
     * @param date
     *            bulletin date
     * @return a geostationnary orbit
     */
    public static Orbit getGeostationnaryOrbit(final double mu,
                                               final Frame frame,
                                               AbsoluteDate date) {
        /** geostationnary orbit */
        double a = 42166712;
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));
        return new EquinoctialOrbit(a, 1e-4, 2e-4, hx, hy, 0, PositionAngle.MEAN, frame, date, mu);
    }

    /**
     * Create a mean orbit from osculating parameters. Cannot be applied to equatorial orbits. This
     * method averages osculating parameters over a specific number of revolution and creates a mean
     * orbit at mean date. To achieve that, a node computation is done. Averaraging process is done
     * between node 1 and node 'n', where n is the input parameter 'nodeNumberValue'. The returned
     * orbit is build at mean date, i.e, date at witch the satellite is at nodeNumberValue / 2.
     * 
     * @param numericalPropagator
     *            numerical propagator used for extrapolation
     * @param deltaT
     *            search period for node finding
     * @param nodeNumberValue
     *            number of node wanted to average the orbit
     * @param averageStep
     *            step used for averaging the orbit (in seconds).
     * @return mean and osculating orbit at mid-date
     * @throws Exception
     */
    public static SpacecraftState[] getMeanOrbitFromOsculating(final Propagator numericalPropagator,
                                                               final double deltaT,
                                                               final int nodeNumberValue,
                                                               final int averageStep) throws Exception {
        if (nodeNumberValue % 2 != 0) {
            throw new Exception("need even value for averaging value");
        }
        SpacecraftState orbit = numericalPropagator.getInitialState();
        NodeDetectorForMeanOrbit nodeDetector = new NodeDetectorForMeanOrbit(deltaT, orbit.getOrbit(), FramesFactory.getITRF2005(), nodeNumberValue);
        numericalPropagator.addEventDetector(nodeDetector);

        // Propagate to find every wanted nodes :
        numericalPropagator.propagate(orbit.getDate().shiftedBy(deltaT));
        // Get list of states at nodes :
        List<SpacecraftState> listStates = nodeDetector.getSpacecraftStateAtNodes();

        // Get first state at node 0 :
        SpacecraftState firstState = listStates.get(0);
        // Reset the propagator state and operate the averaging operator between the first and the
        // last state (last node)
        numericalPropagator.resetInitialState(firstState);
        // Averaging operator :
        AveragingOperator average = new AveragingOperator(firstState.getOrbit());
        numericalPropagator.setMasterMode(averageStep, average);
        SpacecraftState lastState = listStates.get(nodeNumberValue - 1);
        numericalPropagator.propagate(lastState.getDate());
        // Get shift from osculating and mean elements in terms of a, ex, ey, hx, hy
        double[] deltaElements = average.getDeltaFromMeanElements();

        // Build the mean orbit at median node = nodeNumberValue / 2 :
        SpacecraftState middleState = listStates.get(nodeNumberValue / 2 - 1);
        // Reset the propagator for new extrapolation at middle state :
        numericalPropagator.resetInitialState(firstState);
        // Propagate at middle state :
        SpacecraftState stateMedOsc = numericalPropagator.propagate(middleState.getDate());

        double a = stateMedOsc.getOrbit().getA() - (deltaElements[0]);
        double ex = stateMedOsc.getOrbit().getEquinoctialEx() - (deltaElements[1]);
        double ey = stateMedOsc.getOrbit().getEquinoctialEy() - (deltaElements[2]);
        double hx = stateMedOsc.getOrbit().getHx() - (deltaElements[3]);
        double hy = stateMedOsc.getOrbit().getHy() - (deltaElements[4]);
        EquinoctialOrbit meanOrbit = new EquinoctialOrbit(a, ex, ey, hx, hy, stateMedOsc.getLM(), PositionAngle.MEAN, stateMedOsc.getFrame(), stateMedOsc.getDate(), stateMedOsc.getMu());
        return new SpacecraftState[] { new SpacecraftState(meanOrbit), stateMedOsc };

    }

    /**
     * Node detector. Detect as many node as wanted.
     */
    private static class NodeDetectorForMeanOrbit extends AbstractDetector {

        /**
         * Default UID
         */
        private static final long serialVersionUID     = 1L;

        /** Number of detected node */
        private int               numberOfDetectedNode = 0;

        /** Target */
        private final int         nodeWanded;

        /** state list */
        List<SpacecraftState>     listeState           = new ArrayList<SpacecraftState>();

        /** Frame in which the equator is defined. */
        private final Frame       frame;

        /**
         * Constructor deltaT search period for node finding
         * 
         * @param nodeNumberValue
         *            number of node wanted to average the orbit
         * @param orbit
         *            orbit
         * @param frame
         *            Frame in which the equator is defined
         * @param nodeWanted
         *            stop at node number...
         */
        public NodeDetectorForMeanOrbit(final double deltaT,
                                        final Orbit orbit,
                                        final Frame frame,
                                        final int nodeWanted) {
            super(deltaT, 1.0e-13 * orbit.getKeplerianPeriod());
            this.frame = frame;
            this.nodeWanded = nodeWanted;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Action eventOccurred(SpacecraftState s,
                                    boolean increasing) throws OrekitException {
            Action action = Action.CONTINUE;
            // Print ascending nodes :
            if (increasing) {
                numberOfDetectedNode++;
                listeState.add(s);
                if (numberOfDetectedNode == nodeWanded) {
                    action = Action.STOP;
                }
            }
            return action;

        }

        /** Get the satellite state at each node */
        public List<SpacecraftState> getSpacecraftStateAtNodes() {
            return this.listeState;
        }

        /** Node computation */
        @Override
        public double g(SpacecraftState s) throws OrekitException {
            return s.getPVCoordinates(frame).getPosition().getZ();
        }
    }

    /** Create the mean orbit from the osculating parameters */
    private static class AveragingOperator implements OrekitFixedStepHandler {

        /**
         * Generated UID
         */
        private static final long serialVersionUID = 602440246251820479L;

        /** Initial orbit */
        private Orbit             initialOrbit;

        /** Current index */
        private int               index            = 0;

        // Current satellite state
        private double            a;
        private double            ex;
        private double            ey;
        private double            hx;
        private double            hy;

        // contains aMean, exMean, eyMean, hxMean, hyMean
        private double[]          meanElements;

        /**
         * Default constructor
         * 
         * @param initialOrbit
         *            initial orbit
         */
        private AveragingOperator(final Orbit initialOrbit) {
            // Store initial orbit
            this.initialOrbit = initialOrbit;
            // Store initial parameters
            this.a = initialOrbit.getA();
            this.ex = initialOrbit.getEquinoctialEx();
            this.ey = initialOrbit.getEquinoctialEy();
            this.hx = initialOrbit.getHx();
            this.hy = initialOrbit.getHy();
        }

        /**
         * Step handler : at each step, sum the different contributions. If the last step is
         * reached, mean parameters value are computed
         */
        public void handleStep(SpacecraftState currentState,
                               boolean isLast) throws PropagationException {
            this.a += currentState.getA();
            this.ex += currentState.getEquinoctialEx();
            this.ey += currentState.getEquinoctialEy();
            this.hx += currentState.getHx();
            this.hy += currentState.getHy();
            index++;
            if (isLast) {
                this.a /= index + 1;
                this.ex /= index + 1;
                this.ey /= index + 1;
                this.hx /= index + 1;
                this.hy /= index + 1;

                double deltaA = initialOrbit.getA() - this.a;
                double deltaEx = initialOrbit.getEquinoctialEx() - this.ex;
                double deltaEy = initialOrbit.getEquinoctialEy() - this.ey;
                double deltaHx = initialOrbit.getHx() - this.hx;
                double deltaHy = initialOrbit.getHy() - this.hy;

                meanElements = new double[] { deltaA, deltaEx, deltaEy, deltaHx, deltaHy };
            }

        }

        /** Get shift from mean elements, i.e (a<sub>i</sub> - a<sub>mean</sub>) */
        public double[] getDeltaFromMeanElements() {
            return meanElements;
        }
    }

}
