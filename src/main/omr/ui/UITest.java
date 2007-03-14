//-----------------------------------------------------------------------//
//                                                                       //
//                              U I T e s t                              //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui;

import omr.glyph.Glyph;

import omr.sheet.Sheet;
import omr.sheet.SheetManager;
import omr.sheet.SystemInfo;

import omr.util.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * A utility class, just used for small test action triggered from UI
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class UITest
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(UITest.class);

    //~ Constructors -----------------------------------------------------------

    private UITest ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    public static void test ()
    {
        Sheet sheet = SheetManager.getSelectedSheet();
        sheet.getVerticalLag()
             .dump("Sheet vLag");

        List<Glyph> actives = new ArrayList<Glyph>(sheet.getActiveGlyphs());
        List<Glyph> inactives = new ArrayList<Glyph>(
            sheet.getVerticalLag().getAllGlyphs());
        inactives.removeAll(actives);

        for (SystemInfo system : sheet.getSystems()) {
            System.out.println("\n" + system);
            System.out.println(
                "system " + system.getGlyphs().size() +
                Glyph.toString(system.getGlyphs()));

            for (Glyph glyph : system.getGlyphs()) {
                System.out.println(glyph.toString());
            }

            actives.removeAll(system.getGlyphs());
        }

        System.out.println("\nActives in no system (" + actives.size() + ") :");

        for (Glyph glyph : actives) {
            System.out.println(glyph.toString());
        }

        System.out.println("\nInactives in systems :");

        for (SystemInfo system : sheet.getSystems()) {
            System.out.println(system);

            for (Glyph glyph : system.getGlyphs()) {
                if (inactives.contains(glyph)) {
                    System.out.println("Inactive :" + glyph.toString());
                }
            }
        }
    }
}
