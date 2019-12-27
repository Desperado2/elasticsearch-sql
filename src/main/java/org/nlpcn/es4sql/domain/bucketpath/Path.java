package org.nlpcn.es4sql.domain.bucketpath;

/**
 * @author mujingjing
 * @date 2019/12/27 16:41
 */
public class Path {
    private final String path;
    private final String separator;
    private final PathType type;

    private Path(String path, String separator, PathType type) {
        this.path = path;
        this.separator = separator;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public String getSeparator() {
        return separator;
    }

    public PathType getType() {
        return type;
    }

    public boolean isMetricPath() {
        return type == PathType.METRIC;
    }

    public boolean isAggPath() {
        return type == PathType.AGG;
    }

    public static Path getAggPath(String path) {
        return new Path(path, ">", PathType.AGG);
    }

    public static Path getMetricPath(String path) {
        return new Path(path, ".", PathType.METRIC);
    }

    public enum PathType {
        AGG, METRIC
    }
}
