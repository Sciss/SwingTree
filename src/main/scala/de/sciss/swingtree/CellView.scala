/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2007-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

package de.sciss.swingtree
import swing.{Publisher, Component}
import collection.mutable

/**
* Describes components that have a concept of a "cell", each of which contains a value, may be selected, 
 * and may support pluggable Renderers and Editors.
*/
trait CellView[+A] {
  _: Component =>
    
  def editable: Boolean 
  def cellValues: Iterator[A]
  
  /**
  * Provides common functionality for the `selection` object found in CellView implementation.  Each 
  * will have one or more selection sets based on different types of cell coordinate, such as row, 
  * column, index or tree path.  All events published from `selection` objects will derive from 
  * scala.swing.event.SelectionEvent.
  */
  trait CellSelection extends Publisher {
    /**
    * Allows querying and modification of the current selection state, for some unique coordinate S.
    * There may be more than one selection set supporting different coordinates, such as rows and columns.
    */
    protected abstract class SelectionSet[S](a: => Seq[S]) extends mutable.Set[S] { 
      def -=(s: S): this.type 
      def +=(s: S): this.type
      def --=(nn: Seq[S]): this.type = { nn foreach -=; this }
      def ++=(nn: Seq[S]): this.type = { nn foreach +=; this }
      override def size = nonNullOrEmpty(a).length
      def contains(s: S) = nonNullOrEmpty(a) contains s
      def iterator = nonNullOrEmpty(a).iterator
      protected def nonNullOrEmpty[A1](s: Seq[A1]) = if (s != null) s else Seq.empty
    }
    
    /**
    *  Returns an iterator that traverses the currently selected cell values.
    */
    def cellValues: Iterator[A]
    
    /**
    * Whether or not the current selection is empty.
    */
    def isEmpty: Boolean
    
    /**
    * Returns the number of cells currently selected.
    */
    def size: Int
  } 
  
  def selection: CellSelection
}

/**
* This should be mixed in to CellView implementations that support pluggable renderers.
*/
trait RenderableCells[A] {
  _: CellView[A] =>
  val companion: RenderableCellsCompanion
  def renderer: companion.Renderer[A]
  def renderer_=(r: companion.Renderer[A]): Unit
}

/**
* This should be mixed in to CellView implementations that support pluggable editors.
*/
trait EditableCells[A]  {
  _: CellView[A] =>
  val companion: EditableCellsCompanion
  def editor: companion.Editor[A]
  def editor_=(r: companion.Editor[A]): Unit
}