package com.sbg.bdd.wiremock.scoped.server.decorated;

import com.github.tomakehurst.wiremock.matching.RequestPattern;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import com.github.tomakehurst.wiremock.verification.InMemoryRequestJournal;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import com.google.common.base.Optional;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.sbg.bdd.wiremock.scoped.common.Reflection.getValue;
import static com.sbg.bdd.wiremock.scoped.server.ScopePathMatcher.matches;

public class InMemoryRequestJournalDecorator extends InMemoryRequestJournal{
    private final InMemoryRequestJournal delegate;
    private Queue<ServeEvent> serveEvents;
    public InMemoryRequestJournalDecorator(InMemoryRequestJournal delegate) {
        super(Optional.fromNullable(1));
        serveEvents =getValue(delegate,"serveEvents");
        this.delegate=delegate;
    }
    public void removeServedStubsForScope(Pattern pattern){
        Iterator<ServeEvent> iterator = serveEvents.iterator();
        while(iterator.hasNext()){
            if(matches(pattern,iterator.next().getRequest())){
                iterator.remove();;
            }
        }
    }

    @Override
    public int countRequestsMatching(RequestPattern requestPattern) {
        return delegate.countRequestsMatching(requestPattern);
    }

    @Override
    public List<LoggedRequest> getRequestsMatching(RequestPattern requestPattern) {
        return delegate.getRequestsMatching(requestPattern);
    }

    @Override
    public void requestReceived(ServeEvent serveEvent) {
        delegate.requestReceived(serveEvent);
    }

    @Override
    public List<ServeEvent> getAllServeEvents() {
        return delegate.getAllServeEvents();
    }

    @Override
    public Optional<ServeEvent> getServeEvent(UUID id) {
        return delegate.getServeEvent(id);
    }

    @Override
    public void reset() {
        delegate.reset();
    }
}
