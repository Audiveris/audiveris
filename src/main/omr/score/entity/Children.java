//----------------------------------------------------------------------------//
//                                                                            //
//                              C h i l d r e n                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
package omr.score.entity;

import java.lang.annotation.*;

/**
 * Annotation <code>Children</code> designates a member that should be
 * considered as a collection of children of the current class in the browsable
 * score hierarchy
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Children {}
