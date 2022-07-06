package org.orekit.estimation.sequential;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.KeyValueFileParser;
import org.orekit.Utils;
import org.orekit.attitudes.AttitudeProvider;
import org.orekit.bodies.CelestialBody;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultKalman;
import org.orekit.forces.ForceModel;
import org.orekit.forces.drag.DragForce;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.empirical.AccelerationModel;
import org.orekit.forces.empirical.ParametricAcceleration;
import org.orekit.forces.empirical.PolynomialAccelerationModel;
import org.orekit.forces.gravity.HolmesFeatherstoneAttractionModel;
import org.orekit.forces.gravity.OceanTides;
import org.orekit.forces.gravity.Relativity;
import org.orekit.forces.gravity.SolidTides;
import org.orekit.forces.gravity.ThirdBodyAttraction;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.NormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.KnockeRediffusedForceModel;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.forces.radiation.SolarRadiationPressure;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.orbits.PositionAngle;
import org.orekit.propagation.conversion.NumericalPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.time.TimeScalesFactory;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

public class UnscentedKalmanOrbitDeterminationTest2 extends AbstractOrbitDetermination<NumericalPropagatorBuilder> {
    /** Gravity field. */
    private NormalizedSphericalHarmonicsProvider gravityField;
    
    /** Flag for unscented estimation. */
    private static final Boolean IS_UNSCENTED = true;
    
    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        gravityField = GravityFieldFactory.getNormalizedProvider(degree, order);

    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return gravityField.getMu();
    }

    /** {@inheritDoc} */
    @Override
    protected NumericalPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                                 final ODEIntegratorBuilder builder,
                                                                 final double positionScale) {
        return new NumericalPropagatorBuilder(referenceOrbit, builder, PositionAngle.MEAN,
                                              positionScale);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final NumericalPropagatorBuilder propagatorBuilder,
                                final double mass) {
        propagatorBuilder.setMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setGravity(final NumericalPropagatorBuilder propagatorBuilder,
                                               final OneAxisEllipsoid body) {
        final ForceModel gravityModel = new HolmesFeatherstoneAttractionModel(body.getBodyFrame(), gravityField);
        propagatorBuilder.addForceModel(gravityModel);
        return gravityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setOceanTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final int degree, final int order) {
        final ForceModel tidesModel = new OceanTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     degree, order, conventions,
                                                     TimeScalesFactory.getUT1(conventions, true));
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolidTides(final NumericalPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final CelestialBody[] solidTidesBodies) {
        final ForceModel tidesModel = new SolidTides(body.getBodyFrame(),
                                                     gravityField.getAe(), gravityField.getMu(),
                                                     gravityField.getTideSystem(), conventions,
                                                     TimeScalesFactory.getUT1(conventions, true),
                                                     solidTidesBodies);
        propagatorBuilder.addForceModel(tidesModel);
        return tidesModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setThirdBody(final NumericalPropagatorBuilder propagatorBuilder,
                                                 final CelestialBody thirdBody) {
        final ForceModel thirdBodyModel = new ThirdBodyAttraction(thirdBody);
        propagatorBuilder.addForceModel(thirdBodyModel);
        return thirdBodyModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setDrag(final NumericalPropagatorBuilder propagatorBuilder,
                                            final Atmosphere atmosphere, final DragSensitive spacecraft) {
        final ForceModel dragModel = new DragForce(atmosphere, spacecraft);
        propagatorBuilder.addForceModel(dragModel);
        return dragModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolarRadiationPressure(final NumericalPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                              final double equatorialRadius, final RadiationSensitive spacecraft) {
        final ForceModel srpModel = new SolarRadiationPressure(sun, equatorialRadius, spacecraft);
        propagatorBuilder.addForceModel(srpModel);
        return srpModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setAlbedoInfrared(final NumericalPropagatorBuilder propagatorBuilder,
                                                      final CelestialBody sun, final double equatorialRadius,
                                                      final double angularResolution,
                                                      final RadiationSensitive spacecraft) {
        final ForceModel albedoIR = new KnockeRediffusedForceModel(sun, spacecraft, equatorialRadius, angularResolution);
        propagatorBuilder.addForceModel(albedoIR);
        return albedoIR.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setRelativity(final NumericalPropagatorBuilder propagatorBuilder) {
        final ForceModel relativityModel = new Relativity(gravityField.getMu());
        propagatorBuilder.addForceModel(relativityModel);
        return relativityModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setPolynomialAcceleration(final NumericalPropagatorBuilder propagatorBuilder,
                                                              final String name, final Vector3D direction, final int degree) {
        final AccelerationModel accModel = new PolynomialAccelerationModel(name, null, degree);
        final ForceModel polynomialModel = new ParametricAcceleration(direction, true, accModel);
        propagatorBuilder.addForceModel(polynomialModel);
        return polynomialModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final NumericalPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = true;
        
        // input in resources directory
        final String inputPath = KalmanNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/unscented_kalman_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;
        
        // Initial orbital Cartesian covariance matrix
        // These covariances are derived from the deltas between initial and reference orbits
        // So in a way they are "perfect"...
        // Cartesian covariance matrix initialization
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e4, 4e3, 1, 5e-3, 6e-5, 1e-4
        });
        final double qPos;
        final double qVel;
        
        if (IS_UNSCENTED) {
            qPos = 3e-4;
            qVel = 3e-7;
        } else {
            qPos = 1e-4;
            qVel = 1e-10;
        }
        
        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
                qPos, qPos, qPos, qVel, qVel, qVel
            });
        
        // Initial measurement covariance matrix and process noise matrix
        final RealMatrix measurementP = MatrixUtils.createRealDiagonalMatrix(new double [] {
           1., 1., 1., 1. 
        });
        final RealMatrix measurementQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1e-6, 1e-6, 1e-6, 1e-6
         });

        // Kalman orbit determination run.
        ResultKalman kalmanLageos2 = runKalman(input, orbitType, print,
                                               cartesianOrbitalP, cartesianOrbitalQ,
                                               null, null,
                                               measurementP, measurementQ, IS_UNSCENTED);

        // Definition of the reference parameters for the tests
        
        final double distanceAccuracy = 1.68;
        final double velocityAccuracy = 2.71e-3;
        final double[] RefStatRange = { -1.698412, 1.529126, 0.964164, 0.338275 };

        // Tests
        // Note: The reference initial orbit is the same as in the batch LS tests
        // -----
        
        // Number of measurements processed
        final int numberOfMeas  = 258;
        Assert.assertEquals(numberOfMeas, kalmanLageos2.getNumberOfMeasurements());

        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanLageos2.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanLageos2.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-5532131.956902, 10025696.592156, -3578940.040009);
        final Vector3D refVel0 = new Vector3D(-3871.275109, -607.880985, 4280.972530);
        
        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, null,
                                            kalmanLageos2.getEstimatedPV().getDate());
        final Vector3D refPos = refOrbit.getPVCoordinates().getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();
        
        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        Assert.assertEquals(0.0, dP, distanceAccuracy);
        Assert.assertEquals(0.0, dV, velocityAccuracy);
        
        // Print orbit deltas
        if (print) {
            System.out.println("Test performances:");
            System.out.format("\t%-30s\n",
                            "ΔEstimated / Reference");
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔP [m]", dP);
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔV [m/s]", dV);
        }


        //test on statistic for the range residuals
        final long nbRange = 258;
        // Batch LS values
        //final double[] RefStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        
        Assert.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], kalmanLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], kalmanLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], kalmanLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

    @Test
    // Orbit determination for range, azimuth elevation measurements
    public void testW3B() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = true;
        
        // input in resources directory
        final String inputPath = KalmanNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/W3B/od_test_W3B_unscented.in").toURI().getPath();
        final File input  = new File(inputPath);

        // Configure Orekit data access
        Utils.setDataRoot("orbit-determination/W3B:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        // Choice of an orbit type to use
        // Default for test is Cartesian
        final OrbitType orbitType = OrbitType.CARTESIAN;
        
        // Initial orbital Cartesian covariance matrix
        // These covariances are derived from the deltas between initial and reference orbits
        // So in a way they are "perfect"...
        // Cartesian covariance matrix initialization
        final RealMatrix cartesianOrbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {
            FastMath.pow(2.4e4, 2), FastMath.pow(1.e5, 2), FastMath.pow(4.e4, 2),
            FastMath.pow(3.5, 2), FastMath.pow(2., 2), FastMath.pow(0.6, 2)
        });
        
        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });
         
        ResultKalman kalmanW3B = runKalman(input, orbitType, print,
                cartesianOrbitalP, cartesianOrbitalQ,
                null, null,
                null, null, IS_UNSCENTED);

        // Tests
        // -----

        // Number of measurements processed
        final int numberOfMeas  = 521;
        Assert.assertEquals(numberOfMeas, kalmanW3B.getNumberOfMeasurements());

        
        // Test on orbital parameters
        // Done at the end to avoid changing the estimated propagation parameters
        // ----------------------------------------------------------------------
        
        // Estimated position and velocity
        final Vector3D estimatedPos = kalmanW3B.getEstimatedPV().getPosition();
        final Vector3D estimatedVel = kalmanW3B.getEstimatedPV().getVelocity();

        // Reference position and velocity at initial date (same as in batch LS test)
        final Vector3D refPos0 = new Vector3D(-40541446.255, -9905357.41, 206777.413);
        final Vector3D refVel0 = new Vector3D(759.0685, -1476.5156, 54.793);
        
        // Gather the selected propagation parameters and initialize them to the values found
        // with the batch LS method
        final ParameterDriversList refPropagationParameters = kalmanW3B.getPropagatorParameters();
        final double dragCoefRef = -0.215433133145843;
        final double[] leakXRef = {+5.69040439901955E-06, 1.09710906802403E-11};
        final double[] leakYRef = {-7.66440256777678E-07, 1.25467464335066E-10};
        final double[] leakZRef = {-5.574055079952E-06  , 2.78703463746911E-10};
        
        for (DelegatingDriver driver : refPropagationParameters.getDrivers()) {
            switch (driver.getName()) {
                case "drag coefficient" : driver.setValue(dragCoefRef); break;
                case "leak-X[0]"        : driver.setValue(leakXRef[0]); break;
                case "leak-X[1]"        : driver.setValue(leakXRef[1]); break;
                case "leak-Y[0]"        : driver.setValue(leakYRef[0]); break;
                case "leak-Y[1]"        : driver.setValue(leakYRef[1]); break;
                case "leak-Z[0]"        : driver.setValue(leakZRef[0]); break;
                case "leak-Z[1]"        : driver.setValue(leakZRef[1]); break;
            }
        }
        
        // Run the reference until Kalman last date
        final Orbit refOrbit = runReference(input, orbitType, refPos0, refVel0, refPropagationParameters,
                                            kalmanW3B.getEstimatedPV().getDate());
        
        // Test on last orbit
        final Vector3D refPos = refOrbit.getPVCoordinates().getPosition();
        final Vector3D refVel = refOrbit.getPVCoordinates().getVelocity();
        
        // Check distances
        final double dP = Vector3D.distance(refPos, estimatedPos);
        final double dV = Vector3D.distance(refVel, estimatedVel);
        
        final double debugDistanceAccuracy = 17710.87;
        final double debugVelocityAccuracy = 5.020;
        Assert.assertEquals(0.0, Vector3D.distance(refPos, estimatedPos), debugDistanceAccuracy);
        Assert.assertEquals(0.0, Vector3D.distance(refVel, estimatedVel), debugVelocityAccuracy);
        
        // Print orbit deltas
        if (print) {
            System.out.println("Test performances:");
            System.out.format("\t%-30s\n",
                            "ΔEstimated / Reference");
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔP [m]", dP);
            System.out.format(Locale.US, "\t%-10s %20.6f\n",
                              "ΔV [m/s]", dV);
        }
    }

}
