package omr.jibx;

public class Purse
{
    public double[] tips;

    public Double[] getTips()
    {
        Double[] dd = null;
        if (tips != null) {
            dd = new Double[tips.length];
            for (int i = 0; i < tips.length; i++) {
                dd[i] = tips[i];
            }
        }
        return dd;
    }

    public void setTips (Double[] tips)
    {
        this.tips = new double[tips.length];
        for (int i = 0; i < tips.length; i++) {
            this.tips[i] = tips[i];
        }
    }
}
