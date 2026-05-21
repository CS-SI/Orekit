/* Contributed in the public domain.
 * Licensed to CS GROUP (CS) under one or more
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
/**
 *
 * This package provides classes related to generic DAF files.
 *
 * DAF (Double Precision Array File) is a binary file architecture designed to
 * store arrays of double precision arrays used by SPICE, NAIF toolkit software library.
 * The architecture forms the basis of multiple file formats used to store different
 * data related to astrodynamics, such as SPK, PCK and CK files
 * (https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/daf.html).
 *
 * DAF files provide a generic architecture onto which more specific file
 * formats are implemented. They are organized in records of fixed length (1024 bytes)
 * containing different information. The first record is called the file record,
 * and contains global metadata for the file. This is followed by an optional block
 * comprising any number of comment records. After this, the file consits of sets of
 * summary records, name records and element records. These are structured as blocks
 * of 1 summary record (which contains multiple array summaries, providing metadata
 * about each array), followed by 1 name record (comprising names for the corresponding
 * arrays whose summaries were in the previous summary record) and finally by as many
 * element records as required to store the arrays described in the corresponding summary
 * records. For a detailed description of the DAF architecture, see NAIF documentation
 * (https://naif.jpl.nasa.gov/pub/naif/toolkit_docs/C/req/daf.html).
 *
 * @author Rafael Ayala
 * @since 14.0
 *
 */
package org.orekit.files.spice.binary.daf.generic;
