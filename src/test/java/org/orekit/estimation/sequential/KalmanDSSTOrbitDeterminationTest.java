package org.orekit.estimation.sequential;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;

import org.hipparchus.exception.LocalizedCoreFormats;
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
import org.orekit.errors.OrekitException;
import org.orekit.estimation.common.AbstractOrbitDetermination;
import org.orekit.estimation.common.ParameterKey;
import org.orekit.estimation.common.ResultKalman;
import org.orekit.forces.drag.DragSensitive;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.forces.radiation.RadiationSensitive;
import org.orekit.models.earth.atmosphere.Atmosphere;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTAtmosphericDrag;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTSolarRadiationPressure;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList.DelegatingDriver;

public class KalmanDSSTOrbitDeterminationTest extends AbstractOrbitDetermination<DSSTPropagatorBuilder> {

	/** Gravity field. */
    private UnnormalizedSphericalHarmonicsProvider gravityField;

    /** Propagation type (mean or mean + osculating). */
    private PropagationType propagationType;

    /** Initial state type (defined using mean or osculating elements). */
    private PropagationType stateType;

    /** {@inheritDoc} */
    @Override
    protected void createGravityField(final KeyValueFileParser<ParameterKey> parser)
        throws NoSuchElementException {

        final int degree = parser.getInt(ParameterKey.CENTRAL_BODY_DEGREE);
        final int order  = FastMath.min(degree, parser.getInt(ParameterKey.CENTRAL_BODY_ORDER));
        gravityField = GravityFieldFactory.getUnnormalizedProvider(degree, order);
    }

    /** {@inheritDoc} */
    @Override
    protected double getMu() {
        return gravityField.getMu();
    }

    /** {@inheritDoc} */
    @Override
    protected DSSTPropagatorBuilder createPropagatorBuilder(final Orbit referenceOrbit,
                                                            final ODEIntegratorBuilder builder,
                                                            final double positionScale) {
        final EquinoctialOrbit equiOrbit = (EquinoctialOrbit) OrbitType.EQUINOCTIAL.convertType(referenceOrbit);
        return new DSSTPropagatorBuilder(equiOrbit, builder, positionScale, propagationType, stateType);
    }

    /** {@inheritDoc} */
    @Override
    protected void setMass(final DSSTPropagatorBuilder propagatorBuilder,
                                final double mass) {
        propagatorBuilder.setMass(mass);
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setGravity(final DSSTPropagatorBuilder propagatorBuilder,
                                               final OneAxisEllipsoid body) {

        // tesseral terms
        final DSSTForceModel tesseral = new DSSTTesseral(body.getBodyFrame(),
                                                         Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField,
                                                         gravityField.getMaxDegree(), gravityField.getMaxOrder(), 4, 12,
                                                         gravityField.getMaxDegree(), gravityField.getMaxOrder(), 4);
        propagatorBuilder.addForceModel(tesseral);

        // zonal terms
        final DSSTForceModel zonal = new DSSTZonal(gravityField, gravityField.getMaxDegree(), 4,
                                                   2 * gravityField.getMaxDegree() + 1);
        propagatorBuilder.addForceModel(zonal);

        // gather all drivers
        final List<ParameterDriver> drivers = new ArrayList<>();
        drivers.addAll(tesseral.getParametersDrivers());
        drivers.addAll(zonal.getParametersDrivers());
        return drivers;

    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setOceanTides(final DSSTPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final int degree, final int order) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Ocean tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolidTides(final DSSTPropagatorBuilder propagatorBuilder,
                                                  final IERSConventions conventions,
                                                  final OneAxisEllipsoid body,
                                                  final CelestialBody[] solidTidesBodies) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                                  "Solid tides not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setThirdBody(final DSSTPropagatorBuilder propagatorBuilder,
                                                 final CelestialBody thirdBody) {
        final DSSTForceModel thirdBodyModel = new DSSTThirdBody(thirdBody, gravityField.getMu());
        propagatorBuilder.addForceModel(thirdBodyModel);
        return thirdBodyModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setDrag(final DSSTPropagatorBuilder propagatorBuilder,
                                            final Atmosphere atmosphere, final DragSensitive spacecraft) {
        final DSSTForceModel dragModel = new DSSTAtmosphericDrag(atmosphere, spacecraft, gravityField.getMu());
        propagatorBuilder.addForceModel(dragModel);
        return dragModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setSolarRadiationPressure(final DSSTPropagatorBuilder propagatorBuilder, final CelestialBody sun,
                                                              final double equatorialRadius, final RadiationSensitive spacecraft) {
        final DSSTForceModel srpModel = new DSSTSolarRadiationPressure(sun, equatorialRadius,
                                                                       spacecraft, gravityField.getMu());
        propagatorBuilder.addForceModel(srpModel);
        return srpModel.getParametersDrivers();
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setAlbedoInfrared(final DSSTPropagatorBuilder propagatorBuilder,
                                                      final CelestialBody sun, final double equatorialRadius,
                                                      final double angularResolution,
                                                      final RadiationSensitive spacecraft) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Albedo and infrared not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setRelativity(final DSSTPropagatorBuilder propagatorBuilder) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Albedo and infrared not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected List<ParameterDriver> setPolynomialAcceleration(final DSSTPropagatorBuilder propagatorBuilder,
                                                              final String name, final Vector3D direction, final int degree) {
        throw new OrekitException(LocalizedCoreFormats.SIMPLE_MESSAGE,
                        "Polynomial acceleration not implemented in DSST");
    }

    /** {@inheritDoc} */
    @Override
    protected void setAttitudeProvider(final DSSTPropagatorBuilder propagatorBuilder,
                                       final AttitudeProvider attitudeProvider) {
        propagatorBuilder.setAttitudeProvider(attitudeProvider);
    }

    @Test
    // Orbit determination for Lageos2 based on SLR (range) measurements
    public void testLageos2() throws URISyntaxException, IOException {

        // Print results on console
        final boolean print = true;
        
        // input in resources directory
        final String inputPath = KalmanNumericalOrbitDeterminationTest.class.getClassLoader().getResource("orbit-determination/Lageos2/dsst_kalman_od_test_Lageos2.in").toURI().getPath();
        final File input  = new File(inputPath);

        // configure Orekit data acces
        Utils.setDataRoot("orbit-determination/february-2016:potential/icgem-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new ICGEMFormatReader("eigen-6s-truncated", true));

        this.propagationType = PropagationType.MEAN;
        this.stateType = PropagationType.OSCULATING;

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
        
        // Orbital Cartesian process noise matrix (Q)
        final RealMatrix cartesianOrbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {
            1.e-4, 1.e-4, 1.e-4, 1.e-10, 1.e-10, 1.e-10
        });

        // Kalman orbit determination run.
        ResultKalman kalmanLageos2 = runKalman(input, orbitType, print,
                                               cartesianOrbitalP, cartesianOrbitalQ,
                                               null, null,
                                               null, null);

        // Definition of the accuracy for the test
        final double distanceAccuracy = 0.86;
        final double velocityAccuracy = 4.12e-3;

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

        // Test on measurements parameters
        final List<DelegatingDriver> list = new ArrayList<DelegatingDriver>();
        list.addAll(kalmanLageos2.getMeasurementsParameters().getDrivers());
        sortParametersChanges(list);
        // Batch LS values
        //final double[] stationOffSet = { 1.659203,  0.861250,  -0.885352 };
        //final double rangeBias = -0.286275;
        final double[] stationOffSet = { 0.298867,  -0.137456,  0.013315 };
        final double rangeBias = 0.002390;
        Assert.assertEquals(stationOffSet[0], list.get(0).getValue(), distanceAccuracy);
        Assert.assertEquals(stationOffSet[1], list.get(1).getValue(), distanceAccuracy);
        Assert.assertEquals(stationOffSet[2], list.get(2).getValue(), distanceAccuracy);
        Assert.assertEquals(rangeBias,        list.get(3).getValue(), distanceAccuracy);

        //test on statistic for the range residuals
        final long nbRange = 258;
        // Batch LS values
        //final double[] RefStatRange = { -2.431135, 2.218644, 0.038483, 0.982017 };
        final double[] RefStatRange = { -23.561314, 20.436464, 0.964164, 5.687187 };
        Assert.assertEquals(nbRange, kalmanLageos2.getRangeStat().getN());
        Assert.assertEquals(RefStatRange[0], kalmanLageos2.getRangeStat().getMin(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[1], kalmanLageos2.getRangeStat().getMax(),               distanceAccuracy);
        Assert.assertEquals(RefStatRange[2], kalmanLageos2.getRangeStat().getMean(),              distanceAccuracy);
        Assert.assertEquals(RefStatRange[3], kalmanLageos2.getRangeStat().getStandardDeviation(), distanceAccuracy);

    }

}
