//----------------------------------------------------------------------------//
//                                                                            //
//                        S y m b o l s B u i l d e r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.Main;

import omr.glyph.ui.SymbolsEditor;

import omr.score.entity.Note;

import omr.script.AssignTask;
import omr.script.DeassignTask;
import omr.script.SegmentTask;
import omr.script.SlurTask;
import omr.script.TextTask;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>SymbolsBuilder</code> is a GlyphModel specific for symbols
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SymbolsBuilder
    extends GlyphModel
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(SymbolsBuilder.class);

    /** Color for hiding unknown glyphs when filter is ON */
    public static final Color hiddenColor = Color.white;

    //~ Instance fields --------------------------------------------------------

    /** Glyph builder */
    private final GlyphsBuilder glyphsBuilder;

    /** Related symbols editor, if any */
    private final SymbolsEditor editor;

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // SymbolsBuilder //
    //----------------//
    /**
     * Create a handler dedicated to symbol glyphs
     *
     * @param sheet the sheet whose glyphs are considered
     */
    public SymbolsBuilder (Sheet sheet)
    {
        super(sheet, sheet.getVerticalLag());

        // Link with glyph builder & glyph inspector
        glyphsBuilder = sheet.getGlyphsBuilder();

        // Allocation of UI components if needed
        if (Main.getGui() != null) {
            editor = new SymbolsEditor(sheet, this);
        } else {
            editor = null;
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------//
    // getEditor //
    //-----------//
    public SymbolsEditor getEditor ()
    {
        return editor;
    }

    //------------------//
    // assignGlyphShape //
    //------------------//
    /**
     * Manually assign a Shape to a glyph, but preventing to assign a non-noise
     * shape to a noise glyph
     *
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void assignGlyphShape (Glyph   glyph,
                                  Shape   shape,
                                  boolean record)
    {
        if (glyph != null) {
            Collection<Shape> shapes = null;
            boolean           isCompound = glyph.getId() == 0;
            Collection<Glyph> glyphs;

            if (isCompound) {
                glyphs = glyph.getParts();
            } else {
                glyphs = Collections.singleton(glyph);
            }

            // Record this task to the sheet script?
            if (record) {
                shapes = Glyph.shapesOf(glyphs);

                sheet.getScript()
                     .addTask(new AssignTask(shape, isCompound, glyphs));
            }

            // If this is a transient glyph (with no Id yet), insert it
            if (isCompound) {
                glyphsBuilder.insertGlyph(glyph);
                logger.info(
                    "Inserted compound #" + glyph.getId() + " as " + shape);
            }

            if ((shape == Shape.NOISE) || Evaluator.isBigEnough(glyph)) {
                // Force a recomputation of glyph parameters (since environment may
                // have changed since the time they had been computed)
                glyphsBuilder.computeGlyphFeatures(glyph);
                super.assignGlyphShape(glyph, shape, false);
            }

            // Update final steps?
            if (record) {
                shapes.add(shape);
                sheet.updateLastSteps(glyphs, shapes);
            }
        }
    }

    //----------------//
    // assignSetShape //
    //----------------//
    /**
     * Assign a shape to a set of glyphs, either to each glyph individually, or
     * to a compound glyph built from the glyph set
     *
     * @param glyphs the collection of glyphs
     * @param shape the shape to be assigned
     * @param compound flag to indicate a compound is desired
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void assignSetShape (Collection<Glyph> glyphs,
                                Shape             shape,
                                boolean           compound,
                                boolean           record)
    {
        Collection<Shape> shapes = Glyph.shapesOf(glyphs);

        if ((glyphs != null) && (glyphs.size() > 0)) {
            if (compound) {
                // Build & insert a compound
                Glyph glyph = glyphsBuilder.buildCompound(glyphs);
                glyphsBuilder.insertGlyph(glyph);
                assignGlyphShape(glyph, shape, false);
            } else {
                int              noiseNb = 0;
                ArrayList<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

                for (Glyph glyph : glyphsCopy) {
                    if (glyph.getShape() != Shape.NOISE) {
                        assignGlyphShape(glyph, shape, false);
                    } else {
                        noiseNb++;
                    }
                }

                if (logger.isFineEnabled() && (noiseNb > 0)) {
                    logger.fine(noiseNb + " noise glyphs skipped");
                }
            }

            // Record this task to the sheet script?
            if (record) {
                sheet.getScript()
                     .addTask(new AssignTask(shape, compound, glyphs));
                shapes.add(shape);
                sheet.updateLastSteps(glyphs, shapes);
            }
        }
    }

    //------------//
    // assignText //
    //------------//
    /**
     * Assign text characteristics to a (collection of) textual glyphs
     *
     * @param glyphs the impacted glyphs
     * @param textType the type(role) of this textual element
     * @param textContent the content as a string (if not empty)
     * @param record true if this task must be recorded
     */
    public void assignText (Collection<Glyph> glyphs,
                            TextType          textType,
                            String            textContent,
                            boolean           record)
    {
        // Do the job
        for (Glyph glyph : glyphs) {
            // Assign text type
            Sentence sentence = glyph.getSentence();

            if (sentence != null) {
                sentence.setTextType(textType);
            }

            // Assign text only if it is not empty
            if ((textContent != null) && (textContent.length() > 0)) {
                glyph.setTextContent(textContent);
            }
        }

        // Record this task in the sheet script
        if (record) {
            sheet.getScript()
                 .addTask(new TextTask(textType, textContent, glyphs));
            sheet.updateLastSteps(glyphs, new ArrayList<Shape>()); // No shapes
        }
    }

    //-------------//
    // cancelStems //
    //-------------//
    /**
     * Cancel one or several stems, turning them back to just a set of sections,
     * and rebuilding glyphs from their member sections together with the
     * neighbouring non-assigned sections
     *
     * @param stems a list of stems
     */
    public void cancelStems (List<Glyph> stems)
    {
        /**
         * To remove a stem, several infos need to be modified : shape from
         * COMBINING_STEM to null, result from STEM to null, and the Stem must
         * be removed from system list of stems.
         *
         * The stem glyph must be removed (as well as all other non-recognized
         * glyphs that are connected to the former stem)
         *
         * Then, re-glyph extraction from sections when everything is ready
         * (GlyphBuilder). Should work on a micro scale : just the former stem
         * and the neighboring (non-assigned) glyphs.
         */
        Set<SystemInfo> impactedSystems = new HashSet<SystemInfo>();

        for (Glyph stem : stems) {
            SystemInfo system = sheet.getSystemAtY(stem.getContourBox().y);
            glyphsBuilder.removeGlyph(stem, system, /* cutSections => */
                                      true);
            assignGlyphShape(stem, null, false);
            impactedSystems.add(system);
        }

        // Extract brand new glyphs from impacted impactedSystems
        for (SystemInfo system : impactedSystems) {
            glyphsBuilder.extractNewSystemGlyphs(system);
        }

        // Update the UI?
        if (editor != null) {
            editor.refresh();
        }
    }

    //--------------------//
    // deassignGlyphShape //
    //--------------------//
    /**
     * De-assign the shape of a glyph
     *
     * @param glyph the glyph to deassign
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void deassignGlyphShape (Glyph   glyph,
                                    boolean record)
    {
        Collection<Glyph> glyphs = null;
        Collection<Shape> shapes = null;

        // Record this action in the sheet script?
        if (record) {
            glyphs = Collections.singleton(glyph);
            shapes = Glyph.shapesOf(glyphs);
            sheet.getScript()
                 .addTask(new DeassignTask(glyphs));
        }

        Shape shape = glyph.getShape();

        // Processing depends on shape at hand
        switch (shape) {
        case THICK_BAR_LINE :
        case THIN_BAR_LINE :
            sheet.getBarsBuilder()
                 .deassignGlyphShape(glyph, false);

            break;

        case COMBINING_STEM :

            if (logger.isFineEnabled()) {
                logger.fine("Deassigning a Stem as glyph " + glyph.getId());
            }

            cancelStems(Collections.singletonList(glyph));

            break;

        case NOISE :
            logger.info("Skipping Noise as glyph " + glyph.getId());

            break;

        default :

            if (logger.isFineEnabled()) {
                logger.fine(
                    "Deassigning a " + shape + " symbol as glyph " +
                    glyph.getId());
            }

            // If glyph is a compound, deassign also the parts
            for (Glyph p : glyph.getParts()) {
                assignGlyphShape(p, null, false);
            }

            // Deassign the glyph itself
            assignGlyphShape(glyph, null, false);

            break;
        }

        // Update last steps?
        if (record) {
            sheet.updateLastSteps(glyphs, shapes);
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * Deassign all the glyphs of the provided collection
     *
     * @param glyphs the collection of glyphs to deassign
     * @param record true if this action is to be recorded in the script
     */
    @Override
    public void deassignSetShape (Collection<Glyph> glyphs,
                                  boolean           record)
    {
        Collection<Shape> shapes = null;

        if (record) {
            shapes = Glyph.shapesOf(glyphs);
        }

        // First phase, putting the stems apart
        List<Glyph> stems = new ArrayList<Glyph>();
        List<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

        for (Glyph glyph : glyphsCopy) {
            if (glyph.getShape() == Shape.COMBINING_STEM) {
                stems.add(glyph);
            } else if (glyph.isKnown()) {
                deassignGlyphShape(glyph, false);
            }
        }

        // Second phase dedicated to stems, if any
        if (stems.size() > 0) {
            cancelStems(stems);
        }

        // Record this action in the sheet script?
        if (record) {
            sheet.getScript()
                 .addTask(new DeassignTask(glyphs));

            sheet.updateLastSteps(glyphs, shapes);
        }
    }

    //---------------//
    // fixLargeSlurs //
    //---------------//
    public void fixLargeSlurs (List<Glyph> glyphs,
                               boolean     record)
    {
        // Safer
        if ((glyphs == null) || glyphs.isEmpty()) {
            return;
        }

        List<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);
        List<Glyph> slurs = new ArrayList<Glyph>();

        for (Glyph glyph : glyphsCopy) {
            Glyph slur = SlurGlyph.fixLargeSlur(
                glyph,
                sheet.getSystemOf(glyph));

            if (slur != null) {
                slurs.add(slur);
            }
        }

        if (!slurs.isEmpty()) {
            this.assignSetShape(slurs, Shape.SLUR, false, false);
        }

        // Record this task to the sheet script?
        if (record) {
            sheet.getScript()
                 .addTask(new SlurTask(glyphsCopy));
            sheet.updateLastSteps(glyphsCopy, null);
        }
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

    //-------------//
    // stemSegment //
    //-------------//
    public void stemSegment (Collection<Glyph> givenGlyphs,
                             boolean           isShort)
    {
        // Record this task in the sheet script
        sheet.getScript()
             .addTask(new SegmentTask(isShort, givenGlyphs));

        // Use a copy of glyphs selection
        Collection<Glyph> glyphs = new ArrayList<Glyph>(givenGlyphs);
        Collection<Shape> shapes = Glyph.shapesOf(glyphs);

        deassignSetShape(glyphs, false);

        for (Glyph glyph : glyphs) {
            SystemInfo system = sheet.getSystemAtY(glyph.getContourBox().y);
            sheet.getVerticalsBuilder()
                 .stemSegment(
                Collections.singletonList(glyph),
                system,
                isShort);
        }

        sheet.updateLastSteps(glyphs, shapes);
    }
}
