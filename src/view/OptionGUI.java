package view;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Hashtable;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class OptionGUI extends JFrame implements ChangeListener {

	private CollisionGUI colGUI;
	private JPanel speedPanel;
	private JPanel optionPanel;
	private int SLOWEST = 150;
	private int FASTEST = 0;
	private int INIT = 75;
	private int SPACING = SLOWEST/6;
	private JSlider speed = new JSlider(JSlider.HORIZONTAL, FASTEST, SLOWEST, INIT);
	public String[] args;
	
	public OptionGUI( CollisionGUI colGUI ) {
		this.colGUI = colGUI;
		layoutGUI();
	}
	
	private void layoutGUI() {
		setTitle("2D Collisions Options");
	    setBackground(Color.BLUE);
	    setLocation(750, 0);
	    setMinimumSize(new Dimension(200, 100));
	    
	    speed.addChangeListener( this );
	    
	    speed.setMajorTickSpacing( SPACING );
	    speed.setPaintTicks(true);
	    
	    Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
	    labelTable.put( new Integer( FASTEST ), new JLabel("Slow") );
	    labelTable.put( new Integer( SLOWEST ), new JLabel("Fast") );
	    speed.setLabelTable( labelTable );

	    speed.setPaintLabels(true);
	    
	    speedPanel = new JPanel();
	    speedPanel.add(speed);
	    
	    optionPanel = new JPanel();
	    optionPanel.add(speedPanel);
	    
	    this.add(optionPanel);
	    
	    setVisible(true);
	}
	
	@Override
	public void stateChanged(ChangeEvent e) {

		JSlider source = (JSlider)e.getSource();
	    if (!source.getValueIsAdjusting()) {
	        int animSpeed = (int)source.getValue();
	        colGUI.ANIMATIONTIME = SLOWEST - animSpeed;
	        System.out.println(SLOWEST - animSpeed);
	    }
	}
}
