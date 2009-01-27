package visualizer;

import java.io.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.loaders.*;
import com.sun.j3d.loaders.objectfile.*;

public class ModelLoader
{
	private static final double MAX_SIZE = 1.0;

	public TransformGroup getModel(String fnm)
	{
		File file = new java.io.File(fnm);
		Scene scene = null;
		try
		{
			if(!file.exists())
			{
				throw new FileNotFoundException(fnm);
			}
			scene = new ObjectFile().load(file.toURI().toURL());
		} catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		BranchGroup modelBG = scene.getSceneGroup();
		if(modelBG == null)
			return null;

		Transform3D t3d = new Transform3D();
		t3d.setScale(getScaling(modelBG));
		TransformGroup tg = new TransformGroup(t3d);
		tg.addChild(modelBG);
		return tg;
	}

	private double getScaling(BranchGroup modelBG)
	{
		double scaleFactor = 1.0;
		BoundingBox boundBox = new BoundingBox(modelBG.getBounds());

		Point3d lower = new Point3d();
		boundBox.getLower(lower);
		Point3d upper = new Point3d();
		boundBox.getUpper(upper);

		double max = 0;
		if((upper.x - lower.x) > max)
			max = upper.x - lower.x;
		if((upper.y - lower.y) > max)
			max = upper.y - lower.y;
		if((upper.z - lower.z) > max)
			max = upper.z - lower.z;
		if(max > MAX_SIZE)
		{
			scaleFactor = MAX_SIZE / max;
			System.out.println("Applying scaling factor: " + scaleFactor);
		}
		return scaleFactor;
	}
}
