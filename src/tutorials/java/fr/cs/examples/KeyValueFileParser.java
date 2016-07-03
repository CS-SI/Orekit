/* Copyright 2002-2016 CS Systèmes d'Information
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
package fr.cs.examples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hipparchus.exception.DummyLocalizable;
import org.hipparchus.exception.Localizable;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;
import org.orekit.frames.Frame;
import org.orekit.frames.FramesFactory;
import org.orekit.frames.Predefined;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeComponents;
import org.orekit.time.TimeScale;

/** Simple parser for key/value files.
 * @param Key type of the parameter keys
 */
public class KeyValueFileParser<Key extends Enum<Key>> {

    /** Error message for unknown frame. */
    private static final Localizable UNKNOWN_FRAME =
        new DummyLocalizable("unknown frame {0}");

    /** Error message for not Earth frame. */
    private static final Localizable NOT_EARTH_FRAME =
        new DummyLocalizable("frame {0} is not an Earth frame");

    /** Enum type. */
    private final Class<Key> enumType;

    /** Key/scalar value map. */
    private final Map<Key, String> scalarMap;

    /** Key/array value map. */
    private final Map<Key, List<String>> arrayMap;

    /** Simple constructor.
     * @param enumType type of the parameters keys enumerate
     */
    public KeyValueFileParser(Class<Key> enumType) {
        this.enumType  = enumType;
        this.scalarMap = new HashMap<Key, String>();
        this.arrayMap  = new HashMap<Key, List<String>>();
    }

    /** Parse an input file.
     * <p>
     * The input file syntax is a set of {@code key=value} lines or
     * {@code key[i]=value} lines. Blank lines and lines starting with '#'
     * (after whitespace trimming) are silently ignored. The equal sign may
     * be surrounded by space characters. Keys must correspond to the
     * {@link Key} enumerate constants, given that matching is not case
     * sensitive and that '_' characters may appear as '.' characters in
     * the file. This means that the lines:
     * <pre>
     *   # this is the semi-major axis
     *   orbit.circular.a   = 7231582
     * </pre>
     * are perfectly right and correspond to key {@code Key#ORBIT_CIRCULAR_A} if
     * such a constant exists in the enumerate.
     * </p>
     * <p>
     * When the key[i] notation is used, all the values extracted from lines
     * with the same key will be put in a general array that will be retrieved
     * using one of the {@code getXxxArray(key)} methods. They will <em>not</em>
     * be available with the {@code getXxx(key)} methods without the {@code Array}.
     * </p>
     * @param input input stream
     * @exception IOException if input file cannot be read
     * @exception OrekitException if a line cannot be read properly
     */
    public void parseInput(final String name, final InputStream input)
        throws IOException, OrekitException {

        final Pattern        arrayPattern = Pattern.compile("([\\w\\.]+)\\s*\\[([0-9]+)\\]");
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"))) {
            int lineNumber = 0;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                ++lineNumber;
                line = line.trim();
                // we ignore blank lines and line starting with '#'
                if ((line.length() > 0) && !line.startsWith("#")) {
                    String[] fields = line.split("\\s*=\\s*");
                    if (fields.length != 2) {
                        throw new OrekitException(OrekitMessages.UNABLE_TO_PARSE_LINE_IN_FILE,
                                                  lineNumber, name, line);
                    }
                    final Matcher matcher = arrayPattern.matcher(fields[0]);
                    if (matcher.matches()) {
                        // this is a key[i]=value line
                        String canonicalized = matcher.group(1).toUpperCase().replaceAll("\\.", "_");
                        Key key = Key.valueOf(enumType, canonicalized);
                        Integer index = Integer.valueOf(matcher.group(2));
                        List<String> list = arrayMap.get(key);
                        if (list == null) {
                            list = new ArrayList<String>();
                            arrayMap.put(key, list);
                        }
                        while (index >= list.size()) {
                            // insert empty strings for missing elements
                            list.add("");
                        }
                        list.set(index, fields[1]);
                    } else {
                        // this is a key=value line
                        String canonicalized = fields[0].toUpperCase().replaceAll("\\.", "_");
                        Key key = Key.valueOf(enumType, canonicalized);
                        scalarMap.put(key, fields[1]);
                    }
                }
            }
        }

    }

    /** Check if a key is contained in the map.
     * @param key parameter key
     * @return true if the key is contained in the map
     */
    public boolean containsKey(final Key key) {
        return scalarMap.containsKey(key) || arrayMap.containsKey(key);
    }

    /** Get a raw string value from a parameters map.
     * @param key parameter key
     * @return string value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public String getString(final Key key) throws NoSuchElementException {
        final String value = scalarMap.get(key);
        if (value == null) {
            throw new NoSuchElementException(key.toString());
        }
        return value.trim();
    }

    /** Get a raw string values array from a parameters map.
     * @param key parameter key
     * @return string values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public String[] getStringArray(final Key key) throws NoSuchElementException {
        final List<String> list = arrayMap.get(key);
        if (list == null) {
            throw new NoSuchElementException(key.toString());
        }
        String[] array = new String[list.size()];
        for (int i = 0; i < array.length; ++i) {
            array[i] = list.get(i).trim();
        }
        return array;
    }

    /** Get a raw double value from a parameters map.
     * @param key parameter key
     * @return double value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public double getDouble(final Key key) throws NoSuchElementException {
        return Double.parseDouble(getString(key));
    }

    /** Get a raw double values array from a parameters map.
     * @param key parameter key
     * @return double values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public double[] getDoubleArray(final Key key) throws NoSuchElementException {
        String[] strings = getStringArray(key);
        double[] array = new double[strings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Double.parseDouble(strings[i]);
        }
        return array;
    }

    /** Get a raw int value from a parameters map.
     * @param key parameter key
     * @return int value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public int getInt(final Key key) throws NoSuchElementException {
        return Integer.parseInt(getString(key));
    }

    /** Get a raw int values array from a parameters map.
     * @param key parameter key
     * @return int values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public int[] getIntArray(final Key key) throws NoSuchElementException {
        String[] strings = getStringArray(key);
        int[] array = new int[strings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Integer.parseInt(strings[i]);
        }
        return array;
    }

    /** Get a raw boolean value from a parameters map.
     * @param key parameter key
     * @return boolean value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public boolean getBoolean(final Key key) throws NoSuchElementException {
        return Boolean.parseBoolean(getString(key));
    }

    /** Get a raw boolean values array from a parameters map.
     * @param key parameter key
     * @return boolean values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public boolean[] getBooleanArray(final Key key) throws NoSuchElementException {
        String[] strings = getStringArray(key);
        boolean[] array = new boolean[strings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = Boolean.parseBoolean(strings[i]);
        }
        return array;
    }

    /** Get an angle value from a parameters map.
     * <p>
     * The angle is considered to be in degrees in the file, it will be returned in radians
     * </p>
     * @param key parameter key
     * @return angular value corresponding to the key, in radians
     * @exception NoSuchElementException if key is not in the map
     */
    public double getAngle(final Key key) throws NoSuchElementException {
        return FastMath.toRadians(getDouble(key));
    }

    /** Get an angle values array from a parameters map.
     * @param key parameter key
     * @return angle values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public double[] getAngleArray(final Key key) throws NoSuchElementException {
        double[] array = getDoubleArray(key);
        for (int i = 0; i < array.length; ++i) {
            array[i] = FastMath.toRadians(array[i]);
        }
        return array;
    }

    /** Get a date value from a parameters map.
     * @param key parameter key
     * @param scale time scale in which the date is to be parsed
     * @return date value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public AbsoluteDate getDate(final Key key, TimeScale scale) throws NoSuchElementException {
        return new AbsoluteDate(getString(key), scale);
    }

    /** Get a date values array from a parameters map.
     * @param key parameter key
     * @param scale time scale in which the date is to be parsed
     * @return date values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public AbsoluteDate[] getDateArray(final Key key, TimeScale scale) throws NoSuchElementException {
        String[] strings = getStringArray(key);
        AbsoluteDate[] array = new AbsoluteDate[strings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = new AbsoluteDate(strings[i], scale);
        }
        return array;
    }

    /** Get a time value from a parameters map.
     * @param key parameter key
     * @return time value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public TimeComponents getTime(final Key key) throws NoSuchElementException {
        return TimeComponents.parseTime(getString(key));
    }

    /** Get a time values array from a parameters map.
     * @param key parameter key
     * @return time values array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public TimeComponents[] getTimeArray(final Key key) throws NoSuchElementException {
        String[] strings = getStringArray(key);
        TimeComponents[] array = new TimeComponents[strings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = TimeComponents.parseTime(strings[i]);
        }
        return array;
    }

    /** Get a vector value from a parameters map.
     * @param xKey parameter key for abscissa
     * @param yKey parameter key for ordinate
     * @param zKey parameter key for height
     * @param scale time scale in which the date is to be parsed
     * @return date value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public Vector3D getVector(final Key xKey, final Key yKey, final Key zKey)
        throws NoSuchElementException {
        return new Vector3D(getDouble(xKey), getDouble(yKey), getDouble(zKey));
    }

    /** Get a vector values array from a parameters map.
     * @param xKey parameter key for abscissa
     * @param yKey parameter key for ordinate
     * @param zKey parameter key for height
     * @param scale time scale in which the date is to be parsed
     * @return date value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public Vector3D[] getVectorArray(final Key xKey, final Key yKey, final Key zKey)
        throws NoSuchElementException {
        String[] xStrings = getStringArray(xKey);
        String[] yStrings = getStringArray(yKey);
        String[] zStrings = getStringArray(zKey);
        Vector3D[] array = new Vector3D[xStrings.length];
        for (int i = 0; i < array.length; ++i) {
            array[i] = new Vector3D(Double.parseDouble(xStrings[i]),
                                    Double.parseDouble(yStrings[i]),
                                    Double.parseDouble(zStrings[i]));
        }
        return array;
    }

    /** Get a strings list from a parameters map.
     * @param key parameter key
     * @param separator elements separator
     * @return strings list value corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public List<String> getStringsList(final Key key, final char separator)
        throws NoSuchElementException {
        final String value = scalarMap.get(key);
        if (value == null) {
            throw new NoSuchElementException(key.toString());
        }
        return Arrays.asList(value.trim().split("\\s*" + separator + "\\s*"));
    }

    /** Get a strings list array from a parameters map.
     * @param key parameter key
     * @param separator elements separator
     * @return strings list array corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     */
    public List<String>[] getStringsListArray(final Key key, final char separator)
        throws NoSuchElementException {
        final String[] strings = getStringArray(key);
        @SuppressWarnings("unchecked")
        final List<String>[] array = (List<String>[]) Array.newInstance(List.class, strings.length);
        for (int i = 0; i < array.length; ++i) {
            array[i] = Arrays.asList(strings[i].trim().split("\\s*" + separator + "\\s*"));
        }
        return array;
    }

    /** Get an inertial frame from a parameters map.
     * @param key parameter key
     * @return inertial frame corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     * @exception OrekitException if frame cannot be built
     */
    public Frame getInertialFrame(final Key key) throws NoSuchElementException, OrekitException {

        // get the name of the desired frame
        final String frameName = getString(key);

        // check the name against predefined frames
        for (Predefined predefined : Predefined.values()) {
            if (frameName.equals(predefined.getName())) {
                if (FramesFactory.getFrame(predefined).isPseudoInertial()) {
                    return FramesFactory.getFrame(predefined);
                } else {
                    throw new OrekitException(OrekitMessages.NON_PSEUDO_INERTIAL_FRAME,
                                              frameName);
                }
            }
        }

        // none of the frames match the name
        throw new OrekitException(UNKNOWN_FRAME, frameName);

    }

    /** Get an Earth frame from a parameters map.
     * <p>
     * We consider Earth frames are the frames with name starting with "ITRF".
     * </p>
     * @param key parameter key
     * @param parameters key/value map containing the parameters
     * @return Earth frame corresponding to the key
     * @exception NoSuchElementException if key is not in the map
     * @exception OrekitException if frame cannot be built
     */
    public Frame getEarthFrame(final Key key)
            throws NoSuchElementException, OrekitException {

        // get the name of the desired frame
        final String frameName = getString(key);

        // check the name against predefined frames
        for (Predefined predefined : Predefined.values()) {
            if (frameName.equals(predefined.getName())) {
                if (predefined.toString().startsWith("ITRF") ||
                    predefined.toString().startsWith("GTOD")) {
                    return FramesFactory.getFrame(predefined);
                } else {
                    throw new OrekitException(NOT_EARTH_FRAME, frameName);
                }
            }
        }

        // none of the frames match the name
        throw new OrekitException(UNKNOWN_FRAME, frameName);

    }

}
