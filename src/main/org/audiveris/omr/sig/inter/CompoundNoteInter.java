//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                C o m p o u n d N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import static org.audiveris.omr.glyph.Shape.*;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.symbol.InterFactory;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.InterTracker;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.CompoundNoteSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.Link;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import org.audiveris.omr.ui.symbol.Symbols;

/**
 * Class <code>CompoundNoteInter</code> represents a head combined with a stem.
 * <p>
 * Instances of this class are meant to be temporary, not put in SIG, just to ease manual insertion
 * of quarter and half notes.
 * <p>
 * When such compound is dropped, it gets replaced by head + stem + head-stem relation.
 * <p>
 * TODO:
 *
 * Tracker
 *
 * @author Hervé Bitteur
 */
public class CompoundNoteInter
        extends AbstractInter
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(CompoundNoteInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Included head. */
    private HeadInter head;

    /** Included stem. */
    private StemInter stem;

    /** Related model, if any. */
    private Model model;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>HeadInter</code> object.
     *
     * @param glyph  the underlying glyph if any
     * @param bounds the object bounds
     * @param shape  the underlying shape
     * @param grade  quality grade
     */
    public CompoundNoteInter (Glyph glyph,
                              Rectangle bounds,
                              Shape shape,
                              Double grade)
    {
        super(glyph, bounds, shape, grade);

        head = (HeadInter) InterFactory.createManual(
                isQuarter() ? NOTEHEAD_BLACK : NOTEHEAD_VOID, null);

        stem = (StemInter) InterFactory.createManual(STEM, null);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation,
                               Alignment alignment)
    {
        final CompoundNoteSymbol noteSymbol = (CompoundNoteSymbol) symbol;
        model = noteSymbol.getModel(font, dropLocation, alignment);

        // We snap head to lines/ledgers for y
        final Double y = HeadInter.getSnapOrdinate(model.headCenter, staff);

        if (y != null) {
            model.translate(0, y - model.headCenter.getY());
        }

        setBounds(model.box.getBounds());
        logger.debug("@{}: {}", dropLocation, model);

        return true;
    }

    //---------------//
    // getHeadCenter //
    //---------------//
    /**
     * Report the head center (which is vertically shifted from compound center).
     *
     * @return head center
     */
    public Point2D getHeadCenter ()
    {
        if (model != null) {
            return (Point2D) model.headCenter.clone();
        } else {
            return getCenter2D();
        }
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    @Override
    public Point2D getRelationCenter ()
    {
        return getHeadCenter();
    }

    //------------//
    // getTracker //
    //------------//
    @Override
    public InterTracker getTracker (Sheet sheet)
    {
        return new HeadInter.Tracker(this, sheet);
    }

    //----------//
    // getModel //
    //----------//
    /**
     * Build a poor-man model, just from staff and bounds (from glyph?).
     *
     * @return the created model
     */
    private Model buildModel ()
    {
        if (bounds == null || staff == null) {
            return null;
        }

        final Point center = getCenter();
        final int staffInterline = staff.getSpecificInterline();
        final int halfInterline = staffInterline / 2;
        final Point hCenter = new Point(
                center.x,
                isUp() ? bounds.y + bounds.height - halfInterline : bounds.y + halfInterline);
        final CompoundNoteSymbol symbol = (CompoundNoteSymbol) Symbols.getSymbol(shape);
        final MusicFont font = MusicFont.getBaseFont(staffInterline);
        deriveFrom(symbol, staff.getSystem().getSheet(), font, hCenter, AREA_CENTER);
        logger.debug("{}", model);

        return model;
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        final List<UITask> tasks = new ArrayList<>();

        if (model == null) {
            model = buildModel();
        }

        if (model == null) {
            cancel.value = true;
            return tasks;
        }

        // We "convert" compound note addition into:
        // head, stem, head-stem relation, beam-stem relations?, headChord?
        final SystemInfo system = staff.getSystem();
        final SIGraph theSig = system.getSig();
        final int profile = Math.max(getProfile(), system.getProfile());

        // Look for beams around, this also updates head and stem data
        final Collection<Link> stemLinks = stem.lookupBeamLinks(system, profile);

        final Rectangle headBounds = model.headBox.getBounds();
        tasks.add(new AdditionTask(theSig, head, headBounds, Collections.emptySet()));

        final Rectangle stemBounds = model.stemBox.getBounds();
        tasks.add(new AdditionTask(theSig, stem, stemBounds, Collections.emptySet()));

        tasks.add(new LinkTask(theSig, head, stem, new HeadStemRelation()));

        // Stem to beams
        for (Link link : stemLinks) {
            tasks.add(new LinkTask(theSig, link.partner, stem, new BeamStemRelation()));
        }

        if (system.getSheet().getStub().getLatestStep().compareTo(OmrStep.CHORDS) >= 0) {
            // Create the related head chord
            final HeadChordInter chord = new HeadChordInter(null);
            tasks.add(new AdditionTask(theSig, chord, stemBounds, Collections.emptySet()));
            tasks.add(new LinkTask(theSig, chord, head, new Containment()));
            tasks.add(new LinkTask(theSig, chord, stem, new ChordStemRelation()));
        }

        // Addition of needed ledgers
        final Point2D headCenter = GeoUtil.center2D(headBounds);
        tasks.addAll(HeadInter.getNeededLedgerAdditions(headCenter, staff));

        return tasks;
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, look for beams nearby.
     *
     * @return collection of links, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());

        if (model == null) {
            model = buildModel();
        }

        head.setStaff(staff);
        head.setBounds(model.headBox.getBounds());

        stem.setStaff(staff);
        stem.setBounds(model.stemBox.getBounds());

        return stem.lookupBeamLinks(system, profile);
    }

    //-----------//
    // isQuarter //
    //-----------//
    private boolean isQuarter ()
    {
        return (shape == QUARTER_NOTE_UP) || (shape == QUARTER_NOTE_DOWN);
    }

    //------//
    // isUp //
    //------//
    private boolean isUp ()
    {
        return (shape == QUARTER_NOTE_UP) || (shape == HALF_NOTE_UP);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        public Rectangle2D box; // CompoundNote bounds

        public Rectangle2D headBox; // Head bounds

        public Rectangle2D stemBox; // Stem bounds

        public Point2D headCenter; // Head center

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(headCenter, dx, dy);
            GeoUtil.translate2D(box, dx, dy);
            GeoUtil.translate2D(headBox, dx, dy);
            GeoUtil.translate2D(stemBox, dx, dy);
        }

        @Override
        public String toString ()
        {
            return new StringBuilder("noteModel{")
                    .append(" box:").append(box)
                    .append(" headCenter:").append(headCenter)
                    .append('}').toString();
        }
    }
}
