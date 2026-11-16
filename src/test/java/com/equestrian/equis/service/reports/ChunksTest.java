package com.equestrian.equis.service.reports;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ChunksTest {

    // E19 · Rider balances, case 12
    @Test
    void groupsInOrderAndClosingTheResultClosesTheSource() {
        AtomicBoolean closed = new AtomicBoolean();
        Stream<Integer> source = IntStream.range(0, 1001).boxed().onClose(() -> closed.set(true));

        List<List<Integer>> chunks;
        try (Stream<List<Integer>> result = Chunks.of(source, 500)) {
            chunks = result.toList();
            assertThat(closed).isFalse();
        }

        assertThat(chunks).extracting(List::size).containsExactly(500, 500, 1);
        assertThat(chunks.get(0).get(0)).isZero();
        assertThat(chunks.get(2)).containsExactly(1000);
        assertThat(closed).isTrue();
    }

    @Test
    void readsTheSourceOnlyAsChunksAreAskedFor() {
        AtomicInteger read = new AtomicInteger();
        Stream<Integer> source = IntStream.range(0, 1001).boxed().peek(i -> read.incrementAndGet());

        try (Stream<List<Integer>> result = Chunks.of(source, 500)) {
            assertThat(result.findFirst()).hasValueSatisfying(first -> assertThat(first).hasSize(500));
        }

        assertThat(read.get()).isEqualTo(500);
    }

    @Test
    void anEmptySourceGivesNoChunks() {
        assertThat(Chunks.of(Stream.empty(), 500)).isEmpty();
    }

    @Test
    void theSizeMustBePositive() {
        assertThatThrownBy(() -> Chunks.of(Stream.of(1), 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
