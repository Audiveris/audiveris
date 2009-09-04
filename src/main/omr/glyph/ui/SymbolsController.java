//----------------------------------------------------------------------------//
//                                                                            //
//                     S y m b o l s C o n t r o l l e r                      //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph.ui;

import omr.glyph.*;
import omr.glyph.Shape;
import omr.glyph.text.TextRole;

import omr.log.Logger;

import omr.score.entity.Note;

import omr.script.SegmentTask;
import omr.script.SlurTask;
import omr.script.TextTask;

import org.jdesktop.application.Task;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>SymbolsController</code> is a GlyphsController specifically
 * meant for symbol glyphs, adding handling for assigning Texts, for fixing
 * Slurs and for segmenting on Stems.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsController
    extends GlyphsController
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        SymbolsController.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Constructors -----------------------------------------------------------

    //-------------------//
    // SymbolsController //
    //-------------------//
    /**
     * Create a handler dedicated to symbol glyphs
     *
     * @param model the related glyphs model
     */
    public SymbolsController (SymbolsModel model)
    {
        super(model);
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // asyncAssignText //
    //-----------------//
    /**
     * Asynchronously assign text characteristics to a collection of textual
     * glyphs
     *
     * @param glyphs the impacted glyphs
     * @param textRole the type (role) of this textual element
     * @param textContent the content as a string (if not empty)
     * @return the task that carries out the processing
     */
    public Task asyncAssignText (Collection<Glyph> glyphs,
                                 final TextRole    textRole,
                                 final String      textContent)
    {
        Impact impact = Impact.createGlyphsImpact(sheet, null, glyphs);

        return launch(
            new TextTask(textRole, textContent, glyphs),
            impact,
            new ImpactRunnable() {
                    public void run (Impact impact)
                    {
                        syncAssignText(impact, textRole, textContent);
                    }
                });
    }

    //--------------------//
    // asyncFixLargeSlurs //
    //--------------------//
    /**
     * Asynchronously fix a collection of glyphs as large slurs
     *
     * @param glyphs the slur glyphs to fix
     * @return the task that carries out the processing
     */
    public Task asyncFixLargeSlurs (Collection<Glyph> glyphs)
    {
        Impact impact = Impact.createGlyphsImpact(sheet, null, glyphs);

        return launch(
            new SlurTask(glyphs),
            impact,
            new ImpactRunnable() {
                    public void run (Impact impact)
                    {
                        getModel()
                            .fixLargeSlurs(impact.getInitialGlyphs());
                    }
                });
    }

    //--------------//
    // asyncSegment //
    //--------------//
    /**
     * Asynchronously segment a set of glyphs on their stems
     *
     * @param glyphs glyphs to segment in order to retrieve stems
     * @param isShort looking for short (or standard) stems
     * @return the task that carries out the processing
     */
    public Task asyncSegment (Collection<Glyph> glyphs,
                                      final boolean     isShort)
    {
        Impact impact = Impact.createGlyphsImpact(sheet, null, glyphs);

        return launch(
            new SegmentTask(isShort, glyphs),
            impact,
            new ImpactRunnable() {
                    public void run (Impact impact)
                    {
                        getModel()
                            .segmentGlyphSet(
                            impact.getInitialGlyphs(),
                            isShort);
                    }
                });
    }

    //------------------//
    // showTranslations //
    //------------------//
    public void showTranslations (Collection<Glyph> glyphs)
    {
        for (Glyph glyph : glyphs) {
            for (Object entity : glyph.getTranslations()) {
                if (entity instanceof Note) {
                    Note note = (Note) entity;
                    logger.info(note + "->" + note.getChord());
                } else {
                    logger.info(entity.toString());
                }
            }
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return getClass()
                   .getSimpleName();
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Report the underlying model
     * @return the underlying glyphs model
     */
    @Override
    protected SymbolsModel getModel ()
    {
        return (SymbolsModel) model;
    }

    //----------------//
    // syncAssignText //
    //----------------//
    protected void syncAssignText (Impact   impact,
                                   TextRole textRole,
                                   String   textContent)
    {
        if (logger.isFineEnabled()) {
            logger.fine("syncAssignText " + impact);
        }

        getModel()
            .assignText(
            impact.getInitialGlyphs(),
            textRole,
            textContent,
            Evaluation.MANUAL);
    }

    //--------------//
    // syncDeassign //
    //--------------//
    /**
     * {@inheritDoc}
     */
    @Override
    protected void syncDeassign (Impact impact)
    {
        if (logger.isFineEnabled()) {
            logger.fine("syncDeassign " + impact);
        }

        // Use a copy of the glyph list
        List<Glyph> nonStemGlyphs = new ArrayList<Glyph>(
            impact.getInitialGlyphs());

        // Put the stems apart
        List<Glyph> stems = new ArrayList<Glyph>();

        for (Glyph glyph : nonStemGlyphs) {
            if (glyph.getShape() == Shape.COMBINING_STEM) {
                stems.add(glyph);
            }
        }

        nonStemGlyphs.removeAll(stems);

        // First phase, process the (non-stem) glyphs
        model.deassignGlyphs(nonStemGlyphs);

        // Second phase dedicated to stems, if any
        if (!stems.isEmpty()) {
            getModel()
                .cancelStems(stems);
        }

        // Publish modifications
        publish(impact.getInitialGlyphs());
    }
}
