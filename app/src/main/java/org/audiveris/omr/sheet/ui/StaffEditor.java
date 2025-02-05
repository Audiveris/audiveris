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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
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

    private static final Logger logger = LoggerFactory.getLogger(StaffEditor.class);

    //~ Instance fields ----------------------------------------------------------------------------

    protected StaffModel originalModel;

    protected StaffModel model;

    /** Staff lines. */
    protected final List<LineInfo> lines;

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

        /**
         * The map of vertical distances for every defining point WRT staff midLine.
         * <ul>
         * <li>Key: line index in staff lines
         * <li>Value: list of ordinate deltas, one for each defining point of the line
         * </ul>
         */
        private final SortedMap<Integer, List<Double>> dyMap = new TreeMap<>();

        public GlobalEditor (Staff staff)
        {
            super(staff);

            midLine = (StaffLine) staff.getMidLine();

            // Compute the delta ordinates
            populateDyMap();

            // Models
            originalModel = new GlobalModel(staff, midLine);
            model = new GlobalModel(staff, midLine);

            final List<Point2D> points = ((GlobalModel) model).midModel.points;
            final int nb = points.size();

            // Side handles
            Arrays.asList(points.get(0), points.get(nb - 1)).forEach(p -> handles.add(new Handle(p)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Move in any direction
                    PointUtil.add(p, dx, dy);

                    return true;
                }
            }));

            // Pre-select the left-side handle
            selectedHandle = handles.get(0);

            // Inside handles
            points.subList(1, nb - 1).forEach(p -> handles.add(new Handle(p)
            {
                @Override
                public boolean move (int dx,
                                     int dy)
                {
                    // Move only vertically
                    if (dy == 0) {
                        return false;
                    }

                    PointUtil.add(p, 0, dy);

                    return true;
                }
            }));
        }

        @Override
        protected void applyModel (StaffModel model)
        {
            final List<Point2D> midPoints = ((GlobalModel) model).midModel.points;
            final Staff staff = getStaff();

            // Special policy for a side handle: all lines ends are kept aligned vertically
            final double left = midPoints.get(0).getX();
            final double right = midPoints.get(midPoints.size() - 1).getX();

            // Adjust midLine
            midLine.setPoints(midPoints);
            midLine.getSpline();

            // Apply the dys for the other lines
            for (Entry<Integer, List<Double>> entry : dyMap.entrySet()) {
                final StaffLine line = (StaffLine) lines.get(entry.getKey());
                final List<Point2D> points = line.getPoints();
                final int ipLast = points.size() - 1; // Index to last line point
                final List<Double> dys = entry.getValue();
                final List<Point2D> newPoints = new ArrayList<>();

                // Left
                newPoints.add(new Point2D.Double(left, midLine.yAt(left) + dys.get(0)));

                // Inside
                for (int ip = 1; ip < ipLast; ip++) {
                    final Point2D p = points.get(ip);
                    final double x = p.getX();
                    final double midY = midLine.yAt(x);
                    final double dy = dys.get(ip);
                    newPoints.add(new Point2D.Double(x, midY + dy));
                }

                // Right
                newPoints.add(new Point2D.Double(right, midLine.yAt(right) + dys.get(ipLast)));

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
         * Compute the vertical deltas of every line with respect to the staff middle line.
         */
        private void populateDyMap ()
        {
            for (int il = 0; il < lines.size(); il++) {
                final StaffLine line = (StaffLine) lines.get(il);

                if (line != midLine) {
                    final List<Double> dys = new ArrayList<>();
                    line.getPoints().forEach(p -> dys.add(p.getY() - midLine.yAt(p.getX())));
                    dyMap.put(il, dys);
                }
            }
        }

        /** Driven by midLine model. */
        private static class GlobalModel
                extends StaffModel
        {
            public final LineModel midModel;

            public GlobalModel (Staff staff,
                                StaffLine staffLine)
            {
                super(staff);
                midModel = new LineModel(staffLine);
            }
        }
    }

    //-----------//
    // LineModel //
    //-----------//
    /** Model for one staff line. */
    protected static class LineModel
    {
        // The line defining points
        public final List<Point2D> points = new ArrayList<>();

        public LineModel (StaffLine staffLine)
        {
            // Make a deep copy of provided points, to avoid live sharing of points
            points.addAll(staffLine.getPointsDeepCopy());
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
                    lineModel -> lineModel.points.forEach(p -> handles.add(new Handle(p)
                    {
                        @Override
                        public boolean move (int dx,
                                             int dy)
                        {
                            // Move only vertically
                            if (dy == 0) {
                                return false;
                            }

                            PointUtil.add(p, 0, dy);

                            return true;
                        }
                    })));
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
}
