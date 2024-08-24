package org.orekit.files.iirv;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.annotation.DefaultDataContext;
import org.orekit.data.DataSource;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IIRVEphemerisTest {

    private static UTCScale UTC;
    private IIRVEphemerisFile.IIRVEphemeris iirvEphemeris;
    private IIRVSegment iirvSegment;

    @BeforeEach
    @DefaultDataContext
    public void setUp() {
        Utils.setDataRoot("regular-data");
        UTC = TimeScalesFactory.getUTC();
        final IIRVParser parser = new IIRVParser(2024, UTC);

        final String stereoAheadIirvFile = "/iirv/ahead_20240909_01.iirv";
        final IIRVEphemerisFile iirvEphemerisFile = parser.parse(
            new DataSource(stereoAheadIirvFile,
                () -> getClass().getResourceAsStream(stereoAheadIirvFile)));

        iirvEphemeris = iirvEphemerisFile.getSatellites().get("01");
        iirvSegment = iirvEphemeris.getSegment();
    }

    @Test
    void testConstructors() {
        // Test constructors
        IIRVEphemerisFile fileFromEphemeris = new IIRVEphemerisFile(iirvEphemeris);
        IIRVEphemerisFile fileFromValues = new IIRVEphemerisFile(
            iirvSegment.getMu(),
            iirvSegment.getInterpolationSamples(),
            iirvSegment.getStartYear(),
            iirvSegment.getIIRVMessage()
        );
        IIRVEphemerisFile fileFromMessage = new IIRVEphemerisFile(
            iirvSegment.getStartYear(),
            iirvSegment.getIIRVMessage()
        );
        assertEquals(fileFromEphemeris.getIIRV(), fileFromValues.getIIRV());
        assertEquals(fileFromEphemeris.getIIRV(), fileFromMessage.getIIRV());
    }

    @Test
    void testIIRVSegment() {
        assertEquals(iirvEphemeris.getMu(), iirvSegment.getMu());
        assertEquals(iirvEphemeris.getStart(), iirvSegment.getStart());
        assertEquals(iirvEphemeris.getStop(), iirvSegment.getStop());


        IIRVMessage testMessage = iirvSegment.getIIRVMessage();
        IIRVSegment testSegment = new IIRVSegment(
            Constants.IERS96_EARTH_MU,
            14,
            2020,
            testMessage
        );

        assertEquals(testSegment.getMu(), Constants.IERS96_EARTH_MU);
        assertEquals(testSegment.getFrame().getName(), "GTOD/2010 simple EOP");
        assertEquals(testSegment.getInterpolationSamples(), 14);
        assertEquals(testSegment.getAvailableDerivatives(), CartesianDerivativesFilter.USE_PV);
        assertEquals(testSegment.getStartYear(), 2020);
        assertEquals(testSegment.getStart().toString(UTC), "2020-09-09T00:00:00.000");
        assertEquals(testSegment.getStop().toString(UTC), "2020-09-10T00:00:00.000");
        assertEquals(testSegment.getIIRVMessage(), testMessage);

        // Test frames
        for (IIRVVector iirv : testSegment.getIIRVMessage().getVectors()) {
            assertEquals(testSegment.getFrame().getName(), iirv.getFrame().getName());
            assertEquals(testSegment.getFrame().getName(), iirv.getCoordinateSystem().getFrame().getName());
        }

    }

    @Test
    void testYearRolloverHandling() {
        // Make sure the date/epoch terms roll over at Jan 1 midnight (given that the start year is not represented
        // the IIRV message itself().

        TimeStampedPVCoordinates pv_before_midnight = new TimeStampedPVCoordinates(
            new AbsoluteDate(2020, 12, 31, 23, 59, 59, UTC),
            new Vector3D(1.0e6, 2.0e6, 3.0e6),
            new Vector3D(-300, -200, -100));
        TimeStampedPVCoordinates pv_after_midnight = new TimeStampedPVCoordinates(
            new AbsoluteDate(2021, 1, 1, 0, 0, 1, UTC),
            new Vector3D(1.0e6, 2.0e6, 3.0e6),
            new Vector3D(-300, -200, -100));

        final IIRVBuilder iirvBuilder = new IIRVBuilder(UTC);
        IIRVSegment iirv_segment = new IIRVSegment(2020, iirvBuilder.buildIIRVMessage(Arrays.asList(pv_before_midnight, pv_after_midnight)));

        List<TimeStampedPVCoordinates> coordinates_from_segment = iirv_segment.getCoordinates();
        assert (coordinates_from_segment.get(0).getDate().getComponents(UTC).getDate().getYear() == 2020);
        assert (coordinates_from_segment.get(1).getDate().getComponents(UTC).getDate().getYear() == 2021);
    }
}
