package exercices
import scala.language.postfixOps

abstract class MyList[+A] {

  /*
        head = first element of the list
        tail = remainder of the list
        isEmpty = is this list empty
        add(int) => new list with this element added
        toString => a string representation of the list

    */

    def head: A
    def tail: MyList[A]
    def isEmpty: Boolean
    def add[B >: A](elem: B): MyList[B]
    def printElements: String

    def map[B](transformer: A => B): MyList[B]
    def filter(filter: A => Boolean): MyList[A]
    def flatMap[B](transformer: A => MyList[B]): MyList[B]
    def ++[B >: A](l: MyList[B]) : MyList[B]

    def foreach(f: A => Unit) : Unit
    def sort(f: (A, A) => Int) : MyList[A]
    def zipWith[B, C](list: MyList[B], zip:(A, B) => C): MyList[C]
    def fold[B](start: B, f: (B, A) => B): B


    override def toString: String = s"[$printElements]"
}

case object Empty extends MyList[Nothing] {
    override def head: Nothing = throw new NoSuchElementException
    override def tail: MyList[Nothing] = throw new NoSuchElementException
    override def isEmpty: Boolean = true
    override def add[B >: Nothing](elem: B): MyList[B] = new Cons(elem, Empty)

    override def map[B](transformer: Nothing => B): MyList[B] = Empty
    override def filter(filter: Nothing => Boolean): MyList[Nothing] = Empty
    override def flatMap[B](transformer: Nothing => MyList[B]): MyList[B] = Empty

    override def ++[B >: Nothing](l: MyList[B]) : MyList[B] = l

    override def foreach(f: Nothing => Unit): Unit = ()
    override def sort(f: (Nothing, Nothing) => Int): MyList[Nothing] = Empty

    override def zipWith[B, C](list: MyList[B], zip: (Nothing, B) => C): MyList[C] = Empty

    override def fold[B](start: B, f: (B, Nothing) => B): B = start


    override def printElements: String = ""
}

case class Cons[+A](h: A, t: MyList[A]) extends MyList[A] {
    override def head: A = h
    override def tail : MyList[A] = t
    override def isEmpty: Boolean = false
    override def add[B >: A](elem: B) = new Cons(elem, this)

    override def map[B](transformer: A => B): MyList[B] =
        new Cons[B](transformer(h), t.map(transformer))
    override def filter(filter: A => Boolean): MyList[A] =
        if (filter(h)) new Cons(h, t.filter(filter))
        else t.filter(filter)
    override def flatMap[B](transformer: A => MyList[B]): MyList[B] =
        transformer(h) ++ t.flatMap(transformer)

    override def ++[B >: A](l: MyList[B]): MyList[B] = new Cons(h, t ++ l)

    override def foreach(f: A => Unit): Unit = {
        f(h)
        t.foreach(f)
    }

    /*

    */
    override def sort(compare: (A, A) => Int): MyList[A] = {
        def insert(x: A, sortedList: MyList[A]): MyList[A] =
            if (sortedList.isEmpty) new Cons(x, Empty)
            else if (compare(x, sortedList.head) <= 0) new Cons(x, sortedList)
            else new Cons(sortedList.head, insert(x, sortedList.tail))

        val sortedTail = t.sort(compare)
        insert(h, sortedTail)
    }

    override def zipWith[B, C](list: MyList[B], zip:(A, B) => C): MyList[C] =
        if (list.isEmpty) Empty
        else new Cons(zip(h, list.head), t.zipWith(list.tail, zip))



    override def fold[B](start: B, f: (B, A) => B): B =
        t.fold(f(start, h), f)

    override def printElements: String =
        h.toString + " " + t.printElements
}

/*
        1. Generic trait MyPredicate[-T] with a little method test(T) => Boolean
        2. Generic trait MyTransformer[-A, B] with a method transform(A) => B
        3. MyList:
            - map(transformer) => MyList
            - filter(predicate) => MyList
            - flatMap(transformer from A to MyList[B]) => MyList[B]

            class EvenPredicate extends MyPredicate[Int]
            class StringToIntTransformer extends MyTransformer[String, Int]

            [1,2,3].map(n * 2) = [2,4,6]
            [1,2,3,4].filter(n % 2) = [2,4]
            [1,2,3].flatMap(n => [n, n+1]) => [1,2,2,3,3,4]
    */

object ListTest extends App {
    val list = new Cons(1, new Cons(2, new Cons(3, Empty)))
    println(list.tail.head)
    println(list.add(4).head)
    println(list.isEmpty)

    // polymorphic call
    println(list.toString)

    val listOfIntegers: MyList[Int] = new Cons(1, new Cons(2, new Cons(3, new Cons(4, Empty))))
    val cloneListOfIntegers: MyList[Int] = new Cons(1, new Cons(2, new Cons(3, Empty)))
    val listOfStrings: MyList[String] = new Cons("Hello", new Cons("Scala", Empty))

    println(listOfIntegers)
    println(listOfStrings)


    val listOfCoses1 = new Cons(1, new Cons(2, new Cons(3, Empty)))

    println(listOfIntegers.map(n => n * 2))

//    val newList = listOfIntegers.flatMap(new MyTransformer[Int, MyList[Int]] {
//        override def transform(e: Int): MyList[Int] = new Cons(e, new Cons(e + 1, Empty))
//    })
    val newList = listOfIntegers.flatMap((e: Int) => new Cons(e, new Cons(e + 1, Empty)))

    println(newList)

    println(cloneListOfIntegers == listOfIntegers)




    println(listOfIntegers.sort((x, y) => y - x))

    println(listOfIntegers.zipWith(listOfStrings, (i, s) => i.toString + s))
    //[1,2,3].fold(0)(x + y) = 6

    println(cloneListOfIntegers.fold(0, _ + _))


    val combinations = for {
        n <- listOfIntegers
        string <- listOfStrings
    } yield n + "-" + string

    println(combinations)
}

