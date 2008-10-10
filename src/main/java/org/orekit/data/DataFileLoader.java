/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.data;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import org.orekit.errors.OrekitException;

/** Interface for loading data files.
 * @see DataDirectoryCrawler#crawl(DataFileLoader)
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public interface DataFileLoader {

    /** Check if a file is supported.
     * @param fileName file to check
     * @return true if file name correspond to a supported file
     */
    boolean fileIsSupported(final String fileName);

    /** Load data from a stream.
     * @param input data input stream
     * @param name name of the file (or zip entry)
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     */
    void loadData(InputStream input, String name)
        throws IOException, ParseException, OrekitException;

}
