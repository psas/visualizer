
Chapter 7. Walking Around the Models

From:
  Pro Java 6 3D Game Development
  Andrew Davison
  Apress, April 2007
  ISBN: 1590598172
  http://www.apress.com/book/bookDisplay.html?bID=10256
  Web Site for the book: http://fivedots.coe.psu.ac.th/~ad/jg2


Contact Address:
  Dr. Andrew Davison
  Dept. of Computer Engineering
  Prince of Songkla University
  Hat Yai, Songkhla 90112, Thailand
  E-mail: ad@fivedots.coe.psu.ac.th


If you use this code, please mention my name, and include a link
to the book's Web site.

Thanks,
  Andrew


==================================
Files and directories here:

  * ObjView3D.java, WrapObjView3D.java, TexPlane.java
    ModelLoader.java, GroundShape.java, KeyBehavior.java
    CheckerFloor.java, ColouredTiles.java
       // 8 Java files

  * images/	// a directory holding 5 textures for the GroundShapes
      - tree1.gif, tree2.gif, tree3.gif, tree4.gif
        cactus.gif

  * skyBox/    // a directory holding textures used for the background
     - see the readme.txt file in that directory for details

  * Models/    // a directory holding the 3D OBJ models used by ModelLoader
     - see the readme.txt file in that directory for details

==================================
Requirements:

* J2SE 5.0 (or later) from http://java.sun.com/j2se/

* Java 3D 1.4.0 (or later) from https://java3d.dev.java.net/

==================================
Compilation:
  $ javac *.java

Execution:
  $ java ObjView3D
     /* you can move the camera using the arrow keys, optionally
        combined with the <alt> key. */

-----------
Last updated: 3rd March 2007
