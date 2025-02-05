//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  M e t r o n o m e S y m b o l                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.sig.inter.BeatUnitInter.Note;
import org.audiveris.omr.sig.inter.MetronomeInter;
import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_RIGHT;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.ui.symbol.OmrFont.RATIO_METRO;
import static org.audiveris.omr.ui.symbol.OmrFont.RATIO_TINY;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class <code>MetronomeSymbol</code> implements a metronome, composed of a beat unit,
 * an equal sign, and a bpm text.
 *
 * @author Hervé Bitteur
 */
public class MetronomeSymbol
        extends DecorableSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MetronomeSymbol.class);

    /** The label used for metronome button. */
    private static final String LABEL = "metronome";

    /** The dummy bpm value. */
    public static final String DUMMY_BPM = "00";

    //~ Instance fields ----------------------------------------------------------------------------

    /** The note used as beat unit. */
    protected final Note note;

    /** The bpm textual specification. */
    protected final String bpmString;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>MetronomeSymbol</code> with the dummy bpm value.
     *
     * @param noteShape   one of {@link ShapeSet#BeatUnits}
     * @param musicFamily the music font family
     */
    public MetronomeSymbol (Shape noteShape,
                            MusicFamily musicFamily)
    {
        this(noteShape, musicFamily, DUMMY_BPM);
    }

    /**
     * Create a <code>MetronomeSymbol</code>.
     *
     * @param noteShape   one of {@link ShapeSet#BeatUnits}
     * @param musicFamily the music font family
     * @param bpmString   the bpm textual specification
     */
    public MetronomeSymbol (Shape noteShape,
                            MusicFamily musicFamily,
                            String bpmString)
    {
        super(Shape.METRONOME, musicFamily);
        note = Note.noteOf(noteShape);
        this.bpmString = bpmString;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getModel //
    //----------//
    @Override
    public MetronomeInter.Model getModel (MusicFont font,
                                          Point location)
    {
        final MyParams p = getParams(font);
        p.model.translate(p.vectorTo(location));

        return p.model;
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected MyParams getParams (MusicFont sheetMusicFont)
    {
        final MyParams p = new MyParams();

        final StubsController controller = StubsController.getInstance();
        final SheetStub stub = controller.getSelectedStub();
        final TextFamily textFamily = (stub != null) ? stub.getTextFamily() : TextFamily.SansSerif;

        if (isDecorated && isTiny) {
            // Use the metronome label
            final int fontSize = (int) Math.rint(sheetMusicFont.getSize2D() * RATIO_TINY);
            final TextFont textFont = new TextFont(
                    textFamily.getFontName(),
                    null,
                    Font.PLAIN,
                    fontSize);
            p.layout = textFont.layout(LABEL);
            p.rect = p.layout.getBounds();
        } else {
            // Properly sized music and text fonts
            final int fontSize = (int) Math.rint(sheetMusicFont.getSize2D() * RATIO_METRO);
            final TextFont textFont = new TextFont(
                    textFamily.getFontName(),
                    null,
                    Font.PLAIN,
                    fontSize);
            final MusicFont musicFont = sheetMusicFont.deriveFont((float) fontSize);

            final Symbols symbols = MusicFamily.Bravura.getSymbols();
            final int[] codes = symbols.getCode(note.toShape());
            final String str = new String(codes, 0, codes.length);
            p.layout = musicFont.layout(str);
            final Rectangle2D noteRect = p.layout.getBounds();
            final float noteAdvance = p.layout.getAdvance();
            double minY = noteRect.getMinY();
            double maxY = noteRect.getMaxY();

            p.bpmLayout = textFont.layout(" = " + bpmString);
            final Rectangle2D bpmRect = p.bpmLayout.getBounds();
            final float bpmAdvance = p.bpmLayout.getAdvance();
            minY = Math.min(minY, bpmRect.getMinY());
            maxY = Math.max(maxY, bpmRect.getMaxY());

            p.rect = new Rectangle2D.Double(0, 0, noteAdvance + bpmAdvance, maxY - minY);

            // Offset from box center to note baseline center
            p.offset = new Point2D.Double(
                    -p.rect.getWidth() / 2 + noteAdvance / 2,
                    -p.rect.getHeight() / 2 - noteRect.getY());

            // Model
            p.model = new MetronomeInter.Model();
            p.model.tempo = "";
            p.model.unit = note.toShape();
            p.model.bpmText = bpmString;

            p.model.bpm1 = 0;
            p.model.bpm2 = null;
            p.model.parentheses = false;

            p.model.tempoFontSize = textFont.getSize();
            p.model.unitFontSize = musicFont.getSize();
            p.model.bpmFontSize = textFont.getSize();
            p.model.baseCenter = new Point2D.Double(noteRect.getWidth() / 2, -noteRect.getY());
            p.model.box = p.rect.getBounds2D();
        }

        return p;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals()) //
                .append(' ').append(note) //
                .append(' ').append(bpmString) //
                .toString();
    }

    //-------//
    // paint //
    //-------//
    @Override
    protected void paint (Graphics2D g,
                          Params params,
                          Point2D location,
                          Alignment alignment)
    {
        final MyParams p = (MyParams) params;

        if (isDecorated && isTiny) {
            final Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
            OmrFont.paint(g, p.layout, loc, TOP_LEFT);
        } else {
            // note on left side
            final Point2D loc1 = alignment.translatedPoint(MIDDLE_LEFT, p.rect, location);
            OmrFont.paint(g, p.layout, loc1, MIDDLE_LEFT);

            // = bpm on right side
            final Point2D loc2 = alignment.translatedPoint(MIDDLE_RIGHT, p.rect, location);
            OmrFont.paint(g, p.bpmLayout, loc2, MIDDLE_RIGHT);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //----------//
    // MyParams //
    //----------//
    protected static class MyParams
            extends ShapeSymbol.Params
    {
        // offset: from area center to note baseline
        // layout: note layout
        // rect:   global image rectangle

        // bpm layout
        TextLayout bpmLayout;

        // model
        MetronomeInter.Model model;
    }
}
