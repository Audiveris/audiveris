//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        N e s t V i e w                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.ui.EntityView;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.ViewParameters.SelectionMode;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code NestView} is a view that combines the display of several lags to
 * represent a nest of glyphs.
 *
 * @author Hervé Bitteur
 */
public class NestView
        extends EntityView<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(NestView.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The underlying glyph index */
    protected final GlyphIndex glyphIndex;

    /** The sequence of lags. */
    protected final List<Lag> lags = new ArrayList<Lag>();

    /** Related sheet, if any. */
    @Navigable(false)
    private final Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a nest view.
     *
     * @param glyphService the underlying service on index of glyph instances
     * @param lags         the initial lags to be displayed
     * @param sheet        related sheet, if any
     */
    public NestView (EntityService<Glyph> glyphService,
                     List<Lag> lags,
                     Sheet sheet)
    {
        super(glyphService);

        this.sheet = sheet;
        this.lags.addAll(lags);

        glyphIndex = (GlyphIndex) glyphService.getIndex();

        setName("NestView");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // render //
    //--------//
    @Override
    public void render (Graphics2D g)
    {
        // Should we draw the section borders?
        final boolean drawBorders = ViewParameters.getInstance().getSelectionMode() == SelectionMode.MODE_SECTION;

        // Stroke for borders
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

        for (Lag lag : lags) {
            // Render all sections, using the colors they have been assigned
            for (Section section : lag.getEntities()) {
                section.render(g, drawBorders, null);
            }
        }

        // Restore stroke
        g.setStroke(oldStroke);
    }

    //-------------//
    // renderItems //
    //-------------//
    @Override
    protected void renderItems (Graphics2D g)
    {
        // Global sheet renderers if any
        sheet.renderItems(g);

        switch (ViewParameters.getInstance().getSelectionMode()) {
        case MODE_GLYPH:

            // Render the selected glyph(s) if any
            List<Glyph> selectedGlyphs = glyphIndex.getSelectedGlyphList();

            if (selectedGlyphs != null) {
                // Decorations first
                g.setColor(Color.RED);

                for (Glyph glyph : selectedGlyphs) {
                    // Draw selected glyph in specific color
                    glyph.getRunTable().render(g, glyph.getTopLeft());

                    //
                    //                    // Draw character boxes for textual glyphs?
                    //                    if (glyph.isText()) {
                    //                        if (ViewParameters.getInstance().isLetterBoxPainting()) {
                    //                            TextWord word = glyph.getTextWord();
                    //
                    //                            if (word != null) {
                    //                                for (TextChar ch : word.getChars()) {
                    //                                    Rectangle b = ch.getBounds();
                    //                                    g.drawRect(b.x, b.y, b.width, b.height);
                    //                                }
                    //                            }
                    //                        }
                    //                    }
                    //
                    // Draw attachments, if any
                    glyph.renderAttachments(g);

                    // Draw glyph line?
                    if (ViewParameters.getInstance().isLinePainting()) {
                        glyph.renderLine(g);
                    }
                }

                // Glyph areas second
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(Color.gray);
                UIUtil.setAbsoluteStroke(g2, 1f);

                for (Glyph glyph : selectedGlyphs) {
                    renderBoxArea(glyph.getBounds(), g2);
                }

                g2.dispose();

                //
                //                // Display words of a sentence, if any
                //                if (ViewParameters.getInstance().isSentencePainting()) {
                //                    for (Glyph glyph : glyphs) {
                //                        renderGlyphSentence(glyph, g);
                //                    }
                //                }
                //
                //                // Display translation links, if any
                //                if (ViewParameters.getInstance().isTranslationPainting()) {
                //                    for (Glyph glyph : glyphs) {
                //                        renderGlyphTranslations(glyph, g);
                //                    }
                //                }
            }

            break;

        case MODE_INTER:

            // TODO: something to do with selected inters???
            break;

        case MODE_SECTION:

            for (Lag lag : lags) {
                List<Section> selectedSections = lag.getEntityService().getSelectedEntityList();

                if ((selectedSections != null) && !selectedSections.isEmpty()) {
                    for (Section section : selectedSections) {
                        section.renderSelected(g);
                    }
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //
    //    //---------------------//
    //    // renderGlyphSentence //
    //    //---------------------//
    //    /**
    //     * Display the relation between the glyph/word at hand and the other words of the
    //     * same containing sentence
    //     *
    //     * @param glyph the provided selected glyph
    //     * @param g     graphic context
    //     */
    //    private void renderGlyphSentence (Glyph glyph,
    //                                      Graphics2D g)
    //    {
    //        if (glyph.getTextWord() == null) {
    //            return;
    //        }
    //
    //        TextLine sentence = glyph.getTextWord().getTextLine();
    //        Color oldColor = g.getColor();
    //
    //        if (constants.showSentenceBaseline.isSet()) {
    //            // Display the whole sentence baseline
    //            g.setColor(Colors.SENTENCE_BASELINE);
    //
    //            Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
    //
    //            Path2D path = new Path2D.Double();
    //            TextWord prevWord = null;
    //
    //            for (TextWord word : sentence.getWords()) {
    //                Point2D left = word.getBaseline().getP1();
    //
    //                if (prevWord == null) {
    //                    path.moveTo(left.getX(), left.getY());
    //                } else {
    //                    path.lineTo(left.getX(), left.getY());
    //                }
    //
    //                Point2D right = word.getBaseline().getP2();
    //                path.lineTo(right.getX(), right.getY());
    //                prevWord = word;
    //            }
    //
    //            g.draw(path);
    //
    //            g.setStroke(oldStroke);
    //        } else {
    //            // Display a x-height rectangle between words
    //            g.setColor(Colors.SENTENCE_GAPS);
    //
    //            FontInfo font = sentence.getMeanFont();
    //            double height = font.pointsize * 0.4f; // TODO: Explain this 0.4
    //
    //            TextWord prevWord = null;
    //
    //            for (TextWord word : sentence.getWords()) {
    //                if (prevWord != null) {
    //                    Path2D path = new Path2D.Double();
    //                    Point2D from = prevWord.getBaseline().getP2();
    //                    path.moveTo(from.getX(), from.getY());
    //                    path.lineTo(from.getX(), from.getY() - height);
    //
    //                    Point2D to = word.getBaseline().getP1();
    //                    path.lineTo(to.getX(), to.getY() - height);
    //                    path.lineTo(to.getX(), to.getY());
    //                    path.closePath();
    //
    //                    g.fill(path);
    //                }
    //
    //                prevWord = word;
    //            }
    //        }
    //
    //        g.setColor(oldColor);
    //    }
    //
    //    //-------------------------//
    //    // renderGlyphTranslations //
    //    //-------------------------//
    //    private void renderGlyphTranslations (Glyph glyph,
    //                                          Graphics2D g)
    //    {
    //        if (glyph.getTranslations().isEmpty()) {
    //            return;
    //        }
    //
    //        Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
    //        Color oldColor = g.getColor();
    //        g.setColor(Colors.TRANSLATION_LINK);
    //
    //        // Compute end radius, with fixed size whatever the current zoom
    //        double r = 1 / g.getTransform().getScaleX();
    //
    //        for (OldPartNode node : glyph.getTranslations()) {
    //            for (Line2D line : node.getTranslationLinks(glyph)) {
    //                // Draw line
    //                g.draw(line);
    //
    //                // Draw ending points
    //                Ellipse2D e1 = new Ellipse2D.Double(
    //                        line.getX1() - r,
    //                        line.getY1() - r,
    //                        2 * r,
    //                        2 * r);
    //                g.draw(e1);
    //
    //                Ellipse2D e2 = new Ellipse2D.Double(
    //                        line.getX2() - r,
    //                        line.getY2() - r,
    //                        2 * r,
    //                        2 * r);
    //                g.draw(e2);
    //            }
    //        }
    //
    //        g.setColor(oldColor);
    //        g.setStroke(oldStroke);
    //    }
    //
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean showSentenceBaseline = new Constant.Boolean(
                true,
                "Should we show sentence baseline (vs inter-word gaps)?");
    }
}
