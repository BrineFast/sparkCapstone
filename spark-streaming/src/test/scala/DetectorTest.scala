import FirstDetector.EventDetails
import org.junit.Test
import org.junit.Assert._
import FirstDetector._

class DetectorTest {

  @Test
  def `check event type test` = {
    val clickEvent = Logs("111.1.1.1", 364167423, "click")
    val transitionEvent = Logs("101.1.0.1", 37496234, "transition")

    assertEquals(EventDetails(clickEvent.ip, 1, 0, 1),checkEventType(clickEvent))
    assertEquals(EventDetails(transitionEvent.ip, 0, 1, 1),checkEventType(transitionEvent))
  }

}