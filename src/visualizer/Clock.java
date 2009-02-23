package visualizer;

public class Clock
{
	long stop = 0;
	long start = 0;
	long speed = 0;
	public void startClock()
	{
		start += System.currentTimeMillis()-stop;
	}
	public void stopClock()
	{
		 stop = System.currentTimeMillis();
	}
	public long getTime()
	{
		return (System.currentTimeMillis() - start) * speed;
	}
	public void setSpeed(int i)
	{
		start = System.currentTimeMillis() - (System.currentTimeMillis() - start) * speed / i;
		speed = i;
	}
}
