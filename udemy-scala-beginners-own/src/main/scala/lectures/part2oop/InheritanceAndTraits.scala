package lectures.part2oop

object InheritanceAndTraits extends App {

    class Animal {
        val creatureType = "wild"
         def eat = println("nomnom")
    }

    class Cat extends Animal {

        def crunch = {
            eat
            println("crunch crunch")
        }
    }

    val cat = new Cat

    class Person(name: String, age: Int) {
        def this(name: String) = this(name, 0)
    }
    class Adult(name: String, age: Int, idCard: String) extends Person(name, age)

    class Dog(override val creatureType: String) extends Animal {
        override def eat = println("crunch, crunch")
    }

    // type substitution
    val unkownAnimal: Animal = new Dog("K9")
    unkownAnimal.eat

    
}
