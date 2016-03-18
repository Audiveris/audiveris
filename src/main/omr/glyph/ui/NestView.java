//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        N e s t V i e w                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.GlyphIndex;

import omr.lag.Lag;
import omr.lag.Section;

import omr.sheet.Sheet;

import omr.ui.EntityView;
import omr.ui.ViewParameters;
import omr.ui.util.UIUtil;

import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.List;
import omr.ui.selection.EntityService;

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
        final boolean drawBorders = ViewParameters.getInstance().isSectionMode();

        // Stroke for borders
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);

        //
        //        logger.info("NestView render {} glyphs", nest.getEntities().size());
        //        for (Glyph glyph : nest.getEntities()) {
        //            glyph.getRunTable().render(g, glyph.getTopLeft());
        //        }
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

        if (!ViewParameters.getInstance().isSectionMode()) {
            // Render the selected glyph(s) if any
            List<Glyph> selectedGlyphs = glyphIndex.getSelectedGlyphList();

            if (selectedGlyphs != null) {
                // Decorations first
                g.setColor(Color.blue);

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
        } else {
            // Section selection mode
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
