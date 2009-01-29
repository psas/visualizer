package visualizer;

public class LineFlightPattern implements FlightPattern
{

        public double[] getNewCoords(long start)
        {
                double diff = System.currentTimeMillis()-start-99;
                double[] points = new double[]
                {
                        0, diff/1000, 0
                };
                return points;
        }

}
