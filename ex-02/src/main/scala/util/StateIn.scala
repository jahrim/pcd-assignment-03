package util

/**
 * Model an entity with a state in the specified set of possible states, starting at the specified state.
 *
 * @tparam S the specified sets of possible states
 */
trait StateIn[S](private var currentState: S):
  /** @return the state of this entity */
  def state: S = this.currentState

  /**
   * Sets the state of this entity to the specified value
   *
   * @param s the specified value
   */
  def become(s: S): Unit = this.currentState = s

  /** @return true if this entity is in the specified state, false otherwise. */
  def is(s: S): Boolean = this.state == s
