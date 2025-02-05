//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e l a t i o n V e c t o r                                  //
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.sig.relation.Relations;
import org.audiveris.omr.ui.util.UIUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Class <code>RelationVector</code> represents a dynamic vector from starting inter(s) to
 * potential stopping inter(s), in order to finally set a relation between them.
 *
 * @author Hervé Bitteur
 */
public class RelationVector
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(RelationVector.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Underling sheet. */
    private final Sheet sheet;

    /** Line from starting point to current stopping point. */
    private final Line2D line;

    /** Starting inters, needed to initially create a vector. */
    private final List<Inter> starts;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a useful vector.
     *
     * @param p1     starting point
     * @param starts starting inters (cannot be null or empty)
     */
    public RelationVector (Point p1,
                           List<Inter> starts)
    {
        line = new Line2D.Double(p1, p1);
        this.starts = starts;
        sheet = starts.get(0).getSig().getSystem().getSheet();
        logger.debug("Created {}", this);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------------------//
    // checkCrossSystemConnection //
    //----------------------------//
    /**
     * Check the case of a vector between different systems.
     * <p>
     * Slur connection from end of one system to beginning of the next.
     * Difficult to check if parts are 'compatible'
     *
     * @param source provided source inter
     * @param target provided target inter
     * @return true if successful
     */
    private boolean checkCrossSystemConnection (Inter source,
                                                Inter target)
    {
        // Check configuration
        final SystemInfo srcSystem = source.getSig().getSystem();
        final SystemInfo tgtSystem = target.getSig().getSystem();

        if (srcSystem == tgtSystem) {
            return false;
        }

        // Special case of slurs connection across system break?
        if (!(source instanceof SlurInter) || !(target instanceof SlurInter)) {
            return false;
        }

        final boolean rev = srcSystem.getId() > tgtSystem.getId();

        // Check it's on last measure of first system
        final SystemInfo startSyst = rev ? tgtSystem : srcSystem;
        final SlurInter startSlur = (SlurInter) (rev ? target : source);
        final Point2D startPt = startSlur.getCurve().getP2();
        if (startSyst.getStackAt(startPt) != startSyst.getLastStack()) {
            return false;
        }

        // Check it's on first measure of second system
        final SystemInfo stopSyst = rev ? srcSystem : tgtSystem;
        final SlurInter stopSlur = (SlurInter) (rev ? source : target);
        final Point2D stopPt = stopSlur.getCurve().getP1();
        if (stopSyst.getStackAt(stopPt) != stopSyst.getFirstStack()) {
            return false;
        }

        sheet.getInterController().connect(
                startSlur,
                stopSlur,
                ConnectionTask.Kind.SLUR_CONNECTION);

        return true;
    }

    //----------//
    // extendTo //
    //----------//
    /**
     * Modify vector stopping point.
     *
     * @param pt new stopping point
     */
    public void extendTo (Point pt)
    {
        line.setLine(line.getP1(), pt);
    }

    //---------//
    // process //
    //---------//
    /**
     * Process the vector into a relation.
     */
    public void process ()
    {
        final Point p2 = PointUtil.rounded(line.getP2());
        final List<Inter> stops = sheet.getInterIndex().getContainingEntities(p2);

        if (!stops.isEmpty()) {
            stops.removeAll(starts); // No looping vector!
        }

        if (stops.isEmpty()) {
            return;
        }

        Collections.sort(stops, Inters.membersFirst);
        logger.debug("process starts:{} stops{}", starts, stops);

        for (Inter start : starts) {
            for (Inter stop : stops) {
                if (checkCrossSystemConnection(start, stop)) {
                    return;
                }

                for (boolean reverse : new boolean[]
                { false, true }) {
                    final Inter source = reverse ? stop : start;
                    final Inter target = reverse ? start : stop;
                    final Set<Class<? extends Relation>> suggestions;
                    suggestions = Relations.suggestedRelationsBetween(source, target);

                    if (suggestions.isEmpty()) {
                        continue;
                    }

                    logger.debug("src:{} tgt:{} suggestions:{}", source, target, suggestions);

                    try {
                        final SIGraph sig = source.getSig();
                        final Class<? extends Relation> relClass = suggestions.iterator().next();

                        // Allocate relation to be added
                        final Relation relation = relClass.getDeclaredConstructor().newInstance();

                        if (relation.isForbidden(source, target)) {
                            logger.debug("Not allowed {} src:{} tgt:{}", relClass, source, target);
                        } else {
                            relation.setManual(true);
                            sheet.getInterController().link(sig, source, target, relation);
                            return;
                        }
                    } catch (Exception ex) {
                        logger.warn("Error linking {}", ex.toString(), ex);
                    }
                }
            }
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the vector onto the provided graphics.
     *
     * @param g provided graphics
     */
    public void render (Graphics2D g)
    {
        g.setColor(Color.GRAY);
        UIUtil.setAbsoluteStroke(g, 1f);
        g.draw(line);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("Vector{");
        sb.append("[").append(line.getX1()).append(",").append(line.getY1()).append("]");
        sb.append(" starts:").append(starts);
        sb.append("}");

        return sb.toString();
    }
}
