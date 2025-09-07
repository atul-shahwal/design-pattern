package org.desingpatterns.questions.atmmachine;

import java.util.HashMap;
import java.util.Map;

// Enum representing cash denominations
enum CashType {
    BILL_100(100), BILL_50(50), BILL_20(20), BILL_10(10), BILL_5(5), BILL_1(1);

    public final int value;

    CashType(int value) {
        this.value = value;
    }
}

// Represents user's bank card
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

// Represents user's bank account
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

// Manages ATM's cash inventory
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

// Enum for transaction types
enum TransactionType {
    WITHDRAW_CASH, CHECK_BALANCE
}

// State interface
interface ATMState {
    String getStateName();
    void handleState(ATMMachine context);
}

// Concrete states
class IdleState implements ATMState {
    @Override
    public String getStateName() { return "IdleState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Please insert your card");
    }
}

class HasCardState implements ATMState {
    @Override
    public String getStateName() { return "HasCardState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Please enter your PIN");
    }
}

class SelectOperationState implements ATMState {
    @Override
    public String getStateName() { return "SelectOperationState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Select operation: 1. Withdraw 2. Check Balance");
    }
}

class TransactionState implements ATMState {
    @Override
    public String getStateName() { return "TransactionState"; }

    @Override
    public void handleState(ATMMachine context) {
        System.out.println("Processing transaction...");
    }
}

// Main ATM machine class
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

// Demo class
public class ATMSystem {
    public static void main(String[] args) {
        ATMMachine atm = new ATMMachine();
        Card card = new Card("123456789", 1234, "123456");

        atm.insertCard(card);
        atm.authenticatePin(1234);
        atm.selectOperation(TransactionType.WITHDRAW_CASH);
    }
}
