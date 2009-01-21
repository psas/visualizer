package visualizer;

import java.awt.BorderLayout;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.*;

import javax.imageio.ImageIO;
import javax.media.j3d.*;
import javax.swing.*;
import javax.vecmath.*;

import com.sun.j3d.utils.geometry.Sphere;
import com.sun.j3d.utils.image.TextureLoader;
import com.sun.j3d.utils.universe.PlatformGeometry;
import com.sun.j3d.utils.universe.SimpleUniverse;

public class Visualizer extends JFrame
{
	private static final String RESOURCE_DIR = "resource/";
	private static final String HeightFile = RESOURCE_DIR + "Default_HeightMap.png";
	private static final String TextureFile = RESOURCE_DIR + "Default_TexMap.png";
	private final BranchGroup sceneBG;
	private final BoundingSphere bounds;
	private final TransformGroup tgroup = new TransformGroup();
	private final TransformGroup camera;
	private BufferedImage LoadImage = null;
	private Texture2D tex = null;
	
	private int[] HeightMap;
	private int Width;
	
	private SimpleUniverse SimpleU = null;
	private BranchGroup ObjRoot = null;
	private BranchGroup NavRoot = null;
	private BoundingLeaf AlwaysOnBoundingLeaf = null;
	
	private TMouseBehavior MouseBeh;

	public Visualizer()
	{
		super("PSAS Rocket Visualizer");

		JPanel rocketPanel = new JPanel();
		rocketPanel.setLayout(new BorderLayout());
		sceneBG = new BranchGroup();
		bounds = new BoundingSphere(new Point3d(0, 0, 0), 1000);
		tgroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		addModel();
		lightScene();
//Begin TeVi code
		GraphicsConfigTemplate3D gctTmpl = new GraphicsConfigTemplate3D();
		GraphicsEnvironment gEnv = GraphicsEnvironment
				.getLocalGraphicsEnvironment();
		GraphicsDevice gDevice = gEnv.getDefaultScreenDevice();
		GraphicsConfiguration gConfig = gDevice
				.getBestConfiguration(gctTmpl);
		Canvas3D canvas3D = new Canvas3D(gConfig);
		
		canvas3D.setSize(512, 512);
		canvas3D.setStereoEnable(true);
		rocketPanel.add("Center", canvas3D);
		
		SimpleU = new SimpleUniverse(canvas3D);
		ObjRoot = new BranchGroup();
		NavRoot = new BranchGroup();

		ObjRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		ObjRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		NavRoot.setCapability(BranchGroup.ALLOW_CHILDREN_EXTEND);
		NavRoot.setCapability(BranchGroup.ALLOW_CHILDREN_WRITE);
		SimpleU.addBranchGraph(ObjRoot);
		SimpleU.addBranchGraph(NavRoot);
		
		AlwaysOnBoundingLeaf = new BoundingLeaf(new BoundingSphere(
				new Point3d(), 100000));
		PlatformGeometry platformGeom = new PlatformGeometry();

		platformGeom.addChild(AlwaysOnBoundingLeaf);
		platformGeom.compile();

		SimpleU.getViewingPlatform().setPlatformGeometry(platformGeom);
		
		BranchGroup mbg = new BranchGroup();
		MouseBeh = new TMouseBehavior();

		MouseBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		mbg.addChild(MouseBeh);
		NavRoot.addChild(mbg);

		camera = SimpleU.getViewingPlatform().getViewPlatformTransform();
		sceneBG.compile();
		initTerrain(sceneBG);
//End TeVi code
		add(rocketPanel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setVisible(true);
	}
//Begin TeVi code
	private void loadHeightData(String hFile)
	{
		TextureLoader texLoader = null;
		ImageComponent2D ic2d = null;

		try
		{
			LoadImage = ImageIO.read(new File(hFile));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		texLoader = new TextureLoader(LoadImage, this);
		ic2d = texLoader.getImage();

		int w = Math.min(ic2d.getWidth(), ic2d.getHeight());
		int possibleW = 3; // 0, 3, 5, 9, 17, 33, 65, 129, 257, ...
		if (w < 3)
			Width = 0;
		while (w > possibleW)
		{
			possibleW = (2 * possibleW) - 1;
		}
		if (w == possibleW)
			Width = possibleW;
		else
			Width = (possibleW + 1) / 2;
	}

	private void grabPixels(Image img, int x, int y, int w, int h,
			int[] pix, int off, int scansize)
	{

		PixelGrabber pg = new PixelGrabber(img, x, y, w, h, pix, off,
				scansize);

		try
		{
			pg.grabPixels();
		} catch (InterruptedException e)
		{

			System.err.println("interrupted waiting for pixels!");
		}
	}

	private void initTerrain(BranchGroup sceneBG)
	{
		loadHeightData(HeightFile);

		HeightMap = new int[Width * Width];

		grabPixels(LoadImage, 0, 0, Width, Width, HeightMap, 0, Width);

		for (int i = 0; i < HeightMap.length; i++)
		{
			HeightMap[i] &= 0xff;
		}

		TextureLoader texLoader = null;
		ImageComponent2D ic2d = null;

		try
		{
			LoadImage = ImageIO.read(new File(TextureFile));
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		texLoader = new TextureLoader(LoadImage, this);
		ic2d = texLoader.getImage();
		tex = new Texture2D(Texture.BASE_LEVEL,
				Texture.RGB, ic2d.getWidth(), ic2d
						.getHeight());

		tex.setImage(0, ic2d);
		tex.setEnable(true);
		tex.setMinFilter(Texture.NICEST);
		tex.setMagFilter(Texture.NICEST);

		BranchGroup scene = createSceneGraph(SimpleU, sceneBG);

		ObjRoot.addChild(scene);

		HeightMap = null;
	}

	private BranchGroup createSceneGraph(SimpleUniverse su, BranchGroup sceneBG)
	{
		BranchGroup terRoot = new BranchGroup();
		Transform3D t3d = new Transform3D();
		t3d.lookAt(new Point3d(15, 15, 15), new Point3d(),
				new Vector3d(0, 1, 0));
		t3d.invert();
		camera.setTransform(t3d);

		Terrain terrain = new Terrain(su, HeightMap, tex, 0.0f,
				0.0f, 30.0f / (float) Width, 3.0f);

		terRoot.addChild(terrain);
		terRoot.addChild(getTheBackground());
		terRoot.addChild(sceneBG);

		MouseBeh.init(camera, 0.008f, false);

		BranchGroup kbg = new BranchGroup();
		TKeyBehavior keyBeh = new TKeyBehavior(terrain, 0.5f, tgroup, camera);

		keyBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		kbg.addChild(keyBeh);
		NavRoot.addChild(kbg);

		TEachFrameBehavior efBeh = new TEachFrameBehavior(terrain,
				camera, keyBeh, MouseBeh);

		efBeh.setSchedulingBoundingLeaf(AlwaysOnBoundingLeaf);
		terRoot.addChild(efBeh);

		terRoot.compile();

		return terRoot;
	}
//End TeVi code
	public void dispose()
	{
		SimpleU.removeAllLocales();
		super.dispose();
		System.exit(0);
	}

	private Background getTheBackground()
	{
		String fnm = RESOURCE_DIR + "clouds.jpg";
		TextureLoader texLoader = new TextureLoader(fnm, null);
		Texture2D texture = (Texture2D) texLoader.getTexture();
		if(texture == null)
			System.out.println("Cannot load texture from " + fnm);
		else
		{
			System.out.println("Loaded texture from " + fnm);
			texture.setEnable(true);
		}

		Sphere sphere = new Sphere(1.0f, Sphere.GENERATE_NORMALS_INWARD
				| Sphere.GENERATE_TEXTURE_COORDS, 8);
		Appearance backApp = sphere.getAppearance();
		backApp.setTexture(texture);

		BranchGroup backBG = new BranchGroup();
		backBG.addChild(sphere);

		Background bg = new Background();
		bg.setApplicationBounds(bounds);
		bg.setGeometry(backBG);

		return bg;
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

	private void addModel()
	{
		ModelLoader ml = new ModelLoader();

		Transform3D t3d = new Transform3D();
		t3d.setIdentity();
		t3d.set(new Vector3d(0, 2.4, 0));

		TransformGroup tg4 = new TransformGroup(t3d);
		tg4.addChild(ml.getModel(RESOURCE_DIR + "tintin_rocket.obj"));
		tgroup.addChild(tg4);
		sceneBG.addChild(tgroup);
	}

	public static void main(String[] args)
	{
		SwingUtilities.invokeLater(new Runnable(){
			public void run()
			{
				new Visualizer();
			}
		});
	}

}
