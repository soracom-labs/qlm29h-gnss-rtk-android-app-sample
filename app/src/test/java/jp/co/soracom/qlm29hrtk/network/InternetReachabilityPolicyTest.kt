package jp.co.soracom.qlm29hrtk.network

import org.junit.Assert.assertEquals
import org.junit.Test

class InternetReachabilityPolicyTest {
    @Test fun noCandidateIsOffline() {
        assertEquals(
            InternetReachability.OFFLINE,
            InternetReachabilityPolicy.evaluate(emptyList()),
        )
    }

    @Test fun validatedVpnWithoutPhysicalUnderlayIsOffline() {
        assertEquals(
            InternetReachability.OFFLINE,
            InternetReachabilityPolicy.evaluate(
                listOf(InternetNetworkCandidate(isVpn = true, hasInternetCapability = true, isValidated = true)),
            ),
        )
    }

    @Test fun unvalidatedPhysicalInternetCandidateIsChecking() {
        assertEquals(
            InternetReachability.CHECKING,
            InternetReachabilityPolicy.evaluate(
                listOf(
                    InternetNetworkCandidate(isVpn = true, hasInternetCapability = true, isValidated = true),
                    InternetNetworkCandidate(isVpn = false, hasInternetCapability = true, isValidated = false),
                ),
            ),
        )
    }

    @Test fun validatedPhysicalInternetCandidateIsOnline() {
        assertEquals(
            InternetReachability.ONLINE,
            InternetReachabilityPolicy.evaluate(
                listOf(
                    InternetNetworkCandidate(isVpn = true, hasInternetCapability = true, isValidated = true),
                    InternetNetworkCandidate(isVpn = false, hasInternetCapability = true, isValidated = true),
                ),
            ),
        )
    }

    @Test fun localOnlyPhysicalCandidateIsOffline() {
        assertEquals(
            InternetReachability.OFFLINE,
            InternetReachabilityPolicy.evaluate(
                listOf(InternetNetworkCandidate(isVpn = false, hasInternetCapability = false, isValidated = false)),
            ),
        )
    }
}
