/* Copyright 2002-2023 CS GROUP
 * Licensed to CS GROUP (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.files.ccsds.ndm.odm.ocm;

import java.io.StringReader;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.orekit.Utils;
import org.orekit.attitudes.FrameAlignedProvider;
import org.orekit.bodies.OneAxisEllipsoid;
import org.orekit.data.DataContext;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.TimeSystem;
import org.orekit.files.ccsds.ndm.ParserBuilder;
import org.orekit.files.ccsds.ndm.WriterBuilder;
import org.orekit.files.ccsds.ndm.odm.OdmHeader;
import org.orekit.files.ccsds.ndm.odm.oem.InterpolationMethod;
import org.orekit.files.ccsds.ndm.odm.oem.Oem;
import org.orekit.files.ccsds.ndm.odm.oem.OemParser;
import org.orekit.files.ccsds.ndm.odm.oem.OemSatelliteEphemeris;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.utils.Constants;
import org.orekit.utils.IERSConventions;
import org.orekit.utils.TimeStampedPVCoordinates;

/**
 * Check {@link StreamingOcmWriter}.
 *
 * @author Evan Ward
 */
public class StreamingOcmWriterTest {

    private OneAxisEllipsoid earth;

    /** Set Orekit data. */
    @BeforeEach
    public void setUp() {
        Utils.setDataRoot("regular-data");
        earth = new OneAxisEllipsoid(Constants.WGS84_EARTH_EQUATORIAL_RADIUS,
                                     Constants.WGS84_EARTH_FLATTENING,
                                     FramesFactory.getITRF(IERSConventions.IERS_2010, true));
    }

    @AfterEach
    public void tearDown() {
        earth = null;
    }

    @Test
    public void testWriteOcmEcf() {
        // setup
        // beware, we read on OEM in inertial frame and transform it into an OCM in ITRF frame
        String path = "/ccsds/odm/oem/OEMExample5.txt";
        DataSource source = new DataSource(path, () -> getClass().getResourceAsStream(path));
        final OemParser oemParser = new ParserBuilder().
                                    withMu(Constants.EIGEN5C_EARTH_MU).
                                    buildOemParser();
        final Oem original = oemParser.parse(source);
        final OemSatelliteEphemeris originalEphem =
                original.getSatellites().values().iterator().next();
        final Frame frame = originalEphem.getSegments().get(0).getInertialFrame();
        final BoundedPropagator propagator = originalEphem.getPropagator(new FrameAlignedProvider(frame));
        StringBuilder buffer = new StringBuilder();
        OdmHeader header = original.getHeader();
        OcmMetadata metadata = new OcmMetadata(DataContext.getDefault());
        metadata.setTimeSystem(TimeSystem.UTC);
        metadata.setEpochT0(original.getSegments().get(0).getStart());
        TrajectoryStateHistoryMetadata trajectoryMetadata = new TrajectoryStateHistoryMetadata(metadata.getEpochT0(),
                                                                                               DataContext.getDefault());
        trajectoryMetadata.setTrajReferenceFrame(FrameFacade.map(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        trajectoryMetadata.setInterpolationMethod(InterpolationMethod.LAGRANGE);
        trajectoryMetadata.setInterpolationDegree(2);

        // action
        StreamingOcmWriter writer = new StreamingOcmWriter(
                new KvnGenerator(buffer, OcmWriter.KVN_PADDING_WIDTH, "out",
                                 Constants.JULIAN_DAY, 0),
                new WriterBuilder().buildOcmWriter(),
                header,
                metadata,
                trajectoryMetadata);
        propagator.setStepHandler(30 * 60, writer.newBlock());
        propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

        // verify
        String actualText = buffer.toString();
        String expectedPath = "/ccsds/odm/ocm/OCMExample5ITRF.txt";
        final OcmParser ocmParser = new ParserBuilder().buildOcmParser();
        Ocm expected = ocmParser.parse(new DataSource(expectedPath, () -> getClass().getResourceAsStream(expectedPath)));
        Ocm actual = ocmParser.parse(new DataSource("mem", () -> new StringReader(actualText)));
        partialCompareOcms(expected, actual);

    }

    @Test
    public void testWriteOcmGeodetic() {
        // setup
        // beware, we read on OEM in inertial frame and transform it into an OCM in geodetic coordinates
        String path = "/ccsds/odm/oem/OEMExample5.txt";
        DataSource source = new DataSource(path, () -> getClass().getResourceAsStream(path));
        final OemParser oemParser = new ParserBuilder().
                                    withMu(Constants.EIGEN5C_EARTH_MU).
                                    buildOemParser();
        final Oem original = oemParser.parse(source);
        final OemSatelliteEphemeris originalEphem =
                original.getSatellites().values().iterator().next();
        final Frame frame = originalEphem.getSegments().get(0).getInertialFrame();
        final BoundedPropagator propagator = originalEphem.getPropagator(new FrameAlignedProvider(frame));
        StringBuilder buffer = new StringBuilder();
        OdmHeader header = original.getHeader();
        OcmMetadata metadata = new OcmMetadata(DataContext.getDefault());
        metadata.setTimeSystem(TimeSystem.UTC);
        metadata.setEpochT0(original.getSegments().get(0).getStart());
        TrajectoryStateHistoryMetadata trajectoryMetadata = new TrajectoryStateHistoryMetadata(metadata.getEpochT0(),
                                                                                               DataContext.getDefault());
        trajectoryMetadata.setTrajReferenceFrame(FrameFacade.map(FramesFactory.getITRF(IERSConventions.IERS_2010, true)));
        trajectoryMetadata.setInterpolationMethod(InterpolationMethod.LAGRANGE);
        trajectoryMetadata.setInterpolationDegree(2);
        trajectoryMetadata.setTrajType(OrbitElementsType.GEODETIC);

        // action
        StreamingOcmWriter writer = new StreamingOcmWriter(
                new KvnGenerator(buffer, OcmWriter.KVN_PADDING_WIDTH, "out",
                                 Constants.JULIAN_DAY, 0),
                new WriterBuilder().
                withEquatorialRadius(earth.getEquatorialRadius()).
                withFlattening(earth.getFlattening()).
                buildOcmWriter(),
                header,
                metadata,
                trajectoryMetadata);
        propagator.setStepHandler(30 * 60, writer.newBlock());
        propagator.propagate(propagator.getMinDate(), propagator.getMaxDate());

        // verify
        String actualText = buffer.toString();
        String expectedPath = "/ccsds/odm/ocm/OCMExample5Geodetic.txt";
        final OcmParser ocmParser = new ParserBuilder().buildOcmParser();
        Ocm expected = ocmParser.parse(new DataSource(expectedPath, () -> getClass().getResourceAsStream(expectedPath)));
        Ocm actual = ocmParser.parse(new DataSource("mem", () -> new StringReader(actualText)));
        partialCompareOcms(expected, actual);

    }

    private void partialCompareOcmEphemerisBlocks(TrajectoryStateHistory block1, TrajectoryStateHistory block2) {
        partialCompareOcmEphemerisBlocksMetadata(block1.getMetadata(), block2.getMetadata());
        Assertions.assertEquals(0.0, block1.getStart().durationFrom(block2.getStart()), 1.0e-12);
        Assertions.assertEquals(0.0, block1.getStop().durationFrom(block2.getStop()), 1.0e-12);
        Assertions.assertEquals(block1.getMetadata().getInterpolationDegree(), block2.getMetadata().getInterpolationDegree());
        Assertions.assertEquals(block1.getMetadata().getInterpolationMethod(), block2.getMetadata().getInterpolationMethod());
        Assertions.assertEquals(block1.getTrajectoryStates().size(), block2.getTrajectoryStates().size());
        for (int i = 0; i < block1.getTrajectoryStates().size(); i++) {
            TrajectoryState c1 = block1.getTrajectoryStates().get(i);
            TrajectoryState c2 = block2.getTrajectoryStates().get(i);
            Assertions.assertEquals(0.0, c1.getDate().durationFrom(c2.getDate()), 1.0e-12);
            Assertions.assertEquals(c1.getType(), c2.getType());
            Assertions.assertEquals(c1.getElements().length, c2.getElements().length);
            TimeStampedPVCoordinates pv1 = c1.getType().toCartesian(c1.getDate(), c1.getElements(), earth, block1.getMu());
            TimeStampedPVCoordinates pv2 = c2.getType().toCartesian(c2.getDate(), c2.getElements(), earth, block2.getMu());
            pv2 = block2.getFrame().getTransformTo(block1.getFrame(), pv2.getDate()).transformPVCoordinates(pv2);
            Assertions.assertEquals(0.0, Vector3D.distance(pv1.getPosition(), pv2.getPosition()), 5.0e-9);
            if (c1.getElements().length > 3) {
                Assertions.assertEquals(0.0, Vector3D.distance(pv1.getVelocity(), pv2.getVelocity()), 1.0e-10);
            }
        }
    }

    private void partialCompareOcmEphemerisBlocksMetadata(TrajectoryStateHistoryMetadata meta1, TrajectoryStateHistoryMetadata meta2) {
        Assertions.assertEquals(meta1.getTrajID(),                                 meta2.getTrajID());
        Assertions.assertEquals(meta1.getTrajPrevID(),                             meta2.getTrajPrevID());
        Assertions.assertEquals(meta1.getTrajNextID(),                             meta2.getTrajNextID());
        Assertions.assertEquals(meta1.getTrajBasis(),                              meta2.getTrajBasis());
        Assertions.assertEquals(meta1.getTrajBasisID(),                            meta2.getTrajBasisID());
        Assertions.assertEquals(meta1.getInterpolationMethod(),                    meta2.getInterpolationMethod());
        Assertions.assertEquals(meta1.getInterpolationDegree(),                    meta2.getInterpolationDegree());
        Assertions.assertEquals(meta1.getPropagator(),                             meta2.getPropagator());
        Assertions.assertEquals(meta1.getCenter().getName(),                       meta2.getCenter().getName());
        // we intentionally don't check trajectory reference frame
        Assertions.assertEquals(meta1.getTrajFrameEpoch(),                         meta2.getTrajFrameEpoch());
        // we intentionally don't check start and stop times
        Assertions.assertEquals(meta1.getOrbRevNum(),                              meta2.getOrbRevNum());
        Assertions.assertEquals(meta1.getOrbRevNumBasis(),                         meta2.getOrbRevNumBasis());
        Assertions.assertEquals(meta1.getOrbAveraging(),                           meta2.getOrbAveraging());
        Assertions.assertEquals(meta1.getTrajType(),                               meta2.getTrajType());
        if (meta1.getTrajUnits() == null) {
            Assertions.assertNull(meta2.getTrajUnits());
        } else {
            Assertions.assertEquals(meta1.getTrajUnits().size(), meta2.getTrajUnits().size());
            for (int i = 0; i < meta1.getTrajUnits().size(); ++i) {
                Assertions.assertEquals(meta1.getTrajUnits().get(i), meta2.getTrajUnits().get(i));
            }
        }
    }

    void partialCompareOcms(Ocm file1, Ocm file2) {
        Assertions.assertEquals(file1.getHeader().getOriginator(), file2.getHeader().getOriginator());
        Assertions.assertEquals(file1.getSegments().get(0).getData().getTrajectoryBlocks().size(),
                                file2.getSegments().get(0).getData().getTrajectoryBlocks().size());
        for (int i = 0; i < file1.getSegments().get(0).getData().getTrajectoryBlocks().size(); i++) {
            partialCompareOcmEphemerisBlocks(file1.getSegments().get(0).getData().getTrajectoryBlocks().get(i),
                                             file2.getSegments().get(0).getData().getTrajectoryBlocks().get(i));
        }
    }


}
