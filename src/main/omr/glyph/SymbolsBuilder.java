//----------------------------------------------------------------------------//
//                                                                            //
//                        S y m b o l s B u i l d e r                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.glyph;

import omr.glyph.text.Sentence;
import omr.glyph.text.TextType;
import omr.Main;

import omr.glyph.ui.SymbolsEditor;

import omr.score.entity.Note;

import omr.script.AssignTask;
import omr.script.DeassignTask;
import omr.script.ScriptRecording;
import static omr.script.ScriptRecording.*;
import omr.script.SegmentTask;
import omr.script.SlurTask;
import omr.script.TextTask;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.util.BasicTask;
import omr.log.Logger;
import omr.util.Synchronicity;
import static omr.util.Synchronicity.*;

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
     * @param processing specify whether the method must be run (a)synchronously
     * @param glyph the glyph to be assigned
     * @param shape the assigned shape, which may be null
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void assignGlyphShape (Synchronicity         processing,
                                  Glyph                 glyph,
                                  final Shape           shape,
                                  final ScriptRecording record)
    {
        if (glyph != null) {
            if (processing == ASYNC) {
                final Glyph finalGlyph = glyph;
                new BasicTask() {
                        @Override
                        protected Void doInBackground ()
                            throws Exception
                        {
                            assignGlyphShape(SYNC, finalGlyph, shape, record);

                            return null;
                        }
                    }.execute();
            } else {
                Collection<Shape> shapes = null;
                boolean           isCompound = glyph.getId() == 0;
                Collection<Glyph> glyphs;

                if (isCompound) {
                    glyphs = glyph.getParts();
                } else {
                    glyphs = Collections.singleton(glyph);
                }

                // Record this task to the sheet script?
                if (record == RECORDING) {
                    shapes = Glyph.shapesOf(glyphs);

                    sheet.getScript()
                         .addTask(new AssignTask(shape, isCompound, glyphs));
                }

                // If this is a transient glyph (with no Id yet), insert it
                if (isCompound) {
                    glyph = glyphsBuilder.insertGlyph(glyph);
                    logger.info(
                        "Inserted compound #" + glyph.getId() + " as " + shape);
                }

                // Test on glyph weight (noise-like)
                if ((shape == Shape.NOISE) || Evaluator.isBigEnough(glyph)) {
                    // Force a recomputation of glyph parameters
                    // (since environment may have changed since the time they
                    // had been computed)
                    glyphsBuilder.computeGlyphFeatures(glyph);

                    if (shape != null) {
                        switch (shape) {
                        case THICK_BAR_LINE :
                        case THIN_BAR_LINE :
                            sheet.getSystemsBuilder()
                                 .assignGlyphShape(
                                SYNC,
                                glyph,
                                shape,
                                NO_RECORDING);

                            break;

                        case COMBINING_STEM : // TBD
                            break;

                        case NOISE :
                            break;

                        default :
                        }
                    }

                    super.assignGlyphShape(SYNC, glyph, shape, NO_RECORDING);
                }

                // Update final steps?
                if (record == RECORDING) {
                    shapes.add(shape);
                    sheet.updateLastSteps(glyphs, shapes);
                }
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
     * @param processing specify whether we should run (a)synchronously
     * @param glyphs the collection of glyphs
     * @param shape the shape to be assigned
     * @param compound flag to indicate a compound is desired
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void assignSetShape (Synchronicity           processing,
                                final Collection<Glyph> glyphs,
                                final Shape             shape,
                                final boolean           compound,
                                final ScriptRecording   record)
    {
        Collection<Shape> shapes = Glyph.shapesOf(glyphs);

        if ((glyphs != null) && (glyphs.size() > 0)) {
            if (processing == ASYNC) {
                new BasicTask() {
                        @Override
                        protected Void doInBackground ()
                            throws Exception
                        {
                            assignSetShape(
                                SYNC,
                                glyphs,
                                shape,
                                compound,
                                record);

                            return null;
                        }
                    }.execute();
            } else {
                if (compound) {
                    // Build & insert a compound
                    Glyph glyph = glyphsBuilder.buildCompound(glyphs);
                    glyphsBuilder.insertGlyph(glyph);
                    assignGlyphShape(SYNC, glyph, shape, NO_RECORDING);
                } else {
                    int              noiseNb = 0;
                    ArrayList<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

                    for (Glyph glyph : glyphsCopy) {
                        if (glyph.getShape() != Shape.NOISE) {
                            assignGlyphShape(SYNC, glyph, shape, NO_RECORDING);
                        } else {
                            noiseNb++;
                        }
                    }

                    if (logger.isFineEnabled() && (noiseNb > 0)) {
                        logger.fine(noiseNb + " noise glyphs skipped");
                    }
                }

                // Record this task to the sheet script?
                if (record == RECORDING) {
                    sheet.getScript()
                         .addTask(new AssignTask(shape, compound, glyphs));
                    shapes.add(shape);
                    sheet.updateLastSteps(glyphs, shapes);
                }
            }
        }
    }

    //------------//
    // assignText //
    //------------//
    /**
     * Assign text characteristics to a (collection of) textual glyphs
     *
     * @param processing (a)synchronous execution required
     * @param glyphs the impacted glyphs
     * @param textType the type(role) of this textual element
     * @param textContent the content as a string (if not empty)
     * @param record true if this task must be recorded
     */
    public void assignText (Synchronicity           processing,
                            final Collection<Glyph> glyphs,
                            final TextType          textType,
                            final String            textContent,
                            final boolean           record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        assignText(SYNC, glyphs, textType, textContent, record);

                        return null;
                    }
                }.execute();
        } else {
            // Do the job
            for (Glyph glyph : glyphs) {
                // Assign text type
                Sentence sentence = glyph.getTextInfo().getSentence();

                if (sentence != null) {
                    sentence.setTextType(textType);
                }

                // Assign text only if it is not empty
                if ((textContent != null) && (textContent.length() > 0)) {
                    glyph.getTextInfo().setManualContent(textContent);
                }
            }

            // Record this task in the sheet script
            if (record) {
                sheet.getScript()
                     .addTask(new TextTask(textType, textContent, glyphs));
                sheet.updateLastSteps(glyphs, new ArrayList<Shape>()); // No shapes
            }
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
            SystemInfo system = sheet.getSystemOf(stem);
            glyphsBuilder.removeGlyph(stem, system, /* cutSections => */
                                      true);
            assignGlyphShape(SYNC, stem, null, NO_RECORDING);
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
     * @param processing specify whether the method must be run (a)synchronously
     * @param glyph the glyph to deassign
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void deassignGlyphShape (Synchronicity         processing,
                                    final Glyph           glyph,
                                    final ScriptRecording record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        deassignGlyphShape(SYNC, glyph, record);

                        return null;
                    }
                }.execute();
        } else {
            Collection<Glyph> glyphs = null;
            Collection<Shape> shapes = null;

            // Record this action in the sheet script?
            if (record == RECORDING) {
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
                sheet.getSystemsBuilder()
                     .deassignGlyphShape(SYNC, glyph, NO_RECORDING);

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
                    assignGlyphShape(SYNC, p, null, NO_RECORDING);
                }

                // Deassign the glyph itself
                assignGlyphShape(SYNC, glyph, null, NO_RECORDING);

                break;
            }

            // Update last steps?
            if (record == RECORDING) {
                sheet.updateLastSteps(glyphs, shapes);
            }
        }
    }

    //------------------//
    // deassignSetShape //
    //------------------//
    /**
     * Deassign all the glyphs of the provided collection
     *
     * @param processing specify whether the method must be run (a)synchronously
     * @param glyphs the collection of glyphs to deassign
     * @param record specify whether the action must be recorded in the script
     */
    @Override
    public void deassignSetShape (Synchronicity           processing,
                                  final Collection<Glyph> glyphs,
                                  final ScriptRecording   record)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        deassignSetShape(SYNC, glyphs, record);

                        return null;
                    }
                }.execute();
        } else {
            Collection<Shape> shapes = null;

            if (record == RECORDING) {
                shapes = Glyph.shapesOf(glyphs);
            }

            // First phase, putting the stems apart
            List<Glyph> stems = new ArrayList<Glyph>();
            List<Glyph> glyphsCopy = new ArrayList<Glyph>(glyphs);

            for (Glyph glyph : glyphsCopy) {
                if (glyph.getShape() == Shape.COMBINING_STEM) {
                    stems.add(glyph);
                } else if (glyph.isKnown()) {
                    deassignGlyphShape(SYNC, glyph, NO_RECORDING);
                }
            }

            // Second phase dedicated to stems, if any
            if (stems.size() > 0) {
                cancelStems(stems);
            }

            // Record this action in the sheet script?
            if (record == RECORDING) {
                sheet.getScript()
                     .addTask(new DeassignTask(glyphs));

                sheet.updateLastSteps(glyphs, shapes);
            }
        }
    }

    //---------------//
    // fixLargeSlurs //
    //---------------//
    public void fixLargeSlurs (List<Glyph>     glyphs,
                               ScriptRecording record)
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
            assignSetShape(SYNC, slurs, Shape.SLUR, false, NO_RECORDING);
        }

        // Record this task to the sheet script?
        if (record == RECORDING) {
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
    /**
     *
     * @param processing specify whether the method must be run (a)synchronously
     * @param givenGlyphs glyphs to segment in order to retrieve stems
     * @param isShort looking for short (or standard) stems
     */
    public void stemSegment (Synchronicity           processing,
                             final Collection<Glyph> givenGlyphs,
                             final boolean           isShort)
    {
        if (processing == ASYNC) {
            new BasicTask() {
                    @Override
                    protected Void doInBackground ()
                        throws Exception
                    {
                        stemSegment(SYNC, givenGlyphs, isShort);

                        return null;
                    }
                }.execute();
        } else {
            // Record this task in the sheet script
            sheet.getScript()
                 .addTask(new SegmentTask(isShort, givenGlyphs));

            // Use a copy of glyphs selection
            Collection<Glyph> glyphs = new ArrayList<Glyph>(givenGlyphs);
            Collection<Shape> shapes = Glyph.shapesOf(glyphs);

            deassignSetShape(SYNC, glyphs, NO_RECORDING);

            for (Glyph glyph : glyphs) {
                SystemInfo system = sheet.getSystemOf(glyph);
                sheet.getVerticalsBuilder()
                     .stemSegment(
                    Collections.singletonList(glyph),
                    system,
                    isShort);
            }

            sheet.updateLastSteps(glyphs, shapes);
        }
    }
}
