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
 * The format of the expected model file is either:
 * <ul>
 *   <li>combined format as used by the geomag software, available from the
 *       <a href="http://www.ngdc.noaa.gov/IAGA/vmod/igrf.html">IGRF model site</a>;
 *       supports multiple epochs per file</li>
 *   <li>original format as used by the
 *       <a href="http://www.ngdc.noaa.gov/geomag/WMM/DoDWMM.shtml">WMM model site</a>.
 * </ul>
 * <p>
 * <b>Combined Format</b>
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
 * <p>
 * <b>Original WMM Format</b>
 * <pre>
 *    {epoch} {model name} {validity start}
 * {n} {m} {gnm} {hnm} {dgnm} {dhnm}
 * </pre>
 * <p>
 * Example:
 * </p>
 * <pre>
 *    2015.0            WMM-2015        12/15/2014
 *  1  0  -29438.5       0.0       10.7        0.0
 *  1  1   -1501.1    4796.2       17.9      -26.8
 * </pre>
 *
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
        final StreamTokenizer str = new StreamTokenizer(new InputStreamReader(input, "UTF-8"));

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

        if (ttype == StreamTokenizer.TT_WORD) {
            return readCombinedFormat(stream);
        } else {
            return readOriginalWMMFormat(stream);
        }
    }

    /** Read a magnetic field from combined format.
     * @param stream the stream to read the model from
     * @return magnetic field
     * @throws IOException if some read error occurs
     */
    private GeoMagneticField readCombinedFormat(final StreamTokenizer stream)
        throws IOException {
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

    /** Read a magnetic field from original WMM files.
     * @param stream the stream to read the model from
     * @return magnetic field
     * @throws IOException if some read error occurs
     */
    private GeoMagneticField readOriginalWMMFormat(final StreamTokenizer stream)
        throws IOException {

        // hard-coded values in original WMM format
        final int nMax = 12;
        final int nMaxSecVar = 12;

        // the validity start is encoded in format MM/dd/yyyy
        // use the slash as whitespace character to get separate tokens
        stream.whitespaceChars('/', '/');

        final double epoch = stream.nval;
        stream.nextToken();
        final String modelName = stream.sval;
        stream.nextToken();
        final double month = stream.nval;
        stream.nextToken();
        final double day = stream.nval;
        stream.nextToken();
        final double year = stream.nval;

        final double startYear = GeoMagneticField.getDecimalYear((int) day, (int) month, (int) year);

        final GeoMagneticField model = new GeoMagneticField(modelName, epoch, nMax, nMaxSecVar,
                                                            startYear, epoch + 5.0);

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

            done = n == nMax && m == nMax;
        } while (!done);

        // the original format closes with two delimiting lines of '9's
        stream.nextToken();
        stream.nextToken();

        return model;
    }

}
