package com.sbg.bdd.wiremock.scoped.client.strategies;


import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.number.OrderingComparison.lessThan;

public class CountMatcher extends BaseMatcher<Integer> {
    private Matcher<Integer> delegate;

    public CountMatcher(Matcher<Integer> delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean matches(Object item) {
        return delegate.matches(item);
    }

    @Override
    public void describeTo(Description description) {
        delegate.describeTo(description);
    }

    public CountMatcher times() {
        return this;
    }

    public CountMatcher and(int secondVar) {
        delegate = allOf(delegate, lessThan(secondVar));
        return this;
    }

    public static CountMatcher once() {
        return new CountMatcher(equalTo(1));
    }

    public static CountMatcher twice() {
        return new CountMatcher(equalTo(2));
    }

    public static CountMatcher exactly(int times) {
        return new CountMatcher(equalTo(times));
    }

    public static CountMatcher exactly(CountMatcher countMatcher) {
        return countMatcher;
    }

    public static CountMatcher between(int times) {
        return new CountMatcher(greaterThan(times));
    }

}
