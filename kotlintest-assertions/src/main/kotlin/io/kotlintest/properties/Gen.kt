package io.kotlintest.properties

import io.kotlintest.JavaRandoms
import java.io.File
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.math.BigInteger
import java.util.*

/** A shared random number generator. */
private val RANDOM = Random()

class BigIntegerGen(maxNumBits: Int) : Gen<BigInteger> {

  private val numBitsGen: Gen<Int> = Gen.choose(0, maxNumBits)

  override fun always(): Iterable<BigInteger> = emptyList()
  override fun random(): Sequence<BigInteger> =
      numBitsGen.random().map { it.toBigInteger() }
}

/**
 * A Generator, or [Gen] is responsible for generating data
 * to be used in property testing. Each generator will
 * generate data for a specific type <T>.
 *
 * The idea behind property testing is the testing framework
 * will automatically test a range of different values,
 * including edge cases and random values.
 *
 * There are two types of values to consider.
 *
 * The first are values that should always be included - those
 * edge cases values which are common sources of bugs. For
 * example, a generator for [Int]s should always include
 * values like zero, minus 1, positive 1, Integer.MAX_VALUE
 * and Integer.MIN_VALUE.
 *
 * Another example would be for a generator for enums. That
 * should include _all_ the values of the enum to ensure
 * each value is tested.
 *
 * The second set of values are random values, which are
 * used to give us a greater breadth of values tested.
 * The [Int] generator example should return a random int
 * from across the entire integer range.
 */
interface Gen<out T> {

  /**
   * Returns the values that should always be used
   * if this generator is to give complete coverage.
   */
  fun always(): Iterable<T>

  /**
   * Generate a random sequence of type T, that is compatible
   * with the constraints of this generator.
   */
  fun random(): Sequence<T>

  companion object {

    fun bigInteger(maxNumBits: Int = 32): Gen<BigInteger> = BigIntegerGen(maxNumBits)

    /**
     * Returns a stream of values, where each value is
     * random Int between the given min and max.
     */
    fun choose(min: Int, max: Int): Gen<Int> {
      assert(min < max, { "min must be < max" })
      return object : Gen<Int> {
        override fun always(): Iterable<Int> = emptyList()
        override fun random(): Sequence<Int> =
            generateSequence { JavaRandoms.internalNextInt(RANDOM, min, max) }
      }
    }

    /**
     * Returns a stream of values, where each value is a
     * Long between the given min and max.
     */
    fun choose(min: Long, max: Long): Gen<Long> {
      assert(min < max, { "min must be < max" })
      return object : Gen<Long> {
        override fun always(): Iterable<Long> = emptyList()
        override fun random(): Sequence<Long> = generateSequence { JavaRandoms.internalNextLong(RANDOM, min, max) }
      }
    }

    /**
     * Adapts a list into a generator, where random
     * values will be picked. May not choose every
     * item in the list.
     */
    fun <T : Any> from(values: List<T>): Gen<T> = object : Gen<T> {
      override fun always(): Iterable<T> = emptyList()
      override fun random(): Sequence<T> = generateSequence { values[JavaRandoms.internalNextInt(RANDOM, 0, values.size)] }
    }

    inline fun <reified T : Enum<T>> enum(): Gen<T> = object : Gen<T> {
      val values = T::class.java.enumConstants.toList()
      override fun always(): Iterable<T> = values
      override fun random(): Sequence<T> = from(values).random()
    }

    /**
     * Returns a stream of values where each value is a random
     * printed string. In addition the empty string, a multi line string
     * and a UTF8 string is always included.
     */
    fun string(): Gen<String> = object : Gen<String> {
      val literals = listOf("", """\nabc\n123\n""", "\u006c\u0069b/\u0062\u002f\u006d\u0069nd/m\u0061x\u002e\u0070h\u0070")
      override fun always(): Iterable<String> = literals
      override fun random(): Sequence<String> = generateSequence { nextPrintableString(RANDOM.nextInt(100)) }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen [Int]. The values always returned include
     * the following edge cases: [-1, 0, 1, Int.MIN_VALUE, Int.MAX_VALUE]
     */
    fun int() = object : Gen<Int> {
      val literals = listOf(-1, 0, 1, Int.MIN_VALUE, Int.MAX_VALUE)
      override fun always(): Iterable<Int> = literals
      override fun random(): Sequence<Int> = generateSequence { RANDOM.nextInt() }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen positive value. The values returned always include
     * the following edge cases: [0, 1, Int.MAX_VALUE]
     */
    fun positiveIntegers(): Gen<Int> = object : Gen<Int> {
      val literals = listOf(0, 1, Int.MAX_VALUE)
      override fun always(): Iterable<Int> = literals
      override fun random(): Sequence<Int> = generateSequence { Math.abs(RANDOM.nextInt()) }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen natural number. The values returned always include
     * the following edge cases: [1, Int.MAX_VALUE]
     */
    fun nats(): Gen<Int> = object : Gen<Int> {
      val literals = listOf(1, Int.MAX_VALUE)
      override fun always(): Iterable<Int> = literals
      override fun random(): Sequence<Int> = generateSequence { Math.abs(RANDOM.nextInt()) }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen negative value. The values returned always include
     * the following edge cases: [0, -1, Int.MIN_VALUE]
     */
    fun negativeIntegers(): Gen<Int> = object : Gen<Int> {
      val literals = listOf(0, -1, Int.MIN_VALUE)
      override fun always(): Iterable<Int> = literals
      override fun random(): Sequence<Int> = generateSequence { -Math.abs(RANDOM.nextInt()) }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen created File object. The file objects do not necessarily
     * exist on disk.
     */
    fun file(): Gen<File> = object : Gen<File> {
      override fun always(): Iterable<File> = emptyList()
      override fun random(): Sequence<File> = generateSequence { File(nextPrintableString(RANDOM.nextInt(100))) }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen long. The values returned always include
     * the following edge cases: [-1, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE]
     */
    fun long(): Gen<Long> = object : Gen<Long> {
      val literals = listOf(-1, 0, 1, Long.MIN_VALUE, Long.MAX_VALUE)
      override fun always(): Iterable<Long> = literals
      override fun random(): Sequence<Long> = generateSequence { Math.abs(RANDOM.nextLong()) }
    }

    /**
     * Returns both boolean values
     */
    fun bool(): Gen<Boolean> = object : Gen<Boolean> {
      override fun always(): Iterable<Boolean> = listOf(true, false)
      override fun random(): Sequence<Boolean> = generateSequence { RANDOM.nextBoolean() }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen Double.
     */
    fun double(): Gen<Double> = object : Gen<Double> {
      val literals = listOf(-1.0, 0.0, 1.0, Double.MIN_VALUE, Double.MAX_VALUE, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY)
      override fun always(): Iterable<Double> = literals
      override fun random(): Sequence<Double> = generateSequence { RANDOM.nextDouble() }
    }

    /**
     * Returns a stream of values where each value is a randomly
     * chosen Float.
     */
    fun float(): Gen<Float> = object : Gen<Float> {
      val literals = listOf(-1.0F, 0.0F, 1.0F, Float.MIN_VALUE, Float.MAX_VALUE, Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY)
      override fun always(): Iterable<Float> = literals
      override fun random(): Sequence<Float> = generateSequence { RANDOM.nextFloat() }
    }

    /**
     * Returns a stream of values, where each
     * value is generated from the given function
     */
    fun <T : Any> create(fn: () -> T): Gen<T> = object : Gen<T> {
      override fun always(): Iterable<T> = emptyList()
      override fun random(): Sequence<T> = generateSequence { fn() }
    }

    /**
     * Returns a stream of values, where each value is
     * a set of values generated by the given generator.
     */
    fun <T : Any> set(gen: Gen<T>): Gen<Set<T>> = object : Gen<Set<T>> {
      override fun always(): Iterable<Set<T>> = listOf(gen.always().toSet())
      override fun random(): Sequence<Set<T>> = generateSequence {
        val size = RANDOM.nextInt(100)
        gen.random().take(size).toSet()
      }
    }

    /**
     * Returns a stream of values, where each value is
     * a list of values generated by the underlying generator.
     */
    fun <T : Any> list(gen: Gen<T>): Gen<List<T>> = object : Gen<List<T>> {
      override fun always(): Iterable<List<T>> = listOf(gen.always().toList())
      override fun random(): Sequence<List<T>> = generateSequence {
        val size = RANDOM.nextInt(100)
        gen.random().take(size).toList()
      }
    }

    /**
     * Returns a stream of values, where each value is
     * a pair generated by the underlying generators.
     */
    fun <K, V> pair(genK: Gen<K>, genV: Gen<V>): Gen<Pair<K, V>> = object : Gen<Pair<K, V>> {
      override fun always(): Iterable<Pair<K, V>> {
        val keys = genK.always().toList()
        return keys.zip(genV.random().take(keys.size).toList())
      }

      override fun random(): Sequence<Pair<K, V>> = genK.random().zip(genV.random())
    }

    // list(pair(genK, genV)).generate().toMap()

    /**
     * Returns a stream of values, where each value is
     * a Map, which contains keys and values generated
     * from the underlying generators.
     */
    fun <K, V> map(genK: Gen<K>, genV: Gen<V>): Gen<Map<K, V>> = object : Gen<Map<K, V>> {
      override fun always(): Iterable<Map<K, V>> = emptyList()
      override fun random(): Sequence<Map<K, V>> = generateSequence {
        val size = RANDOM.nextInt(100)
        genK.random().take(size).zip(genV.random().take(size)).toMap()
      }
    }

    fun <T : Any> constant(value: T): Gen<T> = object : Gen<T> {
      override fun always(): Iterable<T> = listOf(value)
      override fun random(): Sequence<T> = generateSequence { value }
    }

    fun forClassName(className: String): Gen<*> {
      return when (className) {
        "java.lang.String" -> string()
        "kotlin.String" -> string()
        "java.lang.Integer" -> int()
        "kotlin.Int" -> int()
        "java.lang.Long" -> long()
        "kotlin.Long" -> long()
        "java.lang.Boolean" -> bool()
        "kotlin.Boolean" -> bool()
        "java.lang.Float" -> float()
        "kotlin.Float" -> float()
        "java.lang.Double" -> double()
        "kotlin.Double" -> double()
        else -> throw IllegalArgumentException("Cannot infer generator for $className; specify generators explicitly")
      }
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> default(): Gen<T> {
      return when (T::class.qualifiedName) {
        List::class.qualifiedName -> {
          val type = object : TypeReference<T>() {}.type as ParameterizedType
          val first = type.actualTypeArguments.first() as WildcardType
          val upper = first.upperBounds.first() as Class<*>
          list(forClassName(upper.name) as Gen<Any>) as Gen<T>
        }
        Set::class.qualifiedName -> {
          val type = object : TypeReference<T>() {}.type as ParameterizedType
          val first = type.actualTypeArguments.first() as WildcardType
          val upper = first.upperBounds.first() as Class<*>
          set(forClassName(upper.name) as Gen<Any>) as Gen<T>
        }
        Pair::class.qualifiedName -> {
          val type = object : TypeReference<T>() {}.type as ParameterizedType
          val first = (type.actualTypeArguments[0] as WildcardType).upperBounds.first() as Class<*>
          val second = (type.actualTypeArguments[1] as WildcardType).upperBounds.first() as Class<*>
          pair(forClassName(first.name), forClassName(second.name)) as Gen<T>
        }
        Map::class.qualifiedName -> {
          val type = object : TypeReference<T>() {}.type as ParameterizedType
          //map key type can have or have not variance
          val first = if (type.actualTypeArguments[0] is Class<*>) {
            type.actualTypeArguments[0] as Class<*>
          } else {
            (type.actualTypeArguments[0] as WildcardType).upperBounds.first() as Class<*>
          }
          val second = (type.actualTypeArguments[1] as WildcardType).upperBounds.first() as Class<*>
          map(forClassName(first.name), forClassName(second.name)) as Gen<T>
        }
        else -> forClassName(T::class.qualifiedName!!) as Gen<T>
      }
    }
  }

  /**
   * Returns the next pseudorandom, uniformly distributed value
   * from the ASCII range 33-126.
   */
  private fun Random.nextPrintableChar(): Char {
    val low = 33
    val high = 127
    return (nextInt(high - low) + low).toChar()
  }

  fun nextPrintableString(length: Int): String {
    return (0 until length).map { RANDOM.nextPrintableChar() }.joinToString("")
  }
}

/**
 * A Generator which will return an iterable of a single given value.
 */
@Deprecated("use Gen.constant")
data class ConstGen<out T : Any>(val value: T) : Gen<T> {
  override fun always(): Iterable<T> = listOf(value)
  override fun random(): Sequence<T> = generateSequence { value }
}

/**
 * A [Gen] which will return the values from the underlying
 * generator plus null.
 */
fun <T : Any> Gen<T>.orNull(): Gen<T?> {
  val outer = this
  return object : Gen<T?> {
    override fun always(): Iterable<T?> = outer.always() + listOf(null)
    override fun random(): Sequence<T?> = outer.random().map { if (RANDOM.nextBoolean()) null else it }
  }
}

/**
 * An extension function for [Gen] that filters values
 * from an underlying generator using a predicate function.
 */
@Deprecated("use gen.filter(T -> Boolean", ReplaceWith("generate().filter(isGood)"))
internal fun <T> Gen<T>.generateGood(isGood: (T) -> Boolean) = filter(isGood)

/**
 * A [Gen] which uses an extension function to filter
 * values from the underlying generator.
 */
fun <T> Gen<T>.filter(f: (T) -> Boolean): Gen<T> {
  val outer = this
  return object : Gen<T> {
    override fun always(): Iterable<T> = outer.always().filter(f)
    override fun random(): Sequence<T> = outer.random().filter(f)
  }
}

/**
 * An extension function for [Gen] that maps values
 * from an underlying generator using a mapper function.
 */
fun <A, B> Gen<A>.map(f: (A) -> B): Gen<B> {
  val outer = this
  return object : Gen<B> {
    override fun always(): Iterable<B> = outer.always().map(f)
    override fun random(): Sequence<B> = outer.random().map(f)
  }
}

// need some supertype that types a type param so it gets baked into the class file
abstract class TypeReference<T> : Comparable<TypeReference<T>> {
  val type: Type = (javaClass.genericSuperclass as ParameterizedType).actualTypeArguments[0]
  override fun compareTo(other: TypeReference<T>) = 0
}
