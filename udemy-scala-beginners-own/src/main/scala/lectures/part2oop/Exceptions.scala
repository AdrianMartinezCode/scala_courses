package lectures.part2oop

object Exceptions extends App {

    val x: String = null

//    val aWeirdValue: String = throw new NullPointerException

    // throwable classes extend the Throwable class
    // Exception and Error are the major Throwable subtypes


    def getInt(withExceptions: Boolean) : Int =
        if (withExceptions) throw new RuntimeException("No int for you!")
        else 42

    val potentialFail = try {
        getInt(true)
    } catch {
        case e: RuntimeException => 43
    } finally {
        println("finally")
    }

    println(potentialFail)
}
