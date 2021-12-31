/* Copyright 2002-2022 CS GROUP
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
import org.junit.Assert;
import org.orekit.data.DataContext;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.definitions.OdMethodFacade;
import org.orekit.files.ccsds.definitions.SpacecraftBodyFrame;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.ndm.adm.aem.AemSatelliteEphemeris;
import org.orekit.files.ccsds.ndm.adm.apm.ApmQuaternion;
import org.orekit.files.ccsds.ndm.odm.ocm.Covariance;
import org.orekit.files.ccsds.ndm.odm.ocm.CovarianceHistory;
import org.orekit.files.ccsds.ndm.odm.ocm.Maneuver;
import org.orekit.files.ccsds.ndm.odm.ocm.ManeuverHistory;
import org.orekit.files.ccsds.ndm.odm.ocm.OcmSatelliteEphemeris;
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

    private static final int ULPS = 3;

    public static void checkEquals(final NdmConstituent<?, ?> original, final NdmConstituent<?, ?> rebuilt) {
        checkContainer(original.getHeader(), rebuilt.getHeader());
        Assert.assertEquals(original.getSegments().size(), rebuilt.getSegments().size());
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
        } else if (original instanceof NdmConstituent        ||
                   original instanceof Segment               ||
                   original instanceof Section               ||
                   original instanceof CommentsContainer     ||
                   original instanceof ApmQuaternion         ||
                   original instanceof AttitudeEndoints      ||
                   original instanceof OcmSatelliteEphemeris ||
                   original instanceof OemSatelliteEphemeris ||
                   original instanceof AemSatelliteEphemeris ||
                   original instanceof CovarianceHistory     ||
                   original instanceof ManeuverHistory       ||
                   original instanceof TrajectoryState            ||
                   original instanceof Covariance            ||
                   original instanceof Maneuver              ||
                   original instanceof Observation           ||
                   original instanceof SpacecraftBodyFrame   ||
                   original instanceof PVCoordinates         ||
                   original instanceof AngularCoordinates) {
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
        Assert.assertEquals(original.getClass(), rebuilt.getClass());
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
                Assert.assertTrue(recurseCheck(getter.invoke(original), getter.invoke(rebuilt)));
            } catch (InvocationTargetException e) {
                if (!((getter.getName().equals("getFrame") ||
                       getter.getName().equals("getReferenceFrame") ||
                       getter.getName().equals("getInertialFrame")) &&
                      e.getCause() instanceof OrekitException &&
                      (((OrekitException) e.getCause()).getSpecifier() == OrekitMessages.NO_DATA_LOADED_FOR_CELESTIAL_BODY ||
                       ((OrekitException) e.getCause()).getSpecifier() == OrekitMessages.CCSDS_INVALID_FRAME))) {
                    Assert.fail(e.getCause().getLocalizedMessage());
                }
            } catch (IllegalAccessException | IllegalArgumentException e) {
                Assert.fail(e.getLocalizedMessage());
            }
        });
    }

    public static void checkIntArray(final int[] original, final int[] rebuilt) {
        Assert.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assert.assertEquals(original[i], rebuilt[i]);
        }
    }

    public static void checkDoubleArray(final double[] original, final double[] rebuilt) {
        Assert.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assert.assertTrue(Precision.equalsIncludingNaN(original[i], rebuilt[i], 1));
        }
    }

    public static void checkList(final List<?> original, final List<?> rebuilt) {
        Assert.assertEquals(original.size(), rebuilt.size());
        for (int i = 0; i < original.size(); ++i) {
            Assert.assertTrue(recurseCheck(original.get(i), rebuilt.get(i)));
        }
    }

    public static void checkMap(final Map<?, ?> original, final Map<?, ?> rebuilt) {
        Assert.assertEquals(original.size(), rebuilt.size());
        for (final Map.Entry<?, ?> entry : original.entrySet()) {
            Assert.assertTrue(rebuilt.containsKey(entry.getKey()));
            Assert.assertTrue(recurseCheck(entry.getValue(), rebuilt.get(entry.getKey())));
        }
    }

    public static void checkFrameFacade(final FrameFacade original, final FrameFacade rebuilt) {
        if (original.asFrame() == null) {
            Assert.assertNull(rebuilt.asFrame());
        } else {
            Assert.assertEquals(original.asFrame().getName(),
                                rebuilt.asFrame().getName());
        }
        Assert.assertEquals(original.asCelestialBodyFrame(),
                            rebuilt.asCelestialBodyFrame());
        if (original.asOrbitRelativeFrame() == null) {
            Assert.assertNull(rebuilt.asOrbitRelativeFrame());
        } else {
            Assert.assertEquals(original.asOrbitRelativeFrame().getLofType(),
                                rebuilt.asOrbitRelativeFrame().getLofType());
        }
        if (original.asSpacecraftBodyFrame() == null) {
            Assert.assertNull(rebuilt.asSpacecraftBodyFrame());
        } else {
            Assert.assertEquals(original.asSpacecraftBodyFrame().getBaseEquipment(),
                                rebuilt.asSpacecraftBodyFrame().getBaseEquipment());
            Assert.assertEquals(original.asSpacecraftBodyFrame().getLabel(),
                                rebuilt.asSpacecraftBodyFrame().getLabel());
        }
        Assert.assertEquals(original.getName(), rebuilt.getName());
    }

    public static void checkBodyFacade(final BodyFacade original, final BodyFacade rebuilt) {
        if (original.getBody() == null) {
            Assert.assertNull(rebuilt.getBody());
        } else {
            Assert.assertEquals(original.getBody().getName(),
                                rebuilt.getBody().getName());
        }
        Assert.assertEquals(original.getName().toUpperCase(Locale.US), rebuilt.getName().toUpperCase(Locale.US));
    }

    public static void checkOdMethodFacade(final OdMethodFacade original, final OdMethodFacade rebuilt) {
        Assert.assertEquals(original.getName(), rebuilt.getName());
        Assert.assertEquals(original.getType(), rebuilt.getType());
        Assert.assertEquals(original.getTool(), rebuilt.getTool());
    }

    public static void checkOrbitStateHistory(final TrajectoryStateHistory original, final TrajectoryStateHistory rebuilt) {
        // we don't use checkContainer here because the history getters are redundant
        // with embedded metadata and states, and because the getFrame() method
        // that would be called automatically may trhow an exception
        // so we just jump down to metadata and states
        Assert.assertTrue(recurseCheck(original.getMetadata(), rebuilt.getMetadata()));
        checkList(original.getTrajectoryStates(), rebuilt.getTrajectoryStates());
    }

    public static void checkDate(final AbsoluteDate original, final AbsoluteDate rebuilt) {
        Assert.assertEquals(0.0, rebuilt.durationFrom(original), 1.0e-14);
    }

    public static void checkUnit(final Unit original, final Unit rebuilt) {
        Assert.assertTrue(Precision.equals(original.getScale(), rebuilt.getScale(), 1));
        Assert.assertTrue(rebuilt.sameDimension(original));
    }

    public static void checkFrame(final Frame original, final Frame rebuilt) {
        Assert.assertEquals(original.getName(), rebuilt.getName());
    }

    public static void checkVector3D(final Vector3D original, final Vector3D rebuilt) {
        double eps = ULPS * FastMath.ulp(original.getNorm());
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getX(), rebuilt.getX(), eps));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getY(), rebuilt.getY(), eps));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getZ(), rebuilt.getZ(), eps));
    }

    public static void checkQuaternion(final Quaternion original, final Quaternion rebuilt) {
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ0(), rebuilt.getQ0(), ULPS));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ1(), rebuilt.getQ1(), ULPS));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ2(), rebuilt.getQ2(), ULPS));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ3(), rebuilt.getQ3(), ULPS));
    }

    public static void checkRealMatrix(final RealMatrix original, final RealMatrix rebuilt) {
        Assert.assertEquals(original.getRowDimension(), rebuilt.getRowDimension());
        Assert.assertEquals(original.getColumnDimension(), rebuilt.getColumnDimension());
        for (int i = 0; i < original.getRowDimension(); ++i) {
            for (int j = 0; j < original.getColumnDimension(); ++j) {
                Assert.assertTrue(Precision.equalsIncludingNaN(original.getEntry(i, j), rebuilt.getEntry(i, j), ULPS));
            }
        }
    }

    public static void checkRotation(final Rotation original, final Rotation rebuilt) {
        Assert.assertEquals(0.0, Rotation.distance(original, rebuilt), 1.0e-12);
    }

    public static void checkDouble(final Double original, final Double rebuilt) {
        Assert.assertTrue(Precision.equalsIncludingNaN(original.doubleValue(), rebuilt.doubleValue(), ULPS));
    }

}
