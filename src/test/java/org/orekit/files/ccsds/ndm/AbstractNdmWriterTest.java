/* Copyright 2002-2021 CS GROUP
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

import java.io.ByteArrayInputStream;
import java.io.CharArrayWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.linear.RealMatrix;
import org.hipparchus.util.Precision;
import org.junit.Assert;
import org.junit.Before;
import org.orekit.Utils;
import org.orekit.data.DataSource;
import org.orekit.files.ccsds.definitions.BodyFacade;
import org.orekit.files.ccsds.definitions.FrameFacade;
import org.orekit.files.ccsds.ndm.adm.AttitudeEndoints;
import org.orekit.files.ccsds.ndm.adm.apm.ApmQuaternion;
import org.orekit.files.ccsds.ndm.tdm.Observation;
import org.orekit.files.ccsds.section.CommentsContainer;
import org.orekit.files.ccsds.section.Header;
import org.orekit.files.ccsds.section.Segment;
import org.orekit.files.ccsds.utils.FileFormat;
import org.orekit.files.ccsds.utils.generation.Generator;
import org.orekit.files.ccsds.utils.generation.KvnGenerator;
import org.orekit.files.ccsds.utils.generation.MessageWriter;
import org.orekit.files.ccsds.utils.generation.XmlGenerator;
import org.orekit.files.ccsds.utils.lexical.MessageParser;
import org.orekit.frames.Frame;
import org.orekit.time.AbsoluteDate;

public abstract class AbstractNdmWriterTest<H extends Header, S extends Segment<?, ?>, F extends NdmFile<H, S>> {

    @Before
    public void setUp() {
        Utils.setDataRoot("regular-data");
    }

    protected abstract MessageParser<F>       getParser();
    protected abstract MessageWriter<H, S, F> getWriter();

    protected  void doTest(final String name) {
        doTest(name, FileFormat.KVN);
        doTest(name, FileFormat.XML);
    }

    protected  void doTest(final String name, final FileFormat format) {
        try {
            final DataSource source1  = new DataSource(name, () -> getClass().getResourceAsStream(name));
            final F          original = getParser().parseMessage(source1);

            // write the parsed file back to a characters array
            final CharArrayWriter caw       = new CharArrayWriter();
            final Generator       generator = format == FileFormat.KVN ?
                                              new KvnGenerator(caw, 25, "dummy.kvn") :
                                              new XmlGenerator(caw, XmlGenerator.DEFAULT_INDENT, "dummy.xml");
            getWriter().writeMessage(generator, original);

            // reparse the written file
            final byte[]      bytes  = caw.toString().getBytes(StandardCharsets.UTF_8);
            final DataSource source2 = new DataSource(name, () -> new ByteArrayInputStream(bytes));
            final F          rebuilt = getParser().parseMessage(source2);

            checkEquals(original, rebuilt);

        } catch (IOException ioe) {
            Assert.fail(ioe.getLocalizedMessage());
        }
    }

    private void checkEquals(final F original, final F rebuilt) {
        checkContainer(original.getHeader(), rebuilt.getHeader());
        Assert.assertEquals(original.getSegments().size(), rebuilt.getSegments().size());
        for (int i = 0; i < original.getSegments().size(); ++i) {
            checkContainer(original.getSegments().get(i).getMetadata(), rebuilt.getSegments().get(i).getMetadata());
            checkContainer(original.getSegments().get(i).getData(), rebuilt.getSegments().get(i).getData());
        }
    }

    private boolean recurseCheck(final Object original, final Object rebuilt) {

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
        } else if (original instanceof CommentsContainer) {
            checkContainer(original, rebuilt);
            return true;
        } else if (original instanceof ApmQuaternion) {
            checkContainer(original, rebuilt);
            return true;
        } else if (original instanceof AttitudeEndoints) {
            checkContainer(original, rebuilt);
            return true;
        } else if (original instanceof FrameFacade) {
            checkFrameFacade((FrameFacade) original, (FrameFacade) rebuilt);
            return true;
        } else if (original instanceof BodyFacade) {
            checkBodyFacade((BodyFacade) original, (BodyFacade) rebuilt);
            return true;
        } else if (original instanceof Observation) {
            checkContainer(original, rebuilt);
            return true;
        } else if (original instanceof Frame) {
            checkFrame((Frame) original, (Frame) rebuilt);
            return true;
        } else if (original instanceof AbsoluteDate) {
            checkDate((AbsoluteDate) original, (AbsoluteDate) rebuilt);
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
        } else {
            return false;
        }
        
    }

    private void checkContainer(final Object original, final Object rebuilt) {
        Assert.assertEquals(original.getClass(), rebuilt.getClass());
        final Class<?> cls = original.getClass();
        Stream.of(cls.getMethods()).
        filter(m -> m.getName().startsWith("get")   &&
                    !m.getName().equals("getClass") &&
                    m.getParameterCount() == 0).
        forEach(getter -> {
            try {
                if (!recurseCheck(getter.invoke(original), getter.invoke(rebuilt))) {
                    Assert.fail(cls.getName() + "." + getter.getName() + "(): " +
                                " original → " + getter.invoke(original) +
                                ", rebuilt → "  + getter.invoke(rebuilt));
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                Assert.fail(e.getLocalizedMessage());
            }
        });
    }

    private void checkIntArray(final int[] original, final int[] rebuilt) {
        Assert.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assert.assertEquals(original[i], rebuilt[i]);
        }
    }

    private void checkDoubleArray(final double[] original, final double[] rebuilt) {
        Assert.assertEquals(original.length, rebuilt.length);
        for (int i = 0; i < original.length; ++i) {
            Assert.assertTrue(Precision.equalsIncludingNaN(original[i], rebuilt[i], 1));
        }
    }

    private void checkList(final List<?> original, final List<?> rebuilt) {
        Assert.assertEquals(original.size(), rebuilt.size());
        for (int i = 0; i < original.size(); ++i) {
            Assert.assertTrue(recurseCheck(original.get(i), rebuilt.get(i)));
        }
    }

    private void checkMap(final Map<?, ?> original, final Map<?, ?> rebuilt) {
        Assert.assertEquals(original.size(), rebuilt.size());
        for (final Map.Entry<?, ?> entry : original.entrySet()) {
            Assert.assertTrue(rebuilt.containsKey(entry.getKey()));
            Assert.assertTrue(recurseCheck(entry.getValue(), rebuilt.get(entry.getKey())));
        }
    }

    private void checkFrameFacade(final FrameFacade original, final FrameFacade rebuilt) {
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

    private void checkBodyFacade(final BodyFacade original, final BodyFacade rebuilt) {
        if (original.getBody() == null) {
            Assert.assertNull(rebuilt.getBody());
        } else {
            Assert.assertEquals(original.getBody().getName(),
                                rebuilt.getBody().getName());
        }
        Assert.assertEquals(original.getName(), rebuilt.getName());
    }

    private void checkDate(final AbsoluteDate original, final AbsoluteDate rebuilt) {
        Assert.assertEquals(0.0, rebuilt.durationFrom(original), 1.0e-15);
    }

    private void checkFrame(final Frame original, final Frame rebuilt) {
        Assert.assertEquals(original.getName(), rebuilt.getName());
    }

    private void checkVector3D(final Vector3D original, final Vector3D rebuilt) {
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getX(), rebuilt.getX(), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getY(), rebuilt.getY(), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getZ(), rebuilt.getZ(), 1));
    }

    private void checkQuaternion(final Quaternion original, final Quaternion rebuilt) {
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ0(), rebuilt.getQ0(), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ1(), rebuilt.getQ1(), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ2(), rebuilt.getQ2(), 1));
        Assert.assertTrue(Precision.equalsIncludingNaN(original.getQ3(), rebuilt.getQ3(), 1));
    }

    private void checkRealMatrix(final RealMatrix original, final RealMatrix rebuilt) {
        Assert.assertEquals(original.getRowDimension(), rebuilt.getRowDimension());
        Assert.assertEquals(original.getColumnDimension(), rebuilt.getColumnDimension());
        for (int i = 0; i < original.getRowDimension(); ++i) {
            for (int j = 0; j < original.getColumnDimension(); ++j) {
                Assert.assertTrue(Precision.equalsIncludingNaN(original.getEntry(i, j), rebuilt.getEntry(i, j), 1));
            }
        }
    }

    private void checkDouble(final Double original, final Double rebuilt) {
        Assert.assertTrue(Precision.equalsIncludingNaN(original.doubleValue(), rebuilt.doubleValue(), 1));
    }

}
