package lectures.part2oop

import java.util.{Calendar, Date}

object OOBasics extends App {
    val person = new Person("John", 26)
    println(person.age)
    person.greet("Daniel")
    person.greet()

    val author = new Writer("Charles", "Dickens", 1812)
    val novel = new Novel("Great Expectations", 1861, author)
    println(novel.authorAge)
    println(novel.isWrittenBy(author))

}

// constructor
class Person(name: String, val age: Int) {
    // body
    val x = 2

    println(1 + 3)

    def greet(name: String): Unit = println(s"${this.name} says: Hi $name")

    def greet(): Unit = println(s"Hi, I am $name")

    // multiple constructors

    def this(name: String) = this(name, 0)
    def this() = this("John Doe")

    /*
    Novel and a Writer

    Writer: first name, surname, year
        - method fullname

    Novel: name, year of release, author
        - authorAge
        - isWrittenBy(author)
        - copy (new year of release) = new instance of Novel
    */

    /*
    Counter class
        - receives an int value
        - method current count
        - method to increment/decrement => new Counter
        - overload inc/dec to receive an amount
    */
}

class Writer(
    val firstName: String,
    val surname: String,
    val year: Int
) {
    def fullname(): String = firstName + " " + surname
}

class Novel(
    val name: String,
    val yearRelease: Int,
    var author: Writer
) {
    def authorAge: Int = Calendar.getInstance().get(Calendar.YEAR) - author.year
    def isWrittenBy(author: Writer) = this.author = author
    def copy(newYearRelease: Int) = new Novel(name, newYearRelease, author)
}
class Counter(val value: Int) {
    def currentCount = value
    def increment = new Counter(value + 1)
    def decrement = new Counter(value - 1)
    def increment(inc: Int) = new Counter(value + inc)
    def decrement(dec: Int) = new Counter(value - dec)
}

// class parameters are NOT FIELDS

