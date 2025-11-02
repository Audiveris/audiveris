//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S t a f f E d i t o r                                     //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphIndex;
import org.audiveris.omr.glyph.dynamic.CurvedFilament;
import org.audiveris.omr.glyph.dynamic.Filament;
import org.audiveris.omr.lag.BasicSection;
import org.audiveris.omr.lag.DynamicSection;
import org.audiveris.omr.lag.JunctionRatioPolicy;
import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Lags;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.lag.SectionFactory;
import org.audiveris.omr.math.NaturalSpline;
import org.audiveris.omr.math.PointUtil;
import static org.audiveris.omr.run.Orientation.HORIZONTAL;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.run.RunTableFactory;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.ProcessingSwitch;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SheetStub;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.StaffLine;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.header.StaffHeader;
import org.audiveris.omr.ui.view.RubberPanel;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;
import org.audiveris.omr.util.VerticalSide;
import static org.audiveris.omr.util.VerticalSide.TOP;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import static java.util.stream.Collectors.toList;

/**
 * Class <code>StaffEditor</code> allows the end-user to manually edit the geometry of a staff.
 * <p>
 * There are two editing modes:
 * <ul>
 * <li><b>Lines</b> mode.
 * In this mode, all staff lines can be modified individually.
 * Any staff line defining point becomes a handle that can be moved vertically only.
 * <li><b>Global</b> mode.
 * In this mode, the staff lines can only be modified as a whole, meaning that the staff lines stay
 * parallel with each other.
 * The midLine defining points become handles that can be moved vertically, and the side defining
 * points can be moved both vertically and horizontally, allowing to extend or shrink the staff.
 * If the staff grows significantly on its left or right side, additional defining points
 * (and thus handles) get automatically inserted.
 * </ul>
 * In both modes, as staff geometry is being modified, remaining horizontal sections can get removed
 * (or reinserted) if they are detected as new staff line members.
 * <p>
 * TODO: Similarly, we could envision that remaining vertical sections get removed (or reinserted)
 * if they are detected as new barline members.
 * For the time being, manual barline insertion is a separate (but easy) task for the end-user.
 *
 * @author Hervé Bitteur
 */
public abstract class StaffEditor
        extends ObjectEditor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(StaffEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------

    protected StaffModel originalModel;

    protected StaffModel model;

    /** Staff lines. */
    protected final List<LineInfo> lines;

    /**
     * Typical horizontal gap between the staff lines defining points.
     * This will be used to detect when an intermediate point should be inserted.
     */
    protected final int typicalDx;

    /** Maximum line thickness. */
    protected final int maxLineThickness;

    /** Sheet horizontal lag. */
    protected final Lag hLag;

    /** Original horizontal sections from staff lines glyphs, per line index. */
    protected final TreeMap<Integer, Set<Section>> originalInternals;

    /** All horizontal internal and external sections potentially impacted by staff editing. */
    protected final Collection<Section> allSections = new ArrayList<>();

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a <code>StaffEditor</code> instance.
     *
     * @param staff the staff to edit
     */
    protected StaffEditor (Staff staff)
    {
        super(staff, staff.getSystem());
        lines = staff.getLines();
        maxLineThickness = system.getSheet().getScale().getMaxFore();
        hLag = system.getSheet().getLagManager().getLag(Lags.HLAG);

        // Compute the typical gap between defining points
        // And make sure no too long gap still exists
        typicalDx = getTypicalDx();
        refinePoints();

        // Collect all relevant sections (internals and externals)
        originalInternals = collectInternalSections();
        originalInternals.values().forEach(list -> allSections.addAll(list));
        allSections.addAll(collectExternalSections());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //------------//
    // applyModel //
    //------------//
    /**
     * Apply the geometry of provided model to the staff lines.
     *
     * @param model the model to apply
     */
    protected abstract void applyModel (StaffModel model);

    //-------------------------//
    // collectInternalSections //
    //-------------------------//
    /**
     * Collect the sections from original lines.
     * <p>
     * These line sections are created from the staff line glyphs.
     * They are coded as {@link LineSection} instances, to easily separate them from hLag sections.
     *
     * @return the original internal sections, per line
     */
    private TreeMap<Integer, Set<Section>> collectInternalSections ()
    {
        final TreeMap<Integer, Set<Section>> map = new TreeMap<>();
        final Staff staff = getStaff();
        final Sheet sheet = system.getSheet();
        final int width = sheet.getWidth();
        final int height = sheet.getHeight();
        final BufferedImage img = new BufferedImage(width, height, TYPE_BYTE_GRAY);
        final Graphics2D g = (Graphics2D) img.getGraphics();

        staff.getLines().forEach(line -> {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setColor(Color.BLACK);
            final StaffLine staffLine = (StaffLine) line;
            final Glyph glyph = staffLine.getGlyph();
            glyph.getRunTable().render(g, glyph.getTopLeft());

            final RunTableFactory runFactory = new RunTableFactory(HORIZONTAL);
            final RunTable linesTable = runFactory.createTable(new ByteProcessor(img));

            final SectionFactory sectionFactory = new SectionFactory(
                    HORIZONTAL,
                    JunctionRatioPolicy.DEFAULT);
            final List<DynamicSection> sections = sectionFactory.buildDynamicSections(
                    linesTable,
                    true);
            final Set<Section> set = sections.stream().map(ds -> new LineSection(ds)).collect(
                    Collectors.toSet());
            map.put(staff.getLines().indexOf(line), set);
        });

        g.dispose();

        return map;
    }

    //-------------------------//
    // collectExternalSections //
    //-------------------------//
    /**
     * Collect the external (non-line) sections that may be impacted by staff editing.
     *
     * @return the collection of relevant sections
     */
    private Collection<Section> collectExternalSections ()
    {
        final Staff staff = getStaff();
        final Rectangle staffBox = staff.getAreaBounds().getBounds();

        // Side abscissae may not be the ultimate ones, so let's widen staffBox until sheet limits
        final Sheet sheet = system.getSheet();
        staffBox.setBounds(0, staffBox.y, sheet.getWidth(), staffBox.height);

        return hLag.getEntities().stream() //
                .filter(section -> section.getBounds().intersects(staffBox)) //
                .collect(Collectors.toList());
    }

    //------//
    // doit //
    //------//
    @Override
    public void doit ()
    {
        // Reset standard sections to their initial status
        hLag.insertSections(model.hLagRemovals);
        model.hLagRemovals.clear();

        applyModel(model); // Change lines definition

        final Staff staff = getStaff();
        final int interline = staff.getSpecificInterline();

        // Rebuild each staffLine glyph, according to new line definition
        lines.forEach(line -> {
            final StaffLine staffLine = (StaffLine) line;

            // Retrieve all sections related to the new staff line
            final Set<Section> internals = getInternals(staffLine); // Not perfect, but OK enough

            // Make sure no original internal section has been forgotten in the new internals
            final int idx = staff.getLines().indexOf(line);
            internals.addAll(originalInternals.get(idx));

            // Build a filament from the sections
            final Filament fil = new CurvedFilament(interline, 0);
            internals.forEach(s -> fil.addSection(s));

            // Build the line glyph from the filament
            final Glyph glyph = fil.toGlyph(null);
            staffLine.setGlyph(glyph);

            // Removals on hLag
            model.hLagRemovals.addAll(
                    internals.stream().filter(s -> !(s instanceof LineSection)).collect(toList()));

            hLag.removeSections(model.hLagRemovals);
        });

        updateNoStaff();
    }

    //------------//
    // endProcess //
    //------------//
    @Override
    public void endProcess ()
    {
        if (hasMoved) {
            system.getSheet().getInterController().editObject(this);
        }

        system.getSheet().getSheetEditor().closeEditMode();
    }

    //-----------//
    // finalDoit //
    //-----------//
    @Override
    public void finalDoit ()
    {
        super.finalDoit();

        final Staff staff = getStaff();
        final Sheet sheet = system.getSheet();

        // Register staff lines glyphs
        final GlyphIndex glyphIndex = sheet.getGlyphIndex();
        lines.forEach(line -> {
            final StaffLine staffLine = (StaffLine) line;
            Glyph glyph = staffLine.getGlyph();
            glyph = glyphIndex.registerOriginal(glyph);
            staffLine.setGlyph(glyph);
        });

        // Update header if so needed
        final StaffHeader header = staff.getHeader();
        if (header != null) {
            if (header.stop == header.start) {
                final int stop = staff.getAbscissa(LEFT);
                staff.setHeaderStop(stop);
                model.headerStop = stop;
            }
        }

        system.updateCoordinates();
        system.updateArea();

        // Check for potential change in system indentation
        if (staff == system.getFirstStaff()) {
            final SheetStub stub = sheet.getStub();

            if (stub.getProcessingSwitches().getValue(ProcessingSwitch.indentations)) {
                sheet.getSystemManager().checkNewIndentation(system);
            }
        }
    }

    //--------------//
    // getInternals //
    //--------------//
    /**
     * Retrieve the sections that currently belong to the provided staff line,
     * only defined by its spline points.
     *
     * @param staffLine the staff line to process
     * @return the sections that "compose" the staff line
     */
    private Set<Section> getInternals (StaffLine staffLine)
    {
        final NaturalSpline spline = staffLine.getSpline();
        final Set<Section> internals = new HashSet<>();

        // First, retrieve the sections intersected by the staff line (the core sections)
        for (Section section : allSections) {
            final Rectangle box = section.getBounds();

            if (spline.intersects(box)) {
                // Refine intersection check
                final double yLeft = staffLine.yAt((double) box.x);
                final double yRight = staffLine.yAt((double) (box.x + box.width));
                if ((yLeft >= box.y && yLeft <= box.y + box.height) //
                        || (yRight >= box.y && yRight <= box.y + box.height)) {
                    internals.add(section);
                }
            }
        }

        // Second, retrieve stuck sections
        // Could be improved, based on resulting thickness? Not really needed.
        internals.addAll(getStickers(internals));

        return internals;
    }

    //----------//
    // getStaff //
    //----------//
    protected Staff getStaff ()
    {
        return (Staff) object;
    }

    //-------------//
    // getStickers //
    //-------------//
    /**
     * Retrieve the sections stuck on the provided core sections
     *
     * @param coreSections the provided core sections
     * @return the sticker sections
     */
    private Set<Section> getStickers (Collection<Section> coreSections)
    {
        final Set<Section> stickers = new HashSet<>();
        final List<Section> candidates = new ArrayList<>(allSections);
        candidates.removeAll(coreSections);

        for (Section core : coreSections) {
            final int maxCandidateThickness = maxLineThickness - core.getRunCount();

            for (VerticalSide vSide : VerticalSide.values()) {
                final Run run = (vSide == TOP) ? core.getFirstRun() : core.getLastRun();
                final int pos = (vSide == TOP) ? core.getFirstPos() : core.getLastPos();
                final int nextPos = (vSide == TOP) ? pos - 1 : pos + 1;
                final int xMin = run.getStart();
                final int xMax = run.getStop();

                for (Iterator<Section> it = candidates.iterator(); it.hasNext();) {
                    final Section cand = it.next();

                    // Check ordinate adjacency
                    final int candPos = (vSide == TOP) ? cand.getLastPos() : cand.getFirstPos();

                    if (candPos == nextPos) {
                        // Check abscissa overlap
                        final Run candRun = (vSide == TOP) ? cand.getLastRun() : cand.getFirstRun();

                        if (candRun.getStart() <= xMax && candRun.getStop() >= xMin) {
                            // Check resulting line thickness
                            if (cand.getRunCount() <= maxCandidateThickness) {
                                stickers.add(cand);
                                it.remove();
                            }
                        }
                    }
                }
            }
        }

        return stickers;
    }

    //--------------//
    // getTypicalDx //
    //--------------//
    /**
     * Compute the typical (median) horizontal distance between all line defining points.
     */
    private int getTypicalDx ()
    {
        final List<Integer> dxs = new ArrayList<>();

        for (int il = 0; il < lines.size(); il++) {
            final StaffLine line = (StaffLine) lines.get(il);
            final List<Point2D> points = line.getPoints();

            for (int ip = 1; ip < points.size(); ip++) {
                final Point2D p = points.get(ip);
                dxs.add((int) Math.rint(p.getX() - points.get(ip - 1).getX()));
            }
        }

        Collections.sort(dxs);

        return dxs.get(dxs.size() / 2);
    }

    //--------------//
    // refinePoints //
    //--------------//
    /**
     * Harmonize the defining points along the staff, based on staff width and typicalDx value.
     */
    private void refinePoints ()
    {
        final Staff staff = getStaff();
        final double left = staff.getAbscissa(LEFT);
        final double right = staff.getAbscissa(RIGHT);
        final double width = right - left;
        final int count = (int) Math.rint(width / typicalDx);
        final double gap = width / count;

        for (LineInfo lineInfo : staff.getLines()) {
            final StaffLine line = (StaffLine) lineInfo;
            final List<Point2D> newPoints = new ArrayList<>();

            for (int ip = 0; ip <= count; ip++) {
                final double x = left + ip * gap;
                newPoints.add(new Point2D.Double(x, line.yAt(x)));
            }

            line.setPoints(newPoints);
        }
    }

    //------//
    // undo //
    //------//
    @Override
    public void undo ()
    {
        applyModel(originalModel); // Reset lines definition

        // Replace the lines glyphs by the original glyphs
        lines.forEach(line -> {
            final int idx = lines.indexOf(line);
            ((StaffLine) line).setGlyph(originalModel.staffLineGlyphs.get(idx));
        });

        // Cancel the hLag sections removal
        hLag.insertSections(model.hLagRemovals);

        updateNoStaff();
    }

    //---------------//
    // updateNoStaff //
    //---------------//
    /**
     * Invalidate the no-staff buffer, and trigger a tab refresh.
     */
    private void updateNoStaff ()
    {
        // Invalidate the buffer, to be rebuilt on demand only
        final Sheet sheet = getStaff().getSystem().getSheet();
        sheet.getPicture().disposeSource(Picture.SourceKey.NO_STAFF);

        // Refresh the current tab
        final SheetAssembly assembly = sheet.getStub().getAssembly();
        final RubberPanel panel = assembly.getSelectedScrollView().getView();
        panel.repaint();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio maxHandleXGapRatio = new Constant.Ratio(
                1.5,
                "Maximum handle horizontal gap vs typical gap");
    }

    //--------------//
    // GlobalEditor //
    //--------------//
    /**
     * Editor to edit the staff as a whole, where staff lines are kept parallel.
     * <p>
     * There is a horizontal sequence of handles on staff midline, all moving vertically,
     * except for the side handles which can move both horizontally and vertically.
     */
    public static class GlobalEditor
            extends StaffEditor
    {
        /** The staff middle line. */
        private final StaffLine midLine;

        public GlobalEditor (Staff staff)
        {
            super(staff);

            midLine = (StaffLine) staff.getMidLine();

            // Models
            originalModel = new GlobalModel(staff, midLine);
            model = new GlobalModel(staff, midLine);

            // Handles list, kept parallel to combs list
            final List<Comb> combs = ((GlobalModel) model).combs;
            final int nb = combs.size();
            handles.add(new OmniHandle(combs.get(0).midPoint)); // Left
            combs.subList(1, nb - 1).forEach(c -> handles.add(new VerticalHandle(c.midPoint)));
            handles.add(new OmniHandle(combs.get(nb - 1).midPoint)); // Right

            // Pre-select the left-side handle
            selectedHandle = handles.get(0);
        }

        @Override
        protected void applyModel (StaffModel model)
        {
            final GlobalModel globalModel = (GlobalModel) model;
            final List<Comb> combs = globalModel.combs;
            final Staff staff = getStaff();

            // Special policy for a side handle: all lines ends are kept aligned vertically
            final double left = combs.get(0).midPoint.getX();
            final double right = combs.get(combs.size() - 1).midPoint.getX();

            // Remove any comb (and its handle) that is located beyond staff sides
            for (Iterator<Comb> iterator = globalModel.combs.iterator(); iterator.hasNext();) {
                final Comb comb = iterator.next();
                final double x = comb.midPoint.getX();

                if (x < left || x > right) {
                    final int ic = combs.indexOf(comb);
                    iterator.remove();
                    handles.remove(ic);
                }
            }

            // Check for need of intermediate comb
            checkInsertion(combs, LEFT);
            checkInsertion(combs, RIGHT);

            // Compute the defining points for each line
            for (int i = 0; i < lines.size(); i++) {
                final int idx = i;
                final StaffLine line = (StaffLine) lines.get(idx);
                final List<Point2D> newPoints = new ArrayList<>();
                globalModel.combs.forEach(
                        c -> newPoints.add(
                                new Point2D.Double(
                                        c.midPoint.getX(),
                                        c.midPoint.getY() + c.dys.get(idx))));
                line.setPoints(newPoints);
            }

            staff.setAbscissa(LEFT, (int) Math.rint(left));
            staff.setAbscissa(RIGHT, (int) Math.rint(right));
            staff.setArea(null);

            if (model.headerStop != null) {
                staff.getHeader().stop = model.headerStop;
            }
        }

        /**
         * Check on the provided side for any needed insertion of an intermediate comb
         * when the staff is significantly extended.
         *
         * @param combs the current list of combs
         * @param hSide the horizontal side to check
         */
        private void checkInsertion (List<Comb> combs,
                                     HorizontalSide hSide)
        {
            if (combs.size() >= 2) {
                final double maxDx = constants.maxHandleXGapRatio.getValue() * typicalDx;
                final int extIdx = hSide == LEFT ? 0 : combs.size() - 1; // Exterior index
                final int intIdx = hSide == LEFT ? 1 : combs.size() - 2; // Interior index
                final int dir = hSide == LEFT ? 1 : -1; // X direction from exterior to interior

                final Point2D extPt = combs.get(extIdx).midPoint;
                final Point2D inPt = combs.get(intIdx).midPoint;
                final double dx = Math.abs(extPt.getX() - inPt.getX());

                if (dx > maxDx) {
                    // Insert a comb, with a vertical handle on the mid point
                    final Point2D addedPt = new Point2D.Double(
                            extPt.getX() + dir * dx / 2,
                            (extPt.getY() + inPt.getY()) / 2);
                    final Comb addedComb = new Comb(addedPt, combs.get(extIdx));

                    final int maxIdx = Math.max(extIdx, intIdx);
                    combs.add(maxIdx, addedComb);
                    handles.add(maxIdx, new VerticalHandle(addedPt));
                }
            }
        }

        /** Ordinates around a mid point. */
        private static class Comb
        {
            /** Point on the staff middle line. */
            public final Point2D midPoint;

            /** Delta ordinates of comb points WRT middle point. */
            public final List<Double> dys = new ArrayList<>();

            /** Deltas are copied from another (source) comb. */
            public Comb (Point2D p,
                         Comb source)
            {
                midPoint = p;
                source.dys.forEach(dy -> dys.add(dy));
            }

            /** Deltas are measured from actual staff lines. */
            public Comb (Staff staff,
                         double x)
            {
                final double midY = staff.getMidLine().yAt(x);
                midPoint = new Point2D.Double(x, midY);
                staff.getLines().forEach(l -> dys.add(l.yAt(x) - midY));
            }
        }

        /** Driven by midLine. */
        private static class GlobalModel
                extends StaffModel
        {
            /** The sequence of combs. */
            public final List<Comb> combs = new ArrayList<>();

            public GlobalModel (Staff staff,
                                StaffLine midLine)
            {
                super(staff);
                midLine.getPoints().forEach(p -> combs.add(new Comb(staff, p.getX())));
            }

            public int indexOf (Point2D center)
            {
                for (int i = 0, iBreak = combs.size(); i < iBreak; i++) {
                    final Comb comb = combs.get(i);

                    if (comb.midPoint == center) {
                        return combs.indexOf(comb);
                    }
                }

                return -1;
            }
        }
    }

    //-------------//
    // LineSection //
    //-------------//
    /** Class meant to easily recognize the internal sections built from staff line glyphs. */
    private static class LineSection
            extends BasicSection
    {
        public LineSection (DynamicSection ds)
        {
            super(ds);
            id = ds.getId();
        }
    }

    //-------------//
    // LinesEditor //
    //-------------//
    /**
     * Editor to edit all lines individually.
     * <p>
     * There are as many handles as lines defining points, all moving only vertically.
     */
    public static class LinesEditor
            extends StaffEditor
    {
        public LinesEditor (Staff staff)
        {
            super(staff);

            // Models
            originalModel = new LineArrayModel(staff);
            model = new LineArrayModel(staff);

            // Handles
            ((LineArrayModel) model).lineModels.forEach(
                    lineModel -> lineModel.points.forEach(p -> handles.add(new VerticalHandle(p))));
        }

        @Override
        protected void applyModel (StaffModel model)
        {
            final LineArrayModel lam = (LineArrayModel) model;

            for (int il = 0; il < lines.size(); il++) {
                final StaffLine staffLine = (StaffLine) lines.get(il);
                staffLine.setPoints(lam.lineModels.get(il).points);
            }
        }

        /** One model per staff line. */
        private static class LineArrayModel
                extends StaffModel
        {
            public final List<LineModel> lineModels = new ArrayList<>();

            public LineArrayModel (Staff staff)
            {
                super(staff);
                staff.getLines().forEach(line -> lineModels.add(new LineModel((StaffLine) line)));
            }

            /** Model for one staff line. */
            private static class LineModel
            {
                // The line defining points
                public final List<Point2D> points = new ArrayList<>();

                public LineModel (StaffLine staffLine)
                {
                    // Make a deep copy of provided points, to avoid live sharing of points
                    points.addAll(staffLine.getPointsDeepCopy());
                }
            }
        }
    }

    //------------//
    // OmniHandle //
    //------------//
    /** Handle moving in any direction. */
    protected static class OmniHandle
            extends Handle
    {
        public OmniHandle (Point2D center)
        {
            super(center);
        }

        @Override
        public boolean move (int dx,
                             int dy)
        {
            PointUtil.add(center, dx, dy);

            return true;
        }
    }

    //------------//
    // StaffModel //
    //------------//
    protected static abstract class StaffModel
            implements ObjectUIModel
    {
        /** Original staff lines glyphs, one per line. */
        public final List<Glyph> staffLineGlyphs = new ArrayList<>();

        /** Sections removed from hLag. */
        public final Set<Section> hLagRemovals = new HashSet<>();

        /** Header stop, if any. */
        public Integer headerStop;

        public StaffModel (Staff staff)
        {
            staff.getLines().forEach(l -> staffLineGlyphs.add(((StaffLine) l).getGlyph()));

            if (staff.getHeader() != null) {
                headerStop = staff.getHeader().stop;
            }
        }
    }

    //----------------//
    // VerticalHandle //
    //----------------//
    /** Handle moving only vertically. */
    protected static class VerticalHandle
            extends Handle
    {
        public VerticalHandle (Point2D center)
        {
            super(center);
        }

        @Override
        public boolean move (int dx,
                             int dy)
        {
            PointUtil.add(center, 0, dy);

            return dy != 0;
        }
    }
}
