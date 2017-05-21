package com.github.ampie.wiremock.extended;

import com.github.tomakehurst.wiremock.stubbing.ServeEvent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.github.ampie.wiremock.ScopePathMatcher.matches;

public class ServeEventsQueueDecorator extends ConcurrentLinkedQueue<ServeEvent> {
    private  ConcurrentLinkedQueue<ServeEvent> delegate;

    public ServeEventsQueueDecorator(ConcurrentLinkedQueue<ServeEvent> delegate) {
        this.delegate = delegate;
    }
    public void removeServedStubsForScope(Pattern pattern){
        Iterator<ServeEvent> iterator = delegate.iterator();
        while(iterator.hasNext()){
            if(matches(pattern,iterator.next().getRequest())){
                iterator.remove();;
            }
        }
    }

    @Override
    public boolean add(ServeEvent servedStub) {
        return delegate.add(servedStub);
    }

    @Override
    public boolean offer(ServeEvent servedStub) {
        return delegate.offer(servedStub);
    }

    @Override
    public ServeEvent poll() {
        return delegate.poll();
    }

    @Override
    public ServeEvent peek() {
        return delegate.peek();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public int size() {
        return delegate.size();
    }

    @Override
    public boolean contains(Object o) {
        return delegate.contains(o);
    }

    @Override
    public boolean remove(Object o) {
        return delegate.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends ServeEvent> c) {
        return delegate.addAll(c);
    }

    @Override
    public Object[] toArray() {
        return delegate.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return delegate.toArray(a);
    }

    @Override
    public Iterator<ServeEvent> iterator() {
        return delegate.iterator();
    }

    @Override
    public Spliterator<ServeEvent> spliterator() {
        return delegate.spliterator();
    }

    @Override
    public ServeEvent remove() {
        return delegate.remove();
    }

    @Override
    public ServeEvent element() {
        return delegate.element();
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return delegate.containsAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return delegate.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return delegate.retainAll(c);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public boolean removeIf(Predicate<? super ServeEvent> filter) {
        return delegate.removeIf(filter);
    }

    @Override
    public boolean equals(Object o) {
        return delegate.equals(o);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public Stream<ServeEvent> stream() {
        return delegate.stream();
    }

    @Override
    public Stream<ServeEvent> parallelStream() {
        return delegate.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super ServeEvent> action) {
        delegate.forEach(action);
    }
}
