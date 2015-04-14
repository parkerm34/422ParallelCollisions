package view;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class OptionGUI extends JFrame {

	private CollisionGUI colGUI;
	private JPanel speedPanel;
	private JPanel reset;
	private JPanel optionPanel;
	private JButton up = new JButton("Speed up");
	private JButton down = new JButton("Slow Down");
	private JButton res = new JButton("Reset Animation");
	
	public OptionGUI( CollisionGUI colGUI ) {
		this.colGUI = colGUI;
		layoutGUI();
	}
	
	private void layoutGUI() {
		setTitle("2D Collisions Options");
	    setBackground(Color.BLUE);
	    setLocation(750, 0);
	    setMinimumSize(new Dimension(200, 200));
	    
	    up.setPreferredSize(new Dimension(100, 100));
	    down.setPreferredSize(new Dimension(100, 100));
	    res.setPreferredSize(new Dimension(100,100));
	    
	    speedPanel = new JPanel();
	    speedPanel.add(down);
	    speedPanel.add(up);
	    
	    reset = new JPanel();
	    reset.add(res);
	    
	    optionPanel = new JPanel();
	    optionPanel.add(speedPanel);
	    optionPanel.add(reset);
	    
	    this.add(optionPanel);
	    
	    setVisible(true);
	}
}
