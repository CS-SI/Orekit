package org.orekit.time;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.orekit.data.DataLoader;

/**
 * A {@link DataLoader} based on a {@link UTCTAIOffsetsLoader.Parser} that loads a single
 * file.
 *
 * @author Evan Ward
 * @since 10.1
 */
class UtcTaiOffsetLoader implements DataLoader {

    /** Leap second parser. */
    private final UTCTAIOffsetsLoader.Parser parser;

    /** UTC-TAI offsets. */
    private final List<OffsetModel> offsets;

    /**
     * Create a data loader from a lep second parser.
     *
     * @param parser leap second parser.
     */
    UtcTaiOffsetLoader(final UTCTAIOffsetsLoader.Parser parser) {
        this.parser = parser;
        this.offsets = new ArrayList<>();
    }

    /**
     * Get the parsed offsets.
     *
     * @return parsed offsets
     */
    public List<OffsetModel> getOffsets() {
        return offsets;
    }

    @Override
    public boolean stillAcceptsData() {
        return offsets.isEmpty();
    }

    @Override
    public void loadData(final InputStream input, final String name)
            throws IOException {
        offsets.clear();
        offsets.addAll(parser.parse(input, name));
    }

}
