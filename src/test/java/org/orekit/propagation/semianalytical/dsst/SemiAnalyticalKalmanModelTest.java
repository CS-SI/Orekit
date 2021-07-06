/*
 * Copyright 2020 CS GROUP.
 * All rights reserved.
 */
package org.orekit.propagation.semianalytical.dsst;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.MatrixDecomposer;
import org.hipparchus.linear.MatrixUtils;
import org.hipparchus.linear.QRDecomposer;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.stat.descriptive.StreamingStatistics;
import org.hipparchus.util.FastMath;
import org.junit.Assert;
import org.junit.Test;
import org.orekit.bodies.CelestialBodyFactory;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataFilter;
import org.orekit.data.DataProvidersManager;
import org.orekit.data.DataSource;
import org.orekit.data.DirectoryCrawler;
import org.orekit.data.GzipFilter;
import org.orekit.data.UnixCompressFilter;
import org.orekit.estimation.measurements.ObservableSatellite;
import org.orekit.estimation.measurements.ObservedMeasurement;
import org.orekit.estimation.measurements.Position;
import org.orekit.estimation.sequential.ConstantProcessNoise;
import org.orekit.estimation.sequential.CovarianceMatrixProvider;
import org.orekit.estimation.sequential.SemiAnalyticalKalmanEstimator;
import org.orekit.files.ilrs.CPFFile;
import org.orekit.files.ilrs.CPFFile.CPFEphemeris;
import org.orekit.files.ilrs.CPFParser;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.ICGEMFormatReader;
import org.orekit.forces.gravity.potential.UnnormalizedSphericalHarmonicsProvider;
import org.orekit.frames.FramesFactory;
import org.orekit.gnss.HatanakaCompressFilter;
import org.orekit.orbits.CartesianOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.propagation.PropagationType;
import org.orekit.propagation.conversion.DSSTPropagatorBuilder;
import org.orekit.propagation.conversion.LutherIntegratorBuilder;
import org.orekit.propagation.conversion.ODEIntegratorBuilder;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTNewtonianAttraction;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTTesseral;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody;
import org.orekit.propagation.semianalytical.dsst.forces.DSSTZonal;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.ParameterDriver;
import org.orekit.utils.ParameterDriversList;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Test class for propagation method of POD SLR.
 * @author Bryan Cazabonne
 */
public class SemiAnalyticalKalmanModelTest {

    /** Initial covariance for satellite position. */
    private static final double COV_POS = 100.0;

    /** Initial covariance for satellite velocity. */
    private static final double COV_VEL = 1.0e-5;

    /** Initial covariance for polynomial acceleration coefficient. */
    private static final double COV_ACC_POL = 1.0e-18;

    /** Initial noise for satellite position. */
    private static final double NOISE_POS = 0.0;

    /** Initial noise for satellite velocity. */
    private static final double NOISE_VEL = 1.0e-15;

    /** Initial noise for polynomial acceleration coefficient. */
    private static final double NOISE_ACC_POL = 1.0e-18;

    /**
     * The purpose of this test is to verify the dynamical environment
     * of the satellite by comparison with reference ephemeris.
     */
    @Test
    public void testLageosTest() throws IOException{

        // Orekit data
        setDataRoot("regular-data:potential/icgem-format");
        final File home = new File(System.getProperty("user.home"));
        final File orekitData = new File(home, "orekit-data");
        final DataProvidersManager manager = DataContext.getDefault().getDataProvidersManager();
        manager.addProvider(new DirectoryCrawler(orekitData));
        
        // File path
        final String filePath = "src/test/resources/orbit-determination/Lageos2/";

        // Read CPF file
        final CPFFile cpfFile = readCpfFile(filePath, "lageos2_cpf_160213_5441.sgf");

        for (int i=1; i<2; i++) {
         // Propagator builder
            final DSSTPropagatorBuilder dsstPropagatorBuilder = buildPropagatorBuilder(cpfFile);

            // Kalman esimator
            final SemiAnalyticalKalmanEstimator kalman = buildKalmanFilter(dsstPropagatorBuilder);

            // Position measurements
            final List<ObservedMeasurement<?>> measurements = buildMeasurements(cpfFile);

            // Run the test
            System.out.print(i + "  ");
            run(kalman, measurements.subList(i*287, 288), cpfFile);
        }
        
        

    }

    /**
     * Run the test
     * @param kalman kalman filter
     * @param measurements list of measurements
     * @param podDataContext POD data context
     * @param cpfFile CPF file
     */
    private void run(final SemiAnalyticalKalmanEstimator kalman,
                     final List<ObservedMeasurement<?>> measurements,
                     final CPFFile cpfFile) {

        // Bounded propagator from the CPF file
        final CPFEphemeris ephemeris = cpfFile.getSatellites().get(cpfFile.getHeader().getIlrsSatelliteId());
        final BoundedPropagator bounded = ephemeris.getPropagator();

        // Intialize container for statistics
        final StreamingStatistics positionStatistics = new StreamingStatistics();



        // Process the measurement
        final Orbit estimated = kalman.estimationStep(measurements).getInitialState().getOrbit();

        // Reference position in both Earth and inertial frames
        final TimeStampedPVCoordinates pvITRF     = bounded.getPVCoordinates(estimated.getDate(), ephemeris.getFrame());
        final TimeStampedPVCoordinates pvInertial = ephemeris.getFrame().getTransformTo(FramesFactory.getEME2000(), estimated.getDate()).
                                                                             transformPVCoordinates(pvITRF);

        
        // Update statistics
        positionStatistics.addValue(Vector3D.distance(pvInertial.getPosition(), estimated.getPVCoordinates().getPosition()));

        // Verify date
        Assert.assertEquals(0.0, estimated.getDate().durationFrom(pvInertial.getDate()), 0.0);

        //System.out.println(pvInertial.getPosition());
        //System.out.println(estimated.getPVCoordinates().getPosition());
        
        System.out.println(positionStatistics.getMean());
        //System.out.println(positionStatistics.getMin());
        //System.out.println(positionStatistics.getMax());
        
        //Assert.assertEquals(0.0, positionStatistics.getMean(), 9.31e-3);
        //Assert.assertEquals(0.0, positionStatistics.getMin(),  0.0);
        //Assert.assertEquals(0.0, positionStatistics.getMax(),  3.06e-2);

    }

    /**
     * Read a CPF file.
     * @param filePath path to the folder containg the file
     * @param fileName file name
     * @return a configured CPF file
     * @throws IOException 
     */
    private static CPFFile readCpfFile(final String filePath, final String fileName) throws IOException {

        try {

            // set up filtering for measurements files         
            DataSource nd = new DataSource(fileName,
                                           () -> new FileInputStream(new File(filePath, fileName)));
              for (final DataFilter filter : Arrays.asList(new GzipFilter(),
                                                           new UnixCompressFilter(),
                                                           new HatanakaCompressFilter())) {
                  nd = filter.filter(nd);
              }


            // CPF parser
            final CPFParser parser = new CPFParser();
            final CPFFile file = (CPFFile) parser.parse(nd);

            // Return
            return file;
            
            

        } catch (IOException e) {
            // Throw an exception if a file cannot be read
            throw new IOException("Unable to find file");
        }

    }

    private static DSSTPropagatorBuilder buildPropagatorBuilder(final CPFFile cpfFile) {

        // Bounded propagator from the CPF file
        final CPFEphemeris ephemeris = cpfFile.getSatellites().get(cpfFile.getHeader().getIlrsSatelliteId());
        final BoundedPropagator bounded = ephemeris.getPropagator();

        // Initial date
        final AbsoluteDate initialDate = bounded.getMinDate();

        // Propagator configuration
        //final SLRPropagatorConfig propagatorConfig = new SLRPropagatorConfig(context, initialDate);
        

        // Initial orbit
        final TimeStampedPVCoordinates pvInitITRF = bounded.getPVCoordinates(initialDate, ephemeris.getFrame());
        final TimeStampedPVCoordinates pvInitInertial = ephemeris.getFrame().getTransformTo(FramesFactory.getEME2000(), initialDate).
                                                                             transformPVCoordinates(pvInitITRF);
        
        final UnnormalizedSphericalHarmonicsProvider gravityField = GravityFieldFactory.getUnnormalizedProvider(20, 20);
        OneAxisEllipsoid earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS, Constants.WGS84_EARTH_FLATTENING, FramesFactory.getITRF(IERSConventions.IERS_2010, false));
       
        final Orbit initialOrbit = new CartesianOrbit(pvInitInertial, FramesFactory.getEME2000(), gravityField.getMu());
              
        ODEIntegratorBuilder integrator = new LutherIntegratorBuilder(86400);
        
               
        DSSTPropagatorBuilder propagatorBuilder = new DSSTPropagatorBuilder(initialOrbit, integrator, 10, PropagationType.MEAN, PropagationType.OSCULATING);
        propagatorBuilder.addForceModel(new DSSTNewtonianAttraction(gravityField.getMu()));
        propagatorBuilder.addForceModel(new DSSTZonal(gravityField));
        propagatorBuilder.addForceModel(new DSSTTesseral(earth.getBodyFrame(), Constants.WGS84_EARTH_ANGULAR_VELOCITY, gravityField));
        propagatorBuilder.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getMoon(),gravityField.getMu()));
        propagatorBuilder.addForceModel(new DSSTThirdBody(CelestialBodyFactory.getSun(),gravityField.getMu()));

        // Return a configured propagator builder
        return propagatorBuilder;

    }

    /**
     * Build the Kalman filter used for estimation.
     * @param numericalPropagatorBuilder numerical propagator builder
     * @return a configured semi analytical kalman filter
     */
    private static SemiAnalyticalKalmanEstimator buildKalmanFilter(final DSSTPropagatorBuilder dsstPropagatorBuilder) {

        final MatrixDecomposer  decomposer  = new QRDecomposer(1.0e-15);
        final ParameterDriversList estimatedMeasurementParameters = new ParameterDriversList();
        final CovarianceMatrixProvider processNoiseMatrixProvider = buildCovarianceProvider(dsstPropagatorBuilder);
        final CovarianceMatrixProvider measurementProcessNoiseMatrix   = null;
        
        // Return the semi analytical kalman filter
        return new SemiAnalyticalKalmanEstimator(decomposer, dsstPropagatorBuilder, processNoiseMatrixProvider, 
                                          estimatedMeasurementParameters, measurementProcessNoiseMatrix);


    }

    /**
     * Build the covariance matrix provider.
     * @param numericalPropagatorBuilder numerical propagator builder
     * @return the covariance matrix provider
     */
    private static CovarianceMatrixProvider buildCovarianceProvider(final DSSTPropagatorBuilder dsstPropagatorBuilder)  {

        // Position and velocity covariance
        final RealMatrix orbitalP = MatrixUtils.createRealDiagonalMatrix(new double [] {COV_POS, COV_POS, COV_POS, COV_VEL, COV_VEL, COV_VEL});
        final RealMatrix orbitalQ = MatrixUtils.createRealDiagonalMatrix(new double [] {NOISE_POS, NOISE_POS, NOISE_POS, NOISE_VEL, NOISE_VEL, NOISE_VEL});

        // Number of propagation parameters (only empirical parameters)
        int nbProp = 0;
        for (final ParameterDriver parameter : dsstPropagatorBuilder.getPropagationParametersDrivers().getDrivers()) {
            if (parameter.isSelected()) {
                nbProp++;
            }
        }
        // Propagation parameters covariance
        final double[] propagationPArray = new double[nbProp];
        final double[] propagationQArray = new double[nbProp];
        for (int index = 0; index < nbProp; index++) {
            propagationPArray[index] = COV_ACC_POL;
            propagationQArray[index] = NOISE_ACC_POL;
        }

        // Covariance and noise matrices
        final RealMatrix initialP = MatrixUtils.createRealMatrix(6 + nbProp, 6 + nbProp);
        initialP.setSubMatrix(orbitalP.getData(), 0, 0);
        final RealMatrix Q        = MatrixUtils.createRealMatrix(6 + nbProp, 6 + nbProp);
        Q.setSubMatrix(orbitalQ.getData(), 0, 0);

        // Return
        return new ConstantProcessNoise(initialP, Q);

    }

    /**
     * Build the measurements
     * @param cpfFile CPF file containing satellite positions
     * @param podDataContext data context
     * @return a configured list of measurements
     */
    private static List<ObservedMeasurement<?>> buildMeasurements(final CPFFile cpfFile) {

        // Initialize an empty list of measurements
        List<ObservedMeasurement<?>> measurements = new ArrayList<>();

        // Observable satellite
        final ObservableSatellite satellite = new ObservableSatellite(0);

        // Bounded propagator from the CPF file
        final CPFEphemeris ephemeris = cpfFile.getSatellites().get(cpfFile.getHeader().getIlrsSatelliteId());
        final BoundedPropagator bounded = ephemeris.getPropagator();

        // Initial and final date
        final AbsoluteDate initialDate = bounded.getMinDate();
        final AbsoluteDate finalDate   = bounded.getMaxDate();

        // Loop
        AbsoluteDate currentDate = initialDate;
        while (finalDate.durationFrom(currentDate) >= 0) {

            // Position in both Earth and inertial frames
            final TimeStampedPVCoordinates pvITRF     = bounded.getPVCoordinates(currentDate, ephemeris.getFrame());
            final TimeStampedPVCoordinates pvInertial = ephemeris.getFrame().getTransformTo(FramesFactory.getEME2000(), currentDate).
                                                                             transformPVCoordinates(pvITRF);

            // Add the position measurement to the list
            measurements.add(new Position(currentDate, pvInertial.getPosition(), 0.1, 1.0, satellite));

            // Shift the date
            currentDate = currentDate.shiftedBy(cpfFile.getHeader().getStep());

        }

        // Return the list
        return measurements;

    }

    /**
     * Set the orekit data root.
     * @param root root
     * @return a configured data root
     */
    private static DataContext setDataRoot(String root) {
        try {
            StringBuffer buffer = new StringBuffer();
            for (String component : root.split(":")) {
                String componentPath;
                componentPath = SemiAnalyticalKalmanModelTest.class.getClassLoader().getResource(component).toURI().getPath();
                if (buffer.length() > 0) {
                    buffer.append(System.getProperty("path.separator"));
                }
                buffer.append(componentPath);
            }
            System.setProperty(DataProvidersManager.OREKIT_DATA_PATH, buffer.toString());
            return DataContext.getDefault();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
