package org.orekit.files.iirv;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.files.iirv.terms.CoordinateSystemTerm;
import org.orekit.files.iirv.terms.CrossSectionalAreaTerm;
import org.orekit.files.iirv.terms.DataSourceTerm;
import org.orekit.files.iirv.terms.DragCoefficientTerm;
import org.orekit.files.iirv.terms.MassTerm;
import org.orekit.files.iirv.terms.MessageClassTerm;
import org.orekit.files.iirv.terms.MessageIDTerm;
import org.orekit.files.iirv.terms.MessageSourceTerm;
import org.orekit.files.iirv.terms.MessageTypeTerm;
import org.orekit.files.iirv.terms.OriginIdentificationTerm;
import org.orekit.files.iirv.terms.OriginatorRoutingIndicatorTerm;
import org.orekit.files.iirv.terms.RoutingIndicatorTerm;
import org.orekit.files.iirv.terms.SequenceNumberTerm;
import org.orekit.files.iirv.terms.SolarReflectivityCoefficientTerm;
import org.orekit.files.iirv.terms.SupportIdCodeTerm;
import org.orekit.files.iirv.terms.VectorTypeTerm;
import org.orekit.files.iirv.terms.VehicleIdCodeTerm;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.propagation.analytical.KeplerianPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Unit tests for {@link IIRVBuilder}
 */
public class IIRVBuilderTest {

    private static UTCScale UTC;
    private static List<TimeStampedPVCoordinates> testCoordinates;

    @TempDir
    public Path temporaryFolderPath;

    @BeforeEach
    @DefaultDataContext
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
        testCoordinates = getTestCoordinates(5);
    }

    /** Test coordinates, see {@link org.orekit.files.stk.STKEphemerisFileTest} */
    List<TimeStampedPVCoordinates> getTestCoordinates(int nCoordinates) {
        final KeplerianOrbit orbit =
            new KeplerianOrbit(6.5e6, 0.01, -0.1, 0.1, 0.2, 0.3, PositionAngleType.TRUE,
                FramesFactory.getEME2000(), AbsoluteDate.J2000_EPOCH,
                Constants.WGS84_EARTH_MU);
        final AbsoluteDate startDate = new AbsoluteDate(2009, 1, 16, 0, 0, 0, UTC);

        final Frame eme2000 = FramesFactory.getEME2000();

        final double finalTime = 3600;
        final double stepSizeSeconds = finalTime / nCoordinates;

        final KeplerianPropagator propagator = new KeplerianPropagator(orbit);
        final List<TimeStampedPVCoordinates> coordinates = new ArrayList<>();
        for (double dt = 0.0; dt < finalTime; dt += stepSizeSeconds) {
            coordinates.add(propagator.getPVCoordinates(startDate.shiftedBy(dt), eme2000));
        }
        return coordinates;
    }

    @Test
    @DefaultDataContext
    void testIIRVBuilderBuildMethods() {
        final IIRVBuilder iirvBuilder = new IIRVBuilder(UTC);

        for (TimeStampedPVCoordinates pv : testCoordinates) {

            // Build from TimeStampedPVCoordinates
            IIRVVector iirvPrecise = iirvBuilder.buildVector(pv);
            IIRVVector iirv = new IIRVVector(iirvPrecise.toIIRVStrings(true), UTC);

            // Build from terms
            IIRVVector iirvFromTerms = iirvBuilder.buildVector(
                iirvPrecise.getDayOfYear(),
                iirvPrecise.getVectorEpoch(),
                iirvPrecise.getXPosition(),
                iirvPrecise.getYPosition(),
                iirvPrecise.getZPosition(),
                iirvPrecise.getXVelocity(),
                iirvPrecise.getYVelocity(),
                iirvPrecise.getZVelocity());

            // PositionVectorTerm
            assertEquals(Math.round(pv.getPosition().getX()), iirv.getPositionVector().getX());
            assertEquals(Math.round(pv.getPosition().getX()), iirv.getXPosition().value());
            assertEquals(Math.round(pv.getPosition().getY()), iirv.getPositionVector().getY());
            assertEquals(Math.round(pv.getPosition().getY()), iirv.getYPosition().value());
            assertEquals(Math.round(pv.getPosition().getZ()), iirv.getPositionVector().getZ());
            assertEquals(Math.round(pv.getPosition().getZ()), iirv.getZPosition().value());

            // VelocityVectorTerm
            final double velTol = 1e-3;
            assertEquals(pv.getVelocity().getX(), iirv.getVelocityVector().getX(), velTol);
            assertEquals(pv.getVelocity().getX(), iirv.getXVelocity().value(), velTol);
            assertEquals(pv.getVelocity().getY(), iirv.getVelocityVector().getY(), velTol);
            assertEquals(pv.getVelocity().getY(), iirv.getYVelocity().value(), velTol);
            assertEquals(pv.getVelocity().getZ(), iirv.getVelocityVector().getZ(), velTol);
            assertEquals(pv.getVelocity().getZ(), iirv.getZVelocity().value(), velTol);

            //DayOfYearTerm
            assertEquals(pv.getDate().getComponents(UTC).getDate().getDayOfYear(),
                iirv.getDayOfYear().value());

            // VectorEpochTerm
            TimeComponents timeTruth = pv.getDate().getComponents(UTC).getTime();
            TimeComponents timeIIRV = iirv.getVectorEpoch().value();
            assertEquals(timeTruth.getHour(), timeIIRV.getHour());
            assertEquals(timeTruth.getMinute(), timeIIRV.getMinute());
            assertEquals(timeTruth.getSecond(), timeIIRV.getSecond(), 1e-3);

            assertEquals(iirvPrecise, iirvFromTerms);
            assertEquals(iirv, iirvFromTerms);
        }
    }

    @Test
    void testIIRVBuilderMassTerm() {
        IIRVBuilder iirvBuilder;

        String valStr = "1.2";
        double val = Double.parseDouble(valStr);
        MassTerm massTerm = new MassTerm(val);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMass(massTerm);
        assertEquals(massTerm, iirvBuilder.getMass());

        // Set by value (double)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMass(val);
        assertEquals(massTerm, iirvBuilder.getMass());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMass(massTerm.toEncodedString());
        assertEquals(massTerm, iirvBuilder.getMass());
    }

    @Test
    void testIIRVBuilderMessageID() {
        IIRVBuilder iirvBuilder;

        String valStr = "0012345";
        long val = Long.parseLong(valStr);
        MessageIDTerm messageIDTerm = new MessageIDTerm(valStr);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageID(messageIDTerm);
        assertEquals(messageIDTerm, iirvBuilder.getMessageID());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageID(val);
        assertEquals(messageIDTerm, iirvBuilder.getMessageID());

        // Set by value (int)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageID((int) val);
        assertEquals(messageIDTerm, iirvBuilder.getMessageID());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageID(messageIDTerm.toEncodedString());
        assertEquals(messageIDTerm, iirvBuilder.getMessageID());
    }

    @Test
    void testIIRVBuilderMessageClass() {
        IIRVBuilder iirvBuilder;

        MessageClassTerm messageClassTerm = MessageClassTerm.NOMINAL;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageClass(messageClassTerm);
        assertEquals(messageClassTerm, iirvBuilder.getMessageClass());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageClass(messageClassTerm.value());
        assertEquals(messageClassTerm, iirvBuilder.getMessageClass());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageClass(messageClassTerm.toEncodedString());
        assertEquals(messageClassTerm, iirvBuilder.getMessageClass());
    }

    @Test
    void testIIRVBuilderOriginIdentification() {
        IIRVBuilder iirvBuilder;

        OriginIdentificationTerm originIdentificationTerm = OriginIdentificationTerm.GSFC;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setOriginIdentification(originIdentificationTerm);
        assertEquals(originIdentificationTerm, iirvBuilder.getOriginIdentification());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setOriginIdentification(originIdentificationTerm.toEncodedString());
        assertEquals(originIdentificationTerm, iirvBuilder.getOriginIdentification());
    }

    @Test
    void testIIRVBuilderMessageTypeTerm() {
        IIRVBuilder iirvBuilder;

        MessageTypeTerm messageTypeTerm = new MessageTypeTerm("05");

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageType(messageTypeTerm);
        assertEquals(messageTypeTerm, iirvBuilder.getMessageType());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageType(messageTypeTerm.toEncodedString());
        assertEquals(messageTypeTerm, iirvBuilder.getMessageType());
    }

    @Test
    void testIIRVBuilderMessageSourceTerm() {
        IIRVBuilder iirvBuilder;

        MessageSourceTerm messageSourceTerm = new MessageSourceTerm("1");

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageSource(messageSourceTerm);
        assertEquals(messageSourceTerm, iirvBuilder.getMessageSource());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setMessageSource(messageSourceTerm.toEncodedString());
        assertEquals(messageSourceTerm, iirvBuilder.getMessageSource());
    }

    @Test
    void testIIRVBuilderRoutingIndicator() {
        IIRVBuilder iirvBuilder;

        RoutingIndicatorTerm routingIndicatorTerm = RoutingIndicatorTerm.GSFC;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setRoutingIndicator(routingIndicatorTerm);
        assertEquals(routingIndicatorTerm, iirvBuilder.getRoutingIndicator());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setRoutingIndicator(routingIndicatorTerm.toEncodedString());
        assertEquals(routingIndicatorTerm, iirvBuilder.getRoutingIndicator());
    }

    @Test
    void testIIRVBuilderVectorType() {
        IIRVBuilder iirvBuilder;

        VectorTypeTerm vectorTypeTerm = VectorTypeTerm.POWERED_FLIGHT;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVectorType(vectorTypeTerm);
        assertEquals(vectorTypeTerm, iirvBuilder.getVectorType());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVectorType(vectorTypeTerm.value());
        assertEquals(vectorTypeTerm, iirvBuilder.getVectorType());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVectorType(vectorTypeTerm.toEncodedString());
        assertEquals(vectorTypeTerm, iirvBuilder.getVectorType());
    }

    @Test
    void testIIRVBuilderDataSourceTerm() {
        IIRVBuilder iirvBuilder;

        DataSourceTerm dataSourceTerm = DataSourceTerm.OFFLINE;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDataSource(dataSourceTerm);
        assertEquals(dataSourceTerm, iirvBuilder.getDataSource());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDataSource(dataSourceTerm.value());
        assertEquals(dataSourceTerm, iirvBuilder.getDataSource());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDataSource(dataSourceTerm.toEncodedString());
        assertEquals(dataSourceTerm, iirvBuilder.getDataSource());
    }

    @Test
    void testIIRVBuilderCoordinateSystemTerm() {
        IIRVBuilder iirvBuilder;

        CoordinateSystemTerm coordinateSystemTerm = CoordinateSystemTerm.GEOCENTRIC_TRUE_OF_DATE_ROTATING;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCoordinateSystem(coordinateSystemTerm);
        assertEquals(coordinateSystemTerm, iirvBuilder.getCoordinateSystem());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCoordinateSystem(coordinateSystemTerm.value());
        assertEquals(coordinateSystemTerm, iirvBuilder.getCoordinateSystem());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCoordinateSystem(coordinateSystemTerm.toEncodedString());
        assertEquals(coordinateSystemTerm, iirvBuilder.getCoordinateSystem());
    }

    @Test
    void testIIRVBuilderSupportIdTerm() {
        IIRVBuilder iirvBuilder;

        SupportIdCodeTerm supportIdCodeTerm = new SupportIdCodeTerm(1234);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSupportIdCode(supportIdCodeTerm);
        assertEquals(supportIdCodeTerm, iirvBuilder.getSupportIdCode());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSupportIdCode(supportIdCodeTerm.value());
        assertEquals(supportIdCodeTerm, iirvBuilder.getSupportIdCode());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSupportIdCode(supportIdCodeTerm.toEncodedString());
        assertEquals(supportIdCodeTerm, iirvBuilder.getSupportIdCode());
    }

    @Test
    void testIIRVBuilderVehicleIdTerm() {
        IIRVBuilder iirvBuilder;

        VehicleIdCodeTerm vehicleIdCodeTerm = new VehicleIdCodeTerm(5);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVehicleIdCode(vehicleIdCodeTerm);
        assertEquals(vehicleIdCodeTerm, iirvBuilder.getVehicleIdCode());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVehicleIdCode(vehicleIdCodeTerm.value());
        assertEquals(vehicleIdCodeTerm, iirvBuilder.getVehicleIdCode());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setVehicleIdCode(vehicleIdCodeTerm.toEncodedString());
        assertEquals(vehicleIdCodeTerm, iirvBuilder.getVehicleIdCode());
    }

    @Test
    void testIIRVBuilderSequenceNumberTerm() {
        IIRVBuilder iirvBuilder;

        SequenceNumberTerm sequenceNumberTerm = new SequenceNumberTerm(10);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSequenceNumber(sequenceNumberTerm);
        assertEquals(sequenceNumberTerm, iirvBuilder.getSequenceNumber());

        // Set by value (long)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSequenceNumber(sequenceNumberTerm.value());
        assertEquals(sequenceNumberTerm, iirvBuilder.getSequenceNumber());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSequenceNumber(sequenceNumberTerm.toEncodedString());
        assertEquals(sequenceNumberTerm, iirvBuilder.getSequenceNumber());

        // Exceeds max value
        Assertions.assertThrows(OrekitIllegalArgumentException.class, () -> new IIRVBuilder(UTC).setSequenceNumber(1000));
    }

    @Test
    void testIIRVBuilderCrossSectionalAreaTerm() {
        IIRVBuilder iirvBuilder;

        CrossSectionalAreaTerm crossSectionalAreaTerm = new CrossSectionalAreaTerm(10.5);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCrossSectionalArea(crossSectionalAreaTerm);
        assertEquals(crossSectionalAreaTerm, iirvBuilder.getCrossSectionalArea());

        // Set by value (double)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCrossSectionalArea(crossSectionalAreaTerm.value());
        assertEquals(crossSectionalAreaTerm, iirvBuilder.getCrossSectionalArea());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setCrossSectionalArea(crossSectionalAreaTerm.toEncodedString());
        assertEquals(crossSectionalAreaTerm, iirvBuilder.getCrossSectionalArea());
    }

    @Test
    void testIIRVBuilderDragCoefficientTerm() {
        IIRVBuilder iirvBuilder;

        DragCoefficientTerm dragCoefficientTerm = new DragCoefficientTerm(2.2);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDragCoefficient(dragCoefficientTerm);
        assertEquals(dragCoefficientTerm, iirvBuilder.getDragCoefficient());

        // Set by value (double)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDragCoefficient(dragCoefficientTerm.value());
        assertEquals(dragCoefficientTerm, iirvBuilder.getDragCoefficient());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setDragCoefficient(dragCoefficientTerm.toEncodedString());
        assertEquals(dragCoefficientTerm, iirvBuilder.getDragCoefficient());
    }

    @Test
    void testIIRVBuilderSolarReflectivityCoefficientTerm() {
        IIRVBuilder iirvBuilder;

        SolarReflectivityCoefficientTerm solarReflectivityCoefficientTerm = new SolarReflectivityCoefficientTerm(-7.8);

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSolarReflectivityCoefficient(solarReflectivityCoefficientTerm);
        assertEquals(solarReflectivityCoefficientTerm, iirvBuilder.getSolarReflectivityCoefficient());

        // Set by value (double)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSolarReflectivityCoefficient(solarReflectivityCoefficientTerm.value());
        assertEquals(solarReflectivityCoefficientTerm, iirvBuilder.getSolarReflectivityCoefficient());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSolarReflectivityCoefficient(solarReflectivityCoefficientTerm.toEncodedString());
        assertEquals(solarReflectivityCoefficientTerm, iirvBuilder.getSolarReflectivityCoefficient());
    }

    @Test
    void testIIRVBuilderOriginatorRoutingIndicatorTerm() {
        IIRVBuilder iirvBuilder;

        OriginatorRoutingIndicatorTerm originatorRoutingIndicatorTerm = OriginatorRoutingIndicatorTerm.GCQU;

        // set by IIRVTerm instance
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setOriginatorRoutingIndicator(originatorRoutingIndicatorTerm);
        assertEquals(originatorRoutingIndicatorTerm, iirvBuilder.getOriginatorRoutingIndicator());

        // Set by value (string)
        iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setOriginatorRoutingIndicator(originatorRoutingIndicatorTerm.toEncodedString());
        assertEquals(originatorRoutingIndicatorTerm, iirvBuilder.getOriginatorRoutingIndicator());
    }

    @Test
    void testNumberOfElementsErrorHandling() {
        // Make sure an exception is thrown if the number of coordinates is too large
        List<TimeStampedPVCoordinates> coordinates = getTestCoordinates(1001);
        assertThrows(OrekitIllegalArgumentException.class, () -> new IIRVBuilder(UTC).buildIIRVMessage(coordinates));
        assertThrows(OrekitIllegalArgumentException.class, () -> new IIRVBuilder(UTC).buildEphemerisFile(coordinates));
    }


    @Test
    void exampleUsage() throws IOException {
        // Example script used in documentation in IIRVFileWriter

        // Given a list of {@code TimeStampedPVCoordinates} objects
        List<TimeStampedPVCoordinates> coordinates = testCoordinates;

        // 1. Create an {@link IIRVBuilder} class to define the spacecraft/mission metadata values
        IIRVBuilder iirvBuilder = new IIRVBuilder(TimeScalesFactory.getUTC());
        iirvBuilder.setSupportIdCode(1221);
        iirvBuilder.setDragCoefficient(2.2);
        iirvBuilder.setOriginIdentification(OriginIdentificationTerm.GSFC);
        iirvBuilder.setRoutingIndicator("MANY");
        // ... (additional fields here)

        // 2. Create an {@link IIRVFileWriter} with the builder object
        IIRVFileWriter writer = new IIRVFileWriter(iirvBuilder, IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);

        // 3. Generate an {@link IIRVEphemerisFile} object containing the ephemeris data
        IIRVEphemerisFile iirvFile = iirvBuilder.buildEphemerisFile(coordinates);

        // 4. Write to disk.
        // Recommendation: embed the start year in the filename (year does not appear in the IIRV itself)
        String testFilename = "TestSatellite" + "_" +
            iirvFile.getStartYear() + "_" +
            iirvFile.getIIRV().get(0).getDayOfYear().toEncodedString() + "_" +
            iirvFile.getIIRV().get(0).getVectorEpoch().toEncodedString() + ".iirv";
        writer.write(temporaryFolderPath.resolve(testFilename).toString(), iirvFile);
    }
}