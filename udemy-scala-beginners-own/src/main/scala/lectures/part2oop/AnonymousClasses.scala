package lectures.part2oop

import exercices.MyList

object AnonymousClasses extends App {
    abstract class Animal {
        def eat: Unit
    }

    class AnonymousClasses$$anon$1 extends Animal {
        override def eat: Unit = println("hahahahahaha")
    }
    // anonymous class
    val funnyAnimal: Animal = new Animal {
        override def eat: Unit = println("hahahahahaha")
    }

    println(funnyAnimal.getClass)

    val funnyAnimal2 = new AnonymousClasses$$anon$1

    println(funnyAnimal2.getClass)

    class Person(name: String) {
        def sayHi: Unit = println(s"Hi, my name is $name, how can I help?")
    }

    val jim = new Person("Jim") {
        override def sayHi: Unit = println(s"Hi, my name is Jim, how can i be of service?")
    }

    

}
