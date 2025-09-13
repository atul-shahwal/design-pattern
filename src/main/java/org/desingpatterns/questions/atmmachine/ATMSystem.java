package org.desingpatterns.questions.atmmachine;

import java.util.HashMap;
import java.util.Map;

/**
 * 🎯 Problem Statement: Low-Level Design - ATM System
 *
 * Design an ATM system that handles user authentication, transaction processing, and cash dispensing.
 * The system should manage multiple states (idle, card inserted, PIN validated, etc.), validate user inputs,
 * and handle cash inventory securely and efficiently.
 *
 * ✅ Requirements:
 * - Support card insertion and PIN validation.
 * - Allow cash withdrawal and balance inquiry transactions.
 * - Manage cash inventory with different denominations.
 * - Handle state transitions smoothly (idle → has card → transaction → idle).
 * - Ensure secure authentication and transaction processing.
 *
 * 📦 Key Components:
 * - Card and Account classes for user data and validation.
 * - ATMInventory for cash denomination management.
 * - ATMState interface and concrete states (IdleState, HasCardState, etc.).
 * - ATMMachine class to orchestrate operations and state transitions.
 *
 * 🚀 Example Flow:
 * 1. User inserts card → system transitions to HasCardState.
 * 2. User enters PIN → system validates and moves to SelectOperationState.
 * 3. User selects withdrawal → system processes transaction in TransactionState.
 * 4. If successful, cash is dispensed, and card is ejected → system returns to IdleState.
 */




/**
 * CashType represents the available denominations in the ATM.
 * Each denomination is associated with a specific value.
 *
 * ▶ Cross Questions:
 * Q1: Why use an enum for denominations instead of constants?
 * A1: Enums provide type safety and are easier to maintain, especially when more denominations are added.
 *
 * Q2: Why store the value as an instance field?
 * A2: It allows easy access to the denomination value for calculations like inventory and dispensing.
 */
enum CashType {
    BILL_100(100), BILL_50(50), BILL_20(20), BILL_10(10), BILL_5(5), BILL_1(1);

    public final int value;

    CashType(int value) {
        this.value = value;
    }
}

/**
 * Represents a bank card with card number, PIN, and account association.
 * Handles PIN validation for secure authentication.
 *
 * ▶ Cross Questions:
 * Q1: Why is PIN stored inside the Card object instead of a separate service?
 * A1: For simplicity in this design. In a real-world system, PIN would be stored securely in a database or encrypted format.
 *
 * Q2: How would you enhance security if scaling this application?
 * A2: Implement encryption for PINs, use secure storage mechanisms, and apply multi-factor authentication.
 */
class Card {
    private String cardNumber;
    private int pin;
    private String accountNumber;

    public Card(String cardNumber, int pin, String accountNumber) {
        this.cardNumber = cardNumber;
        this.pin = pin;
        this.accountNumber = accountNumber;
    }

    public boolean validatePin(int enteredPin) {
        return this.pin == enteredPin;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}

/**
 * Represents a user's bank account with account number and balance.
 * Supports withdrawal and deposit operations ensuring funds consistency.
 *
 * ▶ Cross Questions:
 * Q1: How would you ensure thread safety if multiple ATMs access the same account?
 * A1: Implement locking mechanisms or transaction handling in the database layer to ensure atomicity.
 *
 * Q2: Why withdraw method checks balance first?
 * A2: To prevent overdraft and ensure that withdrawals don't exceed available funds.
 */
class Account {
    private String accountNumber;
    private double balance;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
    }

    public boolean withdraw(double amount) {
        if (balance >= amount) {
            balance -= amount;
            return true;
        }
        return false;
    }

    public void deposit(double amount) {
        balance += amount;
    }

    public double getBalance() {
        return balance;
    }

    public String getAccountNumber() {
        return accountNumber;
    }
}

/**
 * Manages ATM's cash inventory using different denominations.
 * Provides functionality to check available cash and dispense it during withdrawal.
 *
 * ▶ Cross Questions:
 * Q1: Why use a map to store denominations and their count?
 * A1: Allows dynamic management of cash quantities and efficient lookups by denomination.
 *
 * Q2: How would you handle cases where the ATM doesn't have exact denominations?
 * A2: Implement fallback strategies or error handling to cancel the transaction and refund the user.
 */

class ATMInventory {
    private Map<CashType, Integer> cashInventory;

    public ATMInventory() {
        cashInventory = new HashMap<>();
        initializeInventory();
    }

    private void initializeInventory() {
        cashInventory.put(CashType.BILL_100, 10);
        cashInventory.put(CashType.BILL_50, 10);
        cashInventory.put(CashType.BILL_20, 20);
        cashInventory.put(CashType.BILL_10, 30);
        cashInventory.put(CashType.BILL_5, 20);
        cashInventory.put(CashType.BILL_1, 50);
    }

    public int getTotalCash() {
        int total = 0;
        for (Map.Entry<CashType, Integer> entry : cashInventory.entrySet()) {
            total += entry.getKey().value * entry.getValue();
        }
        return total;
    }

    public boolean hasSufficientCash(int amount) {
        return getTotalCash() >= amount;
    }

    public Map<CashType, Integer> dispenseCash(int amount) {
        if (!hasSufficientCash(amount)) return null;

        Map<CashType, Integer> dispensedCash = new HashMap<>();
        int remainingAmount = amount;

        for (CashType cashType : CashType.values()) {
            int available = cashInventory.getOrDefault(cashType, 0);
            int count = Math.min(remainingAmount / cashType.value, available);
            if (count > 0) {
                dispensedCash.put(cashType, count);
                remainingAmount -= count * cashType.value;
                cashInventory.put(cashType, available - count);
            }
        }

        if (remainingAmount > 0) {
            for (Map.Entry<CashType, Integer> entry : dispensedCash.entrySet()) {
                cashInventory.put(entry.getKey(),
                        cashInventory.get(entry.getKey()) + entry.getValue());
            }
            return null;
        }
        return dispensedCash;
    }
}

/**
 * Enum to define transaction types supported by the ATM.
 * It helps in selecting and processing transactions like withdrawal or balance inquiry.
 *
 * ▶ Cross Questions:
 * Q1: Why use an enum instead of strings or integers?
 * A1: Enums provide type safety, are self-documenting, and prevent invalid transaction types.
 */
enum TransactionType {
    WITHDRAW_CASH, CHECK_BALANCE
}

/**
 * Interface representing the state of the ATM.
 * It enforces methods for state-specific behavior and state transitions.
 *
 * ▶ Cross Questions:
 * Q1: Why use the state pattern here instead of if-else logic?
 * A1: State pattern separates state-specific behavior, makes the code easier to extend and maintain.
 *
 * Q2: How does this design improve scalability?
 * A2: Adding new states only requires implementing the interface without changing existing code.
 */
interface ATMState {
    String getStateName();
    void handleState(ATMMachine context);
}

/**
 * IdleState represents the initial state of the ATM, prompting users to insert a card.
 * ▶ Cross Questions:
 * Q1: How would you manage transitions from idle to other states?
 * A1: By calling setState() from the context based on user actions.
 */
class IdleState implements ATMState {
    @Override
    public String getStateName() { return "IdleState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Please insert your card");
    }
}
/**
 * SelectOperationState represents the state where the ATM waits for the user to choose a transaction.
 * ▶ Cross Questions:
 * Q1: How would you design the UI integration for transaction selection?
 * A1: Use event-driven input or callback methods connected to UI elements.
 */

class HasCardState implements ATMState {
    @Override
    public String getStateName() { return "HasCardState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Please enter your PIN");
    }
}
/**
 * SelectOperationState represents the state where the ATM waits for the user to choose a transaction.
 * ▶ Cross Questions:
 * Q1: How would you design the UI integration for transaction selection?
 * A1: Use event-driven input or callback methods connected to UI elements.
 */
class SelectOperationState implements ATMState {
    @Override
    public String getStateName() { return "SelectOperationState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Select operation: 1. Withdraw 2. Check Balance");
    }
}
/**
 * TransactionState represents the state where the ATM processes the selected transaction.
 * ▶ Cross Questions:
 * Q1: How would you ensure transaction consistency in this state?
 * A1: Use locks, transactions, or rollback mechanisms in case of failure.
 */
class TransactionState implements ATMState {
    @Override
    public String getStateName() { return "TransactionState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Processing transaction...");
    }
}
/**
 * ATMMachine orchestrates operations by managing states, current account, and cash inventory.
 * It handles transitions between states and provides functionality like authentication, transaction processing, and card ejection.
 *
 * ▶ Cross Questions:
 * Q1: Why is the state pattern appropriate here?
 * A1: It cleanly manages state-specific behaviors without complicating the main class with conditional logic.
 *
 * Q2: How would you scale this ATM to handle concurrent users or sessions?
 * A2: Use session management, thread pools, or distributed architecture with separate services handling account and inventory.
 *
 * Q3: How would you integrate logging or monitoring in this design?
 * A3: Introduce decorators or interceptors in each state to log events and monitor transaction health.
 */
class ATMMachine {
    private ATMState currentState;
    private Card currentCard;
    private Account currentAccount;
    private ATMInventory inventory;
    private Map<String, Account> accounts;
    private TransactionType selectedOperation;

    public ATMMachine() {
        this.currentState = new IdleState();
        this.inventory = new ATMInventory();
        this.accounts = new HashMap<>();
        initializeAccounts();
    }

    private void initializeAccounts() {
        accounts.put("123456", new Account("123456", 1000.0));
        accounts.put("654321", new Account("654321", 500.0));
    }

    public void setState(ATMState state) {
        this.currentState = state;
        state.handleState(this);
    }

    public void insertCard(Card card) {
        if (currentState instanceof IdleState) {
            this.currentCard = card;
            setState(new HasCardState());
        } else {
            System.out.println("Invalid operation");
        }
    }

    public void authenticatePin(int pin) {
        if (currentState instanceof HasCardState) {
            if (currentCard.validatePin(pin)) {
                currentAccount = accounts.get(currentCard.getAccountNumber());
                setState(new SelectOperationState());
            } else {
                System.out.println("Invalid PIN");
                ejectCard();
            }
        }
    }

    public void selectOperation(TransactionType operation) {
        if (currentState instanceof SelectOperationState) {
            this.selectedOperation = operation;
            setState(new TransactionState());
            processTransaction(0);
        }
    }

    public void processTransaction(double amount) {
        if (currentState instanceof TransactionState) {
            switch (selectedOperation) {
                case WITHDRAW_CASH:
                    if (currentAccount.withdraw(amount)) {
                        Map<CashType, Integer> cash = inventory.dispenseCash((int) amount);
                        if (cash != null) {
                            System.out.println("Please take your cash");
                        } else {
                            System.out.println("Insufficient ATM cash");
                            currentAccount.deposit(amount);
                        }
                    } else {
                        System.out.println("Insufficient funds");
                    }
                    break;
                case CHECK_BALANCE:
                    System.out.println("Balance: $" + currentAccount.getBalance());
                    break;
            }
            ejectCard();
        }
    }

    private void ejectCard() {
        System.out.println("Please take your card");
        setState(new IdleState());
        currentCard = null;
        currentAccount = null;
    }
}
/**
 * Entry point for the ATM simulation.
 * Demonstrates card insertion, PIN authentication, and withdrawal operation.
 *
 * ▶ Cross Questions:
 * Q1: How would you expand this to handle UI input or network requests?
 * A1: Replace hardcoded input with user interfaces, REST APIs, or message queues.
 *
 * Q2: How would you test this system?
 * A2: Create unit tests for each state, transaction, and edge cases like invalid PINs and insufficient funds.
 */

public class ATMSystem {
    public static void main(String[] args) {
        ATMMachine atm = new ATMMachine();
        Card card = new Card("123456789", 1234, "123456");

        atm.insertCard(card);
        atm.authenticatePin(1234);
        atm.selectOperation(TransactionType.WITHDRAW_CASH);
    }
}
