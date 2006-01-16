//-----------------------------------------------------------------------//
//                                                                       //
//                      G l y p h D i r e c t o r y                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.glyph;

import omr.util.Directory;

/**
 * Interface <code>GlyphDirectory</code> declares the ability to retrieve a
 * glyph via its Id.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public interface GlyphDirectory
    extends Directory <Integer, Glyph>
{
}
