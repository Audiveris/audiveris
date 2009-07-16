//----------------------------------------------------------------------------//
//                                                                            //
//                    C a t e g o r y M a p A d a p t e r                     //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.math;

import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class <code>CategoryMapAdapter</code> is an adapter to allow Java - XML
 * mapping for CategoryDesc
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class CategoryMapAdapter
    extends XmlAdapter<CategoryDesc[], Map<String, CategoryDesc>>
{
    //~ Methods ----------------------------------------------------------------

    //-----------//
    // unmarshal //
    //-----------//
    @Override
    public CategoryDesc[] marshal (Map<String, CategoryDesc> map)
        throws Exception
    {
        return map.values()
                  .toArray(new CategoryDesc[map.size()]);
    }

    //-----------//
    // unmarshal //
    //-----------//
    @Override
    public Map<String, CategoryDesc> unmarshal (CategoryDesc[] categories)
    {
        Map<String, CategoryDesc> map = new HashMap<String, CategoryDesc>();

        for (CategoryDesc category : categories) {
            map.put(category.getId().toString(), category);
        }

        return map;
    }
}
