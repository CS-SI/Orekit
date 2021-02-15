<!--- Copyright 2002-2021 CS GROUP---
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
    http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
-->

# CCSDS

The `org.orekit.files.ccsds` package provides classes to handle parsing
and writing CCSDS messages.

## Users point of view

### Organization

The package is organized in hierarchical sub-packages that reflect the sections
hierarchy from CCSDS messages, plus some utility sub-packages. The following class
diagram depicts this static organization.

![structure class diagram](../images/design/ccsds-structure-class-diagram.png)

The `org.orekit.files.ccsds.section` sub-package defines the generic sections
found in all CCSDS messages: `Header`, `Metadata` and `Data`. All extends the
Orekit-specific `Section` interface that is used for checks at the end of parsing.
`Metadata` and `Data` are gathered together in a `Segment` structure.

The `org.orekit.files.ccsds.ndm` sub-package defines a single top-level abstract
class `NDMFile`, which stands for Navigation Data Message. All CCDSD messages extend
this top-level abstract class. `NDMfile` is a container for one `Header` and one or
more `Segment` objects, depending on the file type (for example `OPMFile` only contains
one segment whereas `OEMFile` may contain several segments).

There are as many sub-packages as there are CCSDS message types, with
intermediate sub-packages for each officialy published recommendation:
`org.orekit.files.ccsds.ndm.adm.apm`, `org.orekit.files.ccsds.ndm.adm.aem`,
`org.orekit.files.ccsds.ndm.odm.opm`, `org.orekit.files.ccsds.ndm.odm.oem`,
`org.orekit.files.ccsds.ndm.odm.omm`, `org.orekit.files.ccsds.ndm.odm.ocm`,
and `org.orekit.files.ccsds.ndm.tdm`. Each contain the logical structures
that correspond to the message type, among which at least one `##MFile`
class that represents a complete message/file. As some data are common to
several types, there may be some intermediate classes in order to avoid
code duplication. These classes are implementation details and not displayed
in the previous class diagram. If the message type has logical blocks (like state
vector block, Keplerian elements block, maneuvers block in OPM), then
there is one dedicated class for each logical block.

The top-level file also contains some Orekit-specific data that are mandatory
for building some objects but is not present in the CCSDS messages. This
includes for example IERS conventions, data context, and gravitational
coefficient for ODM files as it is sometimes optional in these messages.

This organization has been introduced with Orekit 11.0. Before that, the CCSDS
hierarchy with header, segment, metadata and data was not reproduced in the API
but a flat structure was used.

This organization implies that users wishing to access raw internal entries must
walk through the hierarchy. For message types that allow only one segment, there
are shortcuts to use `file.getMetadata()` and `file.getData()` instead of
`file.getSegments().get(0).getMetadata()` and `file.getSegments().get(0).getData()`
respectively. Where it is relevant, other shortcuts are provided to access
Orekit-compatible objects as shown in the following code snippet:

    OPMFile         opm       = ...;
    AbsoluteDate    fileDate  = opm.getHeader().getCreationDate();
    Vector3D        dV        = opm.getManeuver(0).getdV();
    SpacecraftState state     = opm.generateSpacecraftState();
    // getting orbit date the hard way:
    AbsoluteDate    orbitDate = opm.getSegments().get(0).get(Data).getStateVectorBlock().getEpoch();

Message files can be obtained by parsing an existing file or by using
the setters to create it from scratch, bottom up starting from the
raw elements and building up through logical blocks, data, metadata,
segments, header and finally file.

### Parsing

Parsing a text message to build some kind of `NDMFile` object is performed
by setting up a parser. Each message type has its own parser. Once created,
its `parseMessage` method is called with a data source. It will return the
parsed file as a hierarchical container as depicted in the previous
section.

The Orekit-specific data that are mandatory for building some objects but are
not present in the CCSDS messages are set up when building the parser. This
includes for example IERS conventions, data context, and gravitational
coefficient for ODM files as it is sometimes optional in these messages.
One change introduced in Orekit 11.0 is that the progressive set up of
parser using the fluent API (methods `withXxx()`) has been removed. Now the
few required parameters are all set at once in the constructor. Another change
is that the parsers are mutable objects that gather the data during the parsing.
They can therefore not be used in multi-threaded environment. The recommended way
to use parsers is to either dedicate one parser for each message and drop it
afterwards, or to use a single-thread loop.

Parsers automatically recognize if the file is in Key-Value Notation (KVN) or in
eXtended Markup Language (XML) format and adapt accordingly. This is
transparent for users and works with all CCSDS message types. The data to
be parsed is provided using a `DataSource` object, which combines a name
and a stream opener and can be built directly from these elements, from a file name,
or from a `File` instance. The `DataSource` object delays
the real opening of the file until the `parseMessage` method is called and
takes care to close it properly after parsing, even if parsing is interrupted
due to some parse error.

The `OEMParser` and `OCMParser` have an additional feature: they also implement
the generic `EphemerisFileParser` interface, so they can be used in a more
general way when ephemerides can be read from various formats (CCSDS, CPF, SP3).
The `EphemerisFileParser` interface defines a `parse(dataSource)` method that
is similar to the CCSDS-specific `parseMessage(dataSource)` method.

As the parsers are parameterized with the type of the parsed file, the `parseMessage`
and `parse` methods in all parsers already have the specific type, there is no need
to cast the returned value.

The following code snippet shows how to parse an oem file, in this case using a
file name to create the data source:

    OEMParser  parser = new OEMParser(conventions, simpleEOP, dataContext,
                                      missionReferenceDate, mu, defaultInterpolationDegree);
    OEMFile    oem    = parser.parseMessage(new DataSource(fileName));

### Writing

Writing a CCSDS message is done by using a specific writer class for the message
type and using a low level generator corresponding to the desired file format,
`KVNGenerator` for Key-Value Notation or `XMLGenerator` for eXtended Markup Language.

Ephemeris-type messages (AEM, OEM and OCM) implement the generic ephemeris writer
interfaces (`AttitudeEphemerisFileWriter` and `EphemerisFileWriter`) in addition
to the CCSDS-specific API, so they can be used in a more general way when ephemerides
data was built from non-CCSDS data. The generic `write` methods in these interfaces
take as arguments objects that implement the generic
`AttitudeEphemerisFile.AttitudeEphemerisSegment` and `EphemerisFile.EphemerisSegment`
interfaces. As these interfaces do not provide access to header and metadata informations
that CCSDS writers need, these informations must be provided beforehand to the
writers. This is done by providing directly the header and a metadata template in
the constructor of the writer. Of course, non-CCSDS writers would use different
strategies to get their specific metadata. The metadata provided is only a template that
is incomplete: the frame, start time and stop time will be filled later on when
the data to be written is available, as they will change for each segment. The
argument used as the template is not modified when building a writer, its content
is copied in an internal object that is modified by adding the proper frame and
time data when each segment is created.

Ephemeris-type messages can also be used in a streaming way (with specific
`Streaming##MWriter` classes) if the ephemeris data must be written as it is produced
on-the-fly by a propagator. These specific writers provide a `newSegment()` method that
returns a fixed step handler to register to the propagator. If ephemerides must be split
into different segments, in order to prevent interpolation between two time ranges
separated by a discrete event like a maneuver, then a new step handler must be retrieved
using the `newSegment()` method at discrete event time and a new propagator must be used.
All segments will be gathered properly in the generated CCSDS file. Using the same
propagator and same event handler would not work as expected. The propagator would run
just fine through the discrete event that would reset the state, but the ephemeris would
not be aware of the change and would just continue the same segment. Upon reading the
file produces this way, the reader would not be aware that interpolation should not be
used around this maneuver as the event would not appear in the file.

TODO: describe CCSDS-specific API

## Developers point of view

This section describes the design of the CCSDS framework. It is an implementation
detail and is useful only for Orekit developers or people wishing to extend it,
perhaps by adding support for new messages types. It is not required to simply
parse or write CCSDS messages.

### Parsing

The first level of parsing is lexical analysis. Its aim is to read the
stream of characters from the data source and to generate a stream of
`ParseToken`. Two different lexical analyzers are provided: `KVNLexicalAnalyzer`
for Key-Value Notation and `XMLLexicalAnalyzer` for eXtended Markup Language.
The `LexicalAnalyzerSelector` utility class selects one or the other of these lexical
analyzers depending on the first few bytes read from the data source. If the
start of the XML declaration ("<?xml ...>") which is mandatory in all XML documents
is found, then `XMLLexicalAnalyzer` is selected, otherwise `KVNLexicalAnalyzer`
is selected. Detection works for UCS-4, UTF-16 and UTF-8 encodings, with or
without a Byte Order Mark, and regardless of endianness. After the first few bytes
allowing selection have been read, the characters stream is reset to beginning so
the selected lewical analyzer will see these characters again. Once the lexical
analyzer has been created, the message parser registers itself to this analyzer by calling
its `accept` method, and wait for the lexical analyzer to call it back for processing
the tokens it will generate from the characters stream. This is akin to the visitor
design pattern with the parser visiting the tokens as they are produced by the lexical
analyzer.

The following class diagram presents the static structure of lexical analysis:

![parsing class diagram](../images/design/ccsds-lexical-class-diagram.png)

The dynamic view of lexical analysis is depicted in the following sequence diagram:
![general parsing sequence diagram diagram](../images/design/ccsds-lexical-analysis-sequence-diagram.png)

The second level of parsing is message parsing is syntax analysis. Its aim is
to read the stream of `ParseToken` objects and to progressively build the CCSDS message
from them. Syntax analysis of primitive entries like `EPOCH_TZERO = 1998-12-18T14:28:15.1172`
in KVN or `<EPOCH_TZERO>1998-12-18T00:00:00.0000</EPOCH_TZERO>` in XML is independent
of the file format: in both lexical analyzers will generate a `ParseToken` with type set
to `TokenType.ENTRY`, name set to `EPOCH_TZERO` and content set to `1998-12-18T00:00:00.0000`.
This token will be passed to the message parser for processing and the parse may ignore
that the token was extract from a KVN or an XML file. This simplifies a lot parsing of both
formats and avoids code duplication. This is unfortunately not true for higher level structures
like header, segments, metadata, data or logical blocks. For all these cases, the parser must
know if the file is in Key-Value Notation or in eXtended Markup Language, so the lexical
analyzer starts parsing by calling the parser `reset` method with the file format as an
argument, so the parser knows how to handle the higher level structures.

CCSDS messages are complex, with a lot of sub-structures and we want to parse several types
(APM, AEM, OPM, OEM, OMM, OCM and TDM as of version 11.0). There are hundreds of keys to
manage (i.e. a lot of different names a `ParseToken` can have). Prior to version 11.0, Orekit
used a single big enumerate class for all these keys, but it proved unmanageable as more
message types were supported. The framework set up with version 11.0 is based on the fact
these numerous keys belong to a smaller set of logical blocks that are always parsed as a
whole (header, metadata, state vector, covariance...). Parsing can be performed with the
parser switching between a small number of well-known states. When one state is active,
say metadata parsing, then lookup is limited to the keys allowed in metadata. If an
unknown token arrives, then the parser assumes we have finished the current section, and
it switches into another state, say data parsing, that is the fallback to use after
metadata. This is an implementation of the State design pattern. Parsers always have
one current `ProcessingState` that remains active as long as it can process the tokens
provided to it by the lexical analyzer, and the have a fallback `ProcessingState` to
switch to when a token could not be handled by the current one. The following class
diagram shows this design:

![parsing class diagram](../images/design/ccsds-parsing-class-diagram.png)

All parsers set up the initial processing state when their `reset` method is called
by the lexical analyzer at the beginning of the message, and they manage the fallback
processing state by anticipating what the next state could be when one state is
activated. This is highly specific for each message type, and unfortunately also
depends on file format (KVN vs. XML). As an example, in KVN files, the initial
processing state is already the `HeaderProcessingState`, but in XML file it is
rather `XMLStructureProvessingState` and `HeaderProcessingState` is triggered only
when the XML `<header>` start element is processed. CCSDS messages type are also not
very consistent, which makes implementation more complex. As an example, APM files
don't have `META_START`, `META_STOP`, `DATA_START` or `DATA_STOP` keys in the
KVN version, whereas AEM file have both, and OEM have `META_START`, `META_STOP`
but have neither `DATA_START` nor `DATA_STOP`. All parsers extend the `AbstractMessageParser`
abstract class from which declares several hooks (`prepareHeader`, `inHeader`,
`finalizeHeader`, `prepareMetadata`...) which can be called by various states
so the parser knows where it is and prepare the fallback processing state. The
`prepareMetadata` hook for example is called by `KVNStructureProcessingState`
when it sees a `META_START` key, and by `XMLStructureProcessingState` when it
sees a `metadata` start element. The parser then knows that metadata parsing is
going to start an set up the fallback state for it.

When the parser is not switching states, one state is active and processes all
upcoming token one after the other. Each processing state may adopt a different
strategy for this, depending on the section it handles. Processing states are
always quite small. Some processing states that can be reused from message type
to message type (like `HeaderProcessingState`, `KVNStructureProcessingState` or
`XMLStructureProcessingstate`) are implemented as classes. Other processing
states that are specific to one message type (and hence to one parser), are
implemented as a single private method within the parser and method references
are used to point directly to the method. This allows one parser class to
provide simultaneously several implementations of the `ProcessingState` interface.

In many cases, the keys that are allowed in a section are fixed so they are defined
in an enumerate. The processing state (in this case often a private method within
the parser) then simply selects the enum constant using the standard `valueOf` method
from the enumerate class and delegates token processing to it. The enum constant
then just call one of the `processAs` method from the token, pointing it to the
metadata/data/logical block setter to call for storing the token content. For
sections that both reuse some keys from a more general section and add their
own keys, several enumerate types can be checked in row. A typical example of this
design is the `processMetadataToken` method in `OEMParser`, which is a single
private method acting as a `ProcessingState` and tries the enumerates `MetadataKey`,
`ODMMetadataKey`, `OCommonMetadataKey` and finally `OEMMetadataKey` to fill up
the metadata section.

Adding a new message type (lets name it XYZ message) involves creating  the `XYZFile`
class that extends `NDMFile`, creating the `XYZData` container for the data part,
and creating one or more `XYZSection1Key`, `XYZSection2Key`... enumerates for each
logical blocks that are allowed in the message format. The final task is to create
the `XYZParser` and set up the state switching logic, using existing classes for
the global structure and header, and private methods `processSection1Token`,
`processSection2Token`... for processing the tokens from each logical block.

Adding a new key to an existing message when a new version of the message format
is published by CCSDS generally consist in adding one field in the data container
with a setter and a getter, and one enum constant that will be recognized by
the existing processing state and that will call one of the `processAs` method from
the token, asking it to call the new setter.

### Writing

The following class diagram presents the implementation of writing:

![writing class diagram](../images/design/ccsds-writing-class-diagram.png)

TODO explain diagram
