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
package org.orekit.iers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.orekit.errors.OrekitException;



/** Base class for IERS files crawlers.
 * @see IERSDirectoryCrawler#crawl(IERSFileCrawler)
 * @author Luc Maisonobe
 * @version $Revision:1665 $ $Date:2008-06-11 12:12:59 +0200 (mer., 11 juin 2008) $
 */
public abstract class IERSFileCrawler {

    /** Current file. */
    private File currentFile;

    /** File name pattern. */
    private final Pattern supportedFilesPattern;

    /** Simple constructor.
     * @param supportedFilesPattern file name pattern for supported files
     */
    protected IERSFileCrawler(final String supportedFilesPattern) {
        this.supportedFilesPattern = Pattern.compile(supportedFilesPattern);
    }

    /** Get the current file.
     * @return current file
     */
    protected File getFile() {
        return currentFile;
    }

    /** Check if a file is supported.
     * @param fileName file to check
     * @return true if file name correspond to a supported file
     */
    public boolean fileIsSupported(final String fileName) {
        return supportedFilesPattern.matcher(fileName).matches();
    }

    /** Visit a file.
     * @param file file to visit
     * @exception OrekitException if some data is missing, can't be read
     * or if some loader specific error occurs
     */
    public void visit(final File file)
        throws OrekitException {
        BufferedReader reader = null;
        try {
            this.currentFile = file;
            InputStream is = new FileInputStream(file);
            if (file.getName().endsWith(".gz")) {
                // add the decompression filter
                is = new GZIPInputStream(is);
            }
            reader = new BufferedReader(new InputStreamReader(is));
            visit(reader);
            reader.close();
        } catch (IOException ioe) {
            throw new OrekitException(ioe.getMessage(), ioe);
        } catch (ParseException pe) {
            throw new OrekitException(pe.getMessage(), pe);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ioe) {
                    throw new OrekitException(ioe.getMessage(), ioe);
                }
            }
        }
    }

    /** Visit a file from a reader.
     * @param reader data stream reader
     * @exception IOException if data can't be read
     * @exception ParseException if data can't be parsed
     * @exception OrekitException if some data is missing
     * or if some loader specific error occurs
     */
    protected abstract void visit(BufferedReader reader)
        throws IOException, ParseException, OrekitException;

}
