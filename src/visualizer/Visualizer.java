package visualizer;

import javax.swing.*;
import java.awt.*;

public class Visualizer extends JFrame
{
	public Visualizer()
	{
		super("ObjView3D");
		Container c = getContentPane();
		c.setLayout(new BorderLayout());
		RocketPanel w3d = new RocketPanel();
		c.add(w3d, BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setResizable(false);
		setVisible(true);
		w3d.moveModel();
	}

	public static void main(String[] args)
	{
		new Visualizer();
	}

}
