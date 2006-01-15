package omr.jibx;

import java.awt.Point;
import java.util.List;

public class Waiter
{
    public int id;
    public String firstName;
    public Point location;
    public Purse purse;

    private List<Day> days;

    public List<Day> getDays()
    {
        return days;
    }

    public void setDays(List<Day> days)
    {
        this.days = days;
    }
}
