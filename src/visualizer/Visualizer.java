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
		RocketPanel panel = new RocketPanel();
		c.add(panel, BorderLayout.CENTER);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
		setResizable(false);
		setVisible(true);
		panel.moveModel();
	}

	public static void main(String[] args)
	{
		new Visualizer();
	}

}
