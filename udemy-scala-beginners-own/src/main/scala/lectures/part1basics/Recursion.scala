package lectures.part1basics

import scala.annotation.tailrec

object Recursion extends App {



    def anoherFactorial(n: Int): Int = {
        def factHelper(x: Int, accumulator: Int) : Int =
            if (x <= 1) accumulator
            else factHelper(x - 1, x * accumulator)

        factHelper(n, 1)
    }


    @tailrec
    def concatenateNTimes(n: Int, str: String, accumulator: String) : String =
        if (n <= 0) accumulator
        else concatenateNTimes(n - 1, str, str + accumulator)

    println(concatenateNTimes(5, "buenas", ""))

    def isPrime(n: Int): Boolean = {
        @tailrec
        def isPrimeTailrec(t: Int, isStillPrime: Boolean): Boolean =
            if (!isStillPrime) false
            else if (t <= 1) true
            else isPrimeTailrec(t - 1, n % t != 0)

        isPrimeTailrec(n / 2, true)
    }

    println(isPrime(2003))
    println(isPrime(639))

    def fibonacci(n: Int): Int = {
        @tailrec
        def fibTailRec(i: Int, last: Int, nextToLast: Int): Int =
            if (i >= n) last
            else fibTailRec(i + 1, last + nextToLast, last)

        if (n <= 2) 1
        else fibTailRec(3, 1, 1)
    }

    println(fibonacci(5))
}
