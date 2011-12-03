//----------------------------------------------------------------------------//
//                                                                            //
//                             I m p l e m e n t                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation {@code Implement} is used to flag a method which is (part of)
 * the implementation of an interface. It is foreseen to be used by a tool such
 * 'apt' in the future.
 */
@Documented
@Target(ElementType.METHOD)
public @interface Implement {
    /**
     * @return the interface whose method is implemented
     */
    Class value();
}
