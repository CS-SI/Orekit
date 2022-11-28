package org.orekit.files.stk;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.StringReader;
import java.util.EnumMap;
import java.util.List;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.errors.OrekitException;
import org.orekit.files.stk.STKEphemerisFile.STKCoordinateSystem;
import org.orekit.files.stk.STKEphemerisFile.STKEphemeris;
import org.orekit.files.stk.STKEphemerisFile.STKEphemerisSegment;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;
import org.orekit.time.UTCScale;
import org.orekit.utils.CartesianDerivativesFilter;
import org.orekit.utils.Constants;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Unit tests for {@link STKEphemerisFileParser}.
 */
public final class STKEphemerisFileParserTest {

  private static final double MU = Constants.WGS84_EARTH_MU;
  private static UTCScale UTC;

  @BeforeAll
  public static void setUp() {
      Utils.setDataRoot("regular-data");
      UTC = TimeScalesFactory.getUTC();
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} throws an exception if the file is empty.
   */
  @Test
  public void testParseEmptyFile() {
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getGCRF();
    frameMapping.put(STKCoordinateSystem.ICRF, frame);
    final DataSource source = new DataSource("", () -> new StringReader(""));
    final STKEphemerisFileParser parser = new STKEphemerisFileParser("99999", MU, UTC, frameMapping);
    assertThrows(OrekitException.class, () -> parser.parse(source));
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} correctly parses a file using the
   * EphemerisTimePos format and which has a single segment.
   */
  @Test
  public void testParseEphemerisTimePosWithSingleSegment() {
    final String ex = "/stk/stk_02674_p.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getEME2000();
    frameMapping.put(STKCoordinateSystem.J2000, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);

    final STKEphemerisFile file = parser.parse(source);

    assertEquals("stk.v.12.0", file.getSTKVersion());
    assertEquals(1, file.getSatellites().size());

    final AbsoluteDate startDate = new AbsoluteDate(2007, 01, 12, 00, 00, 00.000883, UTC);
    final AbsoluteDate stopDate = new AbsoluteDate(2007, 01, 12, 00, 10, 00.000883, UTC);

    final STKEphemeris ephemeris = file.getSatellites().get(satelliteId);
    assertEquals(satelliteId, ephemeris.getId());
    assertEquals(MU, ephemeris.getMu());
    assertEquals(startDate, ephemeris.getStart());
    assertEquals(stopDate, ephemeris.getStop());
    assertEquals(1, ephemeris.getSegments().size());

    final STKEphemerisSegment segment = ephemeris.getSegments().get(0);
    assertEquals(frame, segment.getFrame());
    assertEquals(MU, segment.getMu());
    assertEquals(startDate, segment.getStart());
    assertEquals(stopDate, segment.getStop());
    assertEquals(6, segment.getInterpolationSamples());
    assertEquals(CartesianDerivativesFilter.USE_P, segment.getAvailableDerivatives());
    assertEquals(11, segment.getCoordinates().size());

    final List<TimeStampedPVCoordinates> coordinates = segment.getCoordinates();

    assertEquals(startDate, coordinates.get(0).getDate());
    assertEquals(new Vector3D(-4.2001828159554983e+06, -3.9105939267270239e+06, -4.5819301444368772e+06),
        coordinates.get(0).getPosition());
    assertEquals(Vector3D.ZERO, coordinates.get(0).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(0).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), coordinates.get(5).getDate());
    assertEquals(new Vector3D(-2.3930847437297828e+06, -5.1030332553920103e+06, -4.6952642374704480e+06),
        coordinates.get(5).getPosition());
    assertEquals(Vector3D.ZERO, coordinates.get(5).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(5).getAcceleration());

    assertEquals(stopDate, coordinates.get(10).getDate());
    assertEquals(new Vector3D(-3.7051401425713405e+05, -5.8354092318012062e+06, -4.3842831794793038e+06),
        coordinates.get(10).getPosition());
    assertEquals(Vector3D.ZERO, coordinates.get(10).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(10).getAcceleration());
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} correctly parses a file using the
   * EphemerisTimePosVel format and which has a single segment.
   */
  @Test
  public void testParseEphemerisTimePosVelWithSingleSegment() {
    final String ex = "/stk/stk_02674_pv.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getEME2000();
    frameMapping.put(STKCoordinateSystem.J2000, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);

    final STKEphemerisFile file = parser.parse(source);

    assertEquals("stk.v.12.0", file.getSTKVersion());
    assertEquals(1, file.getSatellites().size());

    final AbsoluteDate startDate = new AbsoluteDate(2007, 01, 12, 00, 00, 00.000883, UTC);
    final AbsoluteDate stopDate = new AbsoluteDate(2007, 01, 12, 00, 10, 00.000883, UTC);

    final STKEphemeris ephemeris = file.getSatellites().get(satelliteId);
    assertEquals(satelliteId, ephemeris.getId());
    assertEquals(MU, ephemeris.getMu());
    assertEquals(startDate, ephemeris.getStart());
    assertEquals(stopDate, ephemeris.getStop());
    assertEquals(1, ephemeris.getSegments().size());

    final STKEphemerisSegment segment = ephemeris.getSegments().get(0);
    assertEquals(frame, segment.getFrame());
    assertEquals(MU, segment.getMu());
    assertEquals(startDate, segment.getStart());
    assertEquals(stopDate, segment.getStop());
    assertEquals(6, segment.getInterpolationSamples());
    assertEquals(CartesianDerivativesFilter.USE_PV, segment.getAvailableDerivatives());
    assertEquals(11, segment.getCoordinates().size());

    final List<TimeStampedPVCoordinates> coordinates = segment.getCoordinates();

    assertEquals(startDate, coordinates.get(0).getDate());
    assertEquals(new Vector3D(-4.2001828159554983e+06, -3.9105939267270239e+06, -4.5819301444368772e+06),
        coordinates.get(0).getPosition());
    assertEquals(new Vector3D(5.4770282903204152e+03, -4.6296785954320931e+03, -1.0817325337227874e+03),
        coordinates.get(0).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(0).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), coordinates.get(5).getDate());
    assertEquals(new Vector3D(-2.3930847437297828e+06, -5.1030332553920103e+06, -4.6952642374704480e+06),
        coordinates.get(5).getPosition());
    assertEquals(new Vector3D(6.4794691416813621e+03, -3.2589590411954355e+03, 3.3269516839203544e+02),
        coordinates.get(5).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(5).getAcceleration());

    assertEquals(stopDate, coordinates.get(10).getDate());
    assertEquals(new Vector3D(-3.7051401425713405e+05, -5.8354092318012062e+06, -4.3842831794793038e+06),
        coordinates.get(10).getPosition());
    assertEquals(new Vector3D(6.9022331002597584e+03, -1.5830164360578574e+03, 1.7272100173740068e+03),
        coordinates.get(10).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates.get(10).getAcceleration());
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} correctly parses a file using the
   * EphemerisTimePosVel format and which has multiple segments.
   */
  @Test
  public void testParseEphemerisTimePosVelWithMultipleSegments() {
    final String ex = "/stk/stk_impulsive_maneuver.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getEME2000();
    frameMapping.put(STKCoordinateSystem.J2000, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);

    final STKEphemerisFile file = parser.parse(source);

    assertEquals("stk.v.12.0", file.getSTKVersion());
    assertEquals(1, file.getSatellites().size());

    final AbsoluteDate startDate = new AbsoluteDate(2007, 01, 12, 00, 00, 00.000883, UTC);
    final AbsoluteDate stopDate = new AbsoluteDate(2007, 01, 12, 00, 10, 00.000883, UTC);

    final STKEphemeris ephemeris = file.getSatellites().get(satelliteId);
    assertEquals(satelliteId, ephemeris.getId());
    assertEquals(MU, ephemeris.getMu());
    assertEquals(startDate, ephemeris.getStart());
    assertEquals(stopDate, ephemeris.getStop());
    assertEquals(2, ephemeris.getSegments().size());

    final STKEphemerisSegment segment1 = ephemeris.getSegments().get(0);
    assertEquals(frame, segment1.getFrame());
    assertEquals(MU, segment1.getMu());
    assertEquals(startDate, segment1.getStart());
    assertEquals(5, segment1.getInterpolationSamples());
    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), segment1.getStop());
    assertEquals(CartesianDerivativesFilter.USE_PV, segment1.getAvailableDerivatives());
    assertEquals(6, segment1.getCoordinates().size());

    final List<TimeStampedPVCoordinates> coordinates1 = segment1.getCoordinates();

    assertEquals(startDate, coordinates1.get(0).getDate());
    assertEquals(new Vector3D(6.9999999999999553e+06, -1.2559721622877134e-03, -7.9186900106575397e-01),
        coordinates1.get(0).getPosition());
    assertEquals(new Vector3D(4.1824064674153972e-04, 6.7895303333415395e+03, 3.6864141134955967e+03),
        coordinates1.get(0).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates1.get(0).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 01, 00.000883, UTC), coordinates1.get(1).getDate());
    assertEquals(new Vector3D(6.9853436721346313e+06, 4.0708747687269317e+05, 2.2102922871869511e+05),
        coordinates1.get(1).getPosition());
    assertEquals(new Vector3D(-4.8834963369038530e+02, 6.7753160807054328e+03, 3.6786747792111828e+03),
        coordinates1.get(1).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates1.get(1).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), coordinates1.get(5).getDate());
    assertEquals(new Vector3D(6.6370788283674866e+06, 2.0015702515387577e+06, 1.0867112801511162e+06),
        coordinates1.get(5).getPosition());
    assertEquals(new Vector3D(-2.3954153910685013e+03, 6.4383927907853858e+03, 3.4952501591096920e+03),
        coordinates1.get(5).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates1.get(5).getAcceleration());

    final STKEphemerisSegment segment2 = ephemeris.getSegments().get(1);
    assertEquals(frame, segment2.getFrame());
    assertEquals(MU, segment2.getMu());
    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), segment2.getStart());
    assertEquals(stopDate, segment2.getStop());
    assertEquals(5, segment2.getInterpolationSamples());
    assertEquals(CartesianDerivativesFilter.USE_PV, segment2.getAvailableDerivatives());
    assertEquals(6, segment2.getCoordinates().size());

    final List<TimeStampedPVCoordinates> coordinates2 = segment2.getCoordinates();

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), coordinates2.get(0).getDate());
    assertEquals(new Vector3D(6.6370788283674866e+06, 2.0015702515387577e+06, 1.0867112801511162e+06),
        coordinates2.get(0).getPosition());
    assertEquals(new Vector3D(-2.4109546214707530e+03, 6.4801591037886537e+03, 3.5179240960554193e+03),
        coordinates2.get(0).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates2.get(0).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 06, 00.000883, UTC), coordinates2.get(1).getDate());
    assertEquals(new Vector3D(6.4787418082331438e+06, 2.3859551936109182e+06, 1.2953780350291547e+06),
        coordinates2.get(1).getPosition());
    assertEquals(new Vector3D(-2.8648687835270284e+03, 6.3283234273332882e+03, 3.4352673416780117e+03),
        coordinates2.get(1).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates2.get(1).getAcceleration());

    assertEquals(stopDate, coordinates2.get(5).getDate());
    assertEquals(new Vector3D(5.5862903748328788e+06, 3.8099552373593030e+06, 2.0682393977866683e+06),
        coordinates2.get(5).getPosition());
    assertEquals(new Vector3D(-4.5259006372314416e+03, 5.4761825144401137e+03, 2.9714200584292998e+03),
        coordinates2.get(5).getVelocity());
    assertEquals(Vector3D.ZERO, coordinates2.get(5).getAcceleration());
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} correctly parses a file using the
   * EphemerisTimePosVelAcc format and which has a single segment.
   */
  @Test
  public void testParseEphemerisTimePosVelAccWithSingleSegment() {
    final String ex = "/stk/stk_02674_pva.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getEME2000();
    frameMapping.put(STKCoordinateSystem.J2000, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);

    final STKEphemerisFile file = parser.parse(source);

    assertEquals("stk.v.12.0", file.getSTKVersion());
    assertEquals(1, file.getSatellites().size());

    final AbsoluteDate startDate = new AbsoluteDate(2007, 01, 12, 00, 00, 00.000883, UTC);
    final AbsoluteDate stopDate = new AbsoluteDate(2007, 01, 12, 00, 10, 00.000883, UTC);

    final STKEphemeris ephemeris = file.getSatellites().get(satelliteId);
    assertEquals(satelliteId, ephemeris.getId());
    assertEquals(MU, ephemeris.getMu());
    assertEquals(startDate, ephemeris.getStart());
    assertEquals(stopDate, ephemeris.getStop());
    assertEquals(1, ephemeris.getSegments().size());

    final STKEphemerisSegment segment = ephemeris.getSegments().get(0);
    assertEquals(frame, segment.getFrame());
    assertEquals(MU, segment.getMu());
    assertEquals(startDate, segment.getStart());
    assertEquals(stopDate, segment.getStop());
    assertEquals(6, segment.getInterpolationSamples());
    assertEquals(CartesianDerivativesFilter.USE_PVA, segment.getAvailableDerivatives());
    assertEquals(11, segment.getCoordinates().size());

    final List<TimeStampedPVCoordinates> coordinates = segment.getCoordinates();

    assertEquals(startDate, coordinates.get(0).getDate());
    assertEquals(new Vector3D(-4.2001828159554983e+06, -3.9105939267270239e+06, -4.5819301444368772e+06),
        coordinates.get(0).getPosition());
    assertEquals(new Vector3D(5.4770282903204152e+03, -4.6296785954320931e+03, -1.0817325337227874e+03),
        coordinates.get(0).getVelocity());
    assertEquals(new Vector3D(4.2195714111001097e+00, 3.9335215635670262e+00, 4.6186527996456137e+00),
        coordinates.get(0).getAcceleration());

    assertEquals(new AbsoluteDate(2007, 01, 12, 00, 05, 00.000883, UTC), coordinates.get(5).getDate());
    assertEquals(new Vector3D(-2.3930847437297828e+06, -5.1030332553920103e+06, -4.6952642374704480e+06),
        coordinates.get(5).getPosition());
    assertEquals(new Vector3D(6.4794691416813621e+03, -3.2589590411954355e+03, 3.3269516839203544e+02),
        coordinates.get(5).getVelocity());
    assertEquals(new Vector3D(2.4098478990321057e+00, 5.1474331925129251e+00, 4.7467936751027020e+00),
        coordinates.get(5).getAcceleration());

    assertEquals(stopDate, coordinates.get(10).getDate());
    assertEquals(new Vector3D(-3.7051401425713405e+05, -5.8354092318012062e+06, -4.3842831794793038e+06),
        coordinates.get(10).getPosition());
    assertEquals(new Vector3D(6.9022331002597584e+03, -1.5830164360578574e+03, 1.7272100173740068e+03),
        coordinates.get(10).getVelocity());
    assertEquals(new Vector3D(3.7440376244610168e-01, 5.9536789668958612e+00, 4.4833066969906747e+00),
        coordinates.get(10).getAcceleration());
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} when the coordinate system is invalid.
   */
  @Test
  public void testParseInvalidCoordinateSystem() {
    final String ex = "/stk/stk_invalid_coordinate_system.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getEME2000();
    frameMapping.put(STKCoordinateSystem.J2000, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);
    assertThrows(OrekitException.class, () -> parser.parse(source));
  }

  /**
   * Tests {@link STKEphemerisFileParser#parse(DataSource)} when the coordinate system is not mapped.
   */
  @Test
  public void testParseUnmappedCoordinateSystem() {
    final String ex = "/stk/stk_02674_p.e";
    final DataSource source = new DataSource(ex, () -> getClass().getResourceAsStream(ex));

    final String satelliteId = "02674";
    final EnumMap<STKCoordinateSystem, Frame> frameMapping = new EnumMap<>(STKCoordinateSystem.class);
    final Frame frame = FramesFactory.getGCRF();
    frameMapping.put(STKCoordinateSystem.ICRF, frame);

    final STKEphemerisFileParser parser = new STKEphemerisFileParser(satelliteId, MU, UTC, frameMapping);
    final OrekitException exception = assertThrows(OrekitException.class, () -> parser.parse(source));
    assertEquals("STK coordinate system \"J2000\" has not been mapped to an Orekit frame", exception.getMessage());
  }

}
