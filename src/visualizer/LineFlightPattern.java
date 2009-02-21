package visualizer;

public class LineFlightPattern implements FlightPattern
{

        public double[] getNewCoords(long start)
        {
                double[] points = new double[]
                {
                        0, start/(double) 1000, 0
                };
                return points;
        }

}
