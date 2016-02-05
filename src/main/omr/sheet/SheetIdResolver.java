//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S h e e t I d R e s o l v e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.glyph.Glyph;

import omr.sig.inter.Inter;

import com.sun.xml.internal.bind.IDResolver; // Sun internals used here!

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xml.sax.SAXException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Class {@code SheetIdResolver} is a custom JAXB unmarshalling IDResolver meant to
 * handle <b>two families of ID/IDREF</b> in a sheet, each with its own scope:
 * one for Inter instances and one for Glyph instances.
 * <p>
 * NOTA: this class extends com.sun.xml.internal.bind.IDResolver which is not publicly available
 * though advertised by JAXB.
 * <p>
 * Compiler javac does not link against {@code rt.jar} but uses {@code lib/ct.sym} symbol table
 * which does not reference this class. Two workarounds are possible until a better JAXB
 * customization approach is provided (we use the latter one):
 * <ul>
 * <li>Explicitly include rt.jar in the compile dependencies, or
 * <li>Use -XDignore.symbol.file as javac (and javadoc) option
 * </ul>
 *
 * @see
 * <a href="http://stackoverflow.com/questions.4065401/using-internal-sun-classes-with-javac">
 * http://stackoverflow.com/questions.4065401/using-internal-sun-classes-with-javac</a>
 *
 * @author Hervé Bitteur
 */
public class SheetIdResolver
        extends IDResolver
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetIdResolver.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Map of glyph IDs. */
    private final Map<String, Glyph> glyphMap = new HashMap<String, Glyph>();

    /** Map of inter IDs. */
    private final Map<String, Inter> interMap = new HashMap<String, Inter>();

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // bind //
    //------//
    /**
     * Binds the given object to the specified ID.
     *
     * @param id  The ID value found in the document being unmarshalled. Always non-null.
     * @param obj The object being unmarshalled which is going to own the ID. Always non-null.
     * @throws SAXException
     */
    @Override
    public void bind (String id,
                      Object obj)
            throws SAXException
    {
        if (obj instanceof Glyph) {
            glyphMap.put(id, (Glyph) obj);
        } else {
            interMap.put(id, (Inter) obj);
        }
    }

    //-----------------//
    // getPropertyName //
    //-----------------//
    /**
     * Report the property name to be used when assigning an IDResolver to unmarshaller.
     *
     * @return class name of IDResolver
     */
    public static String getPropertyName ()
    {
        return IDResolver.class.getName();
    }

    //---------//
    // resolve //
    //---------//
    /**
     * Obtains the object to be pointed by the IDREF value.
     *
     * @param id         The IDREF value found in the document being unmarshalled. Always non-null.
     * @param targetType The expected type to which ID resolves to.
     *                   JAXB infers this information from the signature of the fields that has
     *                   {@link javax.xml.bind.annotation.XmlIDREF}.
     *                   When a property is a collection, this parameter will be the type of the
     *                   individual item in the collection.
     * @return null if the implementation is sure that the parameter combination will never yield a
     *         valid object. Otherwise non-null.
     * @throws SAXException
     */
    @Override
    public Callable<?> resolve (final String id,
                                final Class targetType)
            throws SAXException
    {
        return new Callable()
        {
            @Override
            public Object call ()
            {
                final Object obj;

                // For glyph, we do   get type = BasicGlyph class
                // For inter, we only get type = Object class
                if (Glyph.class.isAssignableFrom(targetType)) {
                    obj = glyphMap.get(id);
                } else {
                    obj = interMap.get(id);
                }

                return obj;
            }
        };
    }
}
