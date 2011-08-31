/************************************* 

  Author: Francisco Valdez de la Fuente
          pacofvf@gmail.com
           
  License: Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported
  
  http://creativecommons.org/licenses/by-nc-sa/3.0/
  
  
  You are free:

to Share — to copy, distribute and transmit the work
to Remix — to adapt the work
Under the following conditions:

Attribution — You must attribute the work in the manner specified by the author or licensor (but not in any way that suggests that they endorse you or your use of the work).
Noncommercial — You may not use this work for commercial purposes.
Share Alike — If you alter, transform, or build upon this work, you may distribute the resulting work only under the same or similar license to this one.
With the understanding that:

Waiver — Any of the above conditions can be waived if you get permission from the copyright holder.
Public Domain — Where the work or any of its elements is in the public domain under applicable law, that status is in no way affected by the license.
Other Rights — In no way are any of the following rights affected by the license:
Your fair dealing or fair use rights, or other applicable copyright exceptions and limitations;
The author's moral rights;
Rights other persons may have either in the work itself or in how the work is used, such as publicity or privacy rights.
Notice — For any reuse or distribution, you must make clear to others the license terms of this work. The best way to do this is with a link to this web page.


  (see LICENSE.txt)
********************************************/

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;
 
class JImageMap extends JComponent 
{
    public static String _filename;
    BufferedImage[] images;
    Area[] shapes;
    int n ;
    
    class MyMouse extends MouseMotionAdapter
    {
        // display tool tip
        public void mouseMoved( MouseEvent e ) {
          int i = getInside(e.getPoint());
          if( i != -1)
            setToolTipText( "(" + e.getX() + "," + e.getY() + "): " + i ); 
        }
    }
 
    JImageMap( String[] imageFilenames )
    {

        n = imageFilenames.length;
        images = new BufferedImage[n];
        shapes = new Area[n];
        int maxWidth=0,maxHeight=0;
        for (int i = 0 ; i < n ; i++){
          BufferedImage img = load(imageFilenames[i]);
          Area shape = getShape(img, new Point());
          if(img.getWidth() > maxWidth)
            maxWidth = img.getWidth();
          if(img.getHeight() > maxHeight)
            maxHeight = img.getHeight();
          images[i]=img;
          shapes[i]=shape;
        }

        
        this.setPreferredSize( new Dimension( maxWidth , maxHeight ));
 
        this.addMouseMotionListener( new MyMouse() );
    }
 
    public int getInside( Point p ){
      boolean opaque = false;
      for(int i = 0; i<n ; i++){
        if ( shapes[i].contains( p ) ){
          if(getOpaque(p)){
           /* for(int j = i+1 ;j<n;j++){
              if ( shapes[j].contains( p ) )
                return j;
            }*/
            return i;
          }
        }
      } 
      return -1;
    }
 
    public boolean getOpaque( Point p ){
      for(int i = 0; i<n ; i++){
        if (!(images[i].getRaster().getSample( (int)p.getX(), (int)p.getY(), 3 ) <= 0 ))
        {
            return true;
        }
      }
      return false;
    }
 
    @Override
    protected void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        // draw the image
        Graphics2D g2d = (Graphics2D)g;
        for(int i=0;i<images.length;i++)
          g2d.drawImage( images[i], null, 0, 0 );
        /*
        // draw the shape of the image 
        g2d.setColor( Color.BLACK );// stroke Black
        for(int i=0;i<shapes.length;i++)
          g2d.draw( shapes[i] );// draw the outline of the shape
        */
    }
    
	public static BufferedImage load( String filename )
    {
        BufferedImage tempImage = null;
        BufferedImage argbImage; 
 
        try 
        {
            File tempFile = new File( filename );
            if ( tempFile.exists() )
            {
                tempImage = ImageIO.read( tempFile );
            } else
            {
                System.out.println( "file does not exist." );
            }
        } catch ( Exception e )
        {
            e.printStackTrace();
        }
    
        argbImage = new BufferedImage( 
                tempImage.getWidth(), 
                tempImage.getHeight(), 
                BufferedImage.TYPE_INT_ARGB 
        );
 
        Graphics2D g2d = (Graphics2D)argbImage.createGraphics();
        g2d.drawImage( tempImage, null, 0, 0 );
        g2d.dispose();
        
        return argbImage;
    }
 
    public static Area getShape( BufferedImage img, Point2D pos )
    {
        Raster data = img.getData();
        Area area = new Area();
        int limit = 0;
        int alpha = 0;
        Point2D start = null;
        Point2D end = null;
        boolean first = true;
        Rectangle2D b;
        Vector<Line2D> lines = new Vector<Line2D>();
 
        for ( int y = 0; y < data.getHeight(); y++ )
        {
            for ( int x = 0; x < data.getWidth()-1; x++ )
            {
                alpha = data.getSample( x, y, 3 );
                if( alpha <= limit )
                {
                    if( start == null )
                    {
                        start = new Point2D.Double( x, y );
                    }
                    else if ( x == data.getWidth() )
                    {
                        end = new Point2D.Double( x+1, y );
                        lines.add( new Line2D.Double( start, end ) );
                        start = null;
                        end = null;
                    }
                }
                else
                {
                    if( x > 0 )
                    {
                        alpha = data.getSample( x-1, y, 3 );
                        if ( start != null && alpha <= limit )
                        {
                            end = new Point2D.Double( x-1, y );
                            if(first)
                              first = false;
                            else
                              lines.add( new Line2D.Double( start, end ) );
                            start = null;
                            end = null;
                        }
                    }
                }
            }
            for ( int i = 0; i < lines.size(); i++ )
            {
                b = ( (Line2D)lines.get( i ) ).getBounds2D();
                // even though it says add, we're really subtracting the 
                // line from the area, leaving left the other material
                area.add( new Area( b ) );
            }
            lines.clear();
        }
 
        return area.createTransformedArea( AffineTransform.getTranslateInstance( pos.getX(), pos.getY() ) );
    }
 
    public static void main( String[] args )
    {
        // i guess this gets rid of flickering
        try {
            System.setProperty("sun.java2d.noddraw", "true");
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch(Exception e) {
        }
        
        JFrame jf = new JFrame( "JImageMap example" );
        JImageMap imagePanel;
        
        String[] filenames = {"r10.png","r1.png","r3.png","r9.png","r15.png","r13.png","r6.png","r7.png","r12.png","r11.png","r14.png","r8.png","r4.png","r5.png","r16.png","r2.png"};
        imagePanel = new JImageMap(filenames);
        jf.add( imagePanel );
        
        
    
            
        ToolTipManager.sharedInstance().setInitialDelay( 0 );
        ToolTipManager.sharedInstance().setReshowDelay( 10 );
        ToolTipManager.sharedInstance().setDismissDelay( 300 );
 
        // exit on close and layout stuff
        jf.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );
        jf.pack();
        jf.setLocation( 100, 100 );
        jf.setVisible( true );
    }
}
