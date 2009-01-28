package visualizer;

public class SpiralFlightPattern implements FlightPattern
{
	static final int RADIUS = 20*20;
	final int num = Math.round((float) Math.sqrt(RADIUS))*4+2;
	final double[] allxcoords = new double[num*100];
	final double[] allycoords = new double[num*100];
	final double[] allzcoords = new double[num*100];

	public SpiralFlightPattern()
	{
		double[] xcoords = new double[num];
		double[] zcoords = new double[num];
		int i = 0;
		for(int x=-RADIUS; x<RADIUS+1; x++)
		{
			double z = Math.sqrt(-x*x + RADIUS);
			if(!Double.isNaN(z))
			{
				xcoords[i] = x;
				zcoords[i] = z;
				i++;
			}
		}
		for(int x=RADIUS; x>-RADIUS; x--)
		{
			double z = -Math.sqrt(-x*x + RADIUS);
			if(!Double.isNaN(z))
			{
				xcoords[i] = x;
				zcoords[i] = z;
				i++;
			}
		}
		for(int x=0; x<num*100; x++)
			allycoords[x] = x;
		int w = 0;
		for(int m=0; m<100; m++)
		{
			int n = 0;
			for(int j=w; j<num+w; j++)
			{
				allxcoords[j] = xcoords[n];
				allzcoords[j] = zcoords[n];
				n++;
			}
			w+=num;
		}
	}
	public double[] getXCoords()
	{
		return allxcoords;
	}
	public double[] getYCoords()
	{
		return allycoords;
	}
	public double[] getZCoords()
	{
		return allzcoords;
	}
}
