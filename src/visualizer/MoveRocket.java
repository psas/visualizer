package visualizer;

import java.awt.event.*;

import javax.media.j3d.*;
import javax.swing.Timer;
import javax.vecmath.*;

public class MoveRocket implements ActionListener
{
	private final TransformGroup camera, rocket;
	private final Timer t;
	private final FlightPattern fp;
	private long start = System.currentTimeMillis();
	private long diff = 0;
	private float lx, ly, lz;

	public MoveRocket(TransformGroup camera, TransformGroup rocket, Timer t, FlightPattern fp)
	{
		this.camera = camera;
		this.rocket = rocket;
		this.t = t;
		this.fp = fp;
		Transform3D first = new Transform3D();
		camera.getTransform(first);
		Vector3d vec = new Vector3d();
		first.get(vec);
		lx = (float) vec.x;
		ly = (float) vec.y;
		lz = (float) vec.z;
	}

	public void setStart(long newstart)
	{
		start = newstart - diff;
	}

	public long getStart()
	{
		return start;
	}

	public void setDiff(long newdiff)
	{
		diff = newdiff;
	}

	public void actionPerformed(ActionEvent ae)
	{
		double[] points = fp.getNewCoords(start);
		if(points == null)
			return;
		float x = (float) points[0];
		float y = (float) points[1];
		float z = (float) points[2];
		Transform3D trans = new Transform3D();
		trans.lookAt(new Point3d(lx + x, ly + y, lz + z), new Point3d(x, y, z), new Vector3d(0, 1, 0));
		trans.invert();
		camera.setTransform(trans);

		Transform3D objectTrans = new Transform3D();
		objectTrans.setTranslation(new Vector3d(x, y, z));
		rocket.setTransform(objectTrans);
		t.restart();
	}
}
