package org.structural.composite.logic;

import java.util.ArrayList;
import java.util.List;

public class Directory implements FileSystem{

    List<FileSystem> fileSystemList = new ArrayList<>();

    public void add(FileSystem fileSystem) {
        fileSystemList.add(fileSystem);
    }

    @Override
    public void ls() {
        fileSystemList.forEach((System.out::println));
    }

    @Override
    public String toString() {
        return "Directory{" +
                "fileSystemList=" + fileSystemList +
                '}';
    }
}
