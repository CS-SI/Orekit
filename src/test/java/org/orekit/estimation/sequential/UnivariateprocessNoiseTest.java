package org.orekit.estimation.sequential;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;

import org.hipparchus.analysis.UnivariateFunction;
import org.hipparchus.analysis.polynomials.PolynomialFunction;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.hipparchus.random.GaussianRandomGenerator;
import org.hipparchus.random.Well1024a;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.Ignore;
import org.junit.Test;
import org.orekit.errors.OrekitException;
import org.orekit.estimation.Context;
import org.orekit.estimation.EstimationTestUtils;
import org.orekit.frames.LOFType;
import org.orekit.frames.Transform;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.Propagator;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.TimeStampedPVCoordinates;

public class UnivariateprocessNoiseTest {
    
    /** Test different functions and check the conversion from LOF to ECI (and back) of the covariances. */
    @Test
    public void testUnivariateProcessNoise() throws OrekitException {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE; // Not used here
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP);

        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        
        // Define the univariate functions for the standard deviations      
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10, 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};
        
        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(7);
        
        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Test on initial value, after 1 min and after a
        final SpacecraftState state0 = propagator.getInitialState();
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate().shiftedBy(60.));
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()/4.));
        
        // Number of samples for the statistics
        final int sampleNumber = 10000;
        
        // Relative tolerance on final standard deviations observed (< 2% here)
        final double relativeTolerance = 1.28e-2;;
        
        checkCovarianceValue(state0, state0, processNoise, sampleNumber, relativeTolerance);
        checkCovarianceValue(state0, state1, processNoise, sampleNumber, relativeTolerance);
        checkCovarianceValue(state0, state2, processNoise, sampleNumber, relativeTolerance);
    }
    
    
    
    private static void checkCovarianceValue(final SpacecraftState previous,
                                     final SpacecraftState current,
                                     final UnivariateProcessNoise univariateProcessNoise,
                                     final int sampleNumber,
                                     final double relativeTolerance) {
        
        // Get the process noise matrix
        final RealMatrix processNoiseMatrix = univariateProcessNoise.getProcessNoiseMatrix(previous, current);
        
        // Initialize a random vector generator
        final CorrelatedRandomVectorGenerator randomVectorGenerator = createSampler(processNoiseMatrix);
        
        // Propagation parameters length
        int propagationParametersLength = processNoiseMatrix.getColumnDimension() - 6;
        if ( propagationParametersLength < 0) {
            propagationParametersLength = 0; 
        }
        
        // Prepare the statistics
        final StreamingStatistics[] orbitalStatistics = new StreamingStatistics[6];
        for (int i = 0; i < 6; i++) {
            orbitalStatistics[i] = new StreamingStatistics();
        }
        StreamingStatistics[] propagationStatistics;
        if (propagationParametersLength > 0) {
            propagationStatistics = new StreamingStatistics[propagationParametersLength];
            for (int i = 0; i < propagationParametersLength; i++) {
                propagationStatistics[i] = new StreamingStatistics();
            }
        } else {
          propagationStatistics = null;  
        }
        
        // Current orbit stored in an array
        // With the position angle defined in the univariate process noise function
        final double[] currentOrbitArray = new double[6];
        current.getOrbit().getType().mapOrbitToArray(current.getOrbit(),
                                                     univariateProcessNoise.getPositionAngle(),
                                                     currentOrbitArray,
                                                     null);
        // Transform from inertial to current spacecraft LOF frame
        final Transform inertialToLof = univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                                                  current.getOrbit().getPVCoordinates());
        // Create the vectors and compute the stats
        for (int i = 0; i < sampleNumber; i++) {
            
            // Create a random vector
            final double[] randomVector = randomVectorGenerator.nextVector();
            
            // Orbital parameters values
            // -------------------------
                        
            // Get the full inertial orbit by adding up the values of current orbit and orbit error (first 6 cop. of random vector)
            final double[] modifiedOrbitArray = new double[6];
            for (int j = 0; j < 6; j++) {
                modifiedOrbitArray[j] = currentOrbitArray[j] + randomVector[j];
            }
            
            // Get the corresponding PV coordinates
            final TimeStampedPVCoordinates inertialPV = 
                            current.getOrbit().getType().mapArrayToOrbit(modifiedOrbitArray,
                                                                         null,
                                                                         univariateProcessNoise.getPositionAngle(),
                                                                         current.getDate(),
                                                                         current.getMu(),
                                                                         current.getFrame())
                            .getPVCoordinates();
            
            
            // Transform from inertial to current LOF
            // The value obtained is the Cartesian error vector in LOF (w/r to current spacecraft position)
            final PVCoordinates lofPV = inertialToLof.transformPVCoordinates(inertialPV);
            
            // Store the LOF PV values in the statistics summaries
            orbitalStatistics[0].addValue(lofPV.getPosition().getX());
            orbitalStatistics[1].addValue(lofPV.getPosition().getY());
            orbitalStatistics[2].addValue(lofPV.getPosition().getZ());
            orbitalStatistics[3].addValue(lofPV.getVelocity().getX());
            orbitalStatistics[4].addValue(lofPV.getVelocity().getY());
            orbitalStatistics[5].addValue(lofPV.getVelocity().getZ());


            // Propagation parameters values
            // -----------------------------
            
            // Extract the propagation parameters random vector and store them in the statistics
            if (propagationParametersLength > 0) {
                for (int j = 6; j < randomVector.length; j++) {
                    propagationStatistics[j - 6].addValue(randomVector[j]);
                }
            }                      
        }
        
        // Get the real values and the statistics
        // --------------------------------------
        
        // DT
        final double dt = current.getDate().durationFrom(previous.getDate());
        
        // Get the values of the orbital functions and the statistics
        final double[] orbitalValues = new double[6];
        final double[] orbitalStatisticsValues = new double[6];
        final double[] orbitalRelativeValues = new double[6];
        
        for (int i = 0; i < 6; i++) {
            orbitalValues[i] = FastMath.abs(univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
            orbitalStatisticsValues[i] = orbitalStatistics[i].getStandardDeviation();
            
            if (FastMath.abs(orbitalValues[i]) > 1e-15) {
                orbitalRelativeValues[i] = FastMath.abs(1. - orbitalStatisticsValues[i]/orbitalValues[i]);
            } else {
                orbitalRelativeValues[i] = orbitalStatisticsValues[i];
            }
        }
        
        // Get the values of the propagation functions and statistics
        final double[] propagationValues = new double[propagationParametersLength];
        final double[] propagationStatisticsValues = new double[propagationParametersLength];
        final double[] propagationRelativeValues = new double[propagationParametersLength];
        for (int i = 0; i < propagationParametersLength; i++) {
            propagationValues[i] = FastMath.abs(univariateProcessNoise.getPropagationParametersEvolution()[i].value(dt));
            propagationStatisticsValues[i] = propagationStatistics[i].getStandardDeviation();
            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                propagationRelativeValues[i] = FastMath.abs(1. - propagationStatisticsValues[i]/propagationValues[i]);
            } else {
                propagationRelativeValues[i] = propagationStatisticsValues[i];
            }
        }
        
        // Test the values
        assertArrayEquals(new double[6], 
                          orbitalRelativeValues,
                          relativeTolerance);
        assertArrayEquals(new double[propagationParametersLength],
                          propagationRelativeValues,
                          relativeTolerance);
    }
    
    /** Create a gaussian random vector generator based on an input covariance matrix.
     * @param covarianceMatrix input covariance matrix
     * @return correlated gaussian random vectors generator
     */
    private static CorrelatedRandomVectorGenerator createSampler(final RealMatrix covarianceMatrix) {
        double small = 10e-20 * covarianceMatrix.getNorm();
        return new CorrelatedRandomVectorGenerator(
                covarianceMatrix,
                small,
                new GaussianRandomGenerator(new Well1024a(0x366a26b94e520f41l)));
    }
    
    /** Bug on Vy when dt gets large (here 1/2 Torb is enough to make it crash).
     *  Other components are ok.
     */
    @Ignore
    @Test
    public void testBugVy() throws OrekitException {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.CARTESIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE; // Not used here
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP);

        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        
        // Define the univariate functions for the standard deviations      
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10, 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};
        
        // Create a dummy initial covariance matrix
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(7);
        
        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Test on initial value, after 1 min and after a
        final SpacecraftState state0 = propagator.getInitialState();
        
        // dt = 1/2 Torb
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()/2.));
        // dt = 1 Torb
        final SpacecraftState state2 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()));
        
        // dt = 2 Torb
        final SpacecraftState state3 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(2. * context.initialOrbit.getKeplerianPeriod()));
        
        // Number of samples for the statistics
        final int sampleNumber = 10000;
        
        // FIXME: Relative tolerance = 87% so that the first two tests pass 
        final double relativeTolerance = 87e-2;;
        
        // dt = 1/2 Torb, error on Vy is 2.63%
        checkBugCovarianceValue(false, state0, state1, processNoise, sampleNumber, relativeTolerance, false);
        // dt = 1 Torb, error on Vy is 26%
        checkBugCovarianceValue(false, state0, state2, processNoise, sampleNumber, relativeTolerance, false);
        // dt = 2Torb, with a wrong velocity composition, it works well, error on Vy is about 0.26%
        checkBugCovarianceValue(false, state0, state3, processNoise, sampleNumber, relativeTolerance, true);
        // dt = 2Torb, error on Vy is 87%
        checkBugCovarianceValue(false, state0, state3, processNoise, sampleNumber, relativeTolerance, false);
        
    }
    
    /** Bugs with Keplerian formalism, all components are out. */
    @Ignore
    @Test
    public void testBugKeplerian() throws OrekitException {

        // Create context
        Context context = EstimationTestUtils.eccentricContext("regular-data:potential:tides");

        // Create initial orbit and propagator builder
        final OrbitType     orbitType     = OrbitType.KEPLERIAN;
        final PositionAngle positionAngle = PositionAngle.TRUE;
        final boolean       perfectStart  = true;
        final double        minStep       = 1.e-6;
        final double        maxStep       = 60.;
        final double        dP            = 1.;
        final NumericalPropagatorBuilder propagatorBuilder = context.createBuilder(orbitType, positionAngle, perfectStart,
                                                                                   minStep, maxStep, dP);

        // Create a propagator
        final Propagator propagator = EstimationTestUtils.createPropagator(context.initialOrbit,
                                                                           propagatorBuilder);
        
        // Define the univariate functions for the standard deviations      
        final UnivariateFunction[] lofCartesianOrbitalParametersEvolution = new UnivariateFunction[6];
        // Evolution for position error
        lofCartesianOrbitalParametersEvolution[0] = new PolynomialFunction(new double[] {100., 0., 1e-4});
        lofCartesianOrbitalParametersEvolution[1] = new PolynomialFunction(new double[] {100., 1e-1, 0.});
        lofCartesianOrbitalParametersEvolution[2] = new PolynomialFunction(new double[] {100., 0., 0.});
        // Evolution for velocity error
        lofCartesianOrbitalParametersEvolution[3] = new PolynomialFunction(new double[] {1., 0., 1.e-6});
        lofCartesianOrbitalParametersEvolution[4] = new PolynomialFunction(new double[] {1., 1e-3, 0.});
        lofCartesianOrbitalParametersEvolution[5] = new PolynomialFunction(new double[] {1., 0., 0.});

        final UnivariateFunction[] propagationParametersEvolution =
                        new UnivariateFunction[] {new PolynomialFunction(new double[] {10, 1., 1e-4}),
                                                  new PolynomialFunction(new double[] {1000., 0., 0.})};
        
        // Create a dummy initial covariance
        final RealMatrix initialCovarianceMatrix = MatrixUtils.createRealIdentityMatrix(7);
        
        // Set the process noise object
        // Define input LOF and output position angle
        final LOFType lofType = LOFType.TNW;
        final UnivariateProcessNoise processNoise = new UnivariateProcessNoise(initialCovarianceMatrix,
                                                                               lofType,
                                                                               positionAngle,
                                                                               lofCartesianOrbitalParametersEvolution,
                                                                               propagationParametersEvolution);
        // Test on initial value, after 1 min and after 1 hour
        final SpacecraftState state0 = propagator.getInitialState();
        final SpacecraftState state1 = propagator.propagate(context.initialOrbit.getDate()
                                                            .shiftedBy(context.initialOrbit.getKeplerianPeriod()/4.));
        
//        //FIXME: debug
//        final Orbit initialOrbit = propagator.getInitialState().getOrbit(); 
//        final double posNorm = initialOrbit.getPVCoordinates().getPosition().getNorm();
//        final double velNorm = initialOrbit.getPVCoordinates().getVelocity().getNorm();
//        final Orbit orbit1 = new CartesianOrbit(new PVCoordinates(new Vector3D(posNorm, 0, 0),
//                                                                  new Vector3D(0, 0, velNorm)),
//                                                initialOrbit.getFrame(),
//                                                initialOrbit.getDate(), initialOrbit.getMu());
//        final SpacecraftState state1 = new SpacecraftState(orbitType.convertType(orbit1));
//        final SpacecraftState state0 = state1.shiftedBy(-initialOrbit.getKeplerianPeriod()/4.);
//        // debug
        
        // Number of samples for the statistics
        final int sampleNumber = 10000;
        
        // FIXME: Relative tolerance = 134% so that the first two tests pass 
        final double relativeTolerance = 134e-2;;
        
        checkBugCovarianceValue(false, state0, state0, processNoise, sampleNumber, relativeTolerance, false);
        checkBugCovarianceValue(false, state0, state1, processNoise, sampleNumber, relativeTolerance, false);
        checkBugCovarianceValue(false, state0, state1, processNoise, sampleNumber, relativeTolerance, true);
    }
    
    private static void checkBugCovarianceValue(final boolean print,
                                                final SpacecraftState previous,
                                                final SpacecraftState current,
                                                final UnivariateProcessNoise univariateProcessNoise,
                                                final int sampleNumber,
                                                final double relativeTolerance,
                                                final boolean wrongVelocityComposition) {

        // Get the process noise matrix
        final RealMatrix processNoiseMatrix = univariateProcessNoise.getProcessNoiseMatrix(previous, current);

        // Initialize a random vector generator
        final CorrelatedRandomVectorGenerator randomVectorGenerator = createSampler(processNoiseMatrix);

        // Propagation parameters length
        int propagationParametersLength = processNoiseMatrix.getColumnDimension() - 6;
        if ( propagationParametersLength < 0) {
            propagationParametersLength = 0; 
        }

        // Prepare the statistics
        final StreamingStatistics[] orbitalStatistics = new StreamingStatistics[6];
        for (int i = 0; i < 6; i++) {
            orbitalStatistics[i] = new StreamingStatistics();
        }
        StreamingStatistics[] propagationStatistics;
        if (propagationParametersLength > 0) {
            propagationStatistics = new StreamingStatistics[propagationParametersLength];
            for (int i = 0; i < propagationParametersLength; i++) {
                propagationStatistics[i] = new StreamingStatistics();
            }
        } else {
            propagationStatistics = null;  
        }

        // Current orbit stored in an array
        // With the position angle defined in the univariate process noise function
        final double[] currentOrbitArray = new double[6];
        current.getOrbit().getType().mapOrbitToArray(current.getOrbit(),
                                                     univariateProcessNoise.getPositionAngle(),
                                                     currentOrbitArray,
                                                     null);
        // Transform from inertial to current spacecraft LOF frame
        final Transform inertialToLof = univariateProcessNoise.getLofType().transformFromInertial(current.getDate(),
                                                                                                  current.getOrbit().getPVCoordinates());
        // Create the vectors and compute the stats
        for (int i = 0; i < sampleNumber; i++) {

            // Create a random vector
            final double[] randomVector = randomVectorGenerator.nextVector();

            // Orbital parameters values
            // -------------------------

            // Get the full inertial orbit by adding up the values of current orbit and orbit error (first 6 components of random vector)
            final double[] modifiedOrbitArray = new double[6];
            for (int j = 0; j < 6; j++) {
                modifiedOrbitArray[j] = currentOrbitArray[j] + randomVector[j];
            }

            // Get the corresponding PV coordinates
            final TimeStampedPVCoordinates inertialPV = 
                            current.getOrbit().getType().mapArrayToOrbit(modifiedOrbitArray,
                                                                         null,
                                                                         univariateProcessNoise.getPositionAngle(),
                                                                         current.getDate(),
                                                                         current.getMu(),
                                                                         current.getFrame())
                            .getPVCoordinates();


            // Transform from inertial to current LOF
            // The values obtained should be the Cartesian error vector in LOF (w/r to current spacecraft position)
            final PVCoordinates lofPV;
            if (!wrongVelocityComposition) {
                // Proper velocity composition
                lofPV = inertialToLof.transformPVCoordinates(inertialPV);
            } else {
                // Wrong velocity composition, ignoring orbital angular velocity
                final Vector3D pos = inertialToLof.transformPosition(inertialPV.getPosition());
                final Vector3D vel = inertialToLof.transformVector(inertialPV.getVelocity());
                lofPV = new PVCoordinates(pos, vel);
            }                    

            // Store the LOF PV values in the statistics summaries
            orbitalStatistics[0].addValue(lofPV.getPosition().getX());
            orbitalStatistics[1].addValue(lofPV.getPosition().getY());
            orbitalStatistics[2].addValue(lofPV.getPosition().getZ());
            orbitalStatistics[3].addValue(lofPV.getVelocity().getX());
            orbitalStatistics[4].addValue(lofPV.getVelocity().getY());
            orbitalStatistics[5].addValue(lofPV.getVelocity().getZ());

            // Propagation parameters
            // ----------------------

            // Extract the propagation parameters random vector and store them in the statistics
            if (propagationParametersLength > 0) {
                for (int j = 6; j < randomVector.length; j++) {
                    propagationStatistics[j - 6].addValue(randomVector[j]);
                }
            }                      
        }

        // Get the real values and the statistics
        // --------------------------------------

        // DT
        final double dt = current.getDate().durationFrom(previous.getDate());

        // Get the values of the orbital functions and the statistics
        final double[] orbitalValues = new double[6];
        final double[] orbitalStatisticsValues = new double[6];
        final double[] orbitalRelativeValues = new double[6];

        for (int i = 0; i < 6; i++) {
            orbitalValues[i] = FastMath.abs(univariateProcessNoise.getLofCartesianOrbitalParametersEvolution()[i].value(dt));
            orbitalStatisticsValues[i] = orbitalStatistics[i].getStandardDeviation();

            if (FastMath.abs(orbitalValues[i]) > 1e-15) {
                orbitalRelativeValues[i] = FastMath.abs(1. - orbitalStatisticsValues[i]/orbitalValues[i]);
            } else {
                orbitalRelativeValues[i] = orbitalStatisticsValues[i];
            }
        }

        // Get the values of the propagation functions and statistics
        final double[] propagationValues = new double[propagationParametersLength];
        final double[] propagationStatisticsValues = new double[propagationParametersLength];
        final double[] propagationRelativeValues = new double[propagationParametersLength];
        for (int i = 0; i < propagationParametersLength; i++) {
            propagationValues[i] = FastMath.abs(univariateProcessNoise.getPropagationParametersEvolution()[i].value(dt));
            propagationStatisticsValues[i] = propagationStatistics[i].getStandardDeviation();
            if (FastMath.abs(propagationValues[i]) > 1e-15) {
                propagationRelativeValues[i] = FastMath.abs(1. - propagationStatisticsValues[i]/propagationValues[i]);
            } else {
                propagationRelativeValues[i] = propagationStatisticsValues[i];
            }
        }

        if (print) {
            // FIXME: Test the values
            System.out.println("dt = " + dt + " / N = " + sampleNumber + "\n");
            if (wrongVelocityComposition) {
                System.out.println("Using wrong velocity computation here" + "\n");
            }

            System.out.println("σ orbit ref   = " + Arrays.toString(orbitalValues));
            System.out.println("σ orbit stat  = " + Arrays.toString(orbitalStatisticsValues));
            System.out.println("σ orbit %     = " + Arrays.toString(Arrays.stream(orbitalRelativeValues).map(i -> i*100.).toArray()) + "\n");

            System.out.println("σ propag ref   = " + Arrays.toString(propagationValues));
            System.out.println("σ propag stat  = " + Arrays.toString(propagationStatisticsValues));
            System.out.println("σ propag %     = " + Arrays.toString(Arrays.stream(propagationRelativeValues).map(i -> i*100.).toArray()) + "\n");
            //debug
        }

        assertArrayEquals(new double[6], 
                          orbitalRelativeValues,
                          relativeTolerance);
        assertArrayEquals(new double[propagationParametersLength],
                          propagationRelativeValues,
                          relativeTolerance);
    }
}
