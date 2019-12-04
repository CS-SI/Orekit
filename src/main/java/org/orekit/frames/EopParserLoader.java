package org.orekit.frames;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.orekit.data.DataLoader;

/**
 * Implementation of {@link DataLoader} based on {@link EOPHistoryLoader.Parser} that
 * loads all files and compiles the results into one data structure.
 *
 * @author Evan Ward
 * @since 10.1
 */
class EopParserLoader implements DataLoader {

    /** Parser for EOP data files. */
    private final EOPHistoryLoader.Parser parser;

    /** History entries. */
    private final List<EOPEntry> history;

    /**
     * Create a {@link DataLoader} based on a {@link EOPHistoryLoader.Parser}. Loads
     * all EOP data into a single collection.
     *
     * @param parser for the EOP data files.
     */
    EopParserLoader(final EOPHistoryLoader.Parser parser) {
        this.parser = parser;
        this.history = new ArrayList<>();
    }

    /**
     * Get the parsed EOP entries.
     *
     * @return the parsed EOP data. The returned collection is a reference, not a
     * copy. It is not guaranteed to be sorted.
     */
    public Collection<EOPEntry> getEop() {
        return history;
    }

    /** {@inheritDoc} */
    public boolean stillAcceptsData() {
        return true;
    }

    @Override
    public void loadData(final InputStream input, final String name)
            throws IOException, ParseException {
        history.addAll(parser.parse(input, name));
    }

}
