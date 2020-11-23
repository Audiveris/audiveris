//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d I n t e r                                       //
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
package org.audiveris.omr.sig.inter;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.image.Anchored.Anchor;
import org.audiveris.omr.image.ShapeDescriptor;
import org.audiveris.omr.image.Template;
import org.audiveris.omr.image.TemplateFactory;
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.run.Orientation.VERTICAL;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.Staff.IndexedLedger;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.note.NotePosition;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.ChordStemRelation;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.sig.relation.HeadStemRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterTracker;
import org.audiveris.omr.sig.ui.AdditionTask;
import org.audiveris.omr.sig.ui.LinkTask;
import org.audiveris.omr.sig.ui.UITask;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.ByteUtil;
import org.audiveris.omr.util.Corner;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.*;
import static org.audiveris.omr.util.VerticalSide.*;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.WrappedBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code HeadInter} represents a note head, that is any head shape including
 * whole and breve, but not a rest.
 * <p>
 * These rather round-shaped symbols are retrieved via template-matching technique.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head")
@XmlAccessorType(XmlAccessType.NONE)
public class HeadInter
        extends AbstractNoteInter
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(HeadInter.class);

    // Persistent data
    //----------------
    //
    /** Deprecated. Old mirror instance, if any. */
    @Deprecated
    @XmlIDREF
    @XmlAttribute(name = "mirror")
    protected AbstractInter oldMirror;

    /** Absolute location of head template pivot. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.PointAdapter.class)
    private final Point pivot;

    /** Relative pivot position WRT head. */
    @XmlAttribute
    private final Anchor anchor;

    // Transient data
    //---------------
    //
    /** Shape template descriptor. */
    private ShapeDescriptor descriptor;

    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param pivot   the template pivot
     * @param anchor  relative pivot configuration
     * @param bounds  the object bounds
     * @param shape   the underlying shape
     * @param impacts the grade details
     * @param staff   the related staff
     * @param pitch   the note pitch
     */
    public HeadInter (Point pivot,
                      Anchor anchor,
                      Rectangle bounds,
                      Shape shape,
                      GradeImpacts impacts,
                      Staff staff,
                      Double pitch)
    {
        super(null, bounds, shape, impacts, staff, pitch);
        this.pivot = pivot;
        this.anchor = anchor;
    }

    /**
     * Creates a new {@code HeadInter} object.
     *
     * @param pivot  the template pivot
     * @param anchor relative pivot configuration
     * @param bounds the object bounds
     * @param shape  the underlying shape
     * @param grade  quality grade
     * @param staff  the related staff
     * @param pitch  the note pitch
     */
    public HeadInter (Point pivot,
                      Anchor anchor,
                      Rectangle bounds,
                      Shape shape,
                      double grade,
                      Staff staff,
                      Double pitch)
    {
        super(null, bounds, shape, grade, staff, pitch);
        this.pivot = pivot;
        this.anchor = anchor;
    }

    /** No-arg constructor needed by JAXB. */
    private HeadInter ()
    {
        this.pivot = null;
        this.anchor = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        if (ShapeSet.StemHeads.contains(shape)) {
            setAbnormal(true); // No stem linked yet
        }
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        if (ShapeSet.StemHeads.contains(shape)) {
            // Check if a stem is connected
            setAbnormal(!sig.hasRelation(this, HeadStemRelation.class));
        }

        return isAbnormal();
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        if (!super.contains(point)) {
            return false;
        }

        Line2D midLine = getMidLine();

        if (midLine != null) {
            // This head is "shared" with another head, and we use the point location with respect
            // to head mid line to disambiguate which head is pointed at.
            return midLine.relativeCCW(point) < 0;
        }

        return true;
    }

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
        // Needed to get head bounds
        super.deriveFrom(symbol, sheet, font, dropLocation, alignment);

        // For a note head, we snap to stems for x & lines/ledgers for y
        if (staff != null) {
            boolean modified = false;

            final Double x = getSnapAbscissa();
            if (x != null) {
                dropLocation.x = (int) Math.rint(x);
                modified = true;
            }

            final Double y = getSnapOrdinate();
            if (y != null) {
                dropLocation.y = (int) Math.rint(y);
                modified = true;
            }

            if (modified) {
                super.deriveFrom(symbol, sheet, font, dropLocation, alignment);
            }
        }

        return true;
    }

    //-----------//
    // duplicate //
    //-----------//
    /**
     * Duplicate this head, keeping the same head shape (black vs void).
     *
     * @return a duplicate head
     */
    public HeadInter duplicate ()
    {
        return duplicateAs(shape);
    }

    //-------------//
    // duplicateAs //
    //-------------//
    /**
     * Build a duplicate of this head, perhaps with a different shape.
     * <p>
     * Used when say a half head is shared, with flag/beam on one side.
     *
     * @param shape precise shape for the duplicate
     * @return duplicate
     */
    public HeadInter duplicateAs (Shape shape)
    {
        HeadInter clone = new HeadInter(pivot, anchor, bounds, shape, impacts, staff, pitch);

        clone.setGlyph(this.glyph);

        if (impacts == null) {
            clone.setGrade(grade);
        }

        return clone;
    }

    //---------------//
    // getAccidental //
    //---------------//
    /**
     * Report the (local) accidental, if any, related to this head.
     *
     * @return the related accidental, or null
     */
    public AlterInter getAccidental ()
    {
        for (Relation rel : sig.getRelations(this, AlterHeadRelation.class)) {
            return (AlterInter) sig.getOppositeInter(this, rel);
        }

        return null;
    }

    //---------------//
    // getAlteration //
    //---------------//
    /**
     * Report the actual alteration of this note, taking into account the accidental of
     * this note if any, the accidental of previous note with same step within the same
     * measure, a tie from previous measure and finally the current key signature.
     *
     * @param fifths fifths value for current key signature
     * @return the actual alteration
     */
    public int getAlteration (Integer fifths)
    {
        return HeadInter.this.getAlteration(fifths, true);
    }

    //---------------//
    // getAlteration //
    //---------------//
    /**
     * Report the actual alteration of this note, taking into account the accidental of
     * this note if any, the accidental of previous note with same step within the same
     * measure, a tie from previous measure and finally the current key signature.
     *
     * @param fifths fifths value for current key signature
     * @param useTie true to use tie for check
     * @return the actual alteration
     */
    public int getAlteration (Integer fifths,
                              boolean useTie)
    {
        // Look for local/measure accidental
        AlterInter accidental = getMeasureAccidental();

        if (accidental != null) {
            return AlterInter.alterationOf(accidental);
        }

        if (useTie) {
            // Look for tie from previous measure (same system or previous system)
            for (Relation rel : sig.getRelations(this, SlurHeadRelation.class)) {
                SlurInter slur = (SlurInter) sig.getOppositeInter(this, rel);

                if (slur.isTie() && (slur.getHead(HorizontalSide.RIGHT) == this)) {
                    // Is the starting head in same system?
                    HeadInter startHead = slur.getHead(HorizontalSide.LEFT);

                    if (startHead != null) {
                        // Use start head alter
                        return startHead.getAlteration(fifths);
                    }

                    // Use slur extension to look into previous system
                    SlurInter prevSlur = slur.getExtension(HorizontalSide.LEFT);

                    if (prevSlur != null) {
                        startHead = prevSlur.getHead(HorizontalSide.LEFT);

                        if (startHead != null) {
                            // Use start head alter
                            return startHead.getAlteration(fifths);
                        }
                    }

                    // TODO: Here we should look in previous sheet/page...
                }
            }
        }

        // Finally, use the current key signature
        if (fifths != null) {
            return KeyInter.getAlterFor(getStep(), fifths);
        }

        // Nothing found, so...
        return 0;
    }

    //----------------------//
    // getMeasureAccidental //
    //----------------------//
    /**
     * Report the accidental (if any) which applies to this head or to a previous
     * compatible one in the same measure.
     *
     * @return the measure scoped accidental found or null
     */
    public AlterInter getMeasureAccidental ()
    {
        // Look for local accidental
        AlterInter accidental = getAccidental();

        if (accidental != null) {
            return accidental;
        }

        // Look for previous accidental with same note step in the same measure
        // Let's avoid the use of time slots (which would require RHYTHMS step to be done!)
        Measure measure = getChord().getMeasure();
        List<Inter> heads = new ArrayList<>();

        for (HeadChordInter headChord : measure.getHeadChords()) {
            heads.addAll(headChord.getMembers());
        }

        boolean started = false;
        Collections.sort(heads, Inters.byReverseCenterAbscissa);

        for (Inter inter : heads) {
            HeadInter head = (HeadInter) inter;

            if (head == this) {
                started = true;
            } else if (started && (head.getStep() == getStep())
                               && (head.getOctave() == getOctave())
                               && (head.getStaff() == getStaff())) {
                accidental = head.getAccidental();

                if (accidental != null) {
                    return accidental;
                }
            }
        }

        return null;
    }

    //----------//
    // getChord //
    //----------//
    /**
     * Report the containing (head) chord, if any.
     *
     * @return containing chord or null
     */
    @Override
    public HeadChordInter getChord ()
    {
        return (HeadChordInter) getEnsemble();
    }

    //---------------//
    // getCoreBounds //
    //---------------//
    @Override
    public Rectangle getCoreBounds ()
    {
        if (coreBounds == null) {
            coreBounds = shrink(getBounds()).getBounds();
        }

        return coreBounds;
    }

    //---------------//
    // getDescriptor //
    //---------------//
    /**
     * Report the descriptor used to generate this head shape with proper size.
     *
     * @return related template descriptor
     */
    public ShapeDescriptor getDescriptor ()
    {
        if (descriptor == null) {
            final int pointSize = staff.getHeadPointSize();
            descriptor = TemplateFactory.getInstance().getCatalog(pointSize).getDescriptor(shape);
        }

        return descriptor;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //------------//
    // getMidLine //
    //------------//
    /**
     * Report the separating line for shared heads.
     * <p>
     * The line is nearly vertical, oriented from head to stem.
     * Thus, the relativeCCW is negative for points located on proper head half.
     *
     * @return oriented middle line or null if not mirrored
     */
    public Line2D getMidLine ()
    {
        if (getMirror() == null) {
            return null;
        }

        final Rectangle box = getBounds();

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            final HeadStemRelation rel = (HeadStemRelation) relation;

            if (rel.getHeadSide() == HorizontalSide.LEFT) {
                return LineUtil.bisector(
                        new Point(box.x, box.y + box.height),
                        new Point(box.x + box.width, box.y));
            } else {
                return LineUtil.bisector(
                        new Point(box.x + box.width, box.y),
                        new Point(box.x, box.y + box.height));
            }
        }

        return null;
    }

    //-------------------//
    // getRelationCenter //
    //-------------------//
    /**
     * {@inheritDoc}
     * <p>
     * For shared heads, the relation center is slightly shifted to the containing chord.
     *
     * @return the head relation center, shifted for a shared head
     */
    @Override
    public Point2D getRelationCenter ()
    {
        final Point center = getCenter();

        if (getMirror() == null) {
            return center;
        }

        final Rectangle box = getBounds();
        final double dx = box.width / 5.0;
        final double dy = box.height / 5.0;

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            final HeadStemRelation rel = (HeadStemRelation) relation;

            if (rel.getHeadSide() == HorizontalSide.LEFT) {
                return new Point2D.Double(center.getX() - dx, center.getY() + dy);
            } else {
                return new Point2D.Double(center.getX() + dx, center.getY() - dy);
            }
        }

        return center; // Should not occur...
    }

    //--------------//
    // getSideStems //
    //--------------//
    /**
     * Report the stems linked to this head, organized by head side.
     *
     * @return the map of linked stems, organized by head side
     * @see #getStems()
     */
    public Map<HorizontalSide, Set<StemInter>> getSideStems ()
    {
        // Split connected stems into left and right sides
        final Map<HorizontalSide, Set<StemInter>> map = new EnumMap<>(HorizontalSide.class);

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            HeadStemRelation rel = (HeadStemRelation) relation;
            HorizontalSide side = rel.getHeadSide();
            Set<StemInter> set = map.get(side);

            if (set == null) {
                map.put(side, set = new LinkedHashSet<>());
            }

            set.add((StemInter) sig.getEdgeTarget(rel));
        }

        return map;
    }

    //-----------------------//
    // getStemReferencePoint //
    //-----------------------//
    /**
     * Report the reference point for a stem connection.
     *
     * @param anchor desired side for stem (typically TOP_RIGHT_STEM or BOTTOM_LEFT_STEM)
     * @return the reference point
     */
    public Point2D getStemReferencePoint (Anchor anchor)
    {
        ShapeDescriptor desc = getDescriptor();
        Rectangle templateBox = desc.getBounds(this.getBounds());
        Point ref = templateBox.getLocation();
        Point offset = desc.getOffset(anchor);
        ref.translate(offset.x, offset.y);

        return ref;
    }

    //----------//
    // getStems //
    //----------//
    /**
     * Report the stems linked to this head, whatever the side.
     *
     * @return set of linked stems
     * @see #getSideStems()
     */
    public Set<StemInter> getStems ()
    {
        final Set<StemInter> set = new LinkedHashSet<>();

        for (Relation relation : sig.getRelations(this, HeadStemRelation.class)) {
            set.add((StemInter) sig.getEdgeTarget(relation));
        }

        return set;
    }

    //------------//
    // getTracker //
    //------------//
    @Override
    public InterTracker getTracker (Sheet sheet)
    {
        return new Tracker(this, sheet);
    }

    //----------//
    // overlaps //
    //----------//
    /**
     * Precise overlap implementation between notes, based on their pitch value.
     * <p>
     * TODO: A clean overlap check might use true distance tables around each of the heads.
     * For the time being, we simply play with the width and area of intersection rectangle.
     *
     * @param that another inter (perhaps a note)
     * @return true if overlap is detected
     */
    @Override
    public boolean overlaps (Inter that)
    {
        // Specific between notes
        if (that instanceof HeadInter) {
            if (this.isVip() && that.isVip()) {
                logger.info("HeadInter checking overlaps between {} and {}", this, that);
            }

            HeadInter thatHead = (HeadInter) that;

            // Check pitch distance
            if (this.getStaff() == that.getStaff()) {
                if (Math.abs(this.getIntegerPitch() - thatHead.getIntegerPitch()) > 1) {
                    return false;
                }
            }

            // Check horizontal distance
            Rectangle thisBounds = this.getBounds();
            Rectangle thatBounds = thatHead.getBounds();
            Rectangle common = thisBounds.intersection(thatBounds);
            int minWidth = Math.min(thisBounds.width, thatBounds.width);
            double widthRatio = (double) common.width / minWidth;

            if (widthRatio <= constants.maxOverlapDxRatio.getValue()) {
                return false;
            }

            // Check area
            int thisArea = thisBounds.width * thisBounds.height;
            int thatArea = thatBounds.width * thatBounds.height;
            int minArea = Math.min(thisArea, thatArea);
            int commonArea = common.width * common.height;
            double areaRatio = (double) commonArea / minArea;
            boolean areaRes = areaRatio > constants.maxOverlapAreaRatio.getValue();

            return areaRes;
        }

        // Basic test
        return super.overlaps(that);
    }

    //--------//
    // preAdd //
    //--------//
    @Override
    public List<? extends UITask> preAdd (WrappedBoolean cancel)
    {
        final List<UITask> tasks = new ArrayList<>();
        final SystemInfo system = staff.getSystem();
        final SIGraph theSig = system.getSig();

        // Include standard addition task for this head
        final Collection<Link> links = searchLinks(staff.getSystem());
        tasks.add(new AdditionTask(theSig, this, getBounds(), links));

        if (system.getSheet().getStub().getLatestStep()
                .compareTo(org.audiveris.omr.step.Step.CHORDS) >= 0) {
            // If we link head to a stem, create/update the related head chord
            boolean stemFound = false;

            for (Link link : links) {
                if (link.relation instanceof HeadStemRelation) {
                    final StemInter stem = (StemInter) link.partner;
                    final HeadChordInter headChord;
                    final List<HeadChordInter> stemChords = stem.getChords();

                    if (stemChords.isEmpty()) {
                        // Create a chord based on stem
                        headChord = new HeadChordInter(-1);
                        tasks.add(
                                new AdditionTask(
                                        theSig, headChord, stem.getBounds(),
                                        Arrays.asList(new Link(stem, new ChordStemRelation(), true))));
                    } else {
                        if (stemChords.size() > 1) {
                            logger.warn("Stem shared by several chords, picked one");
                        }

                        headChord = stemChords.get(0);
                    }

                    // Declare head part of head-chord
                    tasks.add(new LinkTask(theSig, headChord, this, new Containment()));
                    stemFound = true;

                    break;
                }
            }

            if (!stemFound) {
                // Head without stem
                HeadChordInter headChord = new HeadChordInter(-1);
                tasks.add(new AdditionTask(theSig, headChord, getBounds(),
                                           Arrays.asList(new Link(this, new Containment(), true))));
            }
        }

        // Addition of needed ledgers
        tasks.addAll(neededLedgerAdditions());

        return tasks;
    }

    //---------//
    // preEdit //
    //---------//
    @Override
    public List<? extends UITask> preEdit (InterEditor editor)
    {
        final List<UITask> tasks = new ArrayList<>(super.preEdit(editor));

        // Addition of needed ledgers
        tasks.addAll(neededLedgerAdditions());

        return tasks;
    }

    //---------------//
    // retrieveGlyph //
    //---------------//
    /**
     * Use descriptor to build an underlying glyph.
     *
     * @param image the image to read pixels from
     * @return the underlying glyph or null if failed
     */
    public Glyph retrieveGlyph (ByteProcessor image)
    {
        getDescriptor();

        final Sheet sheet = staff.getSystem().getSheet();
        final Template tpl = descriptor.getTemplate();
        final Rectangle interBox = getBounds();
        final Rectangle descBox = descriptor.getBounds(interBox);

        // Foreground points (coordinates WRT descBox)
        final List<Point> fores = tpl.getForegroundPixels(descBox, image);

        if (fores.isEmpty()) {
            logger.info("No foreground pixels for {}", this);

            return null;
        }

        final Rectangle foreBox = PointUtil.boundsOf(fores);

        final ByteProcessor buf = new ByteProcessor(foreBox.width, foreBox.height);
        ByteUtil.raz(buf);

        for (Point p : fores) {
            buf.set(p.x - foreBox.x, p.y - foreBox.y, 0);
        }

        // Runs
        RunTable runTable = new RunTableFactory(VERTICAL).createTable(buf);

        // Glyph
        glyph = sheet.getGlyphIndex().registerOriginal(
                new Glyph(descBox.x + foreBox.x, descBox.y + foreBox.y, runTable));

        // Use glyph bounds as inter bounds
        bounds = glyph.getBounds();

        return glyph;
    }

    //-------------//
    // searchLinks //
    //-------------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, look for stem to allow head attachment.
     *
     * @return stem link, perhaps empty
     */
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        Link link = null;

        if (ShapeSet.StemHeads.contains(shape)) {
            List<Inter> systemStems = system.getSig().inters(StemInter.class);
            Collections.sort(systemStems, Inters.byAbscissa);

            link = lookupLink(systemStems, system);
        }

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, HeadStemRelation.class);
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrade)
    {
        boolean upgraded = false;

        // NOTA: no need to specify a particular version, existence of non-null oldMirror is enough
        if (oldMirror != null) {
            // Upgrade from oldMirror field to MirrorRelation
            // [We keep only Head mirrors (not HeadChord "mirrors" any more)]
            setMirror(oldMirror);
            oldMirror = null;
            upgraded = true;
        }

        return upgraded;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this Head instance and a stem nearby.
     * <p>
     * 1/ Use a lookup area on each horizontal side of the head to filter candidate stems.
     * 2/ Select the best connection among the compatible candidates.
     *
     * @param candidateStems abscissa-ordered collection of candidate stems
     * @param system         containing system
     * @return the link found or null
     */
    public Link lookupLink (List<Inter> candidateStems,
                            SystemInfo system)
    {
        if (candidateStems.isEmpty()) {
            return null;
        }

        final Scale scale = system.getSheet().getScale();
        final int maxHeadInDx = scale.toPixels(HeadStemRelation.getXInGapMaximum(manual));
        final int maxHeadOutDx = scale.toPixels(HeadStemRelation.getXOutGapMaximum(manual));
        final int maxYGap = scale.toPixels(HeadStemRelation.getYGapMaximum(manual));

        Link bestLink = null;
        double bestGrade = 0;

        for (Corner corner : Corner.values) {
            Point refPt = PointUtil.rounded(getStemReferencePoint(corner.stemAnchor()));
            int xMin = refPt.x - ((corner.hSide == RIGHT) ? maxHeadInDx : maxHeadOutDx);
            int yMin = refPt.y - ((corner.vSide == TOP) ? maxYGap : 0);
            Rectangle luBox = new Rectangle(xMin, yMin, maxHeadInDx + maxHeadOutDx, maxYGap);
            List<Inter> stems = Inters
                    .intersectedInters(candidateStems, GeoOrder.BY_ABSCISSA, luBox);
            int xDir = (corner.hSide == RIGHT) ? 1 : (-1);

            for (Inter inter : stems) {
                StemInter stem = (StemInter) inter;
                final Point2D start = stem.getTop();
                final Point2D stop = stem.getBottom();

                double crossX = LineUtil.xAtY(start, stop, refPt.getY());
                final double xGap = xDir * (crossX - refPt.getX());
                final double yGap;

                if (refPt.getY() < start.getY()) {
                    yGap = start.getY() - refPt.getY();
                } else if (refPt.getY() > stop.getY()) {
                    yGap = refPt.getY() - stop.getY();
                } else {
                    yGap = 0;
                }

                HeadStemRelation rel = new HeadStemRelation();
                rel.setInOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), manual);

                if (rel.getGrade() >= rel.getMinGrade()) {
                    if ((bestLink == null) || (rel.getGrade() > bestGrade)) {
                        rel.setExtensionPoint(refPt); // Approximately
                        bestLink = new Link(stem, rel, true);
                        bestGrade = rel.getGrade();
                    }
                }
            }
        }

        return bestLink;
    }

    //--------------------//
    // getShrinkHoriRatio //
    //--------------------//
    /**
     * Report horizontal ratio to check overlap
     *
     * @return horizontal ratio
     */
    public static double getShrinkHoriRatio ()
    {
        return constants.shrinkHoriRatio.getValue();
    }

    //--------------------//
    // getShrinkVertRatio //
    //--------------------//
    /**
     * Report vertical ratio to check overlap
     *
     * @return vertical ratio
     */
    public static double getShrinkVertRatio ()
    {
        return constants.shrinkVertRatio.getValue();
    }

    //----------------------//
    // getNeededLedgerLines //
    //----------------------//
    /**
     * Report the ledger lines that should be added to support this head.
     *
     * @return the sequence of needed ledger lines
     */
    private List<Line2D> getNeededLedgerLines ()
    {
        if (staff == null) {
            return Collections.emptyList();
        }

        final Point center = getCenter();
        final NotePosition np = staff.getNotePosition(center);
        final int thePitch = (int) Math.rint(np.getPitchPosition());
        List<Line2D> lines = null;

        if (Math.abs(thePitch) >= 6) {
            final IndexedLedger iLedger = np.getLedger();
            final int closestIndex = (iLedger != null) ? iLedger.index : 0;
            final Scale scale = staff.getSystem().getSheet().getScale();
            final int ledgerLength = scale.toPixels(LedgerInter.getDefaultLength());
            final int x1 = center.x - ledgerLength / 2;
            final int x2 = center.x + ledgerLength / 2;
            final int dir = Integer.signum(thePitch);

            for (int p = 6 * dir + 2 * closestIndex; p * dir <= thePitch * dir; p += 2 * dir) {
                int y = (int) Math.rint(staff.pitchToOrdinate(center.x, p));

                if (lines == null) {
                    lines = new ArrayList<>();
                }

                lines.add(new Line2D.Double(x1, y, x2, y));
            }
        }

        return (lines != null) ? lines : Collections.emptyList();
    }

    //-----------------//
    // getSnapAbscissa //
    //-----------------//
    /**
     * Report the theoretical abscissa of head center when correctly aligned with
     * a suitable stem.
     * <p>
     * Required properties: staff, shape, bounds
     *
     * @return the proper abscissa if any, null otherwise
     */
    private Double getSnapAbscissa ()
    {
        if (staff == null) {
            return null;
        }

        if (ShapeSet.StemLessHeads.contains(shape)) {
            return null;
        }

        // Stems nearby?
        final Collection<Link> links = searchLinks(staff.getSystem());

        for (Link link : links) {
            // We can have at most one link
            StemInter stem = (StemInter) link.partner;
            double stemX = LineUtil.xAtY(stem.getMedian(), getCenter().y);
            double halfWidth = getBounds().width / 2.0;
            HorizontalSide headSide = (stemX < getCenter().x) ? LEFT : RIGHT;

            return (headSide == LEFT) ? stemX + halfWidth : stemX - halfWidth;
        }

        return null;
    }

    //-----------------//
    // getSnapOrdinate //
    //-----------------//
    /**
     * Report the theoretical ordinate of head center when correctly aligned with staff
     * lines and ledgers.
     * <p>
     * Required properties: staff, bounds
     *
     * @return the proper ordinate if any, null otherwise
     */
    private Double getSnapOrdinate ()
    {
        if (staff == null) {
            return null;
        }

        Point center = getCenter();
        NotePosition notePosition = staff.getNotePosition(center);
        double doublePitch = notePosition.getPitchPosition();
        double roundedPitch = Math.rint(doublePitch);
        double y = staff.pitchToOrdinate(center.x, roundedPitch);

        return y;
    }

    //-----------------------//
    // neededLedgerAdditions //
    //-----------------------//
    /**
     * Report UI tasks to add needed ledgers.
     *
     * @return the list of ledger additions, perhaps empty
     */
    private List<UITask> neededLedgerAdditions ()
    {
        final List<UITask> tasks = new ArrayList<>();
        final SIGraph theSig = (sig != null) ? sig : staff.getSystem().getSig();

        for (Line2D line : getNeededLedgerLines()) {
            LedgerInter ledger = new LedgerInter(line, LedgerInter.DEFAULT_THICKNESS, 1);
            ledger.setManual(true);
            ledger.setStaff(staff);
            tasks.add(new AdditionTask(theSig, ledger, ledger.getBounds(), Collections.emptySet()));
        }

        return tasks;
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink a bit a bounding bounds when checking for note overlap.
     *
     * @param box the bounding bounds
     * @return the shrunk bounds
     */
    public static Rectangle2D shrink (Rectangle box)
    {
        double newWidth = constants.shrinkHoriRatio.getValue() * box.width;
        double newHeight = constants.shrinkVertRatio.getValue() * box.height;

        return new Rectangle2D.Double(
                box.getCenterX() - (newWidth / 2.0),
                box.getCenterY() - (newHeight / 2.0),
                newWidth,
                newHeight);
    }

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends GradeImpacts
    {

        private static final String[] NAMES = new String[]{"dist"};

        private static final double[] WEIGHTS = new double[]{1};

        public Impacts (double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
        }
    }

    //----------------------//
    // getMaxOverlapDxRatio //
    //----------------------//
    /**
     * Report the maximum abscissa overlap (in ratio of head width) before two heads are
     * considered as overlapping.
     *
     * @return maximum acceptable abscissa ratio
     */
    public static double getMaxOverlapDxRatio ()
    {
        return constants.maxOverlapDxRatio.getValue();
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        private final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");

        private final Constant.Ratio maxOverlapDxRatio = new Constant.Ratio(
                0.2,
                "Maximum acceptable abscissa overlap ratio between notes");

        private final Constant.Ratio maxOverlapAreaRatio = new Constant.Ratio(
                0.25,
                "Maximum acceptable box area overlap ratio between notes");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a head.
     * <p>
     * For a head, we provide only one handle:
     * <ul>
     * <li>Middle handle, moving in any direction, but vertically it snaps to lines and ledgers,
     * and horizontally, for shapes that need a stem, it tries to snap to stem.
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        // Original data
        private final Rectangle originalBounds;

        // Latest data
        private final Rectangle latestBounds;

        public Editor (final HeadInter head)
        {
            super(head);

            originalBounds = head.getBounds();
            latestBounds = head.getBounds();

            final double halfHeight = latestBounds.height / 2.0;
            final double halfWidth = latestBounds.width / 2.0;

            handles.add(selectedHandle = new InterEditor.Handle(head.getCenter())
            {
                @Override
                public boolean move (Point vector)
                {
                    final int dx = vector.x;
                    final int dy = vector.y;

                    // Handle
                    PointUtil.add(selectedHandle.getHandleCenter(), dx, dy);

                    // Data
                    Point2D center = selectedHandle.getHandleCenter();
                    latestBounds.x = (int) Math.rint(center.getX() - halfWidth);
                    latestBounds.y = (int) Math.rint(center.getY() - halfHeight);
                    head.setBounds(latestBounds);

                    final Double x = head.getSnapAbscissa();
                    if (x != null) {
                        latestBounds.x = (int) Math.rint(x - halfWidth);
                    }

                    final Double y = head.getSnapOrdinate();
                    if (y != null) {
                        latestBounds.y = (int) Math.rint(y - halfHeight);
                    }

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            inter.setBounds(latestBounds);
            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            inter.setBounds(originalBounds);
            super.undo();
        }
    }

    //---------//
    // Tracker //
    //---------//
    /**
     * Specific tracker for a note head, able to display needed ledgers.
     */
    private static class Tracker
            extends InterTracker
    {

        public Tracker (HeadInter head,
                        Sheet sheet)
        {
            super(head, sheet);
        }

        @Override
        public void render (Graphics2D g)
        {
            super.render(g);

            // Add needed ledgers
            final HeadInter head = (HeadInter) inter;

            for (Line2D line : head.getNeededLedgerLines()) {
                g.setColor(Color.RED);
                g.draw(line);
            }
        }

        @Override
        public Rectangle getSceneBounds ()
        {
            Rectangle box = super.getSceneBounds();

            // Include needed ledgers if any
            final HeadInter head = (HeadInter) inter;

            for (Line2D line : head.getNeededLedgerLines()) {
                box.add(line.getBounds());
            }

            return box;
        }
    }
}
