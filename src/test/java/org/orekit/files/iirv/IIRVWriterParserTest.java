package org.orekit.files.iirv;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitIllegalArgumentException;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.Constants;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests IIRV file writing routines
 */
public class IIRVWriterParserTest {


    private static UTCScale UTC;

    private final String iirvPathMultipleVectors = "/iirv/ISS_ZARYA_25544_NASA_IIRV_1DAY.iirv";
    private IIRVMessage sampleIirvMessage;
    private IIRVParser parser;

    @TempDir
    public Path temporaryFolderPath;

    @DefaultDataContext
    @BeforeEach
    public void setUp() {
        // Sets the root of data to read
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
        parser = new IIRVParser(Constants.EIGEN5C_EARTH_MU, 7, 2024, UTC);

        DataSource iirvTestDataSource = new DataSource(iirvPathMultipleVectors,
            () -> getClass().getResourceAsStream(iirvPathMultipleVectors));
        sampleIirvMessage = parser.parse(iirvTestDataSource).getIIRV();

        IIRVBuilder iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSupportIdCode(6406);
        iirvBuilder.setVehicleIdCode(1);
        iirvBuilder.setMass(1000.0);
        iirvBuilder.setCrossSectionalArea(20);
        iirvBuilder.setDragCoefficient(2.2);
        iirvBuilder.setSolarReflectivityCoefficient(1);
        iirvBuilder.setOriginatorRoutingIndicator("GCQU");
    }

    @Test
    void readWriteStringBuffer() throws IOException {
        final StringBuilder buffer = new StringBuilder();

        StreamingIIRVFileWriter writer = new StreamingIIRVFileWriter(buffer, IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
        assertEquals(writer.getIncludeMessageMetadataSetting(), IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);

        writer.writeIIRVMessage(sampleIirvMessage);
        IIRVEphemerisFile iirvEphemerisFile = parser.parse(buffer.toString());
        IIRVMessage iirvFromStringBuffer = iirvEphemerisFile.getIIRV();
        assertEquals(sampleIirvMessage, iirvFromStringBuffer);
    }

    @Test
    void readWriteMessageMetadata() throws IOException {

        final File allMetadataFile = temporaryFolderPath.resolve("writeAll.iirv").toFile();
        final File firstOnlyMetadataFile = temporaryFolderPath.resolve("firstOnly.iirv").toFile();

        // Write all metadata file
        try (BufferedWriter writer = Files.newBufferedWriter(allMetadataFile.toPath(), StandardCharsets.UTF_8)) {
            StreamingIIRVFileWriter allVectorsWriter = new StreamingIIRVFileWriter(
                writer,
                IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
            allVectorsWriter.writeIIRVMessage(sampleIirvMessage);
        }

        // Write first line only file
        try (BufferedWriter writer = Files.newBufferedWriter(firstOnlyMetadataFile.toPath(), StandardCharsets.UTF_8)) {
            StreamingIIRVFileWriter firstOnlyWriter = new StreamingIIRVFileWriter(
                writer,
                IIRVMessage.IncludeMessageMetadata.FIRST_VECTOR_ONLY);

            firstOnlyWriter.writeIIRVMessage(sampleIirvMessage);
        }

        IIRVMessage parsedAllMetadata = parser.parse(new DataSource(allMetadataFile.toString())).getIIRV();
        IIRVMessage parsedFirstOnlyMetadata = parser.parse(new DataSource(firstOnlyMetadataFile.toString())).getIIRV();

        assertEquals(parsedAllMetadata.get(0).toIIRVString(true),
            parsedFirstOnlyMetadata.get(0).toIIRVString(true));
        assertEquals(parsedAllMetadata.get(0).toIIRVString(true),
            parsedAllMetadata.getVectorStrings(IIRVMessage.IncludeMessageMetadata.ALL_VECTORS).get(0));
        assertEquals(parsedAllMetadata.get(1).toIIRVString(true),
            parsedAllMetadata.getVectorStrings(IIRVMessage.IncludeMessageMetadata.ALL_VECTORS).get(1));

        assertEquals(parsedAllMetadata.get(0).toIIRVString(false),
            parsedFirstOnlyMetadata.get(0).toIIRVString(false));
        assertEquals(parsedAllMetadata.get(1).toIIRVString(false),
            parsedAllMetadata.getVectorStrings(IIRVMessage.IncludeMessageMetadata.FIRST_VECTOR_ONLY).get(1));

    }

    @Test
    void writeMultipleVectorIIRV() throws IOException {

        // Create IIRV Builder object to set each non-default value
        IIRVBuilder iirvBuilder = new IIRVBuilder(UTC);
        iirvBuilder.setSupportIdCode(6406);
        iirvBuilder.setVehicleIdCode(1);
        iirvBuilder.setMass(1000.0);
        iirvBuilder.setCrossSectionalArea(20);
        iirvBuilder.setDragCoefficient(2.2);
        iirvBuilder.setSolarReflectivityCoefficient(1);
        iirvBuilder.setOriginatorRoutingIndicator("GCQU");

        IIRVFileWriter writer = new IIRVFileWriter(iirvBuilder, IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);

        // Write the IIRV message
        final File tempFile = temporaryFolderPath.resolve("writeIIRVExample.iirv").toFile();
        writer.write(tempFile.toString(), new IIRVEphemerisFile(2024, sampleIirvMessage));

        // Read in
        IIRVMessage parsedMessage = parser.parse(new DataSource(tempFile.toString())).getIIRV();
        assertEquals(6, parsedMessage.size());
    }

    @Test
    void readWriteIIRV() throws IOException {

        // Create temporary file for test case
        final Path tempFilepath = temporaryFolderPath.resolve("readWriteIIRVTest.iirv");

        // Write IIRV to file
        IIRVVector originalIIRV = sampleIirvMessage.get(0);

        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFilepath, StandardCharsets.UTF_8)) {
            StreamingIIRVFileWriter writer = new StreamingIIRVFileWriter(
                bufferedWriter,
                IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
            writer.writeIIRVMessage(new IIRVMessage(originalIIRV));
        }

        IIRVMessage parsedIIRVVectors = parser.parse(new DataSource(tempFilepath.toString())).getIIRV();
        assertEquals(1, parsedIIRVVectors.size());

        IIRVVector recoveredIIRV = parsedIIRVVectors.get(0);
        assertEquals(0, originalIIRV.compareTo(recoveredIIRV));
    }

    @Test
    void readWriteMultiLineIIRV() throws IOException, NullPointerException {

        // Read test file
        final String iirvFileResource = "/iirv/ISS_ZARYA_25544_NASA_IIRV_1DAY.iirv";
        final DataSource source = new DataSource(iirvFileResource,
            () -> getClass().getResourceAsStream(iirvFileResource));
        IIRVMessage iirvMessageBeforeWriting = parser.parse(source).getIIRV();

        // Immediately write back out
        final Path tempFilepath = temporaryFolderPath.resolve("readWriteIIRVTest.iirv");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFilepath, StandardCharsets.UTF_8)) {
            StreamingIIRVFileWriter writer = new StreamingIIRVFileWriter(
                bufferedWriter,
                IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
            writer.writeIIRVMessage(iirvMessageBeforeWriting);
        }

        // Immediately read back in
        IIRVMessage iirvVectorsAfterWriting = parser.parse(new DataSource(tempFilepath.toString())).getIIRV();

        // Is it still the same
        for (int i = 0; i < iirvMessageBeforeWriting.size(); i++) {
            IIRVVector originalIIRV = iirvMessageBeforeWriting.get(i);
            IIRVVector readWriteIIRV = iirvVectorsAfterWriting.get(i);
            assertEquals(0, originalIIRV.compareTo(readWriteIIRV));
        }
    }

    @Test
    void checkErrorHandling() throws IOException, NullPointerException {

        // Write the IIRV message
        IIRVFileWriter iirvFileWriter = new IIRVFileWriter(new IIRVBuilder(UTC), IIRVMessage.IncludeMessageMetadata.ALL_VECTORS);
        IIRVEphemerisFile iirvEphemerisFile = new IIRVEphemerisFile(2024, sampleIirvMessage);

        // IIRVFileWriter: null appendable handling
        assertThrows(OrekitIllegalArgumentException.class, () -> iirvFileWriter.write((Appendable) null, iirvEphemerisFile));

        // IIRVFileWriter: null ephemeris file handling
        final Path tempFilepath = temporaryFolderPath.resolve("readWriteIIRVTest.iirv");
        try (BufferedWriter bufferedWriter = Files.newBufferedWriter(tempFilepath, StandardCharsets.UTF_8)) {
            iirvFileWriter.write(bufferedWriter, null);
            assertEquals(0, tempFilepath.toFile().length()); // Check that file is empty
        }

        //IIRVParser: null inputs
        assertThrows(OrekitIllegalArgumentException.class, () -> parser.parse((DataSource) null)); // null data source
        assertThrows(OrekitIllegalArgumentException.class, () -> parser.parse((ArrayList<String>) null)); // null array
        assertThrows(OrekitIllegalArgumentException.class, () -> parser.parse(new ArrayList<>())); // empty array

        // IIRVParser: i/o exception
        assertThrows(OrekitException.class, () -> parser.parse(new DataSource("/nonexistant/data/path.iirv")));
    }

    @Test
    void readSingleVectorIIRV() {
        final String iirvFile = "/iirv/ISS_ZARYA_25544_NASA_IIRV.iirv";
        final DataSource source = new DataSource(iirvFile, () -> getClass().getResourceAsStream(iirvFile));

        IIRVVector iirv = new IIRVParser(2024, UTC).parse(source).getIIRV().get(0);
        assertEquals("030000000010GIIRV GSFC", iirv.buildLine1(true));
        assertEquals("GIIRV GSFC", iirv.buildLine1(false));
    }

}
