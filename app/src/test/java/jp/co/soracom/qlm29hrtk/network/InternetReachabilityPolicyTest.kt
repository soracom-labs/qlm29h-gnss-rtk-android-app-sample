package jp.co.soracom.qlm29hrtk.network

import org.junit.Assert.assertEquals
import org.junit.Test

class InternetReachabilityPolicyTest {
    @Test fun noDefaultNetworkIsOffline() {
        assertEquals(
            InternetReachability.OFFLINE,
            InternetReachabilityPolicy.evaluate(false, false, false),
        )
    }

    @Test fun transportWithoutInternetCapabilityIsOffline() {
        assertEquals(
            InternetReachability.OFFLINE,
            InternetReachabilityPolicy.evaluate(true, false, false),
        )
    }

    @Test fun internetCapabilityWithoutValidationIsChecking() {
        assertEquals(
            InternetReachability.CHECKING,
            InternetReachabilityPolicy.evaluate(true, true, false),
        )
    }

    @Test fun validatedInternetCapabilityIsOnline() {
        assertEquals(
            InternetReachability.ONLINE,
            InternetReachabilityPolicy.evaluate(true, true, true),
        )
    }
}
