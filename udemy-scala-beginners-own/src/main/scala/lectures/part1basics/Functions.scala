package lectures.part1basics

object Functions extends App {

    def aFunction(a: String, b: Int) : String =
        a + " " + b

    def aFunctionB(a: String, b: Int) : String = {
        a + " " + b
    }

    println(aFunction("hello", 3))

    def aParameterlessFunction(): Int = 42
    println(aParameterlessFunction())
//    println(aParameterlessFunction)

    def aRepeatedFunction(aString: String, n: Int): String = {
        if (n == 1) aString
        else aString + aRepeatedFunction(aString, n-1)
    }

    println(aRepeatedFunction("hello", 25))

    // WHEN YOU NEED LOOPS, USE RECURSION

    def aFunctionWithSideEffects(aString: String): Unit = println(aString)

    def aBigFunction(n: Int) : Int = {
        def sSmallerFUnction(a: Int, b: Int) : Int = a + b

        sSmallerFUnction(n, n-1)
    }

    /*
        1. A greeting function (name, age) => "Hi, my name is $name and I am $age years old"
        2. Factorial function 1 * 2 * 3 ... *
        3. A Fibonacci function
            f(1) = 1
            f(2) = 1
            f(n) = f(n - 1) + f(n - 2)

    */

    def greetingFunction(name: String, age: Int) = s"Hi, my name is $name and I am $age years old"
    def factorialFunction(n: Int) : Int =
        if (n <= 0) 1
        else n * factorialFunction(n - 1)

    def fibonacciFunction(n: Int) : Int =
        if (n <= 2) 1
        else fibonacciFunction(n - 1) + fibonacciFunction(n - 2)

    def primeTestFunction(n: Int) : Boolean = {

        def isPrimeUntil(t: Int) : Boolean =
            if (t <= 1) true
            else n % t != 0 && isPrimeUntil(t - 1)

        isPrimeUntil(n / 2)
    }
}
