@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(value = Jaxb.PathAdapter.class, type = Path.class)
})
package omr;

import omr.util.Jaxb;

import java.nio.file.Path;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
