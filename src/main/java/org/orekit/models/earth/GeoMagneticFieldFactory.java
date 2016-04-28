/* Copyright 2011-2012 Space Applications Services
 * Licensed to CS Communication & Systèmes (CS) under one or more
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
package org.orekit.models.earth;

import java.util.Collection;
import java.util.SortedMap;
import java.util.TreeMap;

import org.hipparchus.util.FastMath;
import org.orekit.data.DataProvidersManager;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitMessages;

/** Factory for different {@link GeoMagneticField} models.
 * <p>
 * This is a utility class, so its constructor is private.
 * </p>
 * @author Thomas Neidhart
 */
public class GeoMagneticFieldFactory {

    /** The currently supported geomagnetic field models. */
    public enum FieldModel {
        /** World Magnetic Model. */
        WMM,
        /** International Geomagnetic Reference Field. */
        IGRF
    }

    /** Loaded IGRF models. */
    private static TreeMap<Integer, GeoMagneticField> igrfModels = null;

    /** Loaded WMM models. */
    private static TreeMap<Integer, GeoMagneticField> wmmModels = null;

    /** Private constructor.
     * <p>
     * This class is a utility class, it should neither have a public nor a
     * default constructor. This private constructor prevents the compiler from
     * generating one automatically.
     * </p>
     */
    private GeoMagneticFieldFactory() {
    }

    /** Get the {@link GeoMagneticField} for the given model type and year.
     * @param type the field model type
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year and model
     * @throws OrekitException if the models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getField(final FieldModel type, final double year)
        throws OrekitException {

        switch (type) {
            case WMM:
                return getWMM(year);
            case IGRF:
                return getIGRF(year);
            default:
                throw new OrekitException(OrekitMessages.NON_EXISTENT_GEOMAGNETIC_MODEL, type.name(), year);
        }
    }

    /** Get the IGRF model for the given year.
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @throws OrekitException
     *             if the IGRF models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getIGRF(final double year) throws OrekitException {
        synchronized (GeoMagneticFieldFactory.class) {
            if (igrfModels == null) {
                igrfModels = loadModels("^IGRF\\.COF$");
            }
            return getModel(FieldModel.IGRF, igrfModels, year);
        }
    }

    /** Get the WMM model for the given year.
     * @param year the decimal year
     * @return a {@link GeoMagneticField} for the given year
     * @throws OrekitException if the WMM models could not be loaded
     * @see GeoMagneticField#getDecimalYear(int, int, int)
     */
    public static GeoMagneticField getWMM(final double year) throws OrekitException {
        synchronized (GeoMagneticFieldFactory.class) {
            if (wmmModels == null) {
                wmmModels = loadModels("^WMM\\.COF$");
            }
            return getModel(FieldModel.WMM, wmmModels, year);
        }
    }

    /** Loads the geomagnetic model files from the given filename. The loaded
     * models are inserted in a {@link TreeMap} with their epoch as key in order
     * to retrieve them in a sorted manner.
     * @param supportedNames a regular expression for valid filenames
     * @return a {@link TreeMap} of all loaded models
     * @throws OrekitException if the models could not be loaded
     */
    private static TreeMap<Integer, GeoMagneticField> loadModels(final String supportedNames)
        throws OrekitException {

        TreeMap<Integer, GeoMagneticField> loadedModels = null;
        final GeoMagneticModelLoader loader = new GeoMagneticModelLoader();
        DataProvidersManager.getInstance().feed(supportedNames, loader);

        if (!loader.stillAcceptsData()) {
            final Collection<GeoMagneticField> models = loader.getModels();
            if (models != null) {
                loadedModels = new TreeMap<Integer, GeoMagneticField>();
                for (GeoMagneticField model : models) {
                    // round to a precision of two digits after the comma
                    final int epoch = (int) FastMath.round(model.getEpoch() * 100d);
                    loadedModels.put(epoch, model);
                }
            }
        }

        // if no models could be loaded -> throw exception
        if (loadedModels == null || loadedModels.size() == 0) {
            throw new OrekitException(OrekitMessages.UNABLE_TO_FIND_RESOURCE, supportedNames);
        }

        return loadedModels;
    }

    /** Gets a geomagnetic field model for the given year. In case the specified
     * year does not match an existing model epoch, the resulting field is
     * generated by either time-transforming an existing model using its secular
     * variation coefficients, or by linear interpolating two existing models.
     * @param type the type of the field (e.g. WMM or IGRF)
     * @param models all loaded field models, sorted by their epoch
     * @param year the epoch of the resulting field model
     * @return a {@link GeoMagneticField} model for the given year
     * @throws OrekitException if the specified year is out of range of the available models
     */
    private static GeoMagneticField getModel(final FieldModel type,
                                             final TreeMap<Integer, GeoMagneticField> models,
                                             final double year)
        throws OrekitException {

        final int epochKey = (int) (year * 100d);
        final SortedMap<Integer, GeoMagneticField> head = models.headMap(epochKey, true);

        if (head.isEmpty()) {
            throw new OrekitException(OrekitMessages.NON_EXISTENT_GEOMAGNETIC_MODEL, type.name(), year);
        }

        GeoMagneticField model = models.get(head.lastKey());
        if (model.getEpoch() < year) {
            if (model.supportsTimeTransform()) {
                model = model.transformModel(year);
            } else {
                final SortedMap<Integer, GeoMagneticField> tail = models.tailMap(epochKey, false);
                if (tail.isEmpty()) {
                    throw new OrekitException(OrekitMessages.NON_EXISTENT_GEOMAGNETIC_MODEL, type.name(), year);
                }
                final GeoMagneticField secondModel = models.get(tail.firstKey());
                if (secondModel != model) {
                    model = model.transformModel(secondModel, year);
                }
            }
        }
        return model;
    }
}
