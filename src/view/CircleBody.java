package view;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;

import javax.swing.JPanel;

public class CircleBody extends JPanel 
{ 
	Ellipse2D.Double circle;

    public CircleBody(double d)
    {
        circle = new Ellipse2D.Double(0, 0, d, d);
        setOpaque(false);
    }

    public Dimension getPreferredSize()
    {
         Rectangle bounds = circle.getBounds();
        return new Dimension(bounds.width, bounds.height);
    }

    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor( getForeground() );
        g2.fill(circle);
    }

} 