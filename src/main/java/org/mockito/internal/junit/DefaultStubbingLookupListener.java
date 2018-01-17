/*
 * Copyright (c) 2017 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.junit;

import org.mockito.internal.exceptions.Reporter;
import org.mockito.internal.listeners.StubbingLookupListener;
import org.mockito.invocation.Invocation;
import org.mockito.mock.MockCreationSettings;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Stubbing;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Mockito.mockingDetails;
import static org.mockito.internal.stubbing.StrictnessSelector.determineStrictness;

/**
 * Default implementation of stubbing lookup listener.
 * Fails early if stub called with unexpected arguments, but only if current strictness is set to STRICT_STUBS.
 */
class DefaultStubbingLookupListener implements StubbingLookupListener {

    private Strictness currentStrictness;
    private boolean mismatchesReported;

    DefaultStubbingLookupListener(Strictness strictness) {
        this.currentStrictness = strictness;
    }

    public void onStubbingLookup(Invocation invocation, Stubbing stubbingFound, MockCreationSettings mockSettings) {
        Strictness actualStrictness = determineStrictness(currentStrictness, mockSettings, stubbingFound);

        if (actualStrictness != Strictness.STRICT_STUBS) {
            return;
        }

        if (stubbingFound == null) {
            //If stubbing was not found for invocation it means that either the mock invocation was not stubbed or
            //we have a stubbing arg mismatch.
            List<Invocation> argMismatchStubbings = potentialArgMismatches(invocation);
            if (!argMismatchStubbings.isEmpty()) {
                mismatchesReported = true;
                Reporter.potentialStubbingProblem(invocation, argMismatchStubbings);
            }
        } else {
            //when strict stubs are in use, every time a stub is realized in the code it is implicitly marked as verified
            //this way, the users don't have to repeat themselves to verify stubbed invocations (DRY)
            invocation.markVerified();
        }
    }

    private static List<Invocation> potentialArgMismatches(Invocation invocation) {
        List<Invocation> matchingStubbings = new LinkedList<Invocation>();
        Collection<Stubbing> stubbings = mockingDetails(invocation.getMock()).getStubbings();
        for (Stubbing s : stubbings) {
            if (!s.wasUsed() && s.getInvocation().getMethod().getName().equals(invocation.getMethod().getName())
                //in case the mock is strict but the stubbing is lenient,
                // we don't want to report lenient stubbing as potential arg mismatch
                && s.getStrictness() != Strictness.LENIENT) {
                matchingStubbings.add(s.getInvocation());
            }
        }
        return matchingStubbings;
    }

    /**
     * Enables resetting the strictness to desired level
     */
    void setCurrentStrictness(Strictness currentStrictness) {
        this.currentStrictness = currentStrictness;
    }

    /**
     * Indicates that stubbing argument mismatch was reported
     */
    boolean isMismatchesReported() {
        return mismatchesReported;
    }
}
