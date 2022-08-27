package util

import scala.util.Random

/**
 * Model an entity with an identifier.
 */
trait Id:
  def id: String

/**
 * Companion object of [[Id]].
 */
object Id:
  /** @return a new randomly generated id. The uniqueness of the id is not guaranteed but highly probable. */
  def newId: String = BigInt.long2bigInt(Random.nextLong()).toString(36)