//----------------------------------------------------------------------------//
//                                                                            //
//                     S y m b o l s C o n t r o l l e r                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.*;
import omr.glyph.facets.Glyph;
import omr.glyph.text.TextRole;

import omr.log.Logger;


import omr.score.entity.Note;
import omr.score.entity.Text.CreatorText.CreatorType;

import omr.script.RationalTask;
import omr.script.SegmentTask;
import omr.script.SlurTask;
import omr.script.TextTask;

import org.jdesktop.application.Task;

import java.awt.*;
import java.util.*;
import omr.score.entity.TimeRational;

/**
 * Class <code>SymbolsController</code> is a GlyphsController specifically
 * meant for symbol glyphs, adding handling for assigning Texts, for fixing
 * Slurs and for segmenting on Stems.
 *
 * @author Hervé Bitteur
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

    //----------//
    // getModel //
    //----------//
    /**
     * Report the underlying model
     * @return the underlying glyphs model
     */
    @Override
    public SymbolsModel getModel ()
    {
        return (SymbolsModel) model;
    }

    //----------------------//
    // asyncAssignRationals //
    //----------------------//
    /**
     * Asynchronously assign a rational value to a collection of glyphs with
     * CUSTOM_TIME_SIGNATURE shape
     *
     * @param glyphs the impacted glyphs
     * @param timeRational the time sig rational value
     * @return the task that carries out the processing
     */
    public Task asyncAssignRationals (Collection<Glyph> glyphs,
                                      final TimeRational    timeRational)
    {
        return new RationalTask(sheet, timeRational, glyphs).launch(sheet);
    }

    //------------------//
    // asyncAssignTexts //
    //------------------//
    /**
     * Asynchronously assign text characteristics to a collection of textual
     * glyphs
     *
     * @param glyphs the impacted glyphs
     * @param textType the type of the creator, if relevant
     * @param textRole the role of this textual element
     * @param textContent the content as a string (if not empty)
     * @return the task that carries out the processing
     */
    public Task asyncAssignTexts (Collection<Glyph> glyphs,
                                  final CreatorType textType,
                                  final TextRole    textRole,
                                  final String      textContent)
    {
        return new TextTask(sheet, textType, textRole, textContent, glyphs).launch(
            sheet);
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
        return new SlurTask(sheet, glyphs).launch(sheet);
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
        return new SegmentTask(sheet, isShort, glyphs).launch(sheet);
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
}
