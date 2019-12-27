package org.nlpcn.es4sql.domain.bucketpath;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * @author mujingjing
 * @date 2019/12/27 16:41
 */
public class BucketPath {
    private Deque<Path> pathStack = new ArrayDeque<>();

    public BucketPath add(Path path) {
        if (pathStack.isEmpty()) {
            assert path.isMetricPath() : "The last path in the bucket path must be Metric";
        } else {
            assert path.isAggPath() : "All the other path in the bucket path must be Agg";
        }
        pathStack.push(path);
        return this;
    }

    /**
     * Return the bucket path.
     * Return "", if there is no agg or metric available
     */
    public String getBucketPath() {
        String bucketPath = pathStack.isEmpty() ? "" : pathStack.pop().getPath();
        return pathStack.stream()
                .map(path -> path.getSeparator() + path.getPath())
                .reduce(bucketPath, String::concat);
    }
}
