package util.data;

import util.math.IntRange;

import java.util.ArrayList;
import java.util.List;

/**
 * Model a utility for operations with lists.
 */
public class ListUtil {
    /**
     * @param list the specified list
     * @param numberOfPartitions the number of partitions of the specified list
     * @return a list of ranges with the indexes of the partitions of the specified list
     */
    public static List<IntRange> partition(List<?> list, int numberOfPartitions){
        int numberOfElementsPerPartition = list.size() / numberOfPartitions;
        List<IntRange> listOfRanges = new ArrayList<>();
        for (int i = 0; i < numberOfPartitions - 1; i++){
            listOfRanges.add(new IntRange(i * numberOfElementsPerPartition, (i+1) * numberOfElementsPerPartition));
        }
        listOfRanges.add(new IntRange((numberOfPartitions-1) * numberOfElementsPerPartition, list.size()));
        return listOfRanges;
    }
}