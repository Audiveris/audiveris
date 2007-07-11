package omr.jaxb.basic;

import java.util.List;
import javax.xml.bind.annotation.*;

public class Day
{
    @XmlAttribute
    public Weekday label;
    //
    @XmlElement(name="meeting")
    List<Meeting> meetings;
}
