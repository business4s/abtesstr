package abtesstr

import java.nio.ByteBuffer
import java.security.MessageDigest

trait Hasher {
  def hash(value: String): Point
}

object Hasher {

  val sha256: Hasher = new Hasher {
    val digest = MessageDigest.getInstance("SHA-256")
    override def hash(value: String): Point = {
      val hash     = digest.digest(value.getBytes("UTF-8"))
      val longBits = ByteBuffer.wrap(hash.take(8)).getLong & 0x7fffffffffffffffL // keep it positive
      Point(longBits % SpaceSize)
    }
  }

}
