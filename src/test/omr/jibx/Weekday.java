package omr.jibx;

public enum Weekday
{
    MONDAY,
        TUESDAY,
        WEDNESDAY,
        THURSDAY,
        FRIDAY,
        SATURDAY,
        SUNDAY;

    Weekday ()
    {
        System.out.println("Weekday() called");
    }

//     private static String serializer (Weekday day)
//     {
//         return day.toString();
//     }
}
