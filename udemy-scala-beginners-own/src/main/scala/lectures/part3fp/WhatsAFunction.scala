package lectures.part3fp

object WhatsAFunction extends App {

    val double = new MyFunction[Int, Int] {
        override def apply(element: Int): Int = element * 2
    }

    println(double(2))

    // function types = Function1[A, B]
    val stringToIntConverter = new Function1[String, Int] {
        override def apply(v1: String): Int = v1.toInt
    }

    println(stringToIntConverter("3") + 4)

    val adder: ((Int, Int) => Int) = new Function2[Int, Int, Int] {
        override def apply(v1: Int, v2: Int): Int = v1 + v2
    }

    // Function types Function2[A, B, R] === (A,B) => R

    // ALL SCALA FUNCTIONS ARE OBJECTS

    /*
        1. a function which takes 2 strings and concatenates them
        2. transform the MyPredicate and MyTransformer into function types
        3. define a function which takes an int and returns another function which takes an int and returns an int
            - what's the type of this function
            - how to do it
    */

    def concatenator: (String, String) => String = (v1: String, v2: String) => v1 ++ v2

    val concatenateString = (v1: String, v2: String) => v1 ++ v2

    val specialFunction = (x: Int) => (y: Int) => x + y

    val adder3 = specialFunction(3)

//    val myPredicate = [-T](e: T) =>
//    trait MyPredicate[-T] {
//        def test(e: T): Boolean
//    }
//    trait MyTransformer[-A, B] {
//        def transform(e: A) : B
//    }
}

trait MyFunction[A, B] {
    def apply(element: A): B
}
