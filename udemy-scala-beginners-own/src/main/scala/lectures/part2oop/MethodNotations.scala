package lectures.part2oop
import scala.language.postfixOps

object MethodNotations extends App {

    class Person(val name: String, favoriteMovie: String, val age: Int = 0) {
        def likes(movie: String): Boolean = movie == favoriteMovie
        def hangOutWith(person: Person): String = s"${this.name} is hanging out with ${person.name}"
        def +(person: Person): String = hangOutWith(person)
        def +(nickname: String) = new Person(s"$name ($nickname)", favoriteMovie)
        def unary_+ = Person(name, favoriteMovie, age + 1)
        def unary_! : String = s"$name, what the heck?!"
        def isAlive: Boolean = true
        def apply(): String = s"Hi, my name is $name and I like $favoriteMovie"

        def learns(thing: String) = s"$name learns $thing"
        def learnsScala = this learns "Scala"
        def apply(times: Int) = s"$name watched $favoriteMovie $times times"
    }

    val mary = new Person("Mary", "Inception")
    println(mary.likes("Inception"))
    println(mary likes "Inception")
    // infix notation = operator notation

    // "operators" in Scala
    val tom = new Person("Tom", "Fight Club")
    println(mary hangOutWith tom)
    println(mary + tom)
    println(mary.+(tom))

    // ALL OPERATORS ARE METHODS
    // Akka actors have ! ?

    // prefix notation
    val x = -1
    val y = 1.unary_-
    // unary_ prefix only works with - + ~ !
    println(!mary)
    println(mary.unary_!)

    // postfix notation

    println(mary.isAlive)
//    println(mary isAlive)

    // apply
    println(mary.apply())
    println(mary())

    /*
        1. Overload the + operator
            mary + "the rockstar" => new person "Mary (the rockstar)"

        2. Add an age to the Person class
            Add a unary + operator => new person with tge age + 1
            +mary => mary with the age incrementer

        3. Add a "learns" method in the Person class => "Mary learns Scala"
            Add a learnsScala method, calls learns method with "Scala"
            Use it in postfix notation.

        4. Overload the apply method
            mary.apply(2) => "Mary watched Inception 2 times"
    */
//    class Person2(name: String, favoriteMovie: String, age: Int) {
//        def +(str: String) = Person2(s"$name ($str)", favoriteMovie, age)
//        def unary_+ = Person2(name, favoriteMovie, age + 1)
//        def learns(anything: String) = s"$name learns $anything"
//        def learnsScala = this learns "Scala"
//        def apply(times: Int) = s"$name watched $favoriteMovie $times times"
//    }

    println((mary + "The Rockstar")())
    println((+mary).age)
    println(mary learnsScala)
    println(mary(10))
}
