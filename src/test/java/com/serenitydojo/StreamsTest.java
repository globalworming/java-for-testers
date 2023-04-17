package com.serenitydojo;

import org.hamcrest.number.IsCloseTo;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.*;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StreamsTest {

    @Test
    void streamsAreAnAlternativeToLists() {
        // we can loop, aggregate, filter, sort and transform more conveniently
        List<Integer> numbersList = new ArrayList<>(List.of(3, 1, 2));
        Collections.sort(numbersList);
        assertThat(numbersList, contains(1, 2, 3));

        Stream<Integer> numbersStream = Stream.of(3, 1, 2);
        Stream<Integer> sorted = numbersStream.sorted();
        assertThat(sorted.toList(), contains(1, 2, 3));
    }

    @Test
    void streamsCanComeFromDifferentSources() throws IOException {
        // from collections
        Stream<String> hello1 = List.of("hello").stream();
        // we build one
        Stream<String> hello2 = Stream.of("hello");
        // from other libraries
        long count;
        try (Stream<Path> stream = Files.list(Path.of("/"))) {
            count = stream.count();
        }
        assertThat(count, is(22L));

        // iterating values
        Stream.iterate(0, i -> i < 10, i -> ++i).forEach(System.out::println);

        // generating values
        // combine it with the Faker library
        Stream<Instant> instantStream = Stream.generate(Instant::now);
        instantStream.limit(3).forEach(System.out::println);
    }

    @Test
    void someThingsYouDoWithStreams() {
        // transform with map
        Stream<String> stream = Stream.of("cat", "dog").map(String::toUpperCase);
        assertThat(stream.toList(), contains("CAT", "DOG"));

        // TODO map to different type
        Stream<String> animals = Stream.of("cat", "dog").map(String::toUpperCase);
        Stream<Map<String, String>> name = animals.map(animal -> Map.of("name", animal));


        // flatten (concat multiple stream into a single stream)
        List<String> smallAnimals = List.of("cat", "dog");
        List<String> bigAnimals = List.of("whale");

        assertThat(Stream.of(smallAnimals, bigAnimals).count(), is(2L));
        Stream<String> stringStream = Stream.of(smallAnimals, bigAnimals).flatMap(list -> list.stream());
        assertThat(stringStream.count(), is(3L));

        // remove duplicates
        Stream<Integer> stream1 = Stream.of(1, 5, 3, 5, 8, 8, 5, 3, 2, 2, 3, 4, 5, 6, 7, 8, 9, 8, 9, 8, 5, 4)
                .peek(e -> System.out.println("value: " + e))
                .distinct()
                .sorted()
                .peek(e -> System.out.println("after distinct and sorted: " + e));
        List<Integer> numbers = stream1.toList();
        assertThat(numbers.size(), is(9));
        assertThat(numbers, contains(1, 2, 3, 4, 5, 6, 7, 8, 9));
    }

    @Test
    void youCanHaveManyIntermediateButOnlyOneTerminatingOperation() {
//        // intermediate
        Predicate<String> stringPredicate = s -> s.length() > 0;
        Stream<String> stream = Stream.of("cat", "dog", "")
                .map(String::toUpperCase)
                .filter(stringPredicate)
                .flatMap(s -> Arrays.stream(s.split("")))
                .peek(e -> System.out.println("value: " + e));
//        // terminating like forEach, toList, collect, reduce, count..
        assertThat(stream.count(), is(6L));

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, stream::toList);
        assertThat(illegalStateException.getMessage(), is("stream has already been operated upon or closed"));
    }

    @Test
    void whereThereAreDifferentTypesOfStreams() {
        // Streams usually take objects
        // won't work:
        //Stream<long> numbersStream = Stream.of(3l, 1l, 2l);
//        // for primitives you need special streams
        IntStream intStream = IntStream.of(1, 2, 3);
        LongStream longStream = LongStream.of(1, 2, 3);
        DoubleStream doubleStream = DoubleStream.of(1, 2, 3);
//
//        // transform to primitive
        DoubleStream doubleStream1 = Stream.of("cat", "dog", "snake").mapToDouble(String::length);
//
        // things you can conveniently do with primitiv streams
        assertThat(Stream.of("cat", "dog", "snake").mapToDouble(String::length).sum(), is(11.0));
        assertThat(Stream.of("cat", "dog", "snake").mapToDouble(String::length).average().getAsDouble(), IsCloseTo.closeTo(3.6, 0.1));
        assertThat(Stream.of("cat", "dog", "snake").mapToDouble(String::length).max().getAsDouble(), is(5.0));
        assertThat(Stream.of("cat", "dog", "snake").mapToDouble(String::length).min().getAsDouble(), is(3.0));
    }

    //
//
    @Test
    void streamsDeferExecution() {
        final AtomicInteger counter = new AtomicInteger(0);
        // transform with "map", an intermediate operation, like "filter", "limit", "sort"
        // TODO extract mapping function
        Stream<String> stream = Stream.of("cat", "dog", "hamster")
                .map(animal -> {
                    counter.getAndIncrement();
                    return animal.toUpperCase();
                });
        assertThat(counter.get(), is(0));
    }

    @Test
    void streamsDeferExecutionUntilWeNeedAResult() {
        AtomicInteger mappingFunctionCalls = new AtomicInteger(0);
        // transform with map
        List<String> strings = Stream.of("cat", "dog", "hamster")
                .map(animal -> {
                    mappingFunctionCalls.getAndIncrement();
                    return animal.toUpperCase();
                })
                .toList();
        assertThat(mappingFunctionCalls.get(), is(3));
    }

    @Test
    void streamsCanOptimizeAndNotProcessEveryElement() {
        AtomicInteger mappingFunctionCalls = new AtomicInteger(0);
        // transform with map
        Stream.of("cat", "dog", "hamster").map(animal -> {
            mappingFunctionCalls.getAndIncrement();
            return animal.toUpperCase();
        }).findFirst();
        assertThat(mappingFunctionCalls.get(), is(1));
    }

    @Test
    void thoughParallelProcessingCanMakeItHarder() {
        AtomicInteger mappingFunctionCalls = new AtomicInteger(0);
        // transform with map
        Stream.iterate(0, i -> ++i)
                .limit(50)
                .parallel()
                .map(i -> {
                    mappingFunctionCalls.getAndIncrement();
                    return Math.pow(i, 2);
                })
                .limit(10)
                .toList();
        assertThat(mappingFunctionCalls.get(), allOf(greaterThan(9), lessThan(50)));
        System.out.println(mappingFunctionCalls.get());

        // parallel: good for big number of elements but not worth it for smaller
    }
//
//    // TODO errors
//


    String readValue(String s) throws IOException {
        return "";
    }

    @Test
    void withErrors() {
        Function<String, String> stringStringFunction = s -> {
            try {
                return readValue(s);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
        Stream
                .of("")
                .map(stringStringFunction)
                .collect(Collectors.toList());

    }
}
