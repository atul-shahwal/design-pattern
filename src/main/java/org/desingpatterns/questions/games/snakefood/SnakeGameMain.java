package org.desingpatterns.questions.games.snakefood;

import java.util.*;

// Pair class for representing coordinates
class Pair {
    private final int row;
    private final int col;

    public Pair(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pair pair = (Pair) o;
        return row == pair.row && col == pair.col;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row, col);
    }
}

// Singleton GameBoard class
class GameBoard {
    private static GameBoard instance;
    private final int width;
    private final int height;

    private GameBoard(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static GameBoard getInstance(int width, int height) {
        if (instance == null) {
            instance = new GameBoard(width, height);
        }
        return instance;
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }
}

// Strategy Pattern for movement
interface MovementStrategy {
    Pair getNextPosition(Pair currentHead, String direction);
}

class HumanMovementStrategy implements MovementStrategy {
    @Override
    public Pair getNextPosition(Pair currentHead, String direction) {
        int row = currentHead.getRow();
        int col = currentHead.getCol();
        switch (direction) {
            case "U": return new Pair(row - 1, col);
            case "D": return new Pair(row + 1, col);
            case "L": return new Pair(row, col - 1);
            case "R": return new Pair(row, col + 1);
            default: return currentHead;
        }
    }
}

// Factory Pattern for food
abstract class FoodItem {
    protected int row, col;
    protected int points;

    public FoodItem(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public int getPoints() { return points; }
}

class NormalFood extends FoodItem {
    public NormalFood(int row, int col) {
        super(row, col);
        this.points = 1;
    }
}

class BonusFood extends FoodItem {
    public BonusFood(int row, int col) {
        super(row, col);
        this.points = 3;
    }
}

class FoodFactory {
    public static FoodItem createFood(int[] position, String type) {
        return "bonus".equals(type) ?
                new BonusFood(position[0], position[1]) :
                new NormalFood(position[0], position[1]);
    }
}

// Observer Pattern
interface GameObserver {
    void onMoveMade(Pair newHead);
    void onFoodEaten(int foodIndex, int newScore);
    void onGameOver(int finalScore);
}

class ConsoleGameObserver implements GameObserver {
    @Override
    public void onMoveMade(Pair newHead) {
        System.out.println("Moved to: [" + newHead.getRow() + ", " + newHead.getCol() + "]");
    }

    @Override
    public void onFoodEaten(int foodIndex, int newScore) {
        System.out.println("Food eaten! Score: " + newScore);
    }

    @Override
    public void onGameOver(int finalScore) {
        System.out.println("Game Over! Final score: " + finalScore);
    }
}

// Main Game Controller
class SnakeGame {
    private final GameBoard board;
    private final Deque<Pair> snake;
    private final Map<Pair, Boolean> snakeMap;
    private final int[][] food;
    private int foodIndex;
    private MovementStrategy movementStrategy;
    private final List<GameObserver> observers;
    private final boolean hasWalls;

    public SnakeGame(int width, int height, int[][] food, boolean hasWalls) {
        this.board = GameBoard.getInstance(width, height);
        this.food = food;
        this.hasWalls = hasWalls;
        this.snake = new LinkedList<>();
        this.snakeMap = new HashMap<>();
        this.observers = new ArrayList<>();
        this.movementStrategy = new HumanMovementStrategy();

        Pair initialPos = new Pair(0, 0);
        snake.addFirst(initialPos);
        snakeMap.put(initialPos, true);
    }

    public SnakeGame(int width, int height, int[][] food) {
        this(width, height, food, true);
    }

    public void setMovementStrategy(MovementStrategy strategy) {
        this.movementStrategy = strategy;
    }

    public void addObserver(GameObserver observer) {
        observers.add(observer);
    }

    private Pair wrapPosition(int row, int col) {
        int newRow = row;
        int newCol = col;
        if (row < 0) newRow = board.getHeight() - 1;
        else if (row >= board.getHeight()) newRow = 0;
        if (col < 0) newCol = board.getWidth() - 1;
        else if (col >= board.getWidth()) newCol = 0;
        return new Pair(newRow, newCol);
    }

    public int move(String direction) {
        Pair currentHead = snake.peekFirst();
        Pair newHead = movementStrategy.getNextPosition(currentHead, direction);

        if (!hasWalls) {
            newHead = wrapPosition(newHead.getRow(), newHead.getCol());
        }

        boolean boundaryCollision = newHead.getRow() < 0 || newHead.getRow() >= board.getHeight() ||
                newHead.getCol() < 0 || newHead.getCol() >= board.getWidth();
        if (hasWalls && boundaryCollision) {
            notifyGameOver(snake.size() - 1);
            return -1;
        }

        Pair tail = snake.peekLast();
        boolean selfCollision = snakeMap.containsKey(newHead) && !newHead.equals(tail);
        if (selfCollision) {
            notifyGameOver(snake.size() - 1);
            return -1;
        }

        boolean ateFood = foodIndex < food.length &&
                newHead.getRow() == food[foodIndex][0] &&
                newHead.getCol() == food[foodIndex][1];

        if (ateFood) {
            foodIndex++;
            notifyFoodEaten(foodIndex - 1, snake.size());
        } else {
            snake.pollLast();
            snakeMap.remove(tail);
        }

        snake.addFirst(newHead);
        snakeMap.put(newHead, true);
        notifyMoveMade(newHead);

        return snake.size() - 1;
    }

    private void notifyMoveMade(Pair newHead) {
        for (GameObserver o : observers) o.onMoveMade(newHead);
    }

    private void notifyFoodEaten(int index, int score) {
        for (GameObserver o : observers) o.onFoodEaten(index, score);
    }

    private void notifyGameOver(int score) {
        for (GameObserver o : observers) o.onGameOver(score);
    }

    // Getters for testing
    public Deque<Pair> getSnake() { return new LinkedList<>(snake); }
    public int getFoodIndex() { return foodIndex; }
}

// Main class with testing code
public class SnakeGameMain {
    public static void main(String[] args) {
        // Define game configuration
        int width = 20;
        int height = 15;
        // Define some food positions (more can be generated during gameplay)
        int[][] foodPositions = {
                {0, 1}, // Initial food
                {10, 8}, // Second food
                {3, 12}, // Third food
                {8, 17}, // Fourth food
                {12, 3}  // Fifth food
        };
        // Initialize the game
        SnakeGame game = new SnakeGame(width, height, foodPositions);
        // Add observer
        game.addObserver(new ConsoleGameObserver());

        // Display game instructions
        System.out.println("===== SNAKE GAME =====");
        System.out.println("Controls: W (Up), S (Down), A (Left), D (Right), Q (Quit)");
        System.out.println("Eat food to grow your snake and increase your score.");
        System.out.println("Don't hit the walls or bite yourself!");
        System.out.println("=======================");

        // Create scanner for user input
        Scanner scanner = new Scanner(System.in);
        boolean gameRunning = true;
        int score = 0;

        // Main game loop
        while (gameRunning) {
            // Display current game state
            displayGameState(game);

            // Get user input
            System.out.print("Enter move (W/A/S/D) or Q to quit: ");
            String input = scanner.nextLine().toUpperCase();

            // Handle quit command
            if (input.equals("Q")) {
                System.out.println("Game ended by player. Final score: " + score);
                gameRunning = false;
                continue;
            }

            // Convert WASD input to UDLR for game processing
            String direction = convertInput(input);

            // Skip invalid inputs
            if (direction.isEmpty()) {
                System.out.println("Invalid input! Use W/A/S/D to move or Q to quit.");
                continue;
            }

            // Make the move and get the new score
            score = game.move(direction);

            // Check for game over
            if (score == -1) {
                System.out.println("GAME OVER! You hit a wall or bit yourself.");
                System.out.println("Final score: " + (game.getSnake().size() - 1));
                gameRunning = false;
            } else {
                System.out.println("Score: " + score);
            }
        }
        scanner.close();
        System.out.println("Thanks for playing!");
    }

    // Convert user-friendly WASD input to UDLR for the game engine
    private static String convertInput(String input) {
        switch (input) {
            case "W": return "U"; // Up
            case "S": return "D"; // Down
            case "A": return "L"; // Left
            case "D": return "R"; // Right
            default: return "";   // Invalid input
        }
    }

    // A simple method to display the game state in the console
    private static void displayGameState(SnakeGame game) {
        System.out.println("\nCurrent snake length: " + game.getSnake().size());
        // Display next food location if available
        if (game.getFoodIndex() < 5) { // We know we have 5 food positions
            System.out.println("Next food at: [" +
                    foodPositions[game.getFoodIndex()][0] + ", " +
                    foodPositions[game.getFoodIndex()][1] + "]");
        }
    }

    // Static reference to food positions for display
    private static int[][] foodPositions = {
            {0, 1}, {10, 8}, {3, 12}, {8, 17}, {12, 3}
    };
}