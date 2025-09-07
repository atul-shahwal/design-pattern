package org.desingpatterns.questions.games.chess;

import java.util.ArrayList;
import java.util.Scanner;

// Design Patterns Used:
// 1. Singleton - Board class
// 2. Factory - Piece creation
// 3. Strategy - Movement strategies
// 4. Observer - Game event tracking

enum Status {
    ACTIVE, SAVED, BLACK_WIN, WHITE_WIN, STALEMATE
}

interface MovementStrategy {
    boolean canMove(Board board, Cell startCell, Cell endCell);
}

class KingMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        int rowDiff = Math.abs(startCell.getRow() - endCell.getRow());
        int colDiff = Math.abs(startCell.getCol() - endCell.getCol());
        return (rowDiff <= 1 && colDiff <= 1);
    }
}

class QueenMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        return new RookMovementStrategy().canMove(board, startCell, endCell) ||
                new BishopMovementStrategy().canMove(board, startCell, endCell);
    }
}

class RookMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        return startCell.getRow() == endCell.getRow() ||
                startCell.getCol() == endCell.getCol();
    }
}

class BishopMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        return Math.abs(startCell.getRow() - endCell.getRow()) ==
                Math.abs(startCell.getCol() - endCell.getCol());
    }
}

class KnightMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        int rowDiff = Math.abs(startCell.getRow() - endCell.getRow());
        int colDiff = Math.abs(startCell.getCol() - endCell.getCol());
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }
}

class PawnMovementStrategy implements MovementStrategy {
    @Override
    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        // Implementation simplified for brevity
        return true;
    }
}

abstract class Piece {
    private boolean isWhitePiece;
    private boolean killed = false;
    private MovementStrategy movementStrategy;

    public Piece(boolean isWhitePiece, MovementStrategy movementStrategy) {
        this.isWhitePiece = isWhitePiece;
        this.movementStrategy = movementStrategy;
    }

    public boolean isWhite() { return isWhitePiece; }
    public boolean isKilled() { return killed; }
    public void setKilled(boolean killed) { this.killed = killed; }

    public boolean canMove(Board board, Cell startCell, Cell endCell) {
        return movementStrategy.canMove(board, startCell, endCell);
    }
}

class King extends Piece {
    public King(boolean isWhitePiece) {
        super(isWhitePiece, new KingMovementStrategy());
    }
}

class Queen extends Piece {
    public Queen(boolean isWhitePiece) {
        super(isWhitePiece, new QueenMovementStrategy());
    }
}

class Bishop extends Piece {
    public Bishop(boolean isWhitePiece) {
        super(isWhitePiece, new BishopMovementStrategy());
    }
}

class Knight extends Piece {
    public Knight(boolean isWhitePiece) {
        super(isWhitePiece, new KnightMovementStrategy());
    }
}

class Rook extends Piece {
    public Rook(boolean isWhitePiece) {
        super(isWhitePiece, new RookMovementStrategy());
    }
}

class Pawn extends Piece {
    public Pawn(boolean isWhitePiece) {
        super(isWhitePiece, new PawnMovementStrategy());
    }
}

class PieceFactory {
    public static Piece createPiece(String pieceType, boolean isWhitePiece) {
        switch (pieceType.toLowerCase()) {
            case "king": return new King(isWhitePiece);
            case "queen": return new Queen(isWhitePiece);
            case "bishop": return new Bishop(isWhitePiece);
            case "knight": return new Knight(isWhitePiece);
            case "rook": return new Rook(isWhitePiece);
            case "pawn": return new Pawn(isWhitePiece);
            default: throw new IllegalArgumentException("Unknown piece type: " + pieceType);
        }
    }
}

class Cell {
    private int row, col;
    private Piece piece;

    public Cell(int row, int col, Piece piece) {
        this.row = row;
        this.col = col;
        this.piece = piece;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public Piece getPiece() { return piece; }
    public void setPiece(Piece piece) { this.piece = piece; }
}

class Move {
    private Cell startCell;
    private Cell endCell;

    public Move(Cell startCell, Cell endCell) {
        this.startCell = startCell;
        this.endCell = endCell;
    }

    public boolean isValid() {
        Piece startPiece = startCell.getPiece();
        Piece endPiece = endCell.getPiece();
        return startPiece != null &&
                (endPiece == null || startPiece.isWhite() != endPiece.isWhite());
    }

    public Cell getStartCell() { return startCell; }
    public Cell getEndCell() { return endCell; }
}

class Board {
    private static Board instance;
    private Cell[][] board;
    private int size;

    private Board(int size) {
        this.size = size;
        initializeBoard(size);
    }

    public static Board getInstance(int size) {
        if (instance == null) {
            instance = new Board(size);
        }
        return instance;
    }

    private void initializeBoard(int size) {
        board = new Cell[size][size];
        // Initialize pieces
        setPieceRow(0, false);
        setPawnRow(1, false);
        setPieceRow(size-1, true);
        setPawnRow(size-2, true);

        // Initialize empty cells
        for (int i = 2; i < size-2; i++) {
            for (int j = 0; j < size; j++) {
                board[i][j] = new Cell(i, j, null);
            }
        }
    }

    private void setPieceRow(int row, boolean isWhite) {
        board[row][0] = new Cell(row, 0, PieceFactory.createPiece("rook", isWhite));
        board[row][1] = new Cell(row, 1, PieceFactory.createPiece("knight", isWhite));
        board[row][2] = new Cell(row, 2, PieceFactory.createPiece("bishop", isWhite));
        board[row][3] = new Cell(row, 3, PieceFactory.createPiece("queen", isWhite));
        board[row][4] = new Cell(row, 4, PieceFactory.createPiece("king", isWhite));
        board[row][5] = new Cell(row, 5, PieceFactory.createPiece("bishop", isWhite));
        board[row][6] = new Cell(row, 6, PieceFactory.createPiece("knight", isWhite));
        board[row][7] = new Cell(row, 7, PieceFactory.createPiece("rook", isWhite));
    }

    private void setPawnRow(int row, boolean isWhite) {
        for (int j = 0; j < size; j++) {
            board[row][j] = new Cell(row, j, PieceFactory.createPiece("pawn", isWhite));
        }
    }

    public Cell getCell(int row, int col) {
        if (row < 0 || row >= size || col < 0 || col >= size) return null;
        return board[row][col];
    }

    public int getSize() { return size; }
}

class Player {
    private String name;
    private boolean isWhiteSide;

    public Player(String name, boolean isWhiteSide) {
        this.name = name;
        this.isWhiteSide = isWhiteSide;
    }

    public String getName() { return name; }
    public boolean isWhiteSide() { return isWhiteSide; }
}

interface GameEventListener {
    void onMoveMade(Move move);
    void onGameStateChanged(Status state);
}

class ConsoleGameEventListener implements GameEventListener {
    @Override
    public void onMoveMade(Move move) {
        System.out.println("Move made from (" + move.getStartCell().getRow() +
                "," + move.getStartCell().getCol() + ") to (" +
                move.getEndCell().getRow() + "," + move.getEndCell().getCol() + ")");
    }

    @Override
    public void onGameStateChanged(Status state) {
        System.out.println("Game state changed to: " + state);
    }
}

class ChessGame {
    private Board board;
    private Player player1;
    private Player player2;
    private boolean isWhiteTurn;
    private ArrayList<Move> gameLog;
    private Status status;
    private GameEventListener listener;

    public ChessGame(Player player1, Player player2) {
        this.player1 = player1;
        this.player2 = player2;
        this.board = Board.getInstance(8);
        this.isWhiteTurn = true;
        this.status = Status.ACTIVE;
        this.gameLog = new ArrayList<>();
    }

    public void setObserver(GameEventListener listener) {
        this.listener = listener;
    }

    public void start() {
        Scanner scanner = new Scanner(System.in);
        while (status == Status.ACTIVE) {
            System.out.println(isWhiteTurn ? "White's turn" : "Black's turn");
            System.out.print("Enter start row and column: ");
            int startRow = scanner.nextInt();
            int startCol = scanner.nextInt();
            System.out.print("Enter end row and column: ");
            int endRow = scanner.nextInt();
            int endCol = scanner.nextInt();

            Cell startCell = board.getCell(startRow, startCol);
            Cell endCell = board.getCell(endRow, endCol);

            if (startCell == null || endCell == null) {
                System.out.println("Invalid cells!");
                continue;
            }

            makeMove(new Move(startCell, endCell));
        }
        scanner.close();
    }

    private void makeMove(Move move) {
        if (!move.isValid()) {
            System.out.println("Invalid move!");
            return;
        }

        Piece sourcePiece = move.getStartCell().getPiece();
        if (!sourcePiece.canMove(board, move.getStartCell(), move.getEndCell())) {
            System.out.println("Illegal move for this piece!");
            return;
        }

        // Execute move
        Piece capturedPiece = move.getEndCell().getPiece();
        if (capturedPiece != null) {
            capturedPiece.setKilled(true);
            if (capturedPiece instanceof King) {
                status = sourcePiece.isWhite() ? Status.WHITE_WIN : Status.BLACK_WIN;
                if (listener != null) listener.onGameStateChanged(status);
                return;
            }
        }

        move.getEndCell().setPiece(sourcePiece);
        move.getStartCell().setPiece(null);
        gameLog.add(move);
        if (listener != null) listener.onMoveMade(move);

        isWhiteTurn = !isWhiteTurn;
    }
}

public class ChessMain {
    public static void main(String[] args) {
        Player whitePlayer = new Player("Alice", true);
        Player blackPlayer = new Player("Bob", false);

        ChessGame game = new ChessGame(whitePlayer, blackPlayer);
        game.setObserver(new ConsoleGameEventListener());
        game.start();
    }
}