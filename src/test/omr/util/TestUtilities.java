//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T e s t U t i l i t i e s                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.util;

import java.awt.*;

import javax.swing.*;

/**
 * DOCUMENT ME!
 *
 * @author TBD
 * @version TBD
 */
public class TestUtilities
{
    //~ Static fields/initializers -----------------------------------------------------------------

    static int counter;

    //~ Constructors -------------------------------------------------------------------------------
    private TestUtilities ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------------//
    // getChildIndexed //
    //-----------------//
    public static Component getChildIndexed (Component parent,
                                             String klass,
                                             int index)
    {
        counter = 0;

        // Step in only owned windows and ignore its components in JFrame
        if (parent instanceof Window) {
            Component[] children = ((Window) parent).getOwnedWindows();

            for (int i = 0; i < children.length; ++i) {
                // take only active windows
                if (children[i] instanceof Window && !((Window) children[i]).isActive()) {
                    continue;
                }

                Component child = getChildIndexedInternal(children[i], klass, index);

                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }

    //---------------//
    // getChildNamed //
    //---------------//
    public static Component getChildNamed (Component parent,
                                           String name)
    {
        // Debug line
        //System.out.println("Class: " + parent.getClass() +
        //              " Name: " + parent.getName());
        if (name.equals(parent.getName())) {
            return parent;
        }

        if (parent instanceof Container) {
            Component[] children = (parent instanceof JMenu) ? ((JMenu) parent).getMenuComponents()
                    : ((Container) parent).getComponents();

            for (int i = 0; i < children.length; ++i) {
                Component child = getChildNamed(children[i], name);

                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }

    //-------------------------//
    // getChildIndexedInternal //
    //-------------------------//
    private static Component getChildIndexedInternal (Component parent,
                                                      String klass,
                                                      int index)
    {
        // Debug line
        //System.out.println("Class: " + parent.getClass() +
        //              " Name: " + parent.getName());
        if (parent.getClass().toString().endsWith(klass)) {
            if (counter == index) {
                return parent;
            }

            ++counter;
        }

        if (parent instanceof Container) {
            Component[] children = (parent instanceof JMenu) ? ((JMenu) parent).getMenuComponents()
                    : ((Container) parent).getComponents();

            for (int i = 0; i < children.length; ++i) {
                Component child = getChildIndexedInternal(children[i], klass, index);

                if (child != null) {
                    return child;
                }
            }
        }

        return null;
    }
}
// vim: set ai sw=4 ts=4:
