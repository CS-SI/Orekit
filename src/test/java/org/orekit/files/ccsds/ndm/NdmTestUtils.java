/* Copyright 2002-2023 CS GROUP
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.files.ccsds.ndm;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Rotation;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.FastMath;
import org.hipparchus.util.Precision;
import org.junit.jupiter.api.Assertions;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OdMethodFacade;
import org.orekit.files.ccsds.definitions.PocMethodFacade;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndpoints;
import org.orekit.files.ccsds.ndm.adm.acm.AcmSatelliteEphemeris;
import org.orekit.files.ccsds.ndm.adm.acm.AttitudeCovariance;
import org.orekit.files.ccsds.ndm.adm.acm.AttitudeCovarianceHistory;
import org.orekit.files.ccsds.ndm.adm.acm.AttitudeState;
import org.orekit.files.ccsds.ndm.adm.acm.AttitudeStateHistory;
import org.orekit.files.ccsds.ndm.adm.aem.AemSatelliteEphemeris;
import org.orekit.files.ccsds.ndm.adm.apm.ApmQuaternion;
import org.orekit.files.ccsds.ndm.cdm.CdmRelativeMetadata;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmSatelliteEphemeris;
import org.orekit.files.ccsds.ndm.odm.ocm.OrbitCovariance;
import org.orekit.files.ccsds.ndm.odm.ocm.OrbitCovarianceHistory;
import org.orekit.files.ccsds.ndm.odm.ocm.OrbitManeuver;
import org.orekit.files.ccsds.ndm.odm.ocm.OrbitManeuverHistory;
import org.orekit.files.ccsds.ndm.odm.ocm.TrajectoryState;
import org.orekit.files.ccsds.ndm.odm.ocm.TrajectoryStateHistory;
import org.orekit.files.ccsds.ndm.odm.oem.OemSatelliteEphemeris;
import org.orekit.files.ccsds.ndm.tdm.Observation;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Section;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.AngularCoordinates;
import org.orekit.utils.PVCoordinates;
import org.orekit.utils.units.Unit;

public class NdmTestUtils {

    private static final int ULPS = 207;

    public static void checkEquals(final NdmConstituent<?, ?> original, final NdmConstituent<?, ?> rebuilt) {
        checkContainer(original.getHeader(), rebuilt.getHeader());
        Assertions.assertEquals(original.getSegments().size(), rebuilt.getSegments().size());
        for (int i = 0; i < original.getSegments().size(); ++i) {
            checkContainer(original.getSegments().get(i).getMetadata(), rebuilt.getSegments().get(i).getMetadata());
            checkContainer(original.getSegments().get(i).getData(), rebuilt.getSegments().get(i).getData());
        }
    }

    public static boolean recurseCheck(final Object original, final Object rebuilt) {

        if (original == null) {
            return rebuilt == null;
        } else if (original instanceof String    ||
                   original instanceof Boolean   ||
                   original instanceof Character ||
                   original instanceof Integer   ||
                   original instanceof Enum) {
            return original.equals(rebuilt);
        } else if (original instanceof Double) {
            checkDouble((Double) original, (Double) rebuilt);
            return true;
        } else if (original instanceof int[]) {
            checkIntArray((int[]) original, (int[]) rebuilt);
            return true;
        } else if (original instanceof double[]) {
            checkDoubleArray((double[]) original, (double[]) rebuilt);
            return true;
        } else if (original instanceof List) {
            checkList((List<?>) original, (List<?>) rebuilt);
            return true;
        } else if (original instanceof Map) {
            checkMap((Map<?, ?>) original, (Map<?, ?>) rebuilt);
            return true;
        } else if (original instanceof NdmConstituent            ||
                   original instanceof Segment                   ||
                   original instanceof Section                   ||
                   original instanceof CommentsContainer         ||
                   original instanceof ApmQuaternion             ||
                   original instanceof AttitudeEndpoints         ||
                   original instanceof OcmSatelliteEphemeris     ||
                   original instanceof OemSatelliteEphemeris     ||
                   original instanceof AemSatelliteEphemeris     ||
                   original instanceof OrbitCovarianceHistory    ||
                   original instanceof OrbitManeuverHistory      ||
                   original instanceof TrajectoryState           ||
                   original instanceof OrbitCovariance           ||
                   original instanceof OrbitManeuver             ||
                   original instanceof Observation               ||
                   original instanceof SpacecraftBodyFrame       ||
                   original instanceof PVCoordinates             ||
                   original instanceof AngularCoordinates        ||
                   original instanceof CdmRelativeMetadata       ||
                   original instanceof AttitudeStateHistory      ||
                   original instanceof AttitudeState             ||
                   original instanceof AttitudeCovarianceHistory ||
                   original instanceof AttitudeCovariance        ||
                   original instanceof AcmSatelliteEphemeris) {
            checkContainer(original, rebuilt);
            return true;
        } else if (original instanceof FrameFacade) {
            checkFrameFacade((FrameFacade) original, (FrameFacade) rebuilt);
            return true;
        } else if (original instanceof BodyFacade) {
            checkBodyFacade((BodyFacade) original, (BodyFacade) rebuilt);
            return true;
        } else if (original instanceof OdMethodFacade) {
            checkOdMethodFacade((OdMethodFacade) original, (OdMethodFacade) rebuilt);
            return true;
        } else if (original instanceof PocMethodFacade) {
            checkPocMethodFacade((PocMethodFacade) original, (PocMethodFacade) rebuilt);
            return true;
        } else if (original instanceof TrajectoryStateHistory) {
            checkOrbitStateHistory((TrajectoryStateHistory) original, (TrajectoryStateHistory) rebuilt);
            return true;
        } else if (original instanceof DataContext) {
            return true;
        } else if (original instanceof Frame) {
            checkFrame((Frame) original, (Frame) rebuilt);
            return true;
        } else if (original instanceof AbsoluteDate) {
            checkDate((AbsoluteDate) original, (AbsoluteDate) rebuilt);
            return true;
        } else if (original instanceof Unit) {
            checkUnit((Unit) original, (Unit) rebuilt);
            return true;
        } else if (original instanceof Vector3D) {
            checkVector3D((Vector3D) original, (Vector3D) rebuilt);
            return true;
        } else if (original instanceof Quaternion) {
            checkQuaternion((Quaternion) original, (Quaternion) rebuilt);
            return true;
        } else if (original instanceof RealMatrix) {
            checkRealMatrix((RealMatrix) original, (RealMatrix) rebuilt);
            return true;
        } else if (original instanceof Rotation) {
            checkRotation((Rotation) original, (Rotation) rebuilt);
            return true;
        } else {
            return false;
        }

    }

    public static void checkContainer(final Object original, final Object rebuilt) {
        Assertions.assertEquals(original.getClass(), rebuilt.getClass());
        final Class<?> cls = original.getClass();
        Stream.of(cls.getMethods()).
        filter(m -> m.getName().startsWith("get")              &&
                    !m.getName().equals("getClass")            &&
                    !m.getName().equals("getPropagator")       &&
                    !m.getName().equals("getLaunchYear")       &&
                    !m.getName().equals("getLaunchNumber")     &&
                    !m.getName().equals("getLaunchPiece")      &&
                    !m.getName().equals("getAttitudeProvider") &&
                    m.getParameterCount() == 0).
        forEach(getter -> {
            try {
                Assertions.assertTrue(recurseCheck(getter.invoke(original), getter.invoke(rebuilt)));
            } catch (InvocationTargetException e) {
                if (!((getter.getName().equals("getFrame") ||
                       getter.getName().equals("getReferenceFrame") ||
                       getter.getName().equals("getInertialFrame") ||
                       getter.getName().equals("getAngularCoordinates")) &&
                      e.getCause() instanceof OrekitException &&
                      (((OrekitException) e.getCause()).getSpecifier() == OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY ||
                       ((OrekitException) e.getCause()).getSpecifier() == OrekitMessages.CCSDS_INVALID_FRAME ||
                       ((OrekitException) e.getCause()).getSpecifier() == OrekitMessages.CCSDS_UNSUPPORTED_ELEMENT_SET_TYPE))) {
                    Assertions.fail(e.getCause().getLocalizedMessage());
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                Assertions.fail(e.getLocalizedMessage());
            }
        });
    }

    public static void checkIntArray(final int[] original, final int[] rebuilt) {
        Assertions.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assertions.assertEquals(original[i], rebuilt[i]);
        }
    }

    public static void checkDoubleArray(final double[] original, final double[] rebuilt) {
        Assertions.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assertions.assertTrue(Precision.equalsIncludingNaN(original[i], rebuilt[i], 1));
        }
    }

    public static void checkList(final List<?> original, final List<?> rebuilt) {
        Assertions.assertEquals(original.size(), rebuilt.size());
        for (int i = 0; i < original.size(); ++i) {
            Assertions.assertTrue(recurseCheck(original.get(i), rebuilt.get(i)));
        }
    }

    public static void checkMap(final Map<?, ?> original, final Map<?, ?> rebuilt) {
        Assertions.assertEquals(original.size(), rebuilt.size());
        for (final Map.Entry<?, ?> entry : original.entrySet()) {
            Assertions.assertTrue(rebuilt.containsKey(entry.getKey()));
            Assertions.assertTrue(recurseCheck(entry.getValue(), rebuilt.get(entry.getKey())));
        }
    }

    public static void checkFrameFacade(final FrameFacade original, final FrameFacade rebuilt) {
        if (original.asFrame() == null) {
            Assertions.assertNull(rebuilt.asFrame());
        } else {
            Assertions.assertEquals(original.asFrame().getName(),
                                rebuilt.asFrame().getName());
        }
        Assertions.assertEquals(original.asCelestialBodyFrame(),
                            rebuilt.asCelestialBodyFrame());
        if (original.asOrbitRelativeFrame() == null) {
            Assertions.assertNull(rebuilt.asOrbitRelativeFrame());
        } else {
            Assertions.assertEquals(original.asOrbitRelativeFrame().getLofType(),
                                rebuilt.asOrbitRelativeFrame().getLofType());
        }
        if (original.asSpacecraftBodyFrame() == null) {
            Assertions.assertNull(rebuilt.asSpacecraftBodyFrame());
        } else {
            Assertions.assertEquals(original.asSpacecraftBodyFrame().getBaseEquipment(),
                                rebuilt.asSpacecraftBodyFrame().getBaseEquipment());
            Assertions.assertEquals(original.asSpacecraftBodyFrame().getLabel(),
                                rebuilt.asSpacecraftBodyFrame().getLabel());
        }
        Assertions.assertEquals(original.getName(), rebuilt.getName());
    }

    public static void checkBodyFacade(final BodyFacade original, final BodyFacade rebuilt) {
        if (original.getBody() == null) {
            Assertions.assertNull(rebuilt.getBody());
        } else {
            Assertions.assertEquals(original.getBody().getName(),
                                rebuilt.getBody().getName());
        }
        Assertions.assertEquals(original.getName().toUpperCase(Locale.US), rebuilt.getName().toUpperCase(Locale.US));
    }

    public static void checkOdMethodFacade(final OdMethodFacade original, final OdMethodFacade rebuilt) {
        Assertions.assertEquals(original.getName(), rebuilt.getName());
        Assertions.assertEquals(original.getType(), rebuilt.getType());
        Assertions.assertEquals(original.getTool(), rebuilt.getTool());
    }

    public static void checkPocMethodFacade(final PocMethodFacade original, final PocMethodFacade rebuilt) {
        Assertions.assertEquals(original.getName(), rebuilt.getName());
        Assertions.assertEquals(original.getType(), rebuilt.getType());
    }

    public static void checkOrbitStateHistory(final TrajectoryStateHistory original, final TrajectoryStateHistory rebuilt) {
        // we don't use checkContainer here because the history getters are redundant
        // with embedded metadata and states, and because the getFrame() method
        // that would be called automatically may throw an exception
        // so we just jump down to metadata and states
        Assertions.assertTrue(recurseCheck(original.getMetadata(), rebuilt.getMetadata()));
        checkList(original.getTrajectoryStates(), rebuilt.getTrajectoryStates());
    }

    public static void checkDate(final AbsoluteDate original, final AbsoluteDate rebuilt) {
        Assertions.assertEquals(0.0, rebuilt.durationFrom(original), 4.0e-12);
    }

    public static void checkUnit(final Unit original, final Unit rebuilt) {
        Assertions.assertTrue(Precision.equals(original.getScale(), rebuilt.getScale(), 1));
        Assertions.assertTrue(rebuilt.sameDimension(original));
    }

    public static void checkFrame(final Frame original, final Frame rebuilt) {
        Assertions.assertEquals(original.getName(), rebuilt.getName());
    }

    public static void checkVector3D(final Vector3D original, final Vector3D rebuilt) {
        double eps = ULPS * FastMath.ulp(FastMath.max(1.0, original.getNorm()));
        if (!Precision.equalsIncludingNaN(original.getY(), rebuilt.getY(), eps)) {
            System.out.println("gotcha!");
        }
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getX(), rebuilt.getX(), eps));
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getY(), rebuilt.getY(), eps));
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getZ(), rebuilt.getZ(), eps));
    }

    public static void checkQuaternion(final Quaternion original, final Quaternion rebuilt) {
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getQ0(), rebuilt.getQ0(), ULPS));
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getQ1(), rebuilt.getQ1(), ULPS));
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getQ2(), rebuilt.getQ2(), ULPS));
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.getQ3(), rebuilt.getQ3(), ULPS));
    }

    public static void checkRealMatrix(final RealMatrix original, final RealMatrix rebuilt) {
        Assertions.assertEquals(original.getRowDimension(), rebuilt.getRowDimension());
        Assertions.assertEquals(original.getColumnDimension(), rebuilt.getColumnDimension());
        for (int i = 0; i < original.getRowDimension(); ++i) {
            for (int j = 0; j < original.getColumnDimension(); ++j) {
                Assertions.assertTrue(Precision.equalsIncludingNaN(original.getEntry(i, j), rebuilt.getEntry(i, j), ULPS));
            }
        }
    }

    public static void checkRotation(final Rotation original, final Rotation rebuilt) {
        Assertions.assertEquals(0.0, Rotation.distance(original, rebuilt), 1.0e-12);
    }

    public static void checkDouble(final Double original, final Double rebuilt) {
        Assertions.assertTrue(Precision.equalsIncludingNaN(original.doubleValue(), rebuilt.doubleValue(), ULPS));
    }


}
