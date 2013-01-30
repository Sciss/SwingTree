package de.sciss.swingtree
package test

import java.awt.Dimension
import collection.mutable.ListBuffer
import swing.{Swing, TabbedPane, ScrollPane, GridPanel, Action, Button, BorderPanel, Component, MainFrame, Label, SimpleSwingApplication}
import tree.{InternalTreeModel, ExternalTreeModel, TreeModel, Tree}
import java.io.File
import de.sciss.swingtree.event.TreeNodeSelected
import xml.XML
import util.control.NonFatal
import Swing.pair2Dimension

object TreeDemo extends SimpleSwingApplication {
  import ExampleData._
      
  // Use case 1: Show an XML document
  lazy val xmlTree = new Tree[xml.Node] {
    model = TreeModel(xmlDoc)(_.child filterNot (_.text.trim.isEmpty))
    renderer = Tree.Renderer(n =>
        if (n.label startsWith "#") n.text.trim 
        else n.label)
        
    expandAll()
  }

  
  // Use case 2: Show the filesystem with filter
  lazy val fileSystemTree = new Tree[File] {
    model = TreeModel(new File(".")) {f => 
      if (f.isDirectory) f.listFiles.toSeq 
      else Seq()
    }
    
    renderer = Tree.Renderer.labeled {f =>
      val icon = if (f.isDirectory) folderIcon 
                 else fileIcon
      (icon, f.getName)
    }
    expandRow(0)
  }
  
  
  // Use case 3: Object graph containing diverse elements, reacting to clicks
  lazy val objectGraphTree = new Tree[Any] {
    model = TreeModel[Any](orders: _*) {
      case o @ Order(_, cust, prod, qty) => Seq(cust, prod, "Qty" -> qty, "Cost" -> ("$" + o.price))
      case Product(id, name, price) => Seq("ID" -> id, "Name" -> name, "Price" -> ("$" + price))
      case Customer(id, _, first, last) => Seq("ID" -> id, "First name" -> first, "Last name" -> last)
      case _ => Seq.empty
    } 

    renderer = Tree.Renderer({
      case Order(id, _, _, 1) => "Order #" + id
      case Order(id, _, _, qty) => "Order #" + id + " x " + qty
      case Product(id, _, _) => "Product " + id
      case Customer(_, title, first, last) => title + " " + first + " " + last
      case (field, value) => field + ": " + value
      case x => x.toString
    })

    expandAll()
  }
  
  
  // Use case 4: Infinitely deep structure
  lazy val infiniteTree = new Tree(TreeModel(1000) {n => 1 to n filter (n % _ == 0)}) {
    expandRow(0)
  }

  
  // Use case 5: Mutable external tree model
  val mutableExternalTree = new Tree[PretendFile] {

    model = ExternalTreeModel(pretendFileSystem)(_.children).makeUpdatableWith {
      (pathOfFile, updatedFile) =>       
          val succeeded = pathOfFile.last.rename(updatedFile.name)
          externalTreeStatusBar.text = "Updating file " + (if (succeeded) "succeeded" else "failed")
          pathOfFile.last
          
    }.makeInsertableWith {
      (parentPath, fileToInsert, index) => 
        val parentDir = parentPath.last
        if (parentDir.children contains fileToInsert) false
        else parentDir.insertChild(fileToInsert, index)
      
    }.makeRemovableWith {
      (pathToRemove) => 
        if (pathToRemove.length >= 2) pathToRemove.last.delete()
        else false
    }

    listenTo(selection)
    reactions += {
      case TreeNodeSelected(node) => externalTreeStatusBar.text = "Selected: " + node
    }
    
    renderer = Tree.Renderer.labeled  {f =>
      val icon = if (f.isDirectory) folderIcon 
                 else fileIcon
      (icon, f.name)
    }
    editor = Tree.Editor((_: PretendFile).name, new PretendFile(_: String))
    expandRow(0)
  }

  // Use case 6: Mutable internal tree model
  val mutableInternalTree = new Tree[PretendFile] {

    model = InternalTreeModel(pretendFileSystem)(_.children)

    listenTo(selection)
    reactions += {
      case TreeNodeSelected(node) => internalTreeStatusBar.text = "Selected: " + node
    }
    
    renderer = mutableExternalTree.renderer
    editor = mutableExternalTree.editor
    expandRow(0)
  }
  
  
  class ButtonPanel(pretendFileTree: Tree[PretendFile], setStatus: String => Unit) extends GridPanel(10,1) {
    
    val updateButton = new Button(Action("Directly update") {
      val pathToRename = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToRename) {
        val oldName = path.last.name
        pretendFileTree.model.update(path, PretendFile("directly-updated-file"))
        setStatus("Updated " + oldName)
      }
    })
    
    val editButton = new Button(Action("Edit") {
      val pathToEdit = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToEdit) {
        pretendFileTree.startEditingAtPath(path)
        setStatus("Editing... ")
      }
    })
    
    val insertButton = new Button(Action("Insert under") {
      val pathToInsertUnder = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertUnder) {
        val succeeded = pretendFileTree.model.insertUnder(path, PretendFile("new-under-" + path.last.name), 0)
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val insertBeforeButton = new Button(Action("Insert before") {
      val pathToInsertBefore = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertBefore) {
        val succeeded = pretendFileTree.model.insertBefore(path, PretendFile("new-before-" + path.last.name))
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val insertAfterButton = new Button(Action("Insert after") {
      val pathToInsertAfter = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToInsertAfter) {
        val succeeded = pretendFileTree.model.insertAfter(path, PretendFile("new-after-" + path.last.name))
        setStatus("Inserting " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    val removeButton = new Button(Action("Remove") {
      val pathToRemove = pretendFileTree.selection.paths.leadSelection
      for (path <- pathToRemove) {
        val succeeded = pretendFileTree.model remove path
        setStatus("Remove " + (if (succeeded) "succeeded" else "failed"))
      }
    })
    
    contents += editButton
    contents += updateButton
    contents += insertButton
    contents += insertBeforeButton
    contents += insertAfterButton
    contents += removeButton
  }

  
  val externalTreeStatusBar = new Label {
    preferredSize = (100,12)
  }
  
  val internalTreeStatusBar = new Label {
    preferredSize = (100,12)
  }
  
  // Other setup stuff
  
   
  def top = new MainFrame {
    title = "Scala Swing Tree Demo"
  
    contents = new TabbedPane {
      def southCenterAndEast(north: Component, center: Component, east: Component) = new BorderPanel {
        import BorderPanel.Position._
        layout(north) = South
        layout(center) = Center
        layout(east) = East
      }
      
      pages += new TabbedPane.Page("1: XML file", new ScrollPane(xmlTree))
      pages += new TabbedPane.Page("2: File system", new ScrollPane(fileSystemTree))
      pages += new TabbedPane.Page("3: Diverse object graph", new ScrollPane(objectGraphTree))
      pages += new TabbedPane.Page("4: Infinite structure", new ScrollPane(infiniteTree))
      pages += new TabbedPane.Page("5: Mutable external model", southCenterAndEast(
        externalTreeStatusBar, 
        new ScrollPane(mutableExternalTree),
        new ButtonPanel(mutableExternalTree, externalTreeStatusBar.text_=)))
      
      pages += new TabbedPane.Page("6: Mutable internal model", southCenterAndEast(
        internalTreeStatusBar, 
        new ScrollPane(mutableInternalTree),
        new ButtonPanel(mutableInternalTree, internalTreeStatusBar.text_=)))
    }
    
    size = (1024, 768): Dimension
  }

  object ExampleData {
    
    // File system icons
    def getIconUrl(path: String) = resourceFromClassloader(path) ensuring (_ != null, "Couldn't find icon " + path)
    val fileIcon = Swing.Icon(getIconUrl("images/file.png"))
    val folderIcon = Swing.Icon(getIconUrl("images/folder.png"))
    
    // Contrived class hierarchy
    case class Customer(id: Int, title: String, firstName: String, lastName: String)
    case class Product(id: String, name: String, price: Double)
    case class Order(id: Int, customer: Customer, product: Product, quantity: Int) {
      def price = product.price * quantity
    }

    // Contrived example data
    val bob = Customer(1, "Mr", "Bob", "Baxter")
    val fred = Customer(2, "Dr", "Fred", "Finkelstein")
    val susan = Customer(3, "Ms", "Susan", "Smithers")
    val powerSaw = Product("X-123", "Power Saw", 99.95) 
    val nailGun = Product("Y-456", "Nail gun", 299.95)
    val boxOfNails = Product("Z-789", "Box of nails", 23.50)
    val orders = List(
      Order(1, fred, powerSaw, 1), 
      Order(2, fred, boxOfNails, 3),
      Order(3, bob, boxOfNails, 44),
      Order(4, susan, nailGun, 1))
      
    lazy val xmlDoc: xml.Node = try {XML load resourceFromClassloader("sample.xml")}
                                catch {case NonFatal(_) => <error> Error reading XML file. </error>}
                            
                    
    // Pretend file system, so we can safely add/edit/delete stuff
    case class PretendFile(private var nameVar: String, private val childFiles: PretendFile*) {
      var parent: Option[PretendFile] = None
      childFiles foreach {_.parent = Some(this)}
      private var childBuffer = ListBuffer(childFiles: _*)
      
      override def toString = name
      def name = nameVar
      def rename(str: String): Boolean = if (siblingExists(str)) false 
                                         else { nameVar = str; true }

      def insertChild(child: PretendFile, index: Int): Boolean = {
        if (!isDirectory) false
        else if (childExists(child.name)) false 
        else { 
          child.parent = Some(this)
          childBuffer.insert(index, child)
          true 
        }
      }
      def delete(): Boolean = parent.map(_ removeChild this) getOrElse false
      def removeChild(child: PretendFile): Boolean = if (children contains child) {childBuffer -= child; true}
                                                     else false
                                                     
      def siblingExists(siblingName: String) = parent.map(_ childExists siblingName) getOrElse false
      def childExists(childName: String) = children.exists(_.name == childName)
      def children: Seq[PretendFile] = childBuffer
      def isDirectory = children.nonEmpty
    }
    
    val pretendFileSystem = PretendFile("~", 
        PretendFile("lib", 
            PretendFile("coolstuff-1.1.jar"),
            PretendFile("coolstuff-1.2.jar"),
            PretendFile("robots-0.2.5.jar")), 
        PretendFile("bin", 
            PretendFile("cleanup"), 
            PretendFile("morestuff"), 
            PretendFile("dostuff")), 
        PretendFile("tmp", 
            PretendFile("log", 
                PretendFile("1.log"),
                PretendFile("2.log"),
                PretendFile("3.log"),
                PretendFile("4.log")), 
            PretendFile("readme.txt"), 
            PretendFile("foo.bar"), 
            PretendFile("bar.foo"), 
            PretendFile("dingus")), 
        PretendFile("something.moo"))
  }
}
