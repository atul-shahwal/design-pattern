package org.desingpatterns.questions.filesystem.interview;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 🎯 Problem Statement: File System
 *
 * Design a simple file system supporting files and directories.
 * Implement create, delete, read, write, and display operations using the composite pattern.
 *
 * ❓ Interview Q&A:
 * Q: Why use the composite pattern here?
 * A: It allows treating files and directories uniformly, enabling recursive operations like display or traversal.
 *
 * Q: Why not use inheritance only for files and directories?
 * A: The composite pattern provides flexibility in tree structures and operations without duplicating logic.
 */

// Base class for FileSystemNode
/**
 * ❓ Interview Q&A:
 * Q: Why make FileSystemNode abstract?
 * A: To enforce common behavior and structure for both files and directories while allowing specific implementations.
 */
abstract class FileSystemNode {
    protected String name;
    protected LocalDateTime createdAt;
    protected LocalDateTime modifiedAt;

    /**
     * ❓ Interview Q&A:
     * Q: Why track created and modified timestamps?
     * A: They are useful for auditing, versioning, and maintaining metadata about files and directories.
     */
    public FileSystemNode(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    public String getName() { return name; }

    public abstract boolean isFile();

    /**
     * ❓ Interview Q&A:
     * Q: Why define display here as an abstract method?
     * A: Each node type (file or directory) has a different way of displaying itself, so forcing implementation promotes consistency.
     */
    public abstract void display(int depth);
}

// File node
/**
 * ❓ Interview Q&A:
 * Q: Why does File extend FileSystemNode?
 * A: It inherits common properties and behavior but adds its own content-specific logic.
 */
class File extends FileSystemNode {
    private String content;

    public File(String name) {
        super(name);
        this.content = "";
    }

    /**
     * ❓ Interview Q&A:
     * Q: Why update modified time on write?
     * A: This helps track changes for synchronization, backup, and audit purposes.
     */
    public void setContent(String content) {
        this.content = content;
        this.modifiedAt = LocalDateTime.now();
    }

    public String getContent() {
        return content;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public void display(int depth) {
        String indent = " ".repeat(depth * 2);
        System.out.println(indent + "📄 " + name);
    }
}

// Directory node
/**
 * ❓ Interview Q&A:
 * Q: Why manage children in a map?
 * A: A map allows fast lookup by name, ensuring unique children and efficient traversal.
 */
class Directory extends FileSystemNode {
    private Map<String, FileSystemNode> children;

    public Directory(String name) {
        super(name);
        this.children = new HashMap<>();
    }

    /**
     * ❓ Interview Q&A:
     * Q: Why check for duplicate names when adding a child?
     * A: File systems enforce unique names within a directory to avoid ambiguity.
     */
    public boolean addChild(FileSystemNode node) {
        if (children.containsKey(node.getName())) return false;
        children.put(node.getName(), node);
        this.modifiedAt = LocalDateTime.now();
        return true;
    }

    public FileSystemNode getChild(String name) {
        return children.get(name);
    }

    public boolean removeChild(String name) {
        if (children.containsKey(name)) {
            children.remove(name);
            this.modifiedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public Collection<FileSystemNode> getChildren() {
        return children.values();
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public void display(int depth) {
        String indent = " ".repeat(depth * 2);
        System.out.println(indent + "📁 " + name + " (" + children.size() + " items)");
        for (FileSystemNode node : children.values()) {
            node.display(depth + 1);
        }
    }
}

// FileSystem class
/**
 * ❓ Interview Q&A:
 * Q: Why centralize file system logic here instead of spreading it across nodes?
 * A: It simplifies path parsing, error handling, and operation consistency.
 */
class FileSystem {
    private Directory root;

    public FileSystem() {
        this.root = new Directory("/");
    }

    private String[] parsePath(String path) {
        if (path.equals("/")) return new String[]{"/"};
        return path.split("/");
    }

    /**
     * ❓ Interview Q&A:
     * Q: Why have a navigation method?
     * A: Navigating the tree structure is a common operation; abstracting it avoids repeating logic.
     */
    private FileSystemNode navigate(String path, boolean createMissingDirs, boolean isFileCreation) {
        String[] components = parsePath(path);
        Directory current = root;

        for (int i = 1; i < components.length; i++) {
            String name = components[i];
            if (name.isEmpty()) continue;

            FileSystemNode node = current.getChild(name);
            if (i == components.length - 1) {
                return current;
            }

            if (node == null) {
                if (createMissingDirs) {
                    Directory newDir = new Directory(name);
                    current.addChild(newDir);
                    current = newDir;
                } else {
                    return null;
                }
            } else if (!node.isFile()) {
                current = (Directory) node;
            } else {
                return null;
            }
        }
        return current;
    }

    /**
     * ❓ Interview Q&A:
     * Q: Why separate getNode and navigate methods?
     * A: getNode is used for accessing existing paths, while navigate supports path creation and partial traversal.
     */
    public FileSystemNode getNode(String path) {
        String[] components = parsePath(path);
        FileSystemNode current = root;
        for (int i = 1; i < components.length; i++) {
            String name = components[i];
            if (name.isEmpty()) continue;
            if (!(current instanceof Directory)) {
                return null;
            }
            Directory dir = (Directory) current;
            current = dir.getChild(name);
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    public boolean create(String path) {
        FileSystemNode parent = navigate(path, true, true);
        if (parent == null || parent.isFile()) return false;

        String[] parts = parsePath(path);
        String name = parts[parts.length - 1];

        if (((Directory) parent).getChild(name) != null) return false;

        FileSystemNode newNode;
        if (name.contains(".")) {
            newNode = new File(name);
        } else {
            newNode = new Directory(name);
        }
        return ((Directory) parent).addChild(newNode);
    }

    public boolean delete(String path) {
        if (path.equals("/")) return false;
        FileSystemNode parent = navigate(path, false, false);
        if (parent == null || parent.isFile()) return false;

        String[] parts = parsePath(path);
        String name = parts[parts.length - 1];

        return ((Directory) parent).removeChild(name);
    }

    public boolean write(String path, String content) {
        FileSystemNode node = getNode(path);
        if (node == null || !node.isFile()) {
            return false;
        }
        File file = (File) node;
        file.setContent(content);
        return true;
    }

    /**
     * ❓ Interview Q&A:
     * Q: Why use navigate instead of getNode for reading?
     * A: Using navigate ensures that the parent directory is accessed even if the last component might be missing or malformed.
     */
    public String read(String path) {
        FileSystemNode node = getNode(path);
        if (node != null && node.isFile()) {
            return ((File) node).getContent();
        }
        return null;
    }

    public void display() {
        root.display(0);
    }
}

// Client class
/**
 * ❓ Interview Q&A:
 * Q: Why include example operations in the client?
 * A: Demonstrates typical use cases and helps test edge cases like invalid paths or overwriting existing files.
 */
public class FileSystemClient {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem();

        System.out.println("Creating /docs: " + fs.create("/docs"));
        System.out.println("Creating /docs/note.txt: " + fs.create("/docs/note.txt"));
        System.out.println("Writing to /docs/note.txt: " + fs.write("/docs/note.txt", "Hello File System"));
        System.out.println("Reading from /docs/note.txt: " + fs.read("/docs/note.txt"));

        System.out.println("\nFile System Structure:");
        fs.display();

        System.out.println("\nDeleting /docs/note.txt: " + fs.delete("/docs/note.txt"));

        System.out.println("\nFile System Structure After Deletion:");
        fs.display();
    }
}
