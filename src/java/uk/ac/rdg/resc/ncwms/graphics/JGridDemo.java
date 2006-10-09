/*
 * $Id: JGridDemo.java,v 1.8 2001/02/05 23:28:46 dwd Exp $
 *
 * This software is provided by NOAA for full, free and open release.  It is
 * understood by the recipient/user that NOAA assumes no liability for any
 * errors contained in the code.  Although this software is released without
 * conditions or restrictions in its use, it is expected that appropriate
 * credit be given to its author and to the National Oceanic and Atmospheric
 * Administration should the software be included by the recipient as an
 * element in other product development.
 */
package uk.ac.rdg.resc.ncwms.graphics;

import gov.noaa.pmel.sgt.swing.JPlotLayout;
import gov.noaa.pmel.sgt.swing.JClassTree;
import gov.noaa.pmel.sgt.swing.prop.GridAttributeDialog;
import gov.noaa.pmel.sgt.JPane;
import gov.noaa.pmel.sgt.GridAttribute;
import gov.noaa.pmel.sgt.ContourLevels;
import gov.noaa.pmel.sgt.CartesianRenderer;
import gov.noaa.pmel.sgt.CartesianGraph;
import gov.noaa.pmel.sgt.GridCartesianRenderer;
import gov.noaa.pmel.sgt.IndexedColorMap;
import gov.noaa.pmel.sgt.ColorMap;
import gov.noaa.pmel.sgt.LinearTransform;
import gov.noaa.pmel.sgt.demo.TestData;
import gov.noaa.pmel.sgt.dm.SGTData;
import gov.noaa.pmel.util.Range2D;
import gov.noaa.pmel.util.Dimension2D;
import gov.noaa.pmel.util.Rectangle2D;

import java.awt.*;
import javax.swing.*;
/**
 * Example demonstrating how to use <code>JPlotLayout</code>
 * to create a raster-contour plot.
 * 
 * @author Donald Denbo
 * @version $Revision: 1.8 $, $Date: 2001/02/05 23:28:46 $
 * @since 2.0
 */
public class JGridDemo extends JApplet {
  static JPlotLayout rpl_;
  private GridAttribute gridAttr_;
  JButton edit_;
  JButton space_ = null;
  JButton tree_;

  public void init() {
    /*
     * Create the demo in the JApplet environment.
     */
    getContentPane().setLayout(new BorderLayout(0,0));
    setBackground(java.awt.Color.white);
    setSize(600,550);
    JPanel main = new JPanel();
    rpl_ = makeGraph();
    JPanel button = makeButtonPanel(false);
    rpl_.setBatch(true);
    main.add(rpl_, BorderLayout.CENTER);
    JPane gridKeyPane = rpl_.getKeyPane();
    gridKeyPane.setSize(new Dimension(600,100));
    main.add(gridKeyPane, BorderLayout.SOUTH);
    getContentPane().add(main, "Center");
    getContentPane().add(button, "South");
    rpl_.setBatch(false);
  }

  JPanel makeButtonPanel(boolean mark) {
    JPanel button = new JPanel();
    button.setLayout(new FlowLayout());
    tree_ = new JButton("Tree View");
    MyAction myAction = new MyAction();
    tree_.addActionListener(myAction);
    button.add(tree_);
    edit_ = new JButton("Edit GridAttribute");
    edit_.addActionListener(myAction);
    button.add(edit_);
    /*
     * Optionally leave the "mark" button out of the button panel
     */
    if(mark) {
      space_ = new JButton("Add Mark");
      space_.addActionListener(myAction);
      button.add(space_);
    }
    return button;
  }
  public static void main(String[] args) {
    /*
     * Create the demo as an application
     */
    JGridDemo gd = new JGridDemo();
    /*
     * Create a new JFrame to contain the demo.
     */
    JFrame frame = new JFrame("Grid Demo");
    JPanel main = new JPanel();
    main.setLayout(new BorderLayout());
    frame.setSize(600,500);
    frame.getContentPane().setLayout(new BorderLayout());
    /*
     * Listen for windowClosing events and dispose of JFrame
     */
    frame.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent event) {
	JFrame fr = (JFrame)event.getSource();
	fr.setVisible(false);
	fr.dispose();
	System.exit(0);
      }
      public void windowOpened(java.awt.event.WindowEvent event) {
	rpl_.getKeyPane().draw();
      }
    });
    /*
     * Create button panel with "mark" button
     */
    JPanel button = gd.makeButtonPanel(true);
    /*
     * Create JPlotLayout and turn batching on.  With batching on the
     * plot will not be updated as components are modified or added to
     * the plot tree.
     */
    rpl_ = gd.makeGraph();
    rpl_.setBatch(true);
    /*
     * Layout the plot, key, and buttons.
     */
    main.add(rpl_, BorderLayout.CENTER);
    JPane gridKeyPane = rpl_.getKeyPane();
    gridKeyPane.setSize(new Dimension(600,100));
    rpl_.setKeyLayerSizeP(new Dimension2D(6.0, 1.0));
    rpl_.setKeyBoundsP(new Rectangle2D.Double(0.0, 1.0, 6.0, 1.0));
    main.add(gridKeyPane, BorderLayout.SOUTH);
    frame.getContentPane().add(main, BorderLayout.CENTER);
    frame.getContentPane().add(button, BorderLayout.SOUTH);
    frame.pack();
    frame.setVisible(true);
    /*
     * Turn batching off. JPlotLayout will redraw if it has been
     * modified since batching was turned on.
     */
    rpl_.setBatch(false);
  }

  void edit_actionPerformed(java.awt.event.ActionEvent e) {
    /*
     * Create a GridAttributeDialog and set the renderer.
     */
    GridAttributeDialog gad = new GridAttributeDialog();
    gad.setJPane(rpl_);
    CartesianRenderer rend = ((CartesianGraph)rpl_.getFirstLayer().getGraph()).getRenderer();
    gad.setGridCartesianRenderer((GridCartesianRenderer)rend);
    //        gad.setGridAttribute(gridAttr_);
    gad.setVisible(true);
  }

    void tree_actionPerformed(java.awt.event.ActionEvent e) {
      /*
       * Create a JClassTree for the JPlotLayout objects
       */
        JClassTree ct = new JClassTree();
        ct.setModal(false);
        ct.setJPane(rpl_);
        ct.show();
    }
    
  JPlotLayout makeGraph() {
    /*
     * This example uses a pre-created "Layout" for raster time
     * series to simplify the construction of a plot. The
     * JPlotLayout can plot a single grid with
     * a ColorKey, time series with a LineKey, point collection with a
     * PointCollectionKey, and general X-Y plots with a
     * LineKey. JPlotLayout supports zooming, object selection, and
     * object editing.
     */                
    SGTData newData;
    TestData td;
    JPlotLayout rpl;
    ContourLevels clevels;
    /*
     * Create a test grid with sinasoidal-ramp data.
     */
    Range2D xr = new Range2D(190.0f, 250.0f, 1.0f);
    Range2D yr = new Range2D(0.0f, 45.0f, 1.0f);
    td = new TestData(TestData.XY_GRID, xr, yr, 
		      TestData.SINE_RAMP, 12.0f, 30.f, 5.0f);
    newData = td.getSGTData();
    /*
     * Create the layout without a Logo image and with the
     * ColorKey on a separate Pane object.
     */   
    rpl = new JPlotLayout(true, false, false, "test layout", null, true);
    rpl.setEditClasses(false);
    /*
     * Create a GridAttribute for CONTOUR style.
     */
    Range2D datar = new Range2D(-20.0f, 45.0f, 5.0f);
    clevels = ContourLevels.getDefault(datar);
    gridAttr_ = new GridAttribute(clevels);
    /*
     * Create a ColorMap and change the style to RASTER_CONTOUR.
     */
    ColorMap cmap = createColorMap(datar);
    gridAttr_.setColorMap(cmap);
    gridAttr_.setStyle(GridAttribute.RASTER_CONTOUR);
    /*
     * Add the grid to the layout and give a label for
     * the ColorKey.
     */
    rpl.addData(newData, gridAttr_, "First Data");
    /*
     * Change the layout's three title lines.
     */        
    rpl.setTitles("Raster Plot Demo",
                  "using a JPlotLayout",
                  "");
    /*
     * Resize the graph  and place in the "Center" of the frame.
     */
    rpl.setSize(new Dimension(600, 400));
    /*
     * Resize the key Pane, both the device size and the physical
     * size. Set the size of the key in physical units and place
     * the key pane at the "South" of the frame.
     */
    rpl.setKeyLayerSizeP(new Dimension2D(6.0, 1.02));
    rpl.setKeyBoundsP(new Rectangle2D.Double(0.01, 1.01, 5.98, 1.0));

    return rpl;
  }

  ColorMap createColorMap(Range2D datar) {
    int[] red =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  7, 23, 39, 55, 71, 87,103,
       119,135,151,167,183,199,215,231,
       247,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,246,228,211,193,175,158,140};
    int[] green =
    {  0,  0,  0,  0,  0,  0,  0,  0,
       0, 11, 27, 43, 59, 75, 91,107,
       123,139,155,171,187,203,219,235,
       251,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0};
    int[] blue =
    {  0,143,159,175,191,207,223,239,
       255,255,255,255,255,255,255,255,
       255,255,255,255,255,255,255,255,
       255,247,231,215,199,183,167,151,
       135,119,103, 87, 71, 55, 39, 23,
       7,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0,
       0,  0,  0,  0,  0,  0,  0,  0};

    IndexedColorMap cmap = new IndexedColorMap(red, green, blue);
    cmap.setTransform(new LinearTransform(0.0, (double)red.length,
					  datar.start, datar.end));
    return cmap;
  }
    
  class MyAction implements java.awt.event.ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent event) {
           Object obj = event.getSource();
           if(obj == edit_) 
             edit_actionPerformed(event);
	   if(obj == space_) {
	     System.out.println("  <<Mark>>");
	   }
	   if(obj == tree_)
	       tree_actionPerformed(event);
        }
    }
}



