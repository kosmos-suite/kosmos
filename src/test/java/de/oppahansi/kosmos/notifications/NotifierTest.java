package de.oppahansi.kosmos.notifications;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotifierTest {

  @Test
  void nullEnabledEventsWantsEveryEvent() {
    Notifier notifier = new Notifier();
    notifier.enabledEvents = null;
    assertTrue(notifier.wantsEvent(NotificationEventType.GRAB));
    assertTrue(notifier.wantsEvent(NotificationEventType.IMPORT));
    assertTrue(notifier.wantsEvent(NotificationEventType.BLOCKLIST));
  }

  @Test
  void blankEnabledEventsWantsEveryEvent() {
    Notifier notifier = new Notifier();
    notifier.enabledEvents = "  ";
    assertTrue(notifier.wantsEvent(NotificationEventType.IMPORT));
  }

  @Test
  void onlyListedEventsAreWanted() {
    Notifier notifier = new Notifier();
    notifier.enabledEvents = "GRAB,IMPORT";
    assertTrue(notifier.wantsEvent(NotificationEventType.GRAB));
    assertTrue(notifier.wantsEvent(NotificationEventType.IMPORT));
    assertFalse(notifier.wantsEvent(NotificationEventType.BLOCKLIST));
  }

  @Test
  void joinEventsRoundTripsThroughWantsEvent() {
    String joined = Notifier.joinEvents(List.of(NotificationEventType.BLOCKLIST));
    Notifier notifier = new Notifier();
    notifier.enabledEvents = joined;
    assertTrue(notifier.wantsEvent(NotificationEventType.BLOCKLIST));
    assertFalse(notifier.wantsEvent(NotificationEventType.GRAB));
  }

  @Test
  void joinEventsOfEmptyListMeansEveryEvent() {
    assertNull(Notifier.joinEvents(List.of()));
    assertNull(Notifier.joinEvents(null));
  }
}
