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
import org.orekit.utils.Constants;

public class OrbitFactory {

    private final static double mu = Constants.WGS84_EARTH_MU;

    /**
     * @param alt
     *            satellite altitude
     * @return Heliosynchronous orbit
     */
    public static Orbit getHeliosynchronousOrbit(final double ae,
                                                 final double alt,
                                                 final double eccentricity,
                                                 final double pa,
                                                 final double raan,
                                                 final double meanAnomaly,
                                                 final double mu,
                                                 final Frame frame,
                                                 AbsoluteDate date) {
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

    public static Orbit getGeostationnaryOrbit() {
        /** geostationnary orbit */
        double a = 42166712;
        double ix = 1.200e-04;
        double iy = -1.16e-04;
        double inc = 2 * FastMath.asin(FastMath.sqrt((ix * ix + iy * iy) / 4.));
        double hx = FastMath.tan(inc / 2.) * ix / (2 * FastMath.sin(inc / 2.));
        double hy = FastMath.tan(inc / 2.) * iy / (2 * FastMath.sin(inc / 2.));
        return new EquinoctialOrbit(a, 1e-4, 2e-4, hx, hy, 0, PositionAngle.MEAN, FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH, mu);
    }

    /**
     * @throws Exception 
     *
     */
    public static SpacecraftState[] getMeanOrbitFromOsculating(final Propagator propagator,
                                                               final Orbit orbit,
                                                               final double deltaT,
                                                               final int nodeNumberValue) throws Exception {
        if (nodeNumberValue % 2 != 0) {
            throw new Exception("need even value for averaging value");
        }
        NodeDetector2 nodeDetector = new NodeDetector2(deltaT, orbit, FramesFactory.getITRF2005(), nodeNumberValue);
        propagator.addEventDetector(nodeDetector);
        propagator.resetInitialState(new SpacecraftState(new EquinoctialOrbit(orbit.getPVCoordinates(), orbit.getFrame(), orbit.getDate(), orbit.getMu())));
        // Propagate to find every wanted nodes :
        propagator.propagate(orbit.getDate().shiftedBy(deltaT));
        // Get list of states at nodes :
        List<SpacecraftState> listStates = nodeDetector.getSpacecraftStateAtNodes();

        // Get first state at node 0 :
        SpacecraftState firstState = listStates.get(0);
        // Reset the propagator state and operate the averaging operator between the first and the
        // last state (last node)
        propagator.resetInitialState(firstState);
        // Averaging operator :
        AveragingOperator average = new AveragingOperator(firstState.getOrbit());
        propagator.setMasterMode(10., average);
        SpacecraftState lastState = listStates.get(nodeNumberValue - 1);
        propagator.propagate(lastState.getDate());
        
        // Build the mean orbit at median node = nodeNumberValue / 2 :
        SpacecraftState middleState = listStates.get(nodeNumberValue / 2 - 1);
        // Reset the propagator for new extrapolation at middle state :
        propagator.resetInitialState(firstState);
        // Propagate at middle state :
        SpacecraftState stateMedOsc = propagator.propagate(middleState.getDate());


        double a = stateMedOsc.getOrbit().getA() - (stateMedOsc.getA());
        double ex = stateMedOsc.getOrbit().getEquinoctialEx() - (-2.219374006143401E-5);
        double ey = stateMedOsc.getOrbit().getEquinoctialEy() - (4.4679909371764226E-4);
        double hx = stateMedOsc.getOrbit().getHx() - 0.00937158170758028;
        double hy = stateMedOsc.getOrbit().getHy() - (-0.0015407398054450017);
        EquinoctialOrbit meanOrbit = new EquinoctialOrbit(a, ex, ey, hx, hy, stateMedOsc.getLM(), PositionAngle.MEAN, stateMedOsc.getFrame(), stateMedOsc.getDate(), stateMedOsc.getMu());
        System.out.println(meanOrbit);
        return new SpacecraftState[] { new SpacecraftState(meanOrbit), stateMedOsc };

    }

    private static class NodeDetector2 extends AbstractDetector {

        /**
         * 
         */
        private static final long serialVersionUID     = 1L;

        private int               numberOfDetectedNode = 0;

        private final int         nodeWanded;

        List<SpacecraftState>     listeState           = new ArrayList<SpacecraftState>();

        /** Frame in which the equator is defined. */
        private final Frame       frame;

        public NodeDetector2(final double deltaT,
                             final Orbit orbit,
                             final Frame frame,
                             final int nodeWanted) {
            super(deltaT, 1.0e-13 * orbit.getKeplerianPeriod());
            this.frame = frame;
            this.nodeWanded = nodeWanted;
        }

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

        public List<SpacecraftState> getSpacecraftStateAtNodes() {
            return this.listeState;
        }

        @Override
        public double g(SpacecraftState s) throws OrekitException {
            return s.getPVCoordinates(frame).getPosition().getZ();
        }
    }

    private static class AveragingOperator implements OrekitFixedStepHandler {

        /**
         * Generated UID
         */
        private static final long serialVersionUID = 602440246251820479L;
        private Orbit             initialOrbit;
        private int               index            = 0;

        private double            a;
        private double            ex;
        private double            ey;
        private double            hx;
        private double            hy;

        // contains aMean, exMean, eyMean, hxMean, hyMean
        private double[]          meanElements;

        private AveragingOperator(final Orbit initialOrbit) {
            this.initialOrbit = initialOrbit;
            this.a = initialOrbit.getA();
            this.ex = initialOrbit.getEquinoctialEx();
            this.ey = initialOrbit.getEquinoctialEy();
            this.hx = initialOrbit.getHx();
            this.hy = initialOrbit.getHy();
        }

        @Override
        public void handleStep(SpacecraftState currentState,
                               boolean isLast) throws PropagationException {
            this.a += currentState.getA();
            this.ex += currentState.getEquinoctialEx();
            this.ey += currentState.getEquinoctialEy();
            this.hx += currentState.getHx();
            this.hy += currentState.getHy();
            index++;
            if (isLast) {
                this.a /= index;
                this.ex /= index;
                this.ey /= index;
                this.hx /= index;
                this.hy /= index;

                double deltaA = initialOrbit.getA() - this.a;
                double deltaEx = initialOrbit.getEquinoctialEx() - this.ex;
                double deltaEy = initialOrbit.getEquinoctialEy() - this.ey;
                double deltaHx = initialOrbit.getHx() - this.hx;
                double deltaHy = initialOrbit.getHy() - this.hy;

                meanElements = new double[] { deltaA, deltaEx, deltaEy, deltaHx, deltaHy };
            }

        }

        public double[] getDeltaFromMeanElements() {
            return meanElements;
        }
    }

}
