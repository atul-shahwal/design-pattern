package org.desingpatterns.questions.filesystem;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 🎯 Problem Statement: Low-Level Design - File System
 *
 * Design a file system that supports creating files and directories, reading/writing content, and deleting paths.
 * The system should use composite pattern for nodes and command pattern for operations, with a client interface.
 *
 * ✅ Requirements:
 * - Support hierarchical structure with files and directories.
 * - Allow create, read, write, and delete operations on paths.
 * - Use composite pattern to represent file system nodes.
 * - Use command pattern to encapsulate operations as objects.
 * - Provide a client for user interaction via commands.
 *
 * 📦 Key Components:
 * - FileSystemNode abstract class for files and directories.
 * - File and Directory concrete classes.
 * - FileSystem class to manage the root and operations.
 * - Command interface and concrete commands (Create, Write, etc.).
 * - CommandFactory to parse and create commands.
 *
 * 🚀 Example Flow:
 * 1. User creates a directory path "/docs" → system creates directory.
 * 2. User creates a file "/docs/note.txt" → system creates file.
 * 3. User writes "Hello" to the file → content is stored.
 * 4. User reads the file → content "Hello" is displayed.
 * 5. User deletes the file or directory → path is removed.
 */

// Base class for File System Node (Composite Pattern)
abstract class FileSystemNode {
    private String name;
    private Map<String, FileSystemNode> children;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public FileSystemNode(String name) {
        this.name = name;
        this.children = new HashMap<>();
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }

    public void addChild(String name, FileSystemNode child) {
        this.children.put(name, child);
        this.modifiedAt = LocalDateTime.now();
    }

    public boolean hasChild(String name) {
        return this.children.containsKey(name);
    }

    public FileSystemNode getChild(String name) {
        return this.children.get(name);
    }

    public boolean removeChild(String name) {
        if (hasChild(name)) {
            children.remove(name);
            this.modifiedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }

    public abstract boolean isFile();
    public abstract void display(int depth);

    public String getName() {
        return name;
    }

    public Collection<FileSystemNode> getChildren() {
        return children.values();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }

    protected void updateModifiedTime() {
        this.modifiedAt = LocalDateTime.now();
    }
}

class File extends FileSystemNode {
    private String content;
    private String extension;

    public File(String name) {
        super(name);
        this.extension = extractExtension(name);
    }

    private String extractExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return (dotIndex > 0) ? name.substring(dotIndex + 1) : "";
    }

    public void setContent(String content) {
        this.content = content;
        updateModifiedTime();
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
        System.out.println(indent + "📄 " + getName());
    }
}

class Directory extends FileSystemNode {
    public Directory(String name) {
        super(name);
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public void display(int depth) {
        String indent = " ".repeat(depth * 2);
        System.out.println(indent + "📁 " + getName() + " (" + getChildren().size() + " items)");

        for (FileSystemNode child : getChildren()) {
            child.display(depth + 1);
        }
    }
}

class FileSystem {
    private FileSystemNode root;

    public FileSystem() {
        this.root = new Directory("/");
    }

    public boolean isValidFilePath(String path) {
        return path != null && !path.isEmpty() && path.startsWith("/");
    }

    public boolean createPath(String path) {
        if (!isValidFilePath(path))
            return false;

        String[] pathComponents = path.split("/");

        if (pathComponents.length == 0)
            return false;

        FileSystemNode current = root;

        for (int i = 1; i < pathComponents.length - 1; i++) {
            String component = pathComponents[i];
            if (component.isEmpty())
                continue;

            if (!current.hasChild(component)) {
                FileSystemNode newDir = new Directory(component);
                current.addChild(component, newDir);
            }

            FileSystemNode child = current.getChild(component);
            if (child.isFile()) {
                return false;
            }
            current = child;
        }

        String lastComponent = pathComponents[pathComponents.length - 1];
        if (lastComponent.isEmpty())
            return false;

        if (current.hasChild(lastComponent)) {
            return false;
        }

        FileSystemNode newNode;
        if (lastComponent.contains(".")) {
            newNode = new File(lastComponent);
        } else {
            newNode = new Directory(lastComponent);
        }

        current.addChild(lastComponent, newNode);
        return true;
    }

    private FileSystemNode getNode(String path) {
        if (!isValidFilePath(path))
            return null;

        if (path.equals("/"))
            return root;

        String[] pathComponents = path.split("/");
        FileSystemNode current = root;

        for (int i = 1; i < pathComponents.length; i++) {
            String component = pathComponents[i];
            if (component.isEmpty())
                continue;

            if (!current.hasChild(component)) {
                return null;
            }
            current = current.getChild(component);
        }

        return current;
    }

    public boolean deletePath(String path) {
        if (!isValidFilePath(path) || path.equals("/"))
            return false;

        int lastSlash = path.lastIndexOf('/');
        String parentPath = (lastSlash > 0) ? path.substring(0, lastSlash) : "/";
        String lastComponent = path.substring(lastSlash + 1);

        FileSystemNode parent = getNode(parentPath);
        if (parent == null || parent.isFile())
            return false;

        if (!parent.hasChild(lastComponent)) {
            return false;
        }

        return parent.removeChild(lastComponent);
    }

    public void display() {
        root.display(0);
    }

    public boolean setFileContent(String path, String content) {
        FileSystemNode node = getNode(path);
        if (node == null || !node.isFile())
            return false;

        File file = (File) node;
        file.setContent(content);
        return true;
    }

    public String getFileContent(String path) {
        FileSystemNode node = getNode(path);
        if (node == null || !node.isFile())
            return null;

        File file = (File) node;
        return file.getContent();
    }
}

// Command Pattern Implementation
interface Command {
    void execute();
    boolean isValid();
}

abstract class FileSystemCommand implements Command {
    protected FileSystem fileSystem;
    protected String[] args;

    public FileSystemCommand(FileSystem fileSystem, String[] args) {
        this.fileSystem = fileSystem;
        this.args = args;
    }

    @Override
    public boolean isValid() {
        return true;
    }
}

class CreateCommand extends FileSystemCommand {
    public CreateCommand(FileSystem fileSystem, String[] args) {
        super(fileSystem, args);
    }

    @Override
    public boolean isValid() {
        return args.length >= 2;
    }

    @Override
    public void execute() {
        if (!isValid()) {
            System.out.println("Usage: create <path>");
            return;
        }

        String path = args[1];
        boolean isCreated = fileSystem.createPath(path);
        System.out.println(isCreated ? "Path created successfully" : "Failed to create path");
    }
}

class WriteCommand extends FileSystemCommand {
    public WriteCommand(FileSystem fileSystem, String[] args) {
        super(fileSystem, args);
    }

    @Override
    public boolean isValid() {
        return args.length >= 3;
    }

    @Override
    public void execute() {
        if (!isValid()) {
            System.out.println("Usage: write <path> <content>");
            return;
        }

        String path = args[1];
        String content = args[2];
        boolean isWritten = fileSystem.setFileContent(path, content);
        System.out.println(isWritten ? "Content written successfully" : "Failed to write content");
    }
}

class ReadCommand extends FileSystemCommand {
    public ReadCommand(FileSystem fileSystem, String[] args) {
        super(fileSystem, args);
    }

    @Override
    public boolean isValid() {
        return args.length >= 2;
    }

    @Override
    public void execute() {
        if (!isValid()) {
            System.out.println("Usage: read <path>");
            return;
        }

        String path = args[1];
        String content = fileSystem.getFileContent(path);
        if (content != null) {
            System.out.println("Content: " + content);
        } else {
            System.out.println("Failed to read content");
        }
    }
}

class DeleteCommand extends FileSystemCommand {
    public DeleteCommand(FileSystem fileSystem, String[] args) {
        super(fileSystem, args);
    }

    @Override
    public boolean isValid() {
        return args.length >= 2;
    }

    @Override
    public void execute() {
        if (!isValid()) {
            System.out.println("Usage: delete <path>");
            return;
        }

        String path = args[1];
        boolean isDeleted = fileSystem.deletePath(path);
        System.out.println(isDeleted ? "Path deleted successfully" : "Failed to delete path");
    }
}

class DisplayCommand extends FileSystemCommand {
    public DisplayCommand(FileSystem fileSystem, String[] args) {
        super(fileSystem, args);
    }

    @Override
    public void execute() {
        System.out.println("\nFile System Structure:");
        fileSystem.display();
    }
}

class ExitCommand extends FileSystemCommand {
    private boolean isRunning;

    public ExitCommand(FileSystem fileSystem, String[] args, boolean isRunning) {
        super(fileSystem, args);
        this.isRunning = isRunning;
    }

    @Override
    public void execute() {
        System.out.println("Exiting...");
        this.isRunning = false;
    }

    public boolean isRunning() {
        return isRunning;
    }
}

class CommandFactory {
    private FileSystem fileSystem;

    public CommandFactory(FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    public Command createCommand(String input, boolean isRunning) {
        String[] parts = input.split("\\s+", 3);
        if (parts.length == 0) return null;

        String commandType = parts[0].toLowerCase();
        String[] args = parts;

        switch (commandType) {
            case "create": return new CreateCommand(fileSystem, args);
            case "write": return new WriteCommand(fileSystem, args);
            case "read": return new ReadCommand(fileSystem, args);
            case "delete": return new DeleteCommand(fileSystem, args);
            case "display": return new DisplayCommand(fileSystem, args);
            case "exit": return new ExitCommand(fileSystem, args, isRunning);
            default: return null;
        }
    }
}

public class FileSystemClient {
    public static void main(String[] args) {
        FileSystem fs = new FileSystem();
        Scanner scanner = new Scanner(System.in);
        boolean isRunning = true;
        CommandFactory factory = new CommandFactory(fs);

        System.out.println("File System Manager - Commands:");
        System.out.println("1. create <path> - Create a new path");
        System.out.println("2. write <path> <content> - Write content to a file");
        System.out.println("3. read <path> - Read content from a file");
        System.out.println("4. delete <path> - Delete a path");
        System.out.println("5. display - Show the entire file system structure");
        System.out.println("6. exit - Exit the program");

        while (isRunning) {
            System.out.print("\nEnter command: ");
            String input = scanner.nextLine().trim();

            Command command = factory.createCommand(input, isRunning);

            if (command == null) {
                System.out.println("Unknown command. Available commands: create, write, read, delete, display, exit");
                continue;
            }

            if (command instanceof ExitCommand) {
                command.execute();
                isRunning = ((ExitCommand) command).isRunning();
            } else {
                command.execute();
            }
        }
        scanner.close();
    }
}
