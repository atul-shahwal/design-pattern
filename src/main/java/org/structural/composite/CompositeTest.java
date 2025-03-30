package org.structural.composite;

import org.structural.composite.logic.Directory;
import org.structural.composite.logic.File;
import org.structural.composite.logic.FileSystem;

public class CompositeTest {

    public static void main(String[] args) {
        Directory parentDir = new Directory();
        FileSystem obj1 = new File("xml.txt");
        parentDir.add(obj1);
        Directory childDir = new Directory();
        FileSystem obj2 = new File("yml.txt");
        childDir.add(obj2);
        parentDir.add(childDir);
        System.out.println(parentDir);
        System.out.println(childDir);
    }
}
