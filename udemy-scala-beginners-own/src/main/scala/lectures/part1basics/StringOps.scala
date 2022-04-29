package lectures.part1basics

object StringOps extends App {

    val str: String = "Hello, I am learning Scala"

    println(str.charAt(2))

    println(str.substring(7, 11))

    val aNumberString = "45"
    val aNumber = aNumberString.toInt
    println('a' +: aNumberString :+ 'z' :+ 'a')

    println(raw"This is a")
}
