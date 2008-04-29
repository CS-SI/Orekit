package fr.cs.orekit.potential;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import fr.cs.orekit.errors.OrekitException;

/** This pattern determines which reader to use with the selected file.
 * @author F. Maussion
 */
public class PotentialReaderFactory {

    /** Potential readers. */
    private List readers;

    /** Simple constructor.
     */
    public PotentialReaderFactory() {
        readers = new ArrayList();
        readers.add(new SHMFormatReader());
        readers.add(new EGMFormatReader());
    }

    /** Adds a {@link PotentialCoefficientsReader} to the test list.
     * By construction, the list contains allready the {@link SHMFormatReader}
     * and the {@link EGMFormatReader}.
     * @param reader the reader to add
     */
    public void addPotentialReader(final PotentialCoefficientsReader reader) {
        readers.add(reader);
    }

    /** Determines the proper reader to use wich the selected file.
     * It tests all the readers it contains to see if they match the input format.
     * @param in the file to check (it can be compressed)
     * @return the proper reader
     * @exception OrekitException when no known reader can read the file
     * @exception IOException when the {@link InputStream} is not valid.
     */
    public PotentialCoefficientsReader getPotentialReader(final InputStream in)
        throws OrekitException, IOException {

        BufferedInputStream filter = new BufferedInputStream(in);
        filter.mark(1024 * 1024);

        boolean isCompressed = false;
        try {
            isCompressed = new GZIPInputStream(filter).read() != -1;
        } catch (IOException e) {
            isCompressed = false;
        }
        filter.reset();

        if (isCompressed) {
            filter = new BufferedInputStream(new GZIPInputStream(filter));
        }
        filter.mark(1024 * 1024);
        PotentialCoefficientsReader result = null;

        // test the available readers
        for (final Iterator iter = readers.iterator(); iter.hasNext();) {
            final PotentialCoefficientsReader test = (PotentialCoefficientsReader) iter.next();
            if (test.isFileOK(filter)) {
                result = test;
            }
            filter.reset();
        }

        if (result == null) {
            throw new OrekitException("Unknown file format ", new Object[0]);
        }

        return result;

    }

}
