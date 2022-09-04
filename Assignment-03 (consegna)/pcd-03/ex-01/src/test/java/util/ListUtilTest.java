package util;

import org.junit.Test;
import util.data.ListUtil;
import util.math.IntRange;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class ListUtilTest {
    @Test public void testPartition(){
        assertEquals(
            List.of(
                new IntRange(0, 142),
                new IntRange(142, 284),
                new IntRange(284, 426),
                new IntRange(426, 568),
                new IntRange(568, 710),
                new IntRange(710, 852),
                new IntRange(852, 1000)
            ),
            ListUtil.partition(Stream.iterate(0, x -> x).limit(1000).collect(Collectors.toList()), 7)
        );
        assertEquals(
            List.of(
                new IntRange(0, 200),
                new IntRange(200, 400),
                new IntRange(400, 600),
                new IntRange(600, 800),
                new IntRange(800, 1000)
            ),
            ListUtil.partition(Stream.iterate(0, x -> x).limit(1000).collect(Collectors.toList()), 5)
        );
    }
}