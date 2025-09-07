package org.desingpatterns.questions.games.tictactoe;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

// Enum for player symbols
enum Symbol {
    X, O, EMPTY
}

// Position class to represent board coordinates
class Position {
    public int row;
    public int col;

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    @Override
    public String toString() {
        return "(" + row + ", " + col + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Position)) return false;
        Position other = (Position) obj;
        return this.row == other.row && this.col == other.col;
    }

    @Override
    public int hashCode() {
        return 31 * row + col;
    }
}

// Strategy interface for player moves
interface PlayerStrategy {
    Position makeMove(Board board);
}

// Human player strategy implementation
class HumanPlayerStrategy implements PlayerStrategy {
    private Scanner scanner;
    private String playerName;

    public HumanPlayerStrategy(String playerName) {
        this.playerName = playerName;
        this.scanner = new Scanner(System.in);
    }

    @Override
    public Position makeMove(Board board) {
        while (true) {
            System.out.printf("%s, enter your move (row [0-2] and column [0-2]): ", playerName);
            try {
                int row = scanner.nextInt();
                int col = scanner.nextInt();
                Position move = new Position(row, col);

                if (board.isValidMove(move)) {
                    return move;
                }

                System.out.println("Invalid move. Try again.");
            } catch (Exception e) {
                System.out.println("Invalid input. Please enter row and column as numbers.");
                scanner.nextLine(); // Clear input buffer
            }
        }
    }
}

// Game state interface
interface GameState {
    void next(GameContext context, Player player, boolean hasWon);
    boolean isGameOver();
}

// Concrete game states
class XTurnState implements GameState {
    @Override
    public void next(GameContext context, Player player, boolean hasWon) {
        if (hasWon) {
            context.setState(player.getSymbol() == Symbol.X ? new XWonState() : new OWonState());
        } else {
            context.setState(new OTurnState());
        }
    }

    @Override
    public boolean isGameOver() {
        return false;
    }
}

class OTurnState implements GameState {
    @Override
    public void next(GameContext context, Player player, boolean hasWon) {
        if (hasWon) {
            context.setState(player.getSymbol() == Symbol.X ? new XWonState() : new OWonState());
        } else {
            context.setState(new XTurnState());
        }
    }

    @Override
    public boolean isGameOver() {
        return false;
    }
}

class XWonState implements GameState {
    @Override
    public void next(GameContext context, Player player, boolean hasWon) {
        // Game over, no next state
    }

    @Override
    public boolean isGameOver() {
        return true;
    }
}

class OWonState implements GameState {
    @Override
    public void next(GameContext context, Player player, boolean hasWon) {
        // Game over, no next state
    }

    @Override
    public boolean isGameOver() {
        return true;
    }
}

class DrawState implements GameState {
    @Override
    public void next(GameContext context, Player player, boolean hasWon) {
        // Game over, no next state
    }

    @Override
    public boolean isGameOver() {
        return true;
    }
}

// Game context to manage state
class GameContext {
    private GameState currentState;

    public GameContext() {
        currentState = new XTurnState(); // Start with X's turn
    }

    public void setState(GameState state) {
        this.currentState = state;
    }

    public void next(Player player, boolean hasWon) {
        currentState.next(this, player, hasWon);
    }

    public boolean isGameOver() {
        return currentState.isGameOver();
    }

    public GameState getCurrentState() {
        return currentState;
    }
}

// Player class
class Player {
    private Symbol symbol;
    private PlayerStrategy playerStrategy;

    public Player(Symbol symbol, PlayerStrategy playerStrategy) {
        this.symbol = symbol;
        this.playerStrategy = playerStrategy;
    }

    public Symbol getSymbol() {
        return symbol;
    }

    public PlayerStrategy getPlayerStrategy() {
        return playerStrategy;
    }
}

// Board class
class Board {
    private final int rows;
    private final int columns;
    private Symbol[][] grid;
    private List<GameEventListener> listeners;

    public Board(int rows, int columns) {
        this.rows = rows;
        this.columns = columns;
        grid = new Symbol[rows][columns];
        listeners = new ArrayList<>();

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                grid[i][j] = Symbol.EMPTY;
            }
        }
    }

    public boolean isValidMove(Position pos) {
        return pos.row >= 0 && pos.row < rows &&
                pos.col >= 0 && pos.col < columns &&
                grid[pos.row][pos.col] == Symbol.EMPTY;
    }

    public void makeMove(Position pos, Symbol symbol) {
        grid[pos.row][pos.col] = symbol;
        notifyMoveMade(pos, symbol);
    }

    public void checkGameState(GameContext context, Player currentPlayer) {
        // Check rows
        for (int i = 0; i < rows; i++) {
            if (grid[i][0] != Symbol.EMPTY && isWinningLine(grid[i])) {
                context.next(currentPlayer, true);
                notifyGameStateChanged(context.getCurrentState());
                return;
            }
        }

        // Check columns
        for (int i = 0; i < columns; i++) {
            Symbol[] column = new Symbol[rows];
            for (int j = 0; j < rows; j++) {
                column[j] = grid[j][i];
            }
            if (column[0] != Symbol.EMPTY && isWinningLine(column)) {
                context.next(currentPlayer, true);
                notifyGameStateChanged(context.getCurrentState());
                return;
            }
        }

        // Check diagonals
        Symbol[] diagonal1 = new Symbol[Math.min(rows, columns)];
        Symbol[] diagonal2 = new Symbol[Math.min(rows, columns)];
        for (int i = 0; i < Math.min(rows, columns); i++) {
            diagonal1[i] = grid[i][i];
            diagonal2[i] = grid[i][columns - 1 - i];
        }

        if (diagonal1[0] != Symbol.EMPTY && isWinningLine(diagonal1)) {
            context.next(currentPlayer, true);
            notifyGameStateChanged(context.getCurrentState());
            return;
        }

        if (diagonal2[0] != Symbol.EMPTY && isWinningLine(diagonal2)) {
            context.next(currentPlayer, true);
            notifyGameStateChanged(context.getCurrentState());
            return;
        }

        // Check for draw
        boolean isFull = true;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (grid[i][j] == Symbol.EMPTY) {
                    isFull = false;
                    break;
                }
            }
            if (!isFull) break;
        }

        if (isFull) {
            context.setState(new DrawState());
            notifyGameStateChanged(context.getCurrentState());
        }
    }

    private boolean isWinningLine(Symbol[] line) {
        Symbol first = line[0];
        for (Symbol s : line) {
            if (s != first) {
                return false;
            }
        }
        return true;
    }

    public void printBoard() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                Symbol symbol = grid[i][j];
                switch (symbol) {
                    case X:
                        System.out.print(" X ");
                        break;
                    case O:
                        System.out.print(" O ");
                        break;
                    case EMPTY:
                    default:
                        System.out.print(" . ");
                }
                if (j < columns - 1) {
                    System.out.print("|");
                }
            }
            System.out.println();
            if (i < rows - 1) {
                System.out.println("---+---+---");
            }
        }
        System.out.println();
    }

    public void addListener(GameEventListener listener) {
        listeners.add(listener);
    }

    private void notifyMoveMade(Position position, Symbol symbol) {
        for (GameEventListener listener : listeners) {
            listener.onMoveMade(position, symbol);
        }
    }

    private void notifyGameStateChanged(GameState state) {
        for (GameEventListener listener : listeners) {
            listener.onGameStateChanged(state);
        }
    }
}

// Event listener interface
interface GameEventListener {
    void onMoveMade(Position position, Symbol symbol);
    void onGameStateChanged(GameState state);
}

// Console event listener implementation
class ConsoleGameEventListener implements GameEventListener {
    @Override
    public void onMoveMade(Position position, Symbol symbol) {
        System.out.println("Move made at position: " + position + " by " + symbol);
    }

    @Override
    public void onGameStateChanged(GameState state) {
        System.out.println("Game state changed to: " + state.getClass().getSimpleName());
    }
}

// Main game class
public class TicTacToeGame {
    private Board board;
    private Player playerX;
    private Player playerO;
    private Player currentPlayer;
    private GameContext gameContext;

    public TicTacToeGame(PlayerStrategy xStrategy, PlayerStrategy oStrategy, int rows, int columns) {
        board = new Board(rows, columns);
        playerX = new Player(Symbol.X, xStrategy);
        playerO = new Player(Symbol.O, oStrategy);
        currentPlayer = playerX;
        gameContext = new GameContext();

        // Add event listener
        board.addListener(new ConsoleGameEventListener());
    }

    public void play() {
        System.out.println("Welcome to Tic Tac Toe!");
        System.out.println("Player X goes first.\n");

        do {
            board.printBoard();
            Position move = currentPlayer.getPlayerStrategy().makeMove(board);
            board.makeMove(move, currentPlayer.getSymbol());
            board.checkGameState(gameContext, currentPlayer);
            switchPlayer();
        } while (!gameContext.isGameOver());

        board.printBoard();
        announceResult();
    }

    private void switchPlayer() {
        currentPlayer = (currentPlayer == playerX) ? playerO : playerX;
    }

    private void announceResult() {
        GameState state = gameContext.getCurrentState();
        if (state instanceof XWonState) {
            System.out.println("Player X wins!");
        } else if (state instanceof OWonState) {
            System.out.println("Player O wins!");
        } else {
            System.out.println("It's a draw!");
        }
    }

    public static void main(String[] args) {
        PlayerStrategy playerXStrategy = new HumanPlayerStrategy("Player X");
        PlayerStrategy playerOStrategy = new HumanPlayerStrategy("Player O");
        TicTacToeGame game = new TicTacToeGame(playerXStrategy, playerOStrategy, 3, 3);
        game.play();
    }
}
