package visualizer;

import java.io.*;
import java.net.*;

import javax.media.j3d.*;
import javax.vecmath.*;

import com.sun.j3d.loaders.*;
import com.sun.j3d.loaders.objectfile.*;

public class ModelLoader
{
	private static final double MAX_SIZE = 5.0;
	private ObjectFile objFileloader;

	public ModelLoader()
	{
		objFileloader = new ObjectFile();
	}

	public TransformGroup getModel(String fnm)
	{
		BranchGroup modelBG = loadModel(fnm);
		if(modelBG == null)
			return null;

		double scaleFactor = getScaling(modelBG);

		Transform3D t3d = new Transform3D();
		t3d.setScale(scaleFactor);
		t3d.setTranslation(new Vector3d(0, 0, 0));

		TransformGroup tg = new TransformGroup(t3d);
		tg.addChild(modelBG);
		return tg;
	}

	private BranchGroup loadModel(String fnm)
	{
		File file = new java.io.File(fnm);
		if(!file.exists())
		{
			return null;
		}

		URL url = null;
		try
		{
			url = file.toURI().toURL();
		}
		catch(Exception e)
		{
			return null;
		}

		Scene scene = null;
		try
		{
			scene = objFileloader.load(url);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}

		if(scene != null)
			return scene.getSceneGroup();
		else
			return null;
	}

	private double getScaling(BranchGroup modelBG)
	{
		double scaleFactor = 1.0;
		BoundingBox boundBox = new BoundingBox(modelBG.getBounds());

		Point3d lower = new Point3d();
		boundBox.getLower(lower);

		Point3d upper = new Point3d();
		boundBox.getUpper(upper);

		double maxDim = getMaxDimension(lower, upper);
		if(maxDim > MAX_SIZE)
		{
			scaleFactor = MAX_SIZE / maxDim;
			System.out.println("Applying scaling factor: "
					+ scaleFactor);
		}

		return scaleFactor;
	}

	private double getMaxDimension(Point3d lower, Point3d upper)
	{
		double max = 0;
		if((upper.x - lower.x) > max)
			max = upper.x - lower.x;
		if((upper.y - lower.y) > max)
			max = upper.y - lower.y;
		if((upper.z - lower.z) > max)
			max = upper.z - lower.z;
		return max;
	}
}
