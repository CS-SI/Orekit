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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.text.ParseException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.orekit.data.DataLoader;

/** Loads geomagnetic field models from a given input stream. A stream may contain multiple
 * models, the loader reads all available models in consecutive order.
 * <p>
 * The format of the expected model file is the following:
 * </p>
 * <pre>
 *     {model name} {epoch} {nMax} {nMaxSec} {nMax3} {validity start} {validity end} {minAlt} {maxAlt} {model name} {line number}
 * {n} {m} {gnm} {hnm} {dgnm} {dhnm} {model name} {line number}
 * </pre>
 * <p>
 * Example:
 * </p>
 * <pre>
 *    WMM2010  2010.00 12 12  0 2010.00 2015.00   -1.0  600.0          WMM2010   0
 * 1  0  -29496.6       0.0      11.6       0.0                        WMM2010   1
 * 1  1   -1586.3    4944.4      16.5     -25.9                        WMM2010   2
 * </pre>
 * @author Thomas Neidhart
 */
public class GeoMagneticModelLoader implements DataLoader {

    /** The loaded models. */
    private List<GeoMagneticField> models = new LinkedList<GeoMagneticField>();

    /** Returns a {@link Collection} of the {@link GeoMagneticField} models that
     * have been successfully loaded. The {@link Collection} is in
     * insertion-order, thus it may not be sorted in order of the model epoch.
     * @return a {@link Collection} of {@link GeoMagneticField} models
     */
    public Collection<GeoMagneticField> getModels() {
        return models;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return models == null || models.isEmpty();
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name)
        throws IOException, ParseException {

        // open data file and parse values
        final StreamTokenizer str = new StreamTokenizer(new InputStreamReader(input));

        while (true) {
            final GeoMagneticField model = readModel(str);
            if (model != null) {
                models.add(model);
            } else {
                break;
            }
        }
    }

    /** Read the model from the given {@link StreamTokenizer}.
     * @param stream the stream to read the model from
     * @return the parsed geomagnetic field model
     * @throws IOException if an I/O error occurs
     */
    private GeoMagneticField readModel(final StreamTokenizer stream) throws IOException {

        // check whether there is another model available in the stream
        final int ttype = stream.nextToken();
        if (ttype == StreamTokenizer.TT_EOF) {
            return null;
        }

        final String modelName = stream.sval;
        stream.nextToken();
        final double epoch = stream.nval;
        stream.nextToken();
        final int nMax = (int) stream.nval;
        stream.nextToken();
        final int nMaxSecVar = (int) stream.nval;

        // ignored
        stream.nextToken();

        stream.nextToken();
        final double startYear = stream.nval;

        stream.nextToken();
        final double endYear = stream.nval;

        final GeoMagneticField model = new GeoMagneticField(modelName, epoch, nMax, nMaxSecVar,
                                                            startYear, endYear);

        // the rest is ignored
        stream.nextToken();
        @SuppressWarnings("unused")
        final double altmin = stream.nval;

        stream.nextToken();
        @SuppressWarnings("unused")
        final double altmax = stream.nval;

        // the min/max altitude values are ignored by now

        stream.nextToken();
        stream.nextToken();

        // loop to get model data from file
        boolean done = false;
        int n;
        int m;

        do {
            stream.nextToken();
            n = (int) stream.nval;
            stream.nextToken();
            m = (int) stream.nval;

            stream.nextToken();
            final double gnm = stream.nval;
            stream.nextToken();
            final double hnm = stream.nval;
            stream.nextToken();
            final double dgnm = stream.nval;
            stream.nextToken();
            final double dhnm = stream.nval;

            model.setMainFieldCoefficients(n, m, gnm, hnm);
            if (n <= nMaxSecVar && m <= nMaxSecVar) {
                model.setSecularVariationCoefficients(n, m, dgnm, dhnm);
            }

            stream.nextToken();
            stream.nextToken();

            done = n == nMax && m == nMax;
        } while (!done);

        return model;
    }
}
