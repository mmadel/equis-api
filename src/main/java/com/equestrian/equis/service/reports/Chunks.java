package com.equestrian.equis.service.reports;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

// Groups a stream into lists of at most size items, in order, reading the source only as the lists are asked for.
// Closing the result closes the source, so a database stream stays open just as long as the report reads it.
public final class Chunks {

    private Chunks() {
    }

    public static <T> Stream<List<T>> of(Stream<T> source, int size) {
        if (size < 1) {
            throw new IllegalArgumentException("size must be at least 1");
        }
        Iterator<T> items = source.iterator();
        Iterator<List<T>> chunks = new Iterator<>() {
            @Override
            public boolean hasNext() {
                return items.hasNext();
            }

            @Override
            public List<T> next() {
                if (!items.hasNext()) {
                    throw new NoSuchElementException();
                }
                List<T> chunk = new ArrayList<>(size);
                while (chunk.size() < size && items.hasNext()) {
                    chunk.add(items.next());
                }
                return chunk;
            }
        };
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(chunks, Spliterator.ORDERED), false)
                .onClose(source::close);
    }
}
