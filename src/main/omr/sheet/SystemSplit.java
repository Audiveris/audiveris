//----------------------------------------------------------------------------//
//                                                                            //
//                           S y s t e m S p l i t                            //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.glyph.Glyph;
import omr.glyph.GlyphSection;

import omr.stick.Stick;

import omr.util.Dumper;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Class <code>SystemSplit</code> is a set of methods dedicated to split
 * entities (sections, sticks, ...) according to the system areas previously
 * found in a sheet
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class SystemSplit
{
    //~ Constructors -----------------------------------------------------------

    // No constructor
    private SystemSplit ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //---------------------//
    // computeSystemLimits //
    //---------------------//
    /**
     * Compute the top and bottom ordinates of the related area of each
     * system
     * @param sheet the containing sheet
     */
    public static void computeSystemLimits (Sheet sheet)
    {
        // Compute the dimensions of the picture area of every system
        SystemInfo prevSystem = null;

        for (SystemInfo system : sheet.getSystems()) {
            // Very first system
            if (prevSystem == null) {
                system.setAreaTop(0);
            } else {
                // Top of system area, defined as middle ordinate between
                // ordinate of last line of last staff of previous system and
                // ordinate of first line of first staff of current system
                int middle = (prevSystem.getBottom() + system.getTop()) / 2;
                prevSystem.setAreaBottom(middle);
                system.setAreaTop(middle);
            }

            // Remember this info for next system
            prevSystem = system;
        }

        // Bottom of last system
        prevSystem.setAreaBottom(Integer.MAX_VALUE);
    }

    //------//
    // dump //
    //------//
    /**
     * Utility method, to dump all sheet systems
     *
     * @param sheet the containing sheet
     */
    public static void dump (Sheet sheet)
    {
        System.out.println("--- SystemInfos ---");

        int i = 0;

        for (SystemInfo system : sheet.getSystems()) {
            Dumper.dump(system, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
    }

    //----------------//
    // splitBarSticks //
    //----------------//
    /**
     * Split the collection of provided bar sticks
     * (Used by BarsBuilder).
     *
     * @param sheet the containing sheet
     * @param glyphs the entities to be dispatched
     */
    public static void splitBarSticks (Sheet                      sheet,
                                       Collection<?extends Glyph> glyphs)
    {
        process(
            sheet,
            glyphs,
            new Adapter<Glyph>() {
                    public Collection<Glyph> getTarget (SystemInfo system)
                    {
                        return system.getGlyphs();
                    }

                    public int getXMin (Glyph glyph)
                    {
                        Rectangle box = glyph.getContourBox();

                        return box.x;
                    }

                    public int getYMin (Glyph glyph)
                    {
                        Rectangle box = glyph.getContourBox();

                        return box.y;
                    }
                });
    }

    //------------------//
    // splitHorizontals //
    //------------------//
    /**
     * Split the various horizontals among systems
     * (Used once at end of BarsBuilder).
     *
     * @param sheet the containing sheet
     */
    public static void splitHorizontals (Sheet sheet)
    {
        process(
            sheet,
            sheet.getHorizontals().getLedgers(),
            new Adapter<Ledger>() {
                    public List<Ledger> getTarget (SystemInfo system)
                    {
                        return system.getLedgers();
                    }

                    public int getXMin (Ledger ledger)
                    {
                        Rectangle box = ledger.getContourBox();

                        return box.x;
                    }

                    public int getYMin (Ledger ledger)
                    {
                        Rectangle box = ledger.getContourBox();

                        return box.y;
                    }
                });
        process(
            sheet,
            sheet.getHorizontals().getEndings(),
            new Adapter<Ending>() {
                    public List<Ending> getTarget (SystemInfo system)
                    {
                        return system.getEndings();
                    }

                    public int getXMin (Ending ending)
                    {
                        Rectangle box = ending.getContourBox();

                        return box.x;
                    }

                    public int getYMin (Ending ending)
                    {
                        Rectangle box = ending.getContourBox();

                        return box.y;
                    }
                });
    }

    //-----------------------//
    // splitVerticalSections //
    //-----------------------//
    /**
     * Split the various horizontal sections (Used by Glyphs).
     * (Used once at end of BarsBuilder).
     *
     * @param sheet the containing sheet
     */
    public static void splitVerticalSections (Sheet sheet)
    {
        process(
            sheet,
            sheet.getVerticalLag().getSections(),
            new Adapter<GlyphSection>() {
                    public List<GlyphSection> getTarget (SystemInfo system)
                    {
                        return system.getVerticalSections();
                    }

                    public int getXMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();

                        return box.x;
                    }

                    public int getYMin (GlyphSection section)
                    {
                        Rectangle box = section.getContourBox();

                        return box.y;
                    }
                });
    }

    //---------------------//
    // splitVerticalSticks //
    //---------------------//
    /**
     * Split the collection of provided vertical sticks
     * (Used at every run of VerticalsBuilder).
     *
     * @param sheet the containing sheet
     * @param sticks the entities to be dispatched
     */
    public static void splitVerticalSticks (Sheet       sheet,
                                            List<Stick> sticks)
    {
        process(
            sheet,
            sticks,
            new Adapter<Stick>() {
                    public List<Stick> getTarget (SystemInfo system)
                    {
                        return system.getVerticalSticks();
                    }

                    public int getXMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();

                        return box.x;
                    }

                    public int getYMin (Stick stick)
                    {
                        Rectangle box = stick.getContourBox();

                        return box.y;
                    }
                });
    }

    //---------//
    // process //
    //---------//
    /**
     * A generic method, meant to process a given collection of source entities
     * according to the different systems of the sheet.
     *
     * <p>Within a system, the various glyphs of same kind are then X-sorted.
     *
     * @param entities collection ofentities to be split among the score systems
     * @param adapter generic mediator to specify how each entity has to be
     *                 processed
     */
    private static <T> void process (Sheet                  sheet,
                                     Collection<?extends T> entities,
                                     final Adapter<T>       adapter)
    {
        // Sort entities according to their starting ordinate, in order to split
        // them vertically per systems.
        ArrayList<T> sortedEntities = new ArrayList<T>(entities);
        Collections.sort(
            sortedEntities,
            new Comparator<T>() {
                    public int compare (T o1,
                                        T o2)
                    {
                        return adapter.getYMin(o1) - adapter.getYMin(o2);
                    }
                });

        // Iterator on sorted Entities
        Iterator<T> it = sortedEntities.iterator();

        if (!it.hasNext()) {
            // Nothing to split
            return;
        }

        T obj = it.next();

        // Iterator on systems (info)
        systemloop: 
        for (SystemInfo system : sheet.getSystems()) {
            Collection<T> target = adapter.getTarget(system);

            while (true) {
                // Check end of system
                if (adapter.getYMin(obj) > system.getAreaBottom()) {
                    break; // Move to next system
                }

                // Include this entity, if valid, in the system area
                if (adapter.isValid(obj)) {
                    target.add(obj);
                }

                // Move to next entity if any
                if (it.hasNext()) {
                    obj = it.next();
                } else {
                    break systemloop;
                }
            }
        }

        //        // Sort entities, system by system, according to their left abscissa.
        //        for (SystemInfo system : sheet.getSystems()) {
        //            Collection<T> target = adapter.getTarget(system);
        //            Collections.sort(
        //                target,
        //                new Comparator<T>() {
        //                        public int compare (T o1,
        //                                            T o2)
        //                        {
        //                            return adapter.getXMin(o1) - adapter.getXMin(o2);
        //                        }
        //                    });
        //        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Class <code>Adapter</code> is a generic adapter which allows to specify
     * how a given kind of entities has to be processed in the split.
     */
    private abstract static class Adapter<T>
    {
        /**
         * Report the target collection within each system info structure for
         * each SystemInfo
         *
         * @param system the SystemInfo which contains the target collection
         *
         * @return the target list to fill
         */
        public abstract Collection<T> getTarget (SystemInfo system);

        /**
         * Report the first abscissa of the entity, to be able to split them
         * according to the system abscissae
         *
         * @param obj the entity to be put in the right system
         *
         * @return the left abscissa of the entity
         */
        public abstract int getXMin (T obj);

        /**
         * Report the first ordinate of the entity, to be able to split them
         * according to the system ordinates
         *
         * @param obj the entity to be put in the right system
         *
         * @return the top ordinate of the entity
         */
        public abstract int getYMin (T obj);

        /**
         * Check whether the entity is to be taken in the split
         *
         * @param obj the entity to be checked for validity
         *
         * @return true if valid
         */
        public boolean isValid (T obj)
        {
            return true;
        }
    }
}
