package visualizer;

import java.awt.*;

import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.image.*;
import com.sun.j3d.utils.universe.*;

public class RocketPanel extends JPanel
{
	private static final String RESOURCE_DIR = "resource/";
	private final BranchGroup sceneBG;
	private final BoundingSphere bounds;
	private final TransformGroup tgroup = new TransformGroup();
	private TransformGroup camera = new TransformGroup();

	public RocketPanel()
	{
		sceneBG = new BranchGroup();
		bounds = new BoundingSphere(new Point3d(0, 0, 0), 1000);
		tgroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		camera.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		setLayout(new BorderLayout());
		setOpaque(false);
		setPreferredSize(new Dimension(1300, 1000));

		GraphicsConfiguration config = SimpleUniverse
				.getPreferredConfiguration();
		Canvas3D canvas3D = new Canvas3D(config);
		add("Center", canvas3D);
		canvas3D.setFocusable(true);
		canvas3D.requestFocus();

		SimpleUniverse su = new SimpleUniverse(canvas3D);

		camera = su.getViewingPlatform().getViewPlatformTransform();

		View view = su.getViewer().getView();
		view
				.setTransparencySortingPolicy(View.TRANSPARENCY_SORT_GEOMETRY);

		createSceneGraph();

		sceneBG.compile();
		su.addBranchGraph(sceneBG);
	}

	private void createSceneGraph()
	{
		lightScene();
		addModels();
		addBackground("clouds.jpg");
		addGroundCover();

		sceneBG.addChild(new GroundFloor());
	}

	private void addGroundCover()
	{
		Transform3D t3d = new Transform3D();
		t3d.set(new Vector3d(4, 0, 0));
		TransformGroup tg1 = new TransformGroup(t3d);
		tg1.addChild(new GroundCover(RESOURCE_DIR + "tree1.gif", 3));
		sceneBG.addChild(tg1);

		t3d.set(new Vector3d(-3, 0, 0));
		TransformGroup tg2 = new TransformGroup(t3d);
		tg2.addChild(new GroundCover(RESOURCE_DIR + "tree2.gif", 2));
		sceneBG.addChild(tg2);

		t3d.set(new Vector3d(2, 0, -6));
		TransformGroup tg3 = new TransformGroup(t3d);
		tg3.addChild(new GroundCover(RESOURCE_DIR + "tree4.gif", 3));
		sceneBG.addChild(tg3);

		t3d.set(new Vector3d(-1, 0, -4));
		TransformGroup tg4 = new TransformGroup(t3d);
		tg4.addChild(new GroundCover(RESOURCE_DIR + "cactus.gif"));
		sceneBG.addChild(tg4);
	}

	private void addBackground(String fnm)
	{
		Texture2D tex = loadTexture(RESOURCE_DIR + fnm);

		Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS_INWARD
				| Sphere.GENERATE_TEXTURE_COORDS, 8);
		Appearance backApp = sphere.getAppearance();
		backApp.setTexture(tex);

		BranchGroup backBG = new BranchGroup();
		backBG.addChild(sphere);

		Background bg = new Background();
		bg.setApplicationBounds(bounds);
		bg.setGeometry(backBG);

		sceneBG.addChild(bg);
	}

	private Texture2D loadTexture(String fn)
	{
		TextureLoader texLoader = new TextureLoader(fn, null);
		Texture2D texture = (Texture2D) texLoader.getTexture();
		if(texture == null)
			System.out.println("Cannot load texture from " + fn);
		else
		{
			System.out.println("Loaded texture from " + fn);
			texture.setEnable(true);
		}
		return texture;
	}

	public void moveModel()
	{
		Transform3D trans = new Transform3D();
		float height = 0;
		while(true)
		{
			Transform3D t = new Transform3D();
			trans.setTranslation(new Vector3d(0.0f, height += 0.1,
					15.0f));
			t.mul(trans);
			camera.setTransform(t);
			Transform3D objectTrans = new Transform3D();
			objectTrans.setTranslation(new Vector3d(0.0f, height,
					0.0f));
			tgroup.setTransform(objectTrans);
			try
			{
				Thread.sleep(200);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	private void lightScene()
	{
		Color3f white = new Color3f(1.0f, 1.0f, 1.0f);

		AmbientLight ambientLightNode = new AmbientLight(white);
		ambientLightNode.setInfluencingBounds(bounds);
		sceneBG.addChild(ambientLightNode);

		Vector3f light1Direction = new Vector3f(-1.0f, -1.0f, -1.0f);
		Vector3f light2Direction = new Vector3f(1.0f, -1.0f, 1.0f);

		DirectionalLight light1 = new DirectionalLight(white,
				light1Direction);
		light1.setInfluencingBounds(bounds);
		sceneBG.addChild(light1);

		DirectionalLight light2 = new DirectionalLight(white,
				light2Direction);
		light2.setInfluencingBounds(bounds);
		sceneBG.addChild(light2);
	}

	private void addModels()
	{
		ModelLoader ml = new ModelLoader();

		Transform3D t3d = new Transform3D();
		t3d.setIdentity();
		t3d.setTranslation(new Vector3d(0.0f, 0.0f, 0.0f));

		t3d.set(new Vector3d(0, 0, 0));
		TransformGroup tg4 = new TransformGroup(t3d);
		tg4.addChild(ml.getModel(RESOURCE_DIR + "tintin_rocket.obj"));
		tgroup.addChild(tg4);
		sceneBG.addChild(tgroup);
	}
}
