//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h s M o d e l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.ui.selection.EntityService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Class {@code GlyphsModel} is a common model for synchronous glyph handling.
 * <p>
 * Nota: User gesture should trigger actions in GlyphsController which will asynchronously delegate
 * to this model.
 *
 * @author Hervé Bitteur
 */
public class GlyphsModel
{

    private static final Logger logger = LoggerFactory.getLogger(GlyphsModel.class);

    /** Underlying glyph service. */
    protected final EntityService<? extends Glyph> glyphService;

    /** Related Sheet, if any. */
    protected final Sheet sheet;

    /** Latest shape assigned, if any. */
    protected Shape latestShape;

    /**
     * Create an instance of GlyphsModel, with its underlying glyph glyphService.
     *
     * @param sheet        the related sheet (can be null)
     * @param glyphService the related sheetService (cannot be null)
     */
    public GlyphsModel (Sheet sheet,
                        EntityService<? extends Glyph> glyphService)
    {
        Objects.requireNonNull(glyphService, "Attempt to create a GlyphsModel with null service");

        this.sheet = sheet; // Null sheet is allowed (for SampleVerifier use)
        this.glyphService = glyphService;
    }

    //-------------//
    // assignGlyph //
    //-------------//
    /**
     * Assign a Shape to a glyph, inserting the glyph to its containing
     * system and nest if it is still transient.
     *
     * @param glyph     the glyph to be assigned
     * @param staff     the related staff
     * @param interline interline to use
     * @param shape     the assigned shape, which may be null
     * @param grade     the grade about shape
     * @return the assigned glyph (perhaps an original glyph)
     */
    public Glyph assignGlyph (Glyph glyph,
                              Staff staff,
                              int interline,
                              Shape shape,
                              double grade)
    {
        logger.error("HB. Not yet implemented");

        //
        //        final Inter ghost = SymbolFactory.createGhost(shape, grade);
        //        final SystemInfo system = staff.getSystem();
        //        ghost.setStaff(staff);
        //        ghost.setGlyph(glyph);
        //        ghost.setBounds(glyph.getBounds());
        //        system.getSig().addVertex(ghost);
        //        sheet.getStub().setModified(true);
        //
        //        // Edges? this depends on ghost class...
        //        Collection<Link> links = ghost.searchLinks(system, true);
        //
        //        if (links.isEmpty()) {
        //            logger.info("No partners for {}", ghost);
        //        }
        //
        //        sheet.getGlyphIndex().publish(null);
        //        sheet.getInterIndex().publish(ghost);
        //        logger.info("Added {}", ghost);
        //
        //        final Book book = sheet.getStub().getBook();
        //        final SampleRepository repository = book.getSampleRepository();
        //        final SampleSheet sampleSheet = repository.findSampleSheet(sheet);
        //
        //        // TODO: we need staff information (-> interline and pitch)
        //        repository.addSample(shape, glyph, interline, sampleSheet, null);
        //
        //                if (glyph == null) {
        //                    return null;
        //                }
        //
        //                if (shape != null) {
        //                    List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(glyph);
        //
        //                    //            if (system != null) {
        //                    //                glyph = system.register(glyph); // System then nest
        //                    //            } else {
        //                    //                // Insert in nest directly, which assigns an id to the glyph
        //                    glyph = nest.register(glyph);
        //
        //                    //            }
        //                    boolean isTransient = glyph.isTransient();
        //                    logger.debug(
        //                            "Assign {}{} to {}",
        //                            isTransient ? "compound " : "",
        //                            glyph.idString(),
        //                            shape);
        //
        //                    // Remember the latest shape assigned
        //                    setLatestShape(shape);
        //                }
        //
        //                // Do the assignment of the shape to the glyph
        //                glyph.setShape(shape);
        //
        //                // Should we persist the assigned glyph?
        //                if ((shape != null)
        //                    && (grade == Evaluation.MANUAL)
        //                    && (OMR.gui != null)
        //                    && ScoreActions.getInstance().isManualPersisted()) {
        //                    // Record the glyph description to disk
        //                    SampleRepository.getInstance().recordOneGlyph(glyph, sheet);
        //                }
        //
        return glyph;
    }

    //    //-------------//
    //    // assignGlyph //
    //    //-------------//
    //    /**
    //     * Assign a shape to the selected collection of glyphs.
    //     *
    //     * @param glyph the glyph to be assigned
    //     * @param shape the shape to be assigned
    //     * @param staff the related staff
    //     * @param grade the grade we have wrt the assigned shape
    //     */
    //    public void assignGlyph (Glyph glyph,
    //                             Shape shape,
    //                             Staff staff,
    //                             double grade)
    //    {
    //        // NO! interline is wrong!!!
    //        assignGlyph(glyph, sheet.getScale().getInterline(), shape, grade);
    //    }
    //
    //-----------------//
    // getGlyphService //
    //-----------------//
    /**
     * Report the underlying glyph glyphService.
     *
     * @return the related glyph glyphService
     */
    public EntityService<? extends Glyph> getGlyphService ()
    {
        return glyphService;
    }

    //----------------//
    // getLatestShape //
    //----------------//
    /**
     * Report the latest non null shape that was assigned, or null if
     * none.
     *
     * @return latest shape assigned, or null if none
     */
    public Shape getLatestShape ()
    {
        return latestShape;
    }

    //----------------//
    // setLatestShape //
    //----------------//
    /**
     * Assign the latest useful shape.
     *
     * @param shape the current / latest shape
     */
    public void setLatestShape (Shape shape)
    {
        if (shape != Shape.GLYPH_PART) {
            latestShape = shape;
        }
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the model underlying sheet.
     *
     * @return the underlying sheet instance
     */
    public Sheet getSheet ()
    {
        return sheet;
    }

    //---------------//
    // deassignGlyph //
    //---------------//
    /**
     * Deassign the shape of a glyph.
     *
     * @param glyph the glyph to deassign
     */
    protected void deassignGlyph (Glyph glyph)
    {
        // Assign the null shape to the glyph
        ///assignGlyph(glyph, sheet.getInterline(), null, Evaluation.ALGORITHM);
    }

    //-------------//
    // deleteGlyph //
    //-------------//
    /**
     * Delete a glyph.
     *
     * @param glyph the glyph to delete
     */
    protected void deleteGlyph (Glyph glyph)
    {
        logger.error("HB. Not yet implemented");

        //        if (glyph == null) {
        //            return;
        //        }
        //
        //        if (!glyph.isVirtual()) {
        //            logger.warn("Attempt to delete non-virtual {}", glyph.idString());
        //
        //            return;
        //        }
        //
        //        SystemInfo system = sheet.getSystemOf(glyph);
        //
        //        // Special case for ledger glyph
        //        if (glyph.getShape() == Shape.LEDGER) {
        //            StaffInfo staff = system.getStaffAt(glyph.getCenter());
        //
        //            ///staff.removeLedger(glyph);
        //            //TODO: handle a LedgerInter instead!
        //        }
        //
        //        if (system != null) {
        //            system.removeGlyph(glyph);
        //        }
        //
        //        nest.removeGlyph(glyph);
    }
}
