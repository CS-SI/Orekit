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
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.orekit.data.DataLoader;
import org.orekit.data.DataSource;

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
    private final List<GeoMagneticField> models;

    /** Empty constructor.
     * @since 12.0
     */
    public GeoMagneticModelLoader() {
        models = new LinkedList<>();
    }

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
        return models.isEmpty();
    }

    /** {@inheritDoc} */
    public void loadData(final InputStream input, final String name) throws IOException {
        models.addAll(new GeoMagneticModelParser().parse(new DataSource(name, () -> input)));
    }

}
