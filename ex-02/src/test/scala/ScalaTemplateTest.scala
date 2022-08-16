import org.scalatest.funspec.AnyFunSpec

/**
 * Model a template test with scala test for behaviour-driven-development.
 */
class ScalaTemplateTest extends AnyFunSpec {
    describe("A template test") {
        describe("when executed") {
            it("should not fail") {
                assert(condition = true)
            }
        }
    }
}
