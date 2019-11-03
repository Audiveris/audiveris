package org.audiveris.omr.text;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NoOpOCR implements OCR {
    @Override
    public Set<String> getLanguages() {
        return Collections.emptySet();
    }

    @Override
    public String identify() {
        return "NoOpOCR";
    }

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public List<TextLine> recognize (int interline,
                              BufferedImage image,
                              Point topLeft,
                              String languageCode,
                              LayoutMode layoutMode,
                              String label) {
        return Collections.emptyList();
    }
}
