package lectures.part3fp

object TuplesAndMaps extends App {

    // tuples = finite ordered "lists"
    val aTuple = (2, "hello, scalla") // Tuple2[Int, String] = (Int, String)

    println(aTuple._1)
    println(aTuple.copy(_2 = "good bye java"))
    println(aTuple.swap) // ("hello, Scala", 2)

    // Maps - keys -> values
    val aMap: Map[String, Int] = Map()

    val phonebook = Map(("Jim", 555), "Daniel" -> 789).withDefaultValue(-1)

    println(phonebook)

    // map ops
    println(phonebook.contains("Jim"))
    println(phonebook("Jim"))
    println(phonebook("Antonio"))

    // add a pairing
    val newPairing = "Mary" -> 678
    val newPhonebook = phonebook + newPairing
    println(newPhonebook)

    // functionals on maps
    // map, flatmap, filter
    println(phonebook.map(pair => pair._1.toLowerCase -> pair._2))

    // filterKeys
    println(phonebook.view.filterKeys(_.startsWith("J")).toMap)
    // mapValues
    println(phonebook.view.mapValues(_ * 10).toMap)

    // conversions to other collectiuons
    println(phonebook.toList)
    println(List("Daniel" -> 555).toMap)
    val names = List("Bob", "James", "Angela", "Mary", "Daniel", "Jim")
    println(names.groupBy(_.charAt(0)))


    /*
        1. What would happen if I had two original entries "Jim" -> 555 and "JIM" -> 900
        2. Overly simplified social network based on maps
            Person = String
            - add a person to the network
            - remove
            - friend (mutual)
            - unfriend

            - number of friends of a person
            - person with most friends
            - how many people have NO friends
            - if there is a social connection between two people (direct or not)
    */




    class SocialNetwork(
       val network: Map[String, Set[String]] = Map()
    ) {
        def add(person: String) = SocialNetwork(network + (person -> Set()))
        def remove(person: String) = SocialNetwork((network - person).map(pair => (pair._1, pair._2.filter(p => p != person))))
        def friend(personOne: String, personTwo: String): SocialNetwork = {
            val friendsA = network(personOne)
            val friendsB = network(personTwo)

            return SocialNetwork(
                network + (personOne -> (friendsA + personTwo)) + (personTwo -> (friendsB + personOne))
            )
        }
        def unfriend(personOne: String, personTwo: String): SocialNetwork = {
            val friendsA = network(personOne)
            val friendsB = network(personTwo)

            return SocialNetwork(
                network + (personOne -> (friendsA - personTwo)) + (personTwo -> (friendsB - personOne))
            )
        }
        def numberOfFriends(person: String): Int = network.find(p => person == p._1).fold(0)(p => p._2.size)
        def personWithMostFriends(): String = network.maxBy(_._2.size)._1
        def peopleThatHaveNoFriends(): Int = network.filter(_._2.size == 0).size
        def existsSocialConnection(p1: String, p2: String): Boolean =
            network.find(_._1 == p1).fold(false)(_._2.exists(_ == p2)) ||
            network.find(_._1 == p2).fold(false)(_._2.exists(_ == p1))
    }
}
