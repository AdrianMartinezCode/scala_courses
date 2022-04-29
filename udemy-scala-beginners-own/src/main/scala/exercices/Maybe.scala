package exercices

abstract class Maybe[+T] {
    def map[A](f: T => A): Maybe[A]
    def flatMap[A](f: T => Maybe[A]): Maybe[A]
    def filter(f: T => Boolean): Maybe[T]
}

case object None extends Maybe[Nothing] {

    override def map[A](f: Nothing => A): Maybe[A] = None
    override def flatMap[A](f: Nothing => Maybe[A]): Maybe[A] = None
    override def filter(f: Nothing => Boolean): Maybe[Nothing] = None
}

case class Some[T](v: T) extends Maybe[T] {

    override def map[A](f: T => A): Maybe[A] = Some(f(v))
    override def flatMap[A](f: T => Maybe[A]): Maybe[A] = f(v)
    override def filter(f: T => Boolean): Maybe[T] =
        if (!f(v)) Some(v)
        else None
}

object MaybeTest extends App {
    
}